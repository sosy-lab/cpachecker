// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.automatic_program_repair;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class CFAMutator {

  public static ArrayList<CFAEdge> calcPossibleMutations(CFA cfa, CFAEdge edge) {
    switch (edge.getEdgeType()) {
      case AssumeEdge:
        return generateAssumeEdgeMutations((CAssumeEdge) edge);

      case StatementEdge:
        return generateStatementEdgeMutations(cfa, (CStatementEdge) edge);

      case FunctionCallEdge:
        return generateFunctionCallEdgeMutations((CFunctionCallEdge) edge);

      case FunctionReturnEdge:
        return generateFunctionReturnEdgeMutations(cfa, (CFunctionReturnEdge) edge);

      default:
        return new ArrayList<>();
    }
  }

  public static Map<CType, Set<CExpression>> sortExpressionsByType(Set<CExpression> expressions) {
    final Map<CType, Set<CExpression>> expressionsSortedByType = Maps.newHashMap();

    for (CExpression expression : expressions) {
      CType type = expression.getExpressionType();

      if (expressionsSortedByType.containsKey(type)) {
        expressionsSortedByType.get(type).add(expression);
      } else {
        expressionsSortedByType.put(type, Sets.newHashSet(expression));
      }
    }
    return expressionsSortedByType;
  }

  public static MutableCFA exchangeEdge(
      MutableCFA currentCFA, CFAEdge edgeToRemove, CFAEdge edgeToInsert) {
    final CFANode predecessorNode = edgeToRemove.getPredecessor();
    final CFANode successorNode = edgeToRemove.getSuccessor();

    for (int i = 0; i < predecessorNode.getNumLeavingEdges(); i++) {
      final CFAEdge edge = predecessorNode.getLeavingEdge(i);

      if (edge.getLineNumber() == edgeToInsert.getLineNumber()) {
        predecessorNode.removeLeavingEdge(edge);
        predecessorNode.addLeavingEdge(edgeToInsert);
      }
    }

    for (int a = 0; a < successorNode.getNumEnteringEdges(); a++) {
      final CFAEdge edge = successorNode.getEnteringEdge(a);

      if (edge.getLineNumber() == edgeToInsert.getLineNumber()) {
        successorNode.removeEnteringEdge(edge);
        successorNode.addEnteringEdge(edgeToInsert);
      }
    }

    if (edgeToInsert instanceof CFunctionReturnEdge) {
      CFunctionReturnEdge functionEdgeToInsert = (CFunctionReturnEdge) edgeToInsert;
      final ArrayList<FunctionSummaryEdge> summaryEdges = new ArrayList();

      summaryEdges.add(predecessorNode.getLeavingSummaryEdge());
      summaryEdges.add(predecessorNode.getEnteringSummaryEdge());
      summaryEdges.add(successorNode.getLeavingSummaryEdge());
      summaryEdges.add(successorNode.getEnteringSummaryEdge());

      for (FunctionSummaryEdge summaryEdge : summaryEdges) {
        if (summaryEdge != null && summaryEdge
            .equals(functionEdgeToInsert.getSummaryEdge())) {
          successorNode.removeEnteringSummaryEdge(summaryEdge);
          successorNode.addEnteringSummaryEdge(functionEdgeToInsert.getSummaryEdge());
        }
      }
    }

    return currentCFA;
  }


  /* EDGES  */
  /* TODO extract expression mutation */
  private static ArrayList<CFAEdge> generateAssumeEdgeMutations(CAssumeEdge assumeEdge) {
    CExpression expression = assumeEdge.getExpression();
    ArrayList<CFAEdge> alternativeExpressions = new ArrayList<>();

    if (expression instanceof CBinaryExpression) {
      final CBinaryExpression binaryExpression = (CBinaryExpression) expression;

      for (BinaryOperator operator : BinaryOperator.values()) {
        if (binaryExpression.getOperator().isLogicalOperator() == operator.isLogicalOperator()
            && binaryExpression.getOperator() != operator) {
          final CBinaryExpression modifiedExpression =
              replaceBinaryOperator(binaryExpression, operator);

          alternativeExpressions.add(
              new CAssumeEdge(
                  modifiedExpression.toASTString(),
                  assumeEdge.getFileLocation(),
                  assumeEdge.getPredecessor(),
                  assumeEdge.getSuccessor(),
                  modifiedExpression,
                  assumeEdge.getTruthAssumption()));
        }
      }
    }

    return alternativeExpressions;
  }

  private static ArrayList<CFAEdge> generateStatementEdgeMutations(
      CFA cfa, CStatementEdge originalEdge) {
    CStatement statement = originalEdge.getStatement();
    ArrayList<CStatement> alternativeStatements = new ArrayList<>();
    ArrayList<CFAEdge> alternativeEdges = new ArrayList<>();

    if (statement instanceof CExpressionAssignmentStatement) {
      alternativeStatements =
          calcMutationsFor((CExpressionAssignmentStatement) statement, cfa);
    } else if (statement instanceof CExpressionStatement) {
      alternativeStatements = calcMutationsFor((CExpressionStatement) statement, cfa);
    } else if (statement instanceof CFunctionCallAssignmentStatement) {
      alternativeStatements =
          calcMutationsFor((CFunctionCallAssignmentStatement) statement, cfa);
    } else if (statement instanceof CFunctionCallStatement) {
      alternativeStatements =
          calcMutationsFor((CFunctionCallStatement) statement, cfa);
    }

    for (CStatement alternativeStatement : alternativeStatements) {
      alternativeEdges.add(
          new CStatementEdge(
              alternativeStatement.toASTString(),
              alternativeStatement,
              originalEdge.getFileLocation(),
              originalEdge.getPredecessor(),
              originalEdge.getSuccessor()));
    }

    return alternativeEdges;
  }

  /* TODO implement expression mutation */
  private static ArrayList<CFAEdge> generateFunctionCallEdgeMutations(
      CFunctionCallEdge functionCallEdge) {
    ArrayList<CFAEdge> alternativeExpressions = new ArrayList<>();
    return alternativeExpressions;
  }

  private static ArrayList<CFAEdge> generateFunctionReturnEdgeMutations(
      CFA cfa,
      CFunctionReturnEdge functionReturnEdge) {
    ArrayList<CStatement> alternativeFunctionCalls = new ArrayList<>();
    ArrayList<CFAEdge> alternativeEdges = new ArrayList<>();
    CFunctionSummaryEdge summaryEdge = functionReturnEdge.getSummaryEdge();
    CFunctionCall functionCall = summaryEdge.getExpression();

    if (functionCall instanceof CFunctionCallAssignmentStatement) {
      alternativeFunctionCalls =
          calcMutationsFor((CFunctionCallAssignmentStatement) functionCall, cfa);
    } else if (functionCall instanceof CFunctionCallStatement) {
      alternativeFunctionCalls =
          calcMutationsFor((CFunctionCallStatement) functionCall, cfa);
    }

    for (CStatement alternativeFunctionCall : alternativeFunctionCalls) {
      CFunctionSummaryEdge newSummaryEdge =
          new CFunctionSummaryEdge(alternativeFunctionCall.toASTString(),
              summaryEdge.getFileLocation(),
              summaryEdge.getPredecessor(), summaryEdge.getSuccessor(),
              (CFunctionCall) alternativeFunctionCall,
              summaryEdge.getFunctionEntry());

      alternativeEdges.add(
          new CFunctionReturnEdge(
              functionReturnEdge.getFileLocation(),
              functionReturnEdge.getPredecessor(),
              functionReturnEdge.getSuccessor(),
              newSummaryEdge));
    }

    return alternativeEdges;
  }


  /* STATEMENTS */

  private static ArrayList<CStatement> calcMutationsFor(
      CExpressionAssignmentStatement originalExpressionAssignmentStatement,
      CFA cfa) {
    ArrayList<CStatement> alternativeStatements = new ArrayList<>();
    CExpression originalExpression = originalExpressionAssignmentStatement.getRightHandSide();
    final ArrayList<CExpression> expressions =
        calcMutationsFor(originalExpression, cfa);

    for (CExpression expression : expressions) {
      alternativeStatements.add(
          replaceExpression(originalExpressionAssignmentStatement, expression));
    }
    return alternativeStatements;
  }

  private static ArrayList<CStatement> calcMutationsFor(
      CExpressionStatement originalExpressionStatement,
      CFA cfa) {
    ArrayList<CStatement> alternativeStatements = new ArrayList<>();
    CExpression originalExpression = originalExpressionStatement.getExpression();
    final ArrayList<CExpression> expressions =
        calcMutationsFor(originalExpression, cfa);

    for (CExpression expression : expressions) {
      alternativeStatements.add(
          replaceExpression(originalExpressionStatement, expression));
    }

    return alternativeStatements;
  }

  private static ArrayList<CStatement> calcMutationsFor(
      CFunctionCallAssignmentStatement originalFunctionCallAssignmentStatement,
      CFA cfa) {
    ArrayList<CStatement> alternativeStatements = new ArrayList<>();
    CFunctionCallExpression originalFunctionCallExpression =
        originalFunctionCallAssignmentStatement.getRightHandSide();
    ArrayList<CFunctionCallExpression> alternativeFunctionCalls =
        calcMutationsFor(originalFunctionCallExpression, cfa);

    for (CFunctionCallExpression alternativeFunctionCall : alternativeFunctionCalls) {
      alternativeStatements.add(
          replaceRightHandSide(originalFunctionCallAssignmentStatement,
              alternativeFunctionCall));
    }

    return alternativeStatements;
  }

  private static ArrayList<CStatement> calcMutationsFor(
      CFunctionCallStatement originalFunctionCallStatement,
      CFA cfa) {
    ArrayList<CStatement> alternativeStatements = new ArrayList<>();
    CFunctionCallExpression originalFunctionCallExpression =
        originalFunctionCallStatement.getFunctionCallExpression();
    ArrayList<CFunctionCallExpression> alternativeFunctionCalls =
        calcMutationsFor(originalFunctionCallExpression, cfa);

    for (CFunctionCallExpression alternativeFunctionCall : alternativeFunctionCalls) {
      alternativeStatements.add(
          replaceFunctionCall(originalFunctionCallStatement, alternativeFunctionCall));
    }

    return alternativeStatements;
  }


  /* EXPRESSIONS */

  private static ArrayList<CExpression> calcMutationsFor(
      CExpression originalExpression, CFA cfa) {
    ArrayList<CExpression> alternativeExpressions = new ArrayList();
    final Set<CExpression> expressions = ExpressionCollector.collectExpressions(cfa);
    final Map<CType, Set<CExpression>> sortedExpression = sortExpressionsByType(expressions);

    if (originalExpression instanceof CBinaryExpression) {
      alternativeExpressions =
          calcMutationsFor((CBinaryExpression) originalExpression);
    }

    Set<CExpression> sameTypeExpressions =
        sortedExpression.get(originalExpression.getExpressionType());
    for (CExpression expression : sameTypeExpressions) {
      if (!originalExpression.equals(expression)) {
        alternativeExpressions.add(expression);
      }
    }

    return alternativeExpressions;
  }


  private static ArrayList<CFunctionCallExpression> calcMutationsFor(
      CFunctionCallExpression originalFunctionCallExpression,
      CFA cfa) {
    ArrayList<CFunctionCallExpression> alternativeStatements = new ArrayList<>();
    List<CExpression> originalParameterExpressions =
        originalFunctionCallExpression.getParameterExpressions();

    /* Creates a list of CFunctionCallAssignmentStatements, where each entry has a copy of the original
    parameter list but with one altered parameter */
    for (int i = 0; i < originalParameterExpressions.size(); i++) {
      CExpression originalParameterExpression = originalParameterExpressions.get(i);

      for (CExpression alternativeParameterExpression :
          calcMutationsFor(originalParameterExpression, cfa)) {

        ArrayList<CExpression> alternatives = new ArrayList<>(originalParameterExpressions);
        alternatives.set(i, alternativeParameterExpression);

        alternativeStatements.add(
            replaceParameters(originalFunctionCallExpression, alternatives));
      }
    }

    return alternativeStatements;
  }

  private static ArrayList<CExpression> calcMutationsFor(
      CBinaryExpression binaryExpression) {
    ArrayList<CExpression> alternativeExpressions = new ArrayList();

    for (BinaryOperator operator : BinaryOperator.values()) {
      if (binaryExpression.getOperator().isLogicalOperator() == operator.isLogicalOperator()
          && binaryExpression.getOperator() != operator) {
        final CBinaryExpression modifiedExpression =
            replaceBinaryOperator(binaryExpression, operator);
        alternativeExpressions.add(modifiedExpression);
      }
    }

    return alternativeExpressions;
  }


  /* REPLACEMENTS */

  private static CFunctionCallExpression replaceParameters(
      CFunctionCallExpression originalFunctionCallExpression,
      List<CExpression> newParameterExpressions) {
    return new CFunctionCallExpression(
        originalFunctionCallExpression.getFileLocation(),
        originalFunctionCallExpression.getExpressionType(),
        originalFunctionCallExpression.getFunctionNameExpression(),
        newParameterExpressions,
        originalFunctionCallExpression.getDeclaration());
  }

  private static CFunctionCallAssignmentStatement replaceRightHandSide(
      CFunctionCallAssignmentStatement originalFunctionCallAssignmentStatement,
      CFunctionCallExpression newFunctionCallExpression) {
    return new CFunctionCallAssignmentStatement(
        originalFunctionCallAssignmentStatement.getFileLocation(),
        originalFunctionCallAssignmentStatement.getLeftHandSide(),
        newFunctionCallExpression);
  }

  private static CFunctionCallStatement replaceFunctionCall(
      CFunctionCallStatement originalFunctionCallStatement,
      CFunctionCallExpression newFunctionCallExpression) {
    return new CFunctionCallStatement(
        originalFunctionCallStatement.getFileLocation(),
        newFunctionCallExpression);
  }

  private static CStatement replaceExpression(
      CExpressionStatement expressionAssignmentStatement, CExpression expression) {
    return new CExpressionStatement(expressionAssignmentStatement.getFileLocation(), expression);
  }

  private static CStatement replaceExpression(
      CExpressionAssignmentStatement expressionAssignmentStatement, CExpression expression) {
    return new CExpressionAssignmentStatement(
        expressionAssignmentStatement.getFileLocation(),
        expressionAssignmentStatement.getLeftHandSide(),
        expression);
  }

  private static CBinaryExpression replaceBinaryOperator(
      CBinaryExpression binaryExpression, BinaryOperator operator) {
    return new CBinaryExpression(
        binaryExpression.getFileLocation(),
        binaryExpression.getExpressionType(),
        binaryExpression.getCalculationType(),
        binaryExpression.getOperand1(),
        binaryExpression.getOperand2(),
        operator);
  }

}
