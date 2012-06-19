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
package org.sosy_lab.cpachecker.cfa.parser.eclipse;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTASMDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTProblem;
import org.eclipse.cdt.core.dom.ast.IASTProblemDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

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

  // Data structures for handling function declarations
  private Queue<IASTFunctionDefinition> functionDeclarations = new LinkedList<IASTFunctionDefinition>();
  private final Map<String, CFAFunctionDefinitionNode> cfas = new HashMap<String, CFAFunctionDefinitionNode>();
  private final SortedSetMultimap<String, CFANode> cfaNodes = TreeMultimap.create();

  // Data structure for storing global declarations
  private final List<Pair<org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration, String>> globalDeclarations = Lists.newArrayList();

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

    shouldVisitDeclarations = true;
    shouldVisitEnumerators = true;
    shouldVisitProblems = true;
    shouldVisitTranslationUnit = true;
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
  public List<Pair<org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration, String>> getGlobalDeclarations() {
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
      IASTFunctionDefinition fd = (IASTFunctionDefinition) declaration;
      functionDeclarations.add(fd);

      // add forward declaration to list of global declarations
      org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration functionDefinition = astCreator.convert(fd);
      if (astCreator.numberOfSideAssignments() > 0) {
        throw new CFAGenerationRuntimeException("Function definition has side effect", fd);
      }

      globalDeclarations.add(Pair.of(functionDefinition, fd.getDeclSpecifier().getRawSignature() + " " + fd.getDeclarator().getRawSignature()));

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
      logger.log(Level.WARNING, "Ignoring inline assembler code at line "
          + fileloc.getStartingLineNumber()
          + ", analysis is probably unsound!");
      return PROCESS_SKIP;

    } else {
      throw new CFAGenerationRuntimeException("Unknown declaration type "
          + declaration.getClass().getSimpleName(), declaration);
    }
  }

  private int handleSimpleDeclaration(final IASTSimpleDeclaration sd,
      final IASTFileLocation fileloc) {

    final List<org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration> newDs = astCreator.convert(sd);
    assert !newDs.isEmpty();

    if (astCreator.numberOfSideAssignments() > 0) {
      throw new CFAGenerationRuntimeException("Initializer of global variable has side effect", sd);
    }

    String rawSignature = sd.getRawSignature();

    for (org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration newD : newDs) {
      if (newD instanceof CVariableDeclaration) {
        scope.registerDeclaration(newD);
      } else if (newD instanceof CFunctionDeclaration) {
        scope.registerFunctionDeclaration((CFunctionDeclaration) newD);
      }

      globalDeclarations.add(Pair.of(newD, rawSignature));
    }

    return PROCESS_SKIP; // important to skip here, otherwise we would visit nested declarations
  }

  //Method to handle visiting a parsing problem.  Hopefully none exist
  /* (non-Javadoc)
   * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#visit(org.eclipse.cdt.core.dom.ast.IASTProblem)
   */
  @Override
  public int visit(IASTProblem problem) {
    throw new CFAGenerationRuntimeException(problem.getMessage(), problem);
  }

  @Override
  public int leave(IASTTranslationUnit translationUnit) {
    for (IASTFunctionDefinition declaration : functionDeclarations) {
      CFAFunctionBuilder functionBuilder = new CFAFunctionBuilder(logger, ignoreCasts,
          scope, astCreator);

      declaration.accept(functionBuilder);

      CFAFunctionDefinitionNode startNode = functionBuilder.getStartNode();
      String functionName = startNode.getFunctionName();

      if (cfas.containsKey(functionName)) {
        throw new CFAGenerationRuntimeException("Duplicate function " + functionName);
      }
      cfas.put(functionName, startNode);
      cfaNodes.putAll(functionName, functionBuilder.getCfaNodes());
    }
    return PROCESS_CONTINUE;
  }
}
