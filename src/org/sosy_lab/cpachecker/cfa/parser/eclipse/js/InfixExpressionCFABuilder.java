/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.js;

import static org.sosy_lab.cpachecker.cfa.model.js.JSAssumeEdge.assume;

import com.google.common.collect.ImmutableSet;
import org.eclipse.wst.jsdt.core.dom.InfixExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.js.JSDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.js.JSStatementEdge;

@SuppressWarnings({"ALL", "ResultOfMethodCallIgnored"})
class InfixExpressionCFABuilder implements InfixExpressionAppendable {

  @Override
  public JSExpression append(
      final JavaScriptCFABuilder pBuilder, final InfixExpression pInfixExpression) {
    final BinaryOperator operator = convert(pInfixExpression.getOperator());
    switch (operator) {
      case CONDITIONAL_AND:
      case CONDITIONAL_OR:
        return appendWithSideEffect(pBuilder, pInfixExpression, operator);
      case AND:
      case DIVIDE:
      case EQUALS:
      case EQUAL_EQUAL_EQUAL:
      case GREATER:
      case GREATER_EQUALS:
      case IN:
      case INSTANCEOF:
      case LEFT_SHIFT:
      case LESS:
      case LESS_EQUALS:
      case MINUS:
      case NOT_EQUAL_EQUAL:
      case NOT_EQUALS:
      case OR:
      case PLUS:
      case REMAINDER:
      case RIGHT_SHIFT_SIGNED:
      case RIGHT_SHIFT_UNSIGNED:
      case TIMES:
      case XOR:
        return appendWithoutSideEffect(pBuilder, pInfixExpression, operator);
    }
    throw new CFAGenerationRuntimeException(
        "Unknown kind of unary operator (not handled yet): " + operator, pInfixExpression);
  }

  private JSExpression appendWithSideEffect(
      final JavaScriptCFABuilder pBuilder,
      final InfixExpression pInfixExpression,
      final BinaryOperator pOperator) {
    assert ImmutableSet.of(BinaryOperator.CONDITIONAL_AND, BinaryOperator.CONDITIONAL_OR)
        .contains(pOperator);
    final String resultVariableName = pBuilder.generateVariableName();
    final JSVariableDeclaration resultVariableDeclaration =
        new JSVariableDeclaration(
            FileLocation.DUMMY,
            false,
            resultVariableName,
            resultVariableName,
            pBuilder.getScope().qualifiedVariableNameOf(resultVariableName),
            null);
    final JSIdExpression resultVariableId =
        new JSIdExpression(
            FileLocation.DUMMY, resultVariableName, resultVariableDeclaration);
    pBuilder.appendEdge(
        (pPredecessor, pSuccessor) ->
            new JSDeclarationEdge(
                resultVariableDeclaration.toASTString(),
                resultVariableDeclaration.getFileLocation(),
                pPredecessor,
                pSuccessor,
                resultVariableDeclaration));
    final CFANode exitNode = pBuilder.createNode();
    final JSExpression leftEvaluated = pBuilder.append(pInfixExpression.getLeftOperand());

    final JavaScriptCFABuilder thenBranchBuilder =
        pBuilder.copy().appendEdge(assume(leftEvaluated, true));
    final JSExpression thenValue =
        pOperator == BinaryOperator.CONDITIONAL_AND
            ? thenBranchBuilder.append(pInfixExpression.getRightOperand())
            : leftEvaluated;
    final JSExpressionAssignmentStatement thenStatement =
        new JSExpressionAssignmentStatement(FileLocation.DUMMY, resultVariableId, thenValue);
    final String operatorRightExprDescription =
        pOperator.getOperator() + " " + pInfixExpression.getRightOperand();
    pBuilder.addParseResult(
        thenBranchBuilder
            .appendEdge(
                (pPredecessor, pSuccessor) ->
                    new JSStatementEdge(
                        thenStatement.toASTString(),
                        thenStatement,
                        thenStatement.getFileLocation(),
                        pPredecessor,
                        pSuccessor))
            .appendEdge(
                exitNode, DummyEdge.withDescription("end true " + operatorRightExprDescription))
            .getParseResult());
    final JavaScriptCFABuilder elseBranchBuilder =
        pBuilder.copy().appendEdge(assume(leftEvaluated, false));
    final JSExpression elseValue =
        pOperator == BinaryOperator.CONDITIONAL_AND
            ? leftEvaluated
            : elseBranchBuilder.append(pInfixExpression.getRightOperand());
    final JSExpressionAssignmentStatement elseStatement =
        new JSExpressionAssignmentStatement(FileLocation.DUMMY, resultVariableId, elseValue);
    pBuilder.append(
        elseBranchBuilder
            .appendEdge(
                (pPredecessor, pSuccessor) ->
                    new JSStatementEdge(
                        elseStatement.toASTString(),
                        elseStatement,
                        elseStatement.getFileLocation(),
                        pPredecessor,
                        pSuccessor))
            .appendEdge(
                exitNode, DummyEdge.withDescription("end false " + operatorRightExprDescription))
            .getBuilder());
    return resultVariableId;
  }

