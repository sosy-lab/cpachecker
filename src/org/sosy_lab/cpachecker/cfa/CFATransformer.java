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
package org.sosy_lab.cpachecker.cfa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.transformers.forPredicateAnalysisWithUF.CExpressionTransformer;
import org.sosy_lab.cpachecker.cfa.transformers.forPredicateAnalysisWithUF.CRightHandSideTransformer;
import org.sosy_lab.cpachecker.cfa.transformers.forPredicateAnalysisWithUF.CStatementTransformer;
import org.sosy_lab.cpachecker.cfa.transformers.forPredicateAnalysisWithUF.CachingCTypeTransformer;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

public class CFATransformer extends DefaultCFAVisitor {

  private CFunctionDeclaration transformFunctionDeclaration(final CFunctionDeclaration functionDeclaration) {
    final CFunctionType oldFunctionType = functionDeclaration.getType();
    final CFunctionType functionType = (CFunctionType) oldFunctionType.accept(typeVisitor);

    List<CParameterDeclaration> parameterDeclarations = null;
    int i = 0;
    for (CParameterDeclaration oldDeclaration : functionDeclaration.getParameters()) {
      final CType oldDeclarationType = oldDeclaration.getType();
      final CType declarationType = oldDeclarationType.accept(typeVisitor);

      if (declarationType != oldDeclarationType && parameterDeclarations == null) {
        parameterDeclarations = new ArrayList<>();
        parameterDeclarations.addAll(functionDeclaration.getParameters().subList(0, i));
      }
      if (parameterDeclarations != null) {
        if (declarationType != oldDeclarationType) {
          final CParameterDeclaration declaration = new CParameterDeclaration(oldDeclaration.getFileLocation(),
                                                                              declarationType,
                                                                              oldDeclaration.getName());
          declaration.setQualifiedName(oldDeclaration.getQualifiedName());
          parameterDeclarations.add(declaration);
        } else {
          parameterDeclarations.add(oldDeclaration);
        }
      }
      ++i;
    }

    if (functionType != oldFunctionType || parameterDeclarations != null) {
     return new CFunctionDeclaration(functionDeclaration.getFileLocation(),
                                     functionType,
                                     functionDeclaration.getName(),
                                     parameterDeclarations != null ?
                                       parameterDeclarations :
                                       functionDeclaration.getParameters());
    } else {
      return functionDeclaration;
    }
  }

