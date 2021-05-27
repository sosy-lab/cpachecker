// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.automatic_program_repair;

import java.util.ArrayList;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;

public class CFAMutator {

  public static ArrayList<CFAEdge> calcPossibleMutations(CFA originalCFA, CFAEdge edge) {
    switch (edge.getEdgeType()) {
      case StatementEdge:
        return generateStatementEdgeMutations(originalCFA, (CStatementEdge) edge);

      case AssumeEdge:
        return generateAssumeEdgeMutations((CAssumeEdge) edge);

      default:
        return new ArrayList<>();
    }
  }

  private static ArrayList<CFAEdge> generateStatementEdgeMutations(
      CFA originalCFA,
      CStatementEdge statementEdge) {
    CStatement statement = statementEdge.getStatement();
    ArrayList<CFAEdge> alternativeStatements = new ArrayList<>();

    if (statement instanceof CExpressionAssignmentStatement) {
      final CExpressionAssignmentStatement expressionAssignmentStatement =
          (CExpressionAssignmentStatement) statement;
      final Set<CExpression> expressions = ExpressionCollector.collectExpressions(originalCFA);
      CExpression originalExpression = expressionAssignmentStatement.getRightHandSide();

      for (CExpression expression : expressions) {
        if (!originalExpression.equals(expression)) {

          alternativeStatements.add(
              new CStatementEdge(
                  statementEdge.getRawStatement(),
                  exchangeExpression(expressionAssignmentStatement, expression),
                  statementEdge.getFileLocation(),
                  statementEdge.getPredecessor(),
                  statementEdge.getSuccessor()));
        }
      }
    }

    return alternativeStatements;
  }

  private static CStatement exchangeExpression(
      CExpressionAssignmentStatement expressionAssignmentStatement,
      CExpression expression) {
    return new CExpressionAssignmentStatement(
        expressionAssignmentStatement.getFileLocation(),
        expressionAssignmentStatement.getLeftHandSide(),
        expression);
  }

  private static ArrayList<CFAEdge> generateAssumeEdgeMutations(CAssumeEdge assumeEdge) {
    CExpression expression = assumeEdge.getExpression();
    ArrayList<CFAEdge> alternativeExpressions = new ArrayList<>();

    if (expression instanceof CBinaryExpression) {
      final CBinaryExpression binaryExpression = (CBinaryExpression) expression;

      for (BinaryOperator operator : BinaryOperator.values()) {
        if (binaryExpression.getOperator().isLogicalOperator() == operator.isLogicalOperator()
            && binaryExpression.getOperator() != operator) {
          final CBinaryExpression modifiedExpression =
              exchangeBinaryOperator(binaryExpression, operator);

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

  private static CBinaryExpression exchangeBinaryOperator(
      CBinaryExpression binaryExpression,
      BinaryOperator operator) {
    return
        new CBinaryExpression(
            binaryExpression.getFileLocation(),
            binaryExpression.getExpressionType(),
            binaryExpression.getCalculationType(),
            binaryExpression.getOperand1(),
            binaryExpression.getOperand2(),
            operator);
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

    return currentCFA;
  }

}
