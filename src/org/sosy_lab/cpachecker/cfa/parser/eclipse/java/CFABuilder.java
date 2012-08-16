/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.IADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;

import com.google.common.collect.Lists;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;

/**
 * Builder to traverse AST. This Class has to be reusable for ASTs.
 * Additionally, it tracks variables over the traversed ASTs.
 *
 *
 * Known Limitations:
 */
class CFABuilder extends ASTVisitor {



  private static final boolean SKIP_CHILDREN = false;


  private static final boolean VISIT_CHILDREN = true;


  // Data structure for tracking method Declaration over ASTs
  // Used to resolve dynamic Bindings
  private final Map<String, MethodDeclaration> allParsedMethodDeclaration = new HashMap<String, MethodDeclaration>();


  // Data structures for handling method declarations
  private  Queue<MethodDeclaration> methodDeclarations = new LinkedList<MethodDeclaration>();
  private final Map<String, FunctionEntryNode> cfas = new HashMap<String, FunctionEntryNode>();
  private final SortedSetMultimap<String, CFANode> cfaNodes = TreeMultimap.create();

  // Data structure for storing static field declarations over this Ast
  private final List<Pair<IADeclaration, String>> nonStaticFieldDeclarationsOfThisClass = Lists.newArrayList();

  // Data structure for storing static and nonStatic field declarations over several AST
  private final List<Pair<IADeclaration, String>> staticFieldDeclarations = Lists.newArrayList();
  private final List<Pair<IADeclaration, String>> nonStaticFieldDeclarations = Lists.newArrayList();

  private final Scope scope ;
  private final ASTConverter astCreator;
  private final TypeHierachie typeHierachie;

  private final LogManager logger;
  private final boolean ignoreCasts;

  public CFABuilder(LogManager pLogger, boolean pIgnoreCasts , String fullyQualifiedNameOfMainClass, TypeHierachie pTypeHierachie) {
    logger = pLogger;
    ignoreCasts = pIgnoreCasts;
    typeHierachie = pTypeHierachie;

    if (pIgnoreCasts) {
      logger.log(Level.WARNING, "Ignoring all casts in the program because of user request!");
    }
    scope = new Scope(fullyQualifiedNameOfMainClass);
    astCreator = new ASTConverter(scope, pIgnoreCasts, logger , typeHierachie);

  }


  /**
   * Retrieves list of all functions
   * @return all CFAs in the program
   */
  public Map<String, FunctionEntryNode> getCFAs()  {
    return cfas;
  }

  /**
   * Retrieves list of all nodes
   * @return all CFAs in the program
   */
  public SortedSetMultimap<String, CFANode> getCFANodes()  {
    return cfaNodes;
  }

  /**
   * Retrieves list of all global declarations
   * @return global declarations
   */
  public List<Pair<IADeclaration, String>> getGlobalDeclarations() {
    return staticFieldDeclarations;
  }

  @Override
  public boolean visit(CompilationUnit cu) {

    // To make the builder reusable, past declarations have to be cleared.
    getMethodDeclarations().clear();
    nonStaticFieldDeclarationsOfThisClass.clear();
    return VISIT_CHILDREN;

  }

  /* (non-Javadoc)
   * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#visit(org.eclipse.cdt.core.dom.ast.IADeclaration)
   */
  @Override
  public boolean visit(MethodDeclaration fd) {


      getMethodDeclarations().add(fd);

      // add forward declaration to list of global declarations
      IADeclaration functionDefinition = getAstCreator().convert(fd);
      if (getAstCreator().numberOfSideAssignments() > 0) {
        throw new CFAGenerationRuntimeException("Function definition has side effect", fd);
      }

      staticFieldDeclarations.add(Pair.of(functionDefinition, "What is that" + " " + "What is that"));

      return SKIP_CHILDREN;

  }

  @Override
  public boolean visit(FieldDeclaration fd) {


      final List<IADeclaration> newDs = getAstCreator().convert(fd);
      assert !newDs.isEmpty();

      if (getAstCreator().numberOfPreSideAssignments() > 0) { throw new CFAGenerationRuntimeException(
          "Initializer of global variable has side effect", fd); }

      String rawSignature = fd.toString();

      // static field are declared when a class is loaded
      // non static fields are declared when an object is created
      for (IADeclaration newD : newDs) {
        if( ((JFieldDeclaration) newD).isStatic()){
          staticFieldDeclarations.add(Pair.of(newD, rawSignature));
        }else {
          nonStaticFieldDeclarations.add(Pair.of(newD , rawSignature));
          nonStaticFieldDeclarationsOfThisClass.add(Pair.of(newD, rawSignature));
        }
      }

    return SKIP_CHILDREN;
  }

  @Override
  public void endVisit(CompilationUnit translationUnit) {

    for (MethodDeclaration declaration : getMethodDeclarations()) {
      CFAFunctionBuilder functionBuilder = new CFAFunctionBuilder(logger, ignoreCasts,
          scope, getAstCreator(), nonStaticFieldDeclarationsOfThisClass , typeHierachie);

      declaration.accept(functionBuilder);

      FunctionEntryNode startNode = functionBuilder.getStartNode();
      String functionName = startNode.getFunctionName();

      if (cfas.containsKey(functionName)) {
        throw new CFAGenerationRuntimeException("Duplicate function " + functionName);
      }
      cfas.put(functionName, startNode);
      cfaNodes.putAll(functionName, functionBuilder.getCfaNodes());
      allParsedMethodDeclaration.put(functionName, declaration);
    }

  }


  public Scope getScope() {
    return scope;
  }


  public Queue<MethodDeclaration> getMethodDeclarations() {
    return methodDeclarations;
  }


  public Map<String ,MethodDeclaration> getAllParsedMethodDeclaration() {
    return allParsedMethodDeclaration;
  }


  public ASTConverter getAstCreator() {
    return astCreator;
  }

}