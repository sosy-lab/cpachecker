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
package org.sosy_lab.cpachecker.cfa.transformers.forPredicateAnalysisWithUF;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

public class CFATransformer extends DefaultCFAVisitor {

  private CFAEdge transformEdge(CFAEdge oldEdge) {

    CFAEdge edge = null;

    expressionVisitor.setCurrentEdge(oldEdge);
    try {
      switch (oldEdge.getEdgeType()) {
      case AssumeEdge: {
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
      }
      case BlankEdge: {
        break;
      }
      case DeclarationEdge: {
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
      }
      case FunctionCallEdge: {
        final CFunctionCallEdge functionCallEdge = (CFunctionCallEdge) oldEdge;
        List<CExpression> arguments = null;
        int i = 0;
        for (CExpression oldArgument : functionCallEdge.getArguments()) {
          final CExpression argument = (CExpression) oldArgument.accept(expressionVisitor);
          if (argument != oldArgument && arguments == null) {
            arguments = new ArrayList<>();
            arguments.addAll(functionCallEdge.getArguments().subList(0, i));
          }
          if (arguments != null) {
            arguments.add(argument);
          }
          ++i;
        }
        if (arguments != null) {
          final CFunctionCall oldFunctionCall = functionCallEdge.getSummaryEdge().getExpression();
          final CFunctionCallExpression oldFunctionCallExpression = oldFunctionCall.getFunctionCallExpression();
          final CFunctionCallExpression functionCallExpression = new CFunctionCallExpression(
                                                                       oldFunctionCallExpression.getFileLocation(),
                                                                       oldFunctionCallExpression.getExpressionType(),
                                                                       oldFunctionCallExpression
                                                                         .getFunctionNameExpression(),
                                                                       arguments,
                                                                       oldFunctionCallExpression.getDeclaration());
          final CFunctionCall functionCall;
          if (oldFunctionCall instanceof CFunctionCallStatement) {
            functionCall = new CFunctionCallStatement(((CFunctionCallStatement) oldFunctionCall).getFileLocation(),
                                                      functionCallExpression);
          } else /*oldFunctionCall instanceof CFunctionCallAssignmentStatement*/ {
            functionCall = new CFunctionCallAssignmentStatement(((CFunctionCallAssignmentStatement) oldFunctionCall)
                                                                 .getFileLocation(),
                                                                 ((CFunctionCallAssignmentStatement) oldFunctionCall)
                                                                 .getLeftHandSide(),
                                                                  functionCallExpression);
          }
          edge = new CFunctionCallEdge(functionCallEdge.getRawStatement(),
                                       functionCallEdge.getLineNumber(),
                                       functionCallEdge.getPredecessor(),
                                       functionCallEdge.getSuccessor(),
                                       functionCall,
                                       functionCallEdge.getSummaryEdge());
        }
        break;
      }
      case FunctionReturnEdge: {
        break;
      }
      case ReturnStatementEdge: {
        final CReturnStatementEdge returnStatementEdge = (CReturnStatementEdge) oldEdge;
        if (returnStatementEdge.getRawAST().isPresent() && /* That's because getExpression() assumes rawAST != null */
            returnStatementEdge.getExpression() != null) {
          final CExpression oldExpression = returnStatementEdge.getExpression();
          final CExpression expression = (CExpression) oldExpression.accept(expressionVisitor);
          if (expression != oldExpression) {
            edge = new CReturnStatementEdge(returnStatementEdge.getRawStatement(),
                                            new CReturnStatement(returnStatementEdge.getRawAST()
                                                                                    .get()
                                                                                    .getFileLocation(),
                                                                 expression),
                                            returnStatementEdge.getLineNumber(),
                                            returnStatementEdge.getPredecessor(),
                                            returnStatementEdge.getSuccessor());
          }
        }
        break;
      }
      case StatementEdge: {
        final CStatementEdge statementEdge = (CStatementEdge) oldEdge;
        final CStatement oldStatement = statementEdge.getStatement();
        final CStatement statement = oldStatement.accept(statementVisitor);
        if (!(oldEdge instanceof CFunctionSummaryStatementEdge)) {
          if (statement != oldStatement) {
            edge = new CStatementEdge(statementEdge.getRawStatement(),
                                      statement,
                                      statementEdge.getLineNumber(),
                                      statementEdge.getPredecessor(),
                                      statementEdge.getSuccessor());
          }
        } else /*oldEdge instanceof CFunctionSummaryStatementEdge*/ {
          final CFunctionSummaryStatementEdge summaryStatementEdge = (CFunctionSummaryStatementEdge) statementEdge;
          final CFunctionCallExpression oldCallExpression = summaryStatementEdge.getFunctionCall().getFunctionCallExpression();
          final CFunctionCallExpression callExpression = (CFunctionCallExpression) oldCallExpression
                                                                                       .accept(rhsVisitor);
          if (statement != oldStatement || callExpression != oldCallExpression) {
            final CFunctionCall oldFunctionCall = summaryStatementEdge.getFunctionCall();
            final CFunctionCall functionCall;
            if (oldFunctionCall instanceof CFunctionCallStatement) {
              functionCall = new CFunctionCallStatement(((CFunctionCallStatement) oldFunctionCall).getFileLocation(),
                                                        callExpression);
            } else /*oldFunctionCall instanceof CFunctionCallAssignmentStatement*/ {
              functionCall = new CFunctionCallAssignmentStatement(((CFunctionCallAssignmentStatement) oldFunctionCall)
                                                                   .getFileLocation(),
                                                                   ((CFunctionCallAssignmentStatement) oldFunctionCall)
                                                                   .getLeftHandSide(),
                                                                    callExpression);
            }
            edge = new CFunctionSummaryStatementEdge(summaryStatementEdge.getRawStatement(),
                                                     statement,
                                                     summaryStatementEdge.getLineNumber(),
                                                     summaryStatementEdge.getPredecessor(),
                                                     summaryStatementEdge.getSuccessor(),
                                                     functionCall,
                                                     summaryStatementEdge.getFunctionName());
          }
        }
      }
      }
    } catch (UnrecognizedCCodeException e) {
      logger.log(Level.WARNING, "UnrecognizedCCodeException while pre-processing edge", new Object[]{edge, e});
    }

    if (edge != null) {
      return edge;
    } else {
      return oldEdge;
    }
  }

