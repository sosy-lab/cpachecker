// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.java;

import com.google.common.collect.TreeMultimap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.Pair;

/**
 * Builder to traverse AST. This Class has to be reusable for more ASTs.
 */
class CFABuilder extends ASTVisitor {

  private static final boolean SKIP_CHILDREN = false;
  private static final boolean VISIT_CHILDREN = true;

  // Data structure for tracking method Declaration over ASTs
  // Used to resolve dynamic Bindings
  private final Map<String, JMethodDeclaration> allParsedMethodDeclaration = new HashMap<>();

  // Data structures for handling method declarations
  // private Queue<MethodDeclaration> methodDeclarations = new LinkedList<>();
  private final NavigableMap<String, FunctionEntryNode> cfas = new TreeMap<>();
  private final TreeMultimap<String, CFANode> cfaNodes = TreeMultimap.create();

  private final Scope scope;
  private final ASTConverter astCreator;

  private final LogManager logger;

  public CFABuilder(LogManager pLogger, Scope pScope) {
    logger = pLogger;
    scope = pScope;
    astCreator = new ASTConverter(scope, logger);
  }

  /**
   * Retrieves list of all methods and constructors of program
   *
   * @return all CFAs in the program
   */
  public NavigableMap<String, FunctionEntryNode> getCFAs() {
    return cfas;
  }

  /**
   * Retrieves list of all nodes
   *
   * @return all CFAs in the program
   */
  public TreeMultimap<String, CFANode> getCFANodes() {
    return cfaNodes;
  }

  /**
   * Retrieves list of all static field declarations
   * @return global declarations
   */
  public List<Pair<ADeclaration, String>> getStaticFieldDeclarations() {

    Map<String, JFieldDeclaration> staticFieldDeclarations
                                  = scope.getStaticFieldDeclarations();

    List<Pair<ADeclaration, String>> result = new ArrayList<> (staticFieldDeclarations.size());

    for (Entry<String, JFieldDeclaration> entry : staticFieldDeclarations.entrySet()) {
      ADeclaration declaration = entry.getValue();
      result.add(Pair.of(declaration, entry.getKey()));
    }

    return result;
  }

  @Override
  public boolean visit(AnonymousClassDeclaration pClassDeclaration) {
    ITypeBinding classBinding = pClassDeclaration.resolveBinding();

    if (classBinding == null) {
      logger.logf(Level.WARNING,
          "Binding for anonymous class %s can't be resolved. Skipping class body.",
          pClassDeclaration.toString());

      return SKIP_CHILDREN;
    }

    scope.enterClass(astCreator.convertClassOrInterfaceType(pClassDeclaration.resolveBinding()));

    return super.visit(pClassDeclaration);
  }

  @Override
  public void endVisit(AnonymousClassDeclaration pClassDeclaration) {

    // Create possible constructors of anonymous class. This is either a default constructor
    // or all constructors inherited of a direct super class.
    createConstructors(pClassDeclaration);

    scope.leaveClass();
  }

  private void createConstructors(AnonymousClassDeclaration pClassDeclaration) {
    CFAMethodBuilder methodBuilder = new CFAMethodBuilder(logger, scope, astCreator);

    methodBuilder.createConstructors(pClassDeclaration);

    addMethodToCfas(methodBuilder.getStartNode(), methodBuilder.getCfaNodes());
  }

  @Override
  public boolean visit(TypeDeclaration typeDec) {

    ITypeBinding classBinding = typeDec.resolveBinding();

    if (!typeDec.isPackageMemberTypeDeclaration()) {
      // inner classes not implemented

      ASTConverter.ModifierBean mB =
          ASTConverter.ModifierBean.getModifiers(typeDec.resolveBinding());

      if (mB.isStatic() || typeDec.isInterface()) {

        scope.enterClass(astCreator.convertClassOrInterfaceType(classBinding));

        return VISIT_CHILDREN;
      }

      return SKIP_CHILDREN;
    }

    // enter Top Level Class Scope
    scope.enterClass(astCreator.convertClassOrInterfaceType(classBinding));

    return VISIT_CHILDREN;
  }

