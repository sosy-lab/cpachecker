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
 * Builder to traverse AST.
 * Known Limitations:
 * <p> -- K&R style function definitions not implemented
 * <p> -- Pointer modifiers not tracked (i.e. const, volatile, etc. for *
 */
class CFABuilder extends ASTVisitor {



  private static final boolean SKIP_CHILDREN = false;


  private static final boolean VISIT_CHILDREEN = true;


  // Data structures for handling function declarations
  private Queue<MethodDeclaration> functionDeclarations = new LinkedList<MethodDeclaration>();
  private final Map<String, FunctionEntryNode> cfas = new HashMap<String, FunctionEntryNode>();
  private final SortedSetMultimap<String, CFANode> cfaNodes = TreeMultimap.create();

  // Data structure for storing static and nonStatic field declarations
  private final List<Pair<IADeclaration, String>> staticFieldDeclarations = Lists.newArrayList();
  private final List<Pair<IADeclaration, String>> nonStaticFieldDeclarations = Lists.newArrayList();



  private final Scope scope ;
  private final ASTConverter astCreator;

  private final LogManager logger;
  private final boolean ignoreCasts;

  public CFABuilder(LogManager pLogger, boolean pIgnoreCasts , String fullyQualifiedNameOfMainClass) {
    logger = pLogger;
    ignoreCasts = pIgnoreCasts;

    if (pIgnoreCasts) {
      logger.log(Level.WARNING, "Ignoring all casts in the program because of user request!");
    }
    scope = new Scope(fullyQualifiedNameOfMainClass);
    astCreator = new ASTConverter(scope, pIgnoreCasts, logger);

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
    functionDeclarations.clear();

    return VISIT_CHILDREEN;

  }


  /* (non-Javadoc)
   * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#visit(org.eclipse.cdt.core.dom.ast.IADeclaration)
   */
  @Override
  public boolean visit(MethodDeclaration fd) {


      functionDeclarations.add(fd);

      // add forward declaration to list of global declarations
      IADeclaration functionDefinition = astCreator.convert(fd);
      if (astCreator.numberOfSideAssignments() > 0) {
        throw new CFAGenerationRuntimeException("Function definition has side effect", fd);
      }

      staticFieldDeclarations.add(Pair.of(functionDefinition, "What is that" + " " + "What is that"));

      return SKIP_CHILDREN;

  }

  @Override
  public boolean visit(FieldDeclaration fd) {


      final List<IADeclaration> newDs = astCreator.convert(fd);
      assert !newDs.isEmpty();

      if (astCreator.numberOfPreSideAssignments() > 0) { throw new CFAGenerationRuntimeException(
          "Initializer of global variable has side effect", fd); }

      String rawSignature = fd.toString();

      // static field are declared when a class is loaded
      // non static fields are declared when an object is created
      for (IADeclaration newD : newDs) {
        if( ((JFieldDeclaration) newD).isStatic()){
          staticFieldDeclarations.add(Pair.of(newD, rawSignature));
        }else {
          nonStaticFieldDeclarations.add(Pair.of(newD , rawSignature));
        }
      }

    return SKIP_CHILDREN;
  }

  @Override
  public void endVisit(CompilationUnit translationUnit) {
    for (MethodDeclaration declaration : functionDeclarations) {
      CFAFunctionBuilder functionBuilder = new CFAFunctionBuilder(logger, ignoreCasts,
          scope, astCreator, nonStaticFieldDeclarations);

      declaration.accept(functionBuilder);

      FunctionEntryNode startNode = functionBuilder.getStartNode();
      String functionName = startNode.getFunctionName();

      if (cfas.containsKey(functionName)) {
        throw new CFAGenerationRuntimeException("Duplicate function " + functionName);
      }
      cfas.put(functionName, startNode);
      cfaNodes.putAll(functionName, functionBuilder.getCfaNodes());
    }
  }


  public Scope getScope() {
    return scope;
  }

}