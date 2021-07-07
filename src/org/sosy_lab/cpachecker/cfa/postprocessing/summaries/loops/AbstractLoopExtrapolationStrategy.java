// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops;

import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependencyInterface;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class AbstractLoopExtrapolationStrategy extends AbstractLoopStrategy {
  protected AbstractLoopExtrapolationStrategy(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      StrategyDependencyInterface pStrategyDependencies) {
    super(pLogger, pShutdownNotifier, pStrategyDependencies);
  }

  // For an explanation why this works in conjunction with ConstatnExtrapolationStrategy see
  // https://stackoverflow.com/questions/4595512/java-calling-a-super-method-which-calls-an-overridden-method
  protected boolean linearArithmeticExpressionEdge(final CFAEdge edge) {
    if (edge instanceof BlankEdge) {
      return true;
    }

    if (edge instanceof CAssumeEdge) {
      return true;
    }

    if (!(edge instanceof CStatementEdge)) {
      return false;
    }

    CStatement statement = ((CStatementEdge) edge).getStatement();
    if (!(statement instanceof CExpressionAssignmentStatement)) {
      return false;
    }

    CExpression leftSide = ((CExpressionAssignmentStatement) statement).getLeftHandSide();
    CExpression rigthSide = ((CExpressionAssignmentStatement) statement).getRightHandSide();

    if (!(leftSide instanceof CIdExpression)) {
      return false;
    }
    if (!linearArithemticExpression(rigthSide)) {
      return false;
    }

    return true;
  }

  protected boolean linearArithemticExpression(final CExpression expression) {
    if (expression instanceof CIdExpression || expression instanceof CIntegerLiteralExpression) {
      return true;
    } else if (expression instanceof CBinaryExpression) {
      String operator = ((CBinaryExpression) expression).getOperator().getOperator();
      CExpression operand1 = ((CBinaryExpression) expression).getOperand1();
      CExpression operand2 = ((CBinaryExpression) expression).getOperand2();
      switch (operator) {
        case "+":
        case "-":
          return linearArithemticExpression(operand1) && linearArithemticExpression(operand2);
        case "*":
          return (linearArithemticExpression(operand1)
                  && operand2 instanceof CIntegerLiteralExpression)
              || (operand1 instanceof CIntegerLiteralExpression
                  && linearArithemticExpression(operand2));
        default:
          return false;
      }
    } else {
      return false;
    }
  }

  protected boolean linearArithmeticExpressionsLoop(final CFANode pLoopStartNode, int branchIndex) {
    CFANode nextNode0 = pLoopStartNode.getLeavingEdge(branchIndex).getSuccessor();
    boolean nextNode0Valid = true;
    while (nextNode0 != pLoopStartNode && nextNode0Valid) {
      if (nextNode0Valid
          && nextNode0.getNumLeavingEdges() == 1
          && linearArithmeticExpressionEdge(nextNode0.getLeavingEdge(0))) {
        nextNode0 = nextNode0.getLeavingEdge(0).getSuccessor();
      } else {
        nextNode0Valid = false;
      }
      if (nextNode0 == pLoopStartNode) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected Optional<Integer> getLoopBranchIndex(CFANode loopStartNode) {
    if (loopStartNode.getNumLeavingEdges() != 2) {
      return Optional.empty();
    }
    CFANode nextNode0 = loopStartNode.getLeavingEdge(0).getSuccessor();
    CFANode nextNode1 = loopStartNode.getLeavingEdge(1).getSuccessor();
    boolean nextNode0Valid = true;
    boolean nextNode1Valid = true;
    while (nextNode0 != loopStartNode
        && nextNode1 != loopStartNode
        && (nextNode0Valid || nextNode1Valid)) {
      if (nextNode0Valid && nextNode0.getNumLeavingEdges() == 1) {
        nextNode0 = nextNode0.getLeavingEdge(0).getSuccessor();
      } else {
        nextNode0Valid = false;
      }
      if (nextNode1Valid && nextNode1.getNumLeavingEdges() == 1) {
        nextNode1 = nextNode1.getLeavingEdge(0).getSuccessor();
      } else {
        nextNode1Valid = false;
      }
      if (nextNode0 == loopStartNode) {
        return Optional.of(0);
      } else if (nextNode1 == loopStartNode) {
        return Optional.of(1);
      }
    }
    return Optional.empty();
  }

  // Returns the bound in the form 0 < x where x is the CExpression returned
  protected Optional<CExpression> bound(final CFANode pLoopStartNode) {
    CFAEdge edge = pLoopStartNode.getLeavingEdge(0);
    if (!(edge instanceof CAssumeEdge)) {
      return Optional.empty();
    }
    CExpression expression = ((CAssumeEdge) edge).getExpression();
    if (!(expression instanceof CBinaryExpression)) {
      return Optional.empty();
    }

    String operator = ((CBinaryExpression) expression).getOperator().getOperator();
    CExpression operand1 = ((CBinaryExpression) expression).getOperand1();
    CExpression operand2 = ((CBinaryExpression) expression).getOperand2();
    CType calculationType = ((CBinaryExpression) expression).getCalculationType();
    CType expressionType = ((CBinaryExpression) expression).getExpressionType();

    if (!((operand1 instanceof CIdExpression
            && (operand2 instanceof CIdExpression || operand2 instanceof CIntegerLiteralExpression))
        || (operand1 instanceof CIntegerLiteralExpression && operand2 instanceof CIdExpression))) {
      return Optional.empty();
    }

    switch (operator) {
      case "<":
        return Optional.of(
            new CBinaryExpression(
                FileLocation.DUMMY,
                expressionType,
                calculationType,
                operand2,
                operand1,
                BinaryOperator.MINUS));

      case ">":
        return Optional.of(
            new CBinaryExpression(
                FileLocation.DUMMY,
                expressionType,
                calculationType,
                operand1,
                operand2,
                BinaryOperator.MINUS));
      case "<=":
        return Optional.of(
            new CBinaryExpression(
                FileLocation.DUMMY,
                expressionType,
                calculationType,
                operand2,
                new CBinaryExpression(
                    expression.getFileLocation(),
                    expressionType,
                    calculationType,
                    operand1,
                    CIntegerLiteralExpression.createDummyLiteral(1, CNumericTypes.INT),
                    BinaryOperator.MINUS),
                BinaryOperator.MINUS));
      case ">=":
        return Optional.of(
            new CBinaryExpression(
                FileLocation.DUMMY,
                expressionType,
                calculationType,
                operand1,
                new CBinaryExpression(
                    expression.getFileLocation(),
                    expressionType,
                    calculationType,
                    operand2,
                    CIntegerLiteralExpression.createDummyLiteral(1, CNumericTypes.INT),
                    BinaryOperator.MINUS),
                BinaryOperator.MINUS));
      default:
        return Optional.empty();
    }
  }

  @Override
  public boolean isPrecise() {
    return true;
  }
}
