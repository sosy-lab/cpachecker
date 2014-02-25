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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.ast.IADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;

import com.google.common.collect.Lists;

/**
 * Builder to traverse AST and catch all CComplex Types which are declarated
 * in the global scope.
 */
class CatchAllGlobalTypesVisitor extends ASTVisitor {

  private final List<String> eliminateableDuplicates = new ArrayList<>();

  // Data structure for storing global declarations
  private final List<Pair<org.sosy_lab.cpachecker.cfa.ast.IADeclaration, String>> globalDeclarations = Lists.newArrayList();
  private final Sideassignments sideAssignmentStack;

  private final GlobalScope fileScope;
  private final ASTConverter astCreator;

  private final MachineModel machine;
  private final LogManager logger;

  public CatchAllGlobalTypesVisitor(Configuration pConfig, LogManager pLogger, MachineModel pMachine,
      String pStaticVariablePrefix, Sideassignments pSideAssignmentStack) throws InvalidConfigurationException {
    logger = pLogger;
    machine = pMachine;

    fileScope = new GlobalScope(new HashMap<String, CSimpleDeclaration>(),
                                new HashMap<String, CFunctionDeclaration>(),
                                new HashMap<String, CComplexTypeDeclaration>(),
                                new HashMap<String, CTypeDefDeclaration>(),
                                new HashSet<String>());
    astCreator = new ASTConverter(pConfig, fileScope, logger, machine, pStaticVariablePrefix, true, pSideAssignmentStack);

    shouldVisitDeclarations = true;
    shouldVisitEnumerators = true;
    shouldVisitTranslationUnit = true;
    sideAssignmentStack = pSideAssignmentStack;
  }

  public CComplexType lookupType(String qualifiedName) {
    return fileScope.lookupType(qualifiedName);
  }

  /* (non-Javadoc)
   * @see org.eclipse.cdt.core.dom.ast.ASTVisitor#visit(org.eclipse.cdt.core.dom.ast.IASTDeclaration)
   */
  @Override
  public int visit(IASTDeclaration declaration) {
    IASTFileLocation fileloc = declaration.getFileLocation();

    if (declaration instanceof IASTSimpleDeclaration) {
      return handleSimpleDeclaration((IASTSimpleDeclaration)declaration, fileloc);
    }
    return PROCESS_SKIP;
  }

  private int handleSimpleDeclaration(final IASTSimpleDeclaration sd,
      final IASTFileLocation fileloc) {

    //these are unneccesary semicolons which would cause an abort of CPAchecker
    if (sd.getDeclarators().length == 0  && sd.getDeclSpecifier() instanceof IASTSimpleDeclSpecifier) {
      return PROCESS_SKIP;
    }

    final List<CDeclaration> newDs = astCreator.convert(sd);
    assert !newDs.isEmpty();

    if (sideAssignmentStack.hasConditionalExpression()
        || sideAssignmentStack.hasPostSideAssignments()) {
      throw new CFAGenerationRuntimeException("Initializer of global variable has side effect", sd);
    }

    String rawSignature = sd.getRawSignature();

    for (CAstNode astNode : sideAssignmentStack.getAndResetPreSideAssignments()) {
      if (astNode instanceof CComplexTypeDeclaration) {
        // already registered
        globalDeclarations.add(Pair.of((IADeclaration)astNode, rawSignature));
      } else {
        throw new CFAGenerationRuntimeException("Initializer of global variable has side effect", sd);
      }
    }

    for (CDeclaration newD : newDs) {

      if (newD instanceof CComplexTypeDeclaration) {
        if (fileScope.registerTypeDeclaration((CComplexTypeDeclaration)newD)) {
          if (!eliminateableDuplicates.contains(newD.toASTString())) {
            globalDeclarations.add(Pair.of((IADeclaration)newD, rawSignature));
            eliminateableDuplicates.add(newD.toASTString());
          }
        }
      }
    }

    return PROCESS_SKIP; // important to skip here, otherwise we would visit nested declarations
  }

}