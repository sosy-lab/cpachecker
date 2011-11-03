/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.parser.eclipse;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.logging.Level;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTASMDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTProblem;
import org.eclipse.cdt.core.dom.ast.IASTProblemDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.StorageClass;
import org.sosy_lab.cpachecker.cfa.objectmodel.BlankEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;

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

  // Data structure for maintaining our scope stack in a function
  private final Deque<CFANode> locStack = new ArrayDeque<CFANode>();

  // Data structures for handling function declarations
  private final Map<String, CFAFunctionDefinitionNode> cfas = new HashMap<String, CFAFunctionDefinitionNode>();
  private final SortedSetMultimap<String, CFANode> cfaNodes = TreeMultimap.create();
  private CFAFunctionDefinitionNode currentCfa = null;
  private SortedSet<CFANode> currentCfaNodes = null;

  // Data structure for storing global declarations
  private final List<org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration> globalDeclarations = Lists.newArrayList();

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

    //shouldVisitComments = false;
    shouldVisitDeclarations = true;
    shouldVisitDeclarators = false;
    shouldVisitDeclSpecifiers = false;
    shouldVisitEnumerators = true;
    shouldVisitExpressions = false;
    shouldVisitInitializers = false;
    shouldVisitNames = false;
    shouldVisitParameterDeclarations = true;
    shouldVisitProblems = true;
    shouldVisitStatements = true;
    shouldVisitTranslationUnit = false;
    shouldVisitTypeIds = false;
  }

  /**
   * Retrieves list of all functions
   * @return all CFAs in the program
   */
  public Map<String, CFAFunctionDefinitionNode> getCFAs()  {
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
  public List<org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration> getGlobalDeclarations() {
    return globalDeclarations;
  }

  /* (non-Javadoc)
   * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#visit(org.eclipse.cdt.core.dom.ast.IASTDeclaration)
   */
  @Override
  public int visit(IASTDeclaration declaration) {
    IASTFileLocation fileloc = declaration.getFileLocation();

    if (declaration instanceof IASTSimpleDeclaration) {
      return handleSimpleDeclaration((IASTSimpleDeclaration)declaration, fileloc);

    } else if (declaration instanceof IASTFunctionDefinition) {
      declaration.accept(new CFAFunctionBuilder(logger, ignoreCasts, this,
          scope, astCreator));
      return PROCESS_SKIP;

    } else if (declaration instanceof IASTProblemDeclaration) {
      // CDT parser struggles on GCC's __attribute__((something)) constructs
      // because we use C99 as default.
      // Either insert the following macro before compiling with CIL:
      // #define  __attribute__(x)  /*NOTHING*/
      // or insert "parser.dialect = GNUC" into properties file
      visit(((IASTProblemDeclaration)declaration).getProblem());
      return PROCESS_SKIP;

    } else if (declaration instanceof IASTASMDeclaration) {
      // TODO Assembler code is ignored here
      return ignoreASMDeclaration(fileloc);

    } else {
      throw new CFAGenerationRuntimeException("Unknown declaration type " + declaration.getClass().getSimpleName(), declaration);
    }
  }

  private int handleSimpleDeclaration(final IASTSimpleDeclaration sd, final IASTFileLocation fileloc) {

    final List<org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration> newDs = astCreator.convert(sd);
    assert !newDs.isEmpty();

    for (org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration newD : newDs) {
      if (newD.getStorageClass() != StorageClass.TYPEDEF
          && newD.getName() != null) {
        // this is neither a typedef nor a struct prototype nor a function declaration,
        // so it's a variable declaration

        scope.registerDeclaration(newD);
      }
    }

    if (locStack.size() > 0) {// i.e. we're in a function

      CFANode prevNode = locStack.pop();
      CFANode nextNode = null;

      for (org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration newD : newDs) {
        assert !newD.isGlobal();

        nextNode = new CFANode(fileloc.getStartingLineNumber(), currentCfa.getFunctionName());
        currentCfaNodes.add(nextNode);

        final DeclarationEdge edge = new DeclarationEdge(newD,
            fileloc.getStartingLineNumber(), prevNode, nextNode);
        addToCFA(edge);

        prevNode = nextNode;
      }

      assert nextNode != null;
      locStack.push(nextNode);

    } else {
      assert (sd.getParent() instanceof IASTTranslationUnit) : "not a real global declaration";

      // else we're in the global scope
      globalDeclarations.addAll(newDs);
    }

    return PROCESS_SKIP; // important to skip here, otherwise we would visit nested declarations
  }

  void addFunctionCfa(String pNameOfFunction,
      final CFAFunctionDefinitionNode pStartNode,
      final IASTFunctionDefinition pDeclaration) {

    if (cfas.containsKey(pNameOfFunction)) {
      throw new CFAGenerationRuntimeException("Duplicate function "
          + pNameOfFunction, pDeclaration);
    }

    cfas.put(pNameOfFunction, pStartNode);
  }

  private int ignoreASMDeclaration(final IASTFileLocation fileloc) {
    // TODO Assembler code is ignored here
    logger.log(Level.WARNING, "Ignoring inline assembler code at line "
        + fileloc.getStartingLineNumber() + ", analysis is probably unsound!");

    // locStack may be empty here, which happens when there is assembler code
    // outside of a function
    if (!locStack.isEmpty()) {
      final CFANode prevNode = locStack.pop();

      final CFANode nextNode = new CFANode(fileloc.getStartingLineNumber(), currentCfa.getFunctionName());
      currentCfaNodes.add(nextNode);
      locStack.push(nextNode);

      final BlankEdge edge = new BlankEdge("Ignored inline assembler code",
          fileloc.getStartingLineNumber(), prevNode, nextNode);
      addToCFA(edge);
    }
    return PROCESS_SKIP;
  }

  @Override
  public int leave(IASTDeclaration declaration) {
    return PROCESS_CONTINUE;
  }

  // Methods for to handle visiting and leaving Statements
  /* (non-Javadoc)
   * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#visit(org.eclipse.cdt.core.dom.ast.IASTStatement)
   */
  @Override
  public int visit(IASTStatement statement) {
    throw new CFAGenerationRuntimeException("Statements shouldn't be seen by"
        + " global CFABuilder.", statement);
  }

  /* (non-Javadoc)
   * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#leave(org.eclipse.cdt.core.dom.ast.IASTStatement)
   */
  @Override
  public int leave(IASTStatement statement) {
    throw new CFAGenerationRuntimeException("Statements shouldn't be seen by"
        + " global CFABuilder.", statement);
  }

  //Method to handle visiting a parsing problem.  Hopefully none exist
  /* (non-Javadoc)
   * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#visit(org.eclipse.cdt.core.dom.ast.IASTProblem)
   */
  @Override
  public int visit(IASTProblem problem) {
    throw new CFAGenerationRuntimeException(problem.getMessage(), problem);
  }

  /**
   * This method adds this edge to the leaving and entering edges
   * of its predecessor and successor respectively, but it does so only
   * if the edge does not contain dead code
   */
  private void addToCFA(CFAEdge edge) {
    CFACreationUtils.addEdgeToCFA(edge, logger);
  }
}