  private CFAEdge transformSimpleEdge(final CFAEdge oldEdge) {

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
          final CVariableDeclaration oldVariableDeclaration = (CVariableDeclaration) declaration;
          final CInitializer oldInitializer = oldVariableDeclaration.getInitializer();
          final CInitializer initializer = oldInitializer == null ? null :
            (CInitializer) oldInitializer.accept(expressionVisitor.getInitializerTransformer());
          final CType oldVariableType = oldVariableDeclaration.getType();
          if (initializer instanceof CInitializerList) {
            typeVisitor.setInitializerSize(((CInitializerList) initializer).getInitializers().size(),
                                           initializer.getFileLocation());
          } else if (initializer instanceof CInitializerExpression &&
                     ((CInitializerExpression) initializer).getExpression() instanceof CStringLiteralExpression) {
            typeVisitor.setInitializerSize(
                ((CStringLiteralExpression) ((CInitializerExpression) initializer).getExpression())
                  .getContentString()
                  .length() + 1,
                initializer.getFileLocation());
          }
          final CType variableType = oldVariableType.accept(typeVisitor);
          if (initializer != oldInitializer || variableType != oldVariableType) {
            final CVariableDeclaration variableDeclaration =
                new CVariableDeclaration(oldVariableDeclaration.getFileLocation(),
                                         oldVariableDeclaration.isGlobal(),
                                         oldVariableDeclaration.getCStorageClass(),
                                         variableType,
                                         oldVariableDeclaration.getName(),
                                         oldVariableDeclaration.getOrigName(),
                                         oldVariableDeclaration.getQualifiedName(),
                                         initializer);
            declarationCache.put(variableDeclaration.getQualifiedName(), variableDeclaration);
            edge = new CDeclarationEdge(declarationEdge.getRawStatement(),
                                        declarationEdge.getLineNumber(),
                                        declarationEdge.getPredecessor(),
                                        declarationEdge.getSuccessor(),
                                        variableDeclaration);
          }
        } else if (declaration instanceof CFunctionDeclaration) {
          final CFunctionDeclaration oldFunctionDeclaration = (CFunctionDeclaration) declaration;
          final CFunctionDeclaration functionDeclaration = transformFunctionDeclaration(oldFunctionDeclaration);

          if (functionDeclaration != oldFunctionDeclaration) {
            declarationCache.put(functionDeclaration.getQualifiedName(), functionDeclaration);
            edge = new CDeclarationEdge(declarationEdge.getRawStatement(),
                                        declarationEdge.getLineNumber(),
                                        declarationEdge.getPredecessor(),
                                        declarationEdge.getSuccessor(),
                                        functionDeclaration);
          }
        } else if (declaration instanceof CTypeDeclaration) {
          final CTypeDeclaration oldTypeDeclaration = (CTypeDeclaration) declaration;
          final CType oldType = oldTypeDeclaration.getType();
          final CType type = oldType.accept(typeVisitor);

          if (type != oldType) {
            final CTypeDeclaration typeDeclaration;
            if (oldTypeDeclaration instanceof CComplexTypeDeclaration) {
              typeDeclaration = new CComplexTypeDeclaration(oldTypeDeclaration.getFileLocation(),
                                                            oldTypeDeclaration.isGlobal(),
                                                            (CComplexType) type);
            } else if (oldTypeDeclaration instanceof CTypeDefDeclaration) {
               typeDeclaration = new CTypeDefDeclaration(oldTypeDeclaration.getFileLocation(),
                                                         oldTypeDeclaration.isGlobal(),
                                                         type,
                                                         oldTypeDeclaration.getName(),
                                                         oldTypeDeclaration.getQualifiedName());
            } else {
              throw new IllegalArgumentException("Unexpected type declaration kind");
            }
            declarationCache.put(typeDeclaration.getQualifiedName(), typeDeclaration);
            edge = new CDeclarationEdge(declarationEdge.getRawStatement(),
                                        declarationEdge.getLineNumber(),
                                        declarationEdge.getPredecessor(),
                                        declarationEdge.getSuccessor(),
                                        typeDeclaration);

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
        final CFunctionSummaryEdge oldSummaryEdge = functionCallEdge.getSummaryEdge();
        final CFunctionCall oldFunctionCall = oldSummaryEdge.getExpression();
        final CFunctionCallExpression oldFunctionCallExpression = oldFunctionCall.getFunctionCallExpression();
        final CExpression oldFunctionNameExpression = oldFunctionCallExpression.getFunctionNameExpression();
        final CExpression funcitonNameExpression = (CExpression) oldFunctionNameExpression.accept(expressionVisitor);
        final CType oldFunctionNameExpressionType = oldFunctionCallExpression.getExpressionType();
        final CType funcitonNameExpressionType = oldFunctionNameExpressionType.accept(typeVisitor);
        if (arguments != null ||
            funcitonNameExpression != oldFunctionNameExpression ||
            funcitonNameExpressionType != oldFunctionNameExpressionType) {
          final CFunctionCallExpression functionCallExpression = new CFunctionCallExpression(
                                                                       oldFunctionCallExpression.getFileLocation(),
                                                                       funcitonNameExpressionType,
                                                                       funcitonNameExpression,
                                                                       arguments != null ? arguments :
                                                                         functionCallEdge.getArguments(),
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
                                       new CFunctionSummaryEdge(oldSummaryEdge.getRawStatement(),
                                                                oldSummaryEdge.getLineNumber(),
                                                                oldSummaryEdge.getPredecessor(),
                                                                oldSummaryEdge.getSuccessor(),
                                                                functionCall));
        }
        break;
      }
      case FunctionReturnEdge: {
        final CFunctionReturnEdge oldFunctionReturnEdge = (CFunctionReturnEdge) oldEdge;
        final CFunctionSummaryEdge oldSummaryEdge = oldFunctionReturnEdge.getSummaryEdge();
        final CFunctionCall oldFunctionCall = oldSummaryEdge.getExpression();
        final CFunctionCall functionCall = (CFunctionCall) oldFunctionCall.asStatement().accept(statementVisitor);
        if (functionCall != oldFunctionCall) {
          edge = new CFunctionReturnEdge(oldFunctionReturnEdge.getLineNumber(),
                                         oldFunctionReturnEdge.getPredecessor(),
                                         oldFunctionReturnEdge.getSuccessor(),
                                         new CFunctionSummaryEdge(oldSummaryEdge.getRawStatement(),
                                                                  oldSummaryEdge.getLineNumber(),
                                                                  oldSummaryEdge.getPredecessor(),
                                                                  oldSummaryEdge.getSuccessor(),
                                                                  functionCall));
        }
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
          final CFunctionCallExpression oldCallExpression =
            summaryStatementEdge.getFunctionCall().getFunctionCallExpression();
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
        break;
      }
      default:
        throw new IllegalArgumentException("Illegal edge type: " + oldEdge);
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

  public CFAEdge transformEdge(final CFAEdge oldEdge) {
    final CFANode predecessor = oldEdge.getPredecessor();
    final CFANode successor = oldEdge.getSuccessor();

    final CFAEdge newEdge;
    if (oldEdge instanceof MultiEdge) {
      List<CFAEdge> edges = null;
      int i = 0;
      for (CFAEdge oldSubEdge : ((MultiEdge) oldEdge).getEdges()) {
        final CFAEdge subEdge = transformSimpleEdge(oldSubEdge);
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
        newEdge = new MultiEdge(predecessor, successor, edges);
      } else {
        newEdge = oldEdge;
      }
    } else {
      newEdge = transformSimpleEdge(oldEdge);
    }

    return newEdge;
  }

  @Override
  public TraversalProcess visitNode(CFANode node) {
    if (node instanceof FunctionEntryNode && node != cfa.getMainFunction()) {
      final FunctionEntryNode oldFunctionEntryNode = (FunctionEntryNode) node;
      final CFunctionDeclaration oldFunctionDeclaration =
        (CFunctionDeclaration) oldFunctionEntryNode.getFunctionDefinition();
      final CFunctionDeclaration functionDeclaration = transformFunctionDeclaration(oldFunctionDeclaration);
      if (functionDeclaration != oldFunctionDeclaration) {
        final CFunctionEntryNode functionEntryNode =
          new CFunctionEntryNode(oldFunctionEntryNode.getLineNumber(),
                                 functionDeclaration,
                                 oldFunctionEntryNode.getExitNode(),
                                 oldFunctionEntryNode.getFunctionParameterNames());
        for (int i = 0; i < node.getNumEnteringEdges(); i++) {
          final CFunctionCallEdge oldEdge = (CFunctionCallEdge) node.getEnteringEdge(i);
          final CFANode predecessor = oldEdge.getPredecessor();
          predecessor.removeLeavingEdge(oldEdge);
          final CFAEdge edge = new CFunctionCallEdge(oldEdge.getRawStatement(),
                                                     oldEdge.getLineNumber(),
                                                     predecessor,
                                                     functionEntryNode,
                                                     oldEdge.getSummaryEdge().getExpression(),
                                                     oldEdge.getSummaryEdge());
          predecessor.addLeavingEdge(edge);
          functionEntryNode.addEnteringEdge(edge);
        }
        for (int i = 0; i < node.getNumLeavingEdges(); i++) {
          final CFAEdge oldEdge = node.getLeavingEdge(i);
          final CFANode successor = oldEdge.getSuccessor();
          final CFAEdge edge;
          if (oldEdge instanceof BlankEdge) {
            edge = new BlankEdge(oldEdge.getRawStatement(),
                                 oldEdge.getLineNumber(),
                                 functionEntryNode,
                                 oldEdge.getSuccessor(),
                                 oldEdge.getDescription());
          } else {
            assert oldEdge instanceof MultiEdge;
            edge = new MultiEdge(functionEntryNode,
                                 oldEdge.getSuccessor(),
                                 ((MultiEdge) oldEdge).getEdges());
          }
          successor.removeEnteringEdge(oldEdge);
          successor.addEnteringEdge(edge);
          functionEntryNode.addLeavingEdge(edge);
        }
        cfa.removeNode(node);
        cfa.addNode(functionEntryNode);
        node = functionEntryNode;
        // functionEntryNode.getExitNode().setEntryNode(functionEntryNode);
      }
    }

    edges.clear();
    for (int i = 0; i < node.getNumLeavingEdges(); i++) {
      edges.add(node.getLeavingEdge(i));
    }
    for (final CFAEdge oldEdge : edges) {
      final CFAEdge newEdge = transformEdge(oldEdge);
      if (newEdge != oldEdge) {
        node.removeLeavingEdge(oldEdge);
        node.addLeavingEdge(newEdge);

        oldEdge.getSuccessor().removeEnteringEdge(oldEdge);
        oldEdge.getSuccessor().addEnteringEdge(newEdge);
      }
    }

    return TraversalProcess.CONTINUE;
  }

  private CFATransformer(final MutableCFA cfa,
                         final LogManager logger,
                         final MachineModel machineModel,
                         final boolean transformUnsizedArrays,
                         final boolean ignoreConst,
                         final boolean ignoreVolatile,
                         final boolean transformPointerArithmetic,
                         final boolean transformArrows,
                         final boolean transformStarAmper,
                         final boolean transformFunctionPointers) {
    this.cfa = cfa;

    // Caching is mandatory to prevent infinite recursion
    typeVisitor = new CachingCTypeTransformer(machineModel, transformUnsizedArrays, ignoreConst, ignoreVolatile);
    expressionVisitor = new CExpressionTransformer(typeVisitor,
                                                   declarationCache,
                                                   transformPointerArithmetic,
                                                   transformArrows,
                                                   transformStarAmper,
                                                   transformFunctionPointers);
    statementVisitor = new CStatementTransformer(typeVisitor, expressionVisitor);
    rhsVisitor = new CRightHandSideTransformer(typeVisitor, expressionVisitor);
    this.logger = logger;
  }

  public static void transformCFA(final MutableCFA cfa,
                                  final LogManager logger,
                                  final MachineModel machineModel,
                                  final boolean transformUnsizedArrays,
                                  final boolean ignoreConst,
                                  final boolean ignoreVolatile,
                                  final boolean transformPointerArithmetic,
                                  final boolean transformArrows,
                                  final boolean transformStarAmper,
                                  final boolean transformFunctionPointers) {
    final CFATransformer cfaTransformer = new CFATransformer(cfa,
                                                             logger,
                                                             machineModel,
                                                             transformUnsizedArrays,
                                                             ignoreConst,
                                                             ignoreVolatile,
                                                             transformPointerArithmetic,
                                                             transformArrows,
                                                             transformStarAmper,
                                                             transformFunctionPointers);
    CFATraversal cfaTraversal = CFATraversal.dfs().ignoreSummaryEdges();
    cfaTraversal.traverseOnce(cfa.getMainFunction(), cfaTransformer);
//    for (CFANode functionHead : cfa.getAllFunctionHeads()) {
//      cfaTraversal.traverseOnce(functionHead, cfaTransformer);
//    }
  }

  private final MutableCFA cfa;

  private final Map<String, CDeclaration> declarationCache = new HashMap<>();

  private final CachingCTypeTransformer typeVisitor;
  private final CExpressionTransformer expressionVisitor;
  private final CStatementTransformer statementVisitor;
  private final CRightHandSideTransformer rhsVisitor;
  private final List<CFAEdge> edges = new ArrayList<>(2);
  private final LogManager logger;
}