  @Override
  public TraversalProcess visitEdge(final CFAEdge oldEdge) {
    final CFANode predecessor = oldEdge.getPredecessor();
    final CFANode successor = oldEdge.getSuccessor();

    CFAEdge edge = null;
    if (oldEdge instanceof MultiEdge) {
      List<CFAEdge> edges = null;
      int i = 0;
      for (CFAEdge oldSubEdge : ((MultiEdge) oldEdge).getEdges()) {
        final CFAEdge subEdge = transformEdge(oldSubEdge);
        if (edges == null && subEdge != oldSubEdge) {
          edges = new ArrayList<>();
          edges.addAll(((MultiEdge) oldEdge).getEdges().subList(0, i));
        }
        if (edges != null) {
          edges.add(subEdge);
        }
        ++i;
      }
      if (edges != null) {
        edge = new MultiEdge(predecessor, successor, edges);
      }
    } else {
      final CFAEdge newEdge = transformEdge(oldEdge);
      if (newEdge != oldEdge) {
        edge = newEdge;
      }
    }

    if (edge != null) {
      predecessor.removeLeavingEdge(oldEdge);
      successor.removeEnteringEdge(oldEdge);

      predecessor.addLeavingEdge(edge);
      successor.addEnteringEdge(edge);
    }

    return TraversalProcess.CONTINUE;
  }

  private CFATransformer(final LogManager logger) {
    this.logger = logger;
  }

  public static void transformCFA(MutableCFA cfa, final LogManager logger) {
    CFATraversal.dfs().ignoreSummaryEdges().traverseOnce(cfa.getMainFunction(), new CFATransformer(logger));
  }

  private CExpressionTransformer expressionVisitor = new CExpressionTransformer();
  private CStatementTransformer statementVisitor = new CStatementTransformer(expressionVisitor);
  private CRightHandSideTransformer rhsVisitor = new CRightHandSideTransformer(expressionVisitor);
  private final LogManager logger;
}
