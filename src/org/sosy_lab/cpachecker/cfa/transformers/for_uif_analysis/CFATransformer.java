/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.transformers.for_uif_analysis;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatementVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

public class CFATransformer extends DefaultCFAVisitor {

  @Override
  public TraversalProcess visitEdge(final CFAEdge oldEdge) {
    final CFANode predecessor = oldEdge.getPredecessor();
    final CFANode successor = oldEdge.getSuccessor();

    CFAEdge edge = null;
    expressionVisitor.setCurrentEdge(oldEdge);
    try {
      switch (oldEdge.getEdgeType()) {
      case AssumeEdge:
        final CAssumeEdge assumeEdge = (CAssumeEdge) oldEdge;
        final CExpression oldExpression = assumeEdge.getExpression();
        final CExpression expression = (CExpression) oldExpression.accept(expressionVisitor);
        if (expression != oldExpression) {
          edge = new CAssumeEdge(assumeEdge.getRawStatement(),
                                 assumeEdge.getLineNumber(),
                                 assumeEdge.getPredecessor(),
                                 assumeEdge.getSuccessor(),
                                 expression,
                                 assumeEdge.getTruthAssumption());
        }
        break;
      case BlankEdge:
        break;
      case DeclarationEdge:
        final CDeclarationEdge declarationEdge = (CDeclarationEdge) oldEdge;
        final CDeclaration declaration = declarationEdge.getDeclaration();
        if (declaration instanceof CVariableDeclaration) {
          final CVariableDeclaration variableDeclaration = (CVariableDeclaration) declaration;
          if (variableDeclaration.getInitializer() != null) {
            final CInitializer oldInitializer = variableDeclaration.getInitializer();
            final CInitializer initializer = oldInitializer.accept(expressionVisitor.getInitializerTransformer());
            if (initializer != oldInitializer) {
              edge = new CDeclarationEdge(declarationEdge.getRawStatement(),
                                          declarationEdge.getLineNumber(),
                                          declarationEdge.getPredecessor(),
                                          declarationEdge.getSuccessor(),
                                          new CVariableDeclaration(variableDeclaration.getFileLocation(),
                                                                   variableDeclaration.isGlobal(),
                                                                   variableDeclaration.getCStorageClass(),
                                                                   variableDeclaration.getType(),
                                                                   variableDeclaration.getName(),
                                                                   variableDeclaration.getOrigName(),
                                                                   variableDeclaration.getQualifiedName(),
                                                                   initializer));
            }
          }
        }
        break;
      case FunctionCallEdge:
        final CFunctionCallEdge functionCallEdge = (CFunctionCallEdge) oldEdge;

        break;
      }
    } catch (UnrecognizedCCodeException e) {
      logger.log(Level.WARNING, "UnrecognizedCCodeException while pre-processing edge", new Object[]{edge, e});
    }

    return TraversalProcess.CONTINUE;
  }

  public CFATransformer(final Logger logger) {
    this.logger = logger;
  }

  private CExpressionTransformer expressionVisitor = new CExpressionTransformer();
  private CStatementVisitor<CStatement, UnrecognizedCCodeException> statementVisitor =
          new CStatementTransformer(expressionVisitor);
  private final Logger logger;
}
