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
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IADeclaration;
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
  // Data structures for handling function declarations
  private Queue<MethodDeclaration> functionDeclarations = new LinkedList<MethodDeclaration>();
  private final Map<String, FunctionEntryNode> cfas = new HashMap<String, FunctionEntryNode>();
  private final SortedSetMultimap<String, CFANode> cfaNodes = TreeMultimap.create();

  // Data structure for storing global declarations
  private final List<Pair<IADeclaration, String>> globalDeclarations = Lists.newArrayList();

  private final Scope scope = new Scope();
  private final ASTConverter astCreator;

  private final LogManager logger;
  private final boolean ignoreCasts;

  public CFABuilder(LogManager pLogger, boolean pIgnoreCasts) {
    logger = pLogger;
    ignoreCasts = pIgnoreCasts;
    astCreator = new ASTConverter(scope, pIgnoreCasts, logger);

    if (pIgnoreCasts) {
      logger.log(Level.WARNING, "Ignoring all casts in the program because of user request!");
    }

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
    return globalDeclarations;
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

      globalDeclarations.add(Pair.of(functionDefinition, "What is that" + " " + "What is that"));

      return SKIP_CHILDREN;

  }

  @Override
  public boolean visit(FieldDeclaration fd) {




    final List<IADeclaration> newDs = astCreator.convert(fd);
    assert !newDs.isEmpty();

    if (astCreator.numberOfPreSideAssignments() > 0) {
      throw new CFAGenerationRuntimeException("Initializer of global variable has side effect", fd);
    }

    String rawSignature = fd.toString();

    for (IADeclaration newD : newDs) {
      if (newD instanceof AVariableDeclaration) {
        scope.registerDeclaration(newD);
      } else if (newD instanceof AFunctionDeclaration) {
        scope.registerFunctionDeclaration((AFunctionDeclaration) newD);
      }

      globalDeclarations.add(Pair.of(newD, rawSignature));
    }

    return SKIP_CHILDREN;

}

  @Override
  public void endVisit(CompilationUnit translationUnit) {
    for (MethodDeclaration declaration : functionDeclarations) {
      CFAFunctionBuilder functionBuilder = new CFAFunctionBuilder(logger, ignoreCasts,
          scope, astCreator);

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
}