  @Override
  public boolean visit(MethodDeclaration md) {
    //methodDeclarations.add(fd);

    // parse Method
    CFAMethodBuilder methodBuilder = new CFAMethodBuilder(logger, scope, astCreator);

    md.accept(methodBuilder);

    FunctionEntryNode startNode = methodBuilder.getStartNode();
    Set<CFANode> allMethodNodes = methodBuilder.getCfaNodes();

    addMethodToCfas(startNode, allMethodNodes);

    return SKIP_CHILDREN;
  }

  private void addMethodToCfas(FunctionEntryNode pStartNode, Set<CFANode> pMethodNodes) {
    String methodName = pStartNode.getFunctionName();

    if (cfas.containsKey(methodName)) {
      throw new CFAGenerationRuntimeException("Duplicate method " + methodName);
    }

    cfas.put(methodName, pStartNode);
    cfaNodes.putAll(methodName, pMethodNodes);
    allParsedMethodDeclaration.put(methodName, (JMethodDeclaration) pStartNode.getFunctionDefinition());
  }

  @Override
  public boolean visit(FieldDeclaration fd) {

    final List<JDeclaration> newDs = astCreator.convert(fd);
    assert !newDs.isEmpty();

    // In Java, initializer of field Variable may be Object Creation, which means Side Assignments
    //
    if (astCreator.numberOfPreSideAssignments() > 0) {
      throw new CFAGenerationRuntimeException(
        "Initializer of field variable has side effect.", fd); }

    return SKIP_CHILDREN;
  }

  @Override
  public void endVisit(TypeDeclaration typeDef) {

    if (!typeDef.isPackageMemberTypeDeclaration()) {
      // inner classes not implemented
      ASTConverter.ModifierBean mB =
          ASTConverter.ModifierBean.getModifiers(typeDef.resolveBinding());

      if (!(mB.isStatic() || typeDef.isInterface())) {
        return;
      }
    }

    ITypeBinding classBinding = typeDef.resolveBinding();

    boolean hasDefaultConstructor = hasDefaultConstructor(classBinding);

    // If a class declaration has no constructor, create a standard constructor

    if (hasDefaultConstructor) {

      CFAMethodBuilder methodBuilder = new CFAMethodBuilder(logger,
          scope, astCreator);

      methodBuilder.createDefaultConstructor(classBinding);

      FunctionEntryNode startNode = methodBuilder.getStartNode();
      String methodName = startNode.getFunctionName();

      if (cfas.containsKey(methodName)) {
        throw new CFAGenerationRuntimeException("Duplicate default Constructor "
          + methodName);
      }

      cfas.put(methodName, startNode);
      cfaNodes.putAll(methodName, methodBuilder.getCfaNodes());
      allParsedMethodDeclaration.put(methodName, null);
    }

    scope.leaveClass(); // leave Top Level Scope
  }

  private boolean hasDefaultConstructor(ITypeBinding classBinding) {

    if (classBinding.isInterface()) {
      // Interfaces don't have Constructors
      return false;
    }

    IMethodBinding[] declaredMethods = classBinding.getDeclaredMethods();

    for (IMethodBinding declaredMethod : declaredMethods) {
      if (declaredMethod.isDefaultConstructor()) {
        return true;
      } else if (declaredMethod.isConstructor()) {
        return false;
      }
    }

    // If Class has no Constructors,
    // it has an implicit standard Constructor
    return true;
  }

  public Scope getScope() {
    return scope;
  }

  public Map<String, JMethodDeclaration> getAllParsedMethodDeclaration() {
    return allParsedMethodDeclaration;
  }

  public ASTConverter getAstCreator() {
    return astCreator;
  }

  @Override
  public void preVisit(ASTNode problem) {
    if (ASTNode.RECOVERED == (problem.getFlags() & ASTNode.RECOVERED)
        || ASTNode.MALFORMED == (problem.getFlags() & ASTNode.MALFORMED)) {
      throw new CFAGenerationRuntimeException("Syntax error." , problem);
    }
  }
}