  private JSExpression appendWithoutSideEffect(
      final JavaScriptCFABuilder pBuilder,
      final InfixExpression pInfixExpression,
      final BinaryOperator pOperator) {
    return new JSBinaryExpression(
        FileLocation.DUMMY,
        pBuilder.append(pInfixExpression.getLeftOperand()),
        pBuilder.append(pInfixExpression.getRightOperand()),
        pOperator);
  }

  public BinaryOperator convert(final InfixExpression.Operator pOperator) {
    if (InfixExpression.Operator.AND == pOperator) {
      return BinaryOperator.AND;
    } else if (InfixExpression.Operator.CONDITIONAL_AND == pOperator) {
      return BinaryOperator.CONDITIONAL_AND;
    } else if (InfixExpression.Operator.CONDITIONAL_OR == pOperator) {
      return BinaryOperator.CONDITIONAL_OR;
    } else if (InfixExpression.Operator.DIVIDE == pOperator) {
      return BinaryOperator.DIVIDE;
    } else if (InfixExpression.Operator.EQUALS == pOperator) {
      return BinaryOperator.EQUALS;
    } else if (InfixExpression.Operator.EQUAL_EQUAL_EQUAL == pOperator) {
      return BinaryOperator.EQUAL_EQUAL_EQUAL;
    } else if (InfixExpression.Operator.GREATER == pOperator) {
      return BinaryOperator.GREATER;
    } else if (InfixExpression.Operator.GREATER_EQUALS == pOperator) {
      return BinaryOperator.GREATER_EQUALS;
    } else if (InfixExpression.Operator.IN == pOperator) {
      return BinaryOperator.IN;
    } else if (InfixExpression.Operator.INSTANCEOF == pOperator) {
      return BinaryOperator.INSTANCEOF;
    } else if (InfixExpression.Operator.LEFT_SHIFT == pOperator) {
      return BinaryOperator.LEFT_SHIFT;
    } else if (InfixExpression.Operator.LESS == pOperator) {
      return BinaryOperator.LESS;
    } else if (InfixExpression.Operator.LESS_EQUALS == pOperator) {
      return BinaryOperator.LESS_EQUALS;
    } else if (InfixExpression.Operator.MINUS == pOperator) {
      return BinaryOperator.MINUS;
    } else if (InfixExpression.Operator.NOT_EQUAL_EQUAL == pOperator) {
      return BinaryOperator.NOT_EQUAL_EQUAL;
    } else if (InfixExpression.Operator.NOT_EQUALS == pOperator) {
      return BinaryOperator.NOT_EQUALS;
    } else if (InfixExpression.Operator.OR == pOperator) {
      return BinaryOperator.OR;
    } else if (InfixExpression.Operator.PLUS == pOperator) {
      return BinaryOperator.PLUS;
    } else if (InfixExpression.Operator.REMAINDER == pOperator) {
      return BinaryOperator.REMAINDER;
    } else if (InfixExpression.Operator.RIGHT_SHIFT_SIGNED == pOperator) {
      return BinaryOperator.RIGHT_SHIFT_SIGNED;
    } else if (InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED == pOperator) {
      return BinaryOperator.RIGHT_SHIFT_UNSIGNED;
    } else if (InfixExpression.Operator.TIMES == pOperator) {
      return BinaryOperator.TIMES;
    } else if (InfixExpression.Operator.XOR == pOperator) {
      return BinaryOperator.XOR;
    }
    throw new CFAGenerationRuntimeException(
        "Unknown kind of binary operator (not handled yet): " + pOperator.toString());
  }

}
