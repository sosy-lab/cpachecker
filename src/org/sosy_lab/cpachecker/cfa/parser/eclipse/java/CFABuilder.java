/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa.parser.eclipse.java;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.IADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;

import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;

/**
 * Builder to traverse AST. This Class has to be reusable for more ASTs.
 */
class CFABuilder extends ASTVisitor {

  private static final boolean SKIP_CHILDREN = false;
  private static final boolean VISIT_CHILDREN = true;

  // Data structure for tracking method Declaration over ASTs
  // Used to resolve dynamic Bindings
  private final Map<String, MethodDeclaration> allParsedMethodDeclaration = new HashMap<>();

  // Data structures for handling method declarations
  // private Queue<MethodDeclaration> methodDeclarations = new LinkedList<>();
  private final SortedMap<String, FunctionEntryNode> cfas = new TreeMap<>();
  private final SortedSetMultimap<String, CFANode> cfaNodes = TreeMultimap.create();

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
   * @return all CFAs in the program
   */
  public SortedMap<String, FunctionEntryNode> getCFAs() {
    return cfas;
  }

  /**
   * Retrieves list of all nodes
   * @return all CFAs in the program
   */
  public SortedSetMultimap<String, CFANode> getCFANodes() {
    return cfaNodes;
  }

  /**
   * Retrieves list of all static field declarations
   * @return global declarations
   */
  public List<Pair<IADeclaration, String>> getStaticFieldDeclarations() {

    Map<String, JFieldDeclaration> staticFieldDeclarations
                                  = scope.getStaticFieldDeclarations();

    List<Pair<IADeclaration, String>> result = new ArrayList<> (staticFieldDeclarations.size());

    for (String declName : staticFieldDeclarations.keySet()) {
      IADeclaration declaration = staticFieldDeclarations.get(declName);
      result.add(Pair.of(declaration, declName));
    }

    return result;
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

  /* (non-Javadoc)
   *
   */
  @Override
  public boolean visit(MethodDeclaration md) {
    //methodDeclarations.add(fd);

    // parse Method
    CFAMethodBuilder methodBuilder = new CFAMethodBuilder(logger,
        scope, astCreator);

    md.accept(methodBuilder);

    FunctionEntryNode startNode = methodBuilder.getStartNode();
    String methodName = startNode.getFunctionName();

    if (cfas.containsKey(methodName)) {
      throw new CFAGenerationRuntimeException("Duplicate method "
        + methodName);
    }


    cfas.put(methodName, startNode);
    cfaNodes.putAll(methodName, methodBuilder.getCfaNodes());
    allParsedMethodDeclaration.put(methodName, md);


    return SKIP_CHILDREN;
  }

  @Override
  public boolean visit(FieldDeclaration fd) {

    final List<JDeclaration> newDs = astCreator.convert(fd);
    assert !newDs.isEmpty();

    // In Java, initializer of field Variable may be Object Creation, which means Side Assignments
    //
    if (astCreator.numberOfPreSideAssignments() > 0) {
      throw new CFAGenerationRuntimeException(
        "Initializer of field variable has side effect", fd); }

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

  public Map<String, MethodDeclaration> getAllParsedMethodDeclaration() {
    return allParsedMethodDeclaration;
  }

  public ASTConverter getAstCreator() {
    return astCreator;
  }

  @Override
  public void preVisit(ASTNode problem) {
    if (ASTNode.RECOVERED == (problem.getFlags() & ASTNode.RECOVERED)
        || ASTNode.MALFORMED == (problem.getFlags() & ASTNode.MALFORMED)) {
      throw new CFAGenerationRuntimeException("Syntaxerror in " + problem.toString() +"\n", problem);
    }
  }
}