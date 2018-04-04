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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.truth.Truth;
import org.eclipse.wst.jsdt.core.dom.ExpressionStatement;
import org.eclipse.wst.jsdt.core.dom.InfixExpression;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSAssignment;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.js.JSAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.js.JSDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.js.JSStatementEdge;

public class InfixExpressionCFABuilderTest {

  private EclipseJavaScriptParser parser;
  private ConfigurableJavaScriptCFABuilder builder;
  private CFANode entryNode;

  @Before
  public void init() throws InvalidConfigurationException {
    builder = JavaScriptCFABuilderFactory.createTestJavaScriptCFABuilder();
    parser = new EclipseJavaScriptParser(builder.getLogger());
    entryNode = builder.getExitNode();
  }

  private JavaScriptUnit createAST(final String pCode) {
    return (JavaScriptUnit) parser.createAST(builder.getBuilder().getFilename(), pCode);
  }

  @SuppressWarnings("unchecked")
  private InfixExpression parseInfixExpression(final String pCode) {
    final ExpressionStatement expressionStatement =
        (ExpressionStatement) createAST(pCode).statements().get(0);
    return (InfixExpression) expressionStatement.getExpression();
  }

  @Test
  public void testAnd() {
    testOperator("2 & 7", BinaryOperator.AND);
  }

  @Test
  public void testConditionalAnd() {
    final InfixExpression infixExpression = parseInfixExpression("lhs && rhs");
    // infix expression:
    //    left && right
    // expected side effect:
    //    var result
    //    if (left) {
    //      result = right
    //    } else {
    //      result = left
    //    }
    // expected result:
    //    result
    // expected CFA:   <entryNode> --{var result}--> () ----------------------\
    //                                                                        |
    //                /-------------{result = right}-- () <--[leftEvaluated]--/
    //   (result) <--{                                                       /
    //                \--{result = leftEvaluated}-- () <--[!leftEvaluated]--/

    final JSExpression left = mock(JSExpression.class, "left");
    when(left.getFileLocation()).thenReturn(FileLocation.DUMMY);
    final JSExpression right = mock(JSExpression.class, "right");
    final ExpressionAppendable expressionAppendable = mock(ExpressionAppendable.class);
    when(expressionAppendable.append(any(), any(SimpleName.class)))
        .thenAnswer(
            pInvocationOnMock -> {
              final SimpleName name = pInvocationOnMock.getArgument(1);
              switch (name.getIdentifier()) {
                case "lhs":
                  return left;
                case "rhs":
                  return right;
              }
              throw new RuntimeException("Unexpected SimpleName expression");
            });
    builder.setExpressionAppendable(expressionAppendable);

    final JSIdExpression result =
        (JSIdExpression) new InfixExpressionCFABuilder().append(builder, infixExpression);

    Truth.assertThat(result).isNotNull();
    Truth.assertThat(entryNode.getNumLeavingEdges()).isEqualTo(1);
    // assert expected side effect: var result
    final JSDeclarationEdge resultDeclarationEdge = (JSDeclarationEdge) entryNode.getLeavingEdge(0);
    final JSVariableDeclaration resultDeclaration =
        (JSVariableDeclaration) resultDeclarationEdge.getDeclaration();
    Truth.assertThat(resultDeclaration.getInitializer()).isEqualTo(null);

    // assert expected side effect:
    //    if (left) {
    //      result = right
    //    } else {
    //      result = left
    //    }
    final CFANode resultDeclarationEdgeSuccessor = resultDeclarationEdge.getSuccessor();
    Truth.assertThat(resultDeclarationEdgeSuccessor.getNumLeavingEdges()).isEqualTo(2);
    final JSAssumeEdge firstEdge = (JSAssumeEdge) resultDeclarationEdgeSuccessor.getLeavingEdge(0);
    final JSAssumeEdge secondEdge = (JSAssumeEdge) resultDeclarationEdgeSuccessor.getLeavingEdge(1);
    Truth.assertThat(firstEdge.getTruthAssumption()).isNotEqualTo(secondEdge.getTruthAssumption());
    final JSAssumeEdge thenEdge = firstEdge.getTruthAssumption() ? firstEdge : secondEdge;
    final JSAssumeEdge elseEdge = firstEdge.getTruthAssumption() ? secondEdge : firstEdge;

    // assert expected side effect:
    //    if (left) { ... } else { ... }
    Truth.assertThat(thenEdge.getTruthAssumption()).isTrue();
    Truth.assertThat(elseEdge.getTruthAssumption()).isFalse();
    Truth.assertThat(thenEdge.getExpression()).isEqualTo(left);
    Truth.assertThat(elseEdge.getExpression()).isEqualTo(left);

    // assert expected side effect:
    //      result = right
    final JSStatementEdge thenStatementEdge =
        (JSStatementEdge) thenEdge.getSuccessor().getLeavingEdge(0);
    final JSAssignment thenStatement = (JSAssignment) thenStatementEdge.getStatement();
    Truth.assertThat(((JSIdExpression) thenStatement.getLeftHandSide()).getDeclaration())
        .isEqualTo(resultDeclaration);
    Truth.assertThat(thenStatement.getRightHandSide()).isEqualTo(right);

    // assert expected side effect:
    //      result = left
    final JSStatementEdge elseStatementEdge =
        (JSStatementEdge) elseEdge.getSuccessor().getLeavingEdge(0);
    final JSAssignment elseStatement = (JSAssignment) elseStatementEdge.getStatement();
    Truth.assertThat(((JSIdExpression) elseStatement.getLeftHandSide()).getDeclaration())
        .isEqualTo(resultDeclaration);
    Truth.assertThat(elseStatement.getRightHandSide()).isEqualTo(left);

    // assert that conditional branches are joined in exit node
    final CFANode exitNode = builder.getExitNode();
    Truth.assertThat(exitNode.getNumEnteringEdges()).isEqualTo(2);
    final CFAEdge thenStatementExitEdge = thenStatementEdge.getSuccessor().getLeavingEdge(0);
    Truth.assertThat(thenStatementExitEdge.getSuccessor()).isEqualTo(exitNode);
    Truth.assertThat(thenStatementExitEdge.getDescription()).isEqualTo("end true && rhs");
    final CFAEdge elseStatementExitEdge = elseStatementEdge.getSuccessor().getLeavingEdge(0);
    Truth.assertThat(elseStatementExitEdge.getSuccessor()).isEqualTo(exitNode);
    Truth.assertThat(elseStatementExitEdge.getDescription()).isEqualTo("end false && rhs");

    // assert expected result
    Truth.assertThat(result.getDeclaration()).isEqualTo(resultDeclaration);
  }

  @Test
  public void testConditionalOr() {
    final InfixExpression infixExpression = parseInfixExpression("lhs || rhs");
    // infix expression:
    //    left || right
    // expected side effect:
    //    var result
    //    if (left) {
    //      result = left
    //    } else {
    //      result = right
    //    }
    // expected result:
    //    result
    // expected CFA:   <entryNode> --{var result}--> () -----\
    //                                                       |
    //                /------{result = left}-- () <--[left]--/
    //   (result) <--{                                      /
    //                \--{result = right}-- () <--[!left]--/

    final JSExpression left = mock(JSExpression.class, "left");
    when(left.getFileLocation()).thenReturn(FileLocation.DUMMY);
    final JSExpression right = mock(JSExpression.class, "right");
    final ExpressionAppendable expressionAppendable = mock(ExpressionAppendable.class);
    when(expressionAppendable.append(any(), any(SimpleName.class)))
        .thenAnswer(
            pInvocationOnMock -> {
              final SimpleName name = pInvocationOnMock.getArgument(1);
              switch (name.getIdentifier()) {
                case "lhs":
                  return left;
                case "rhs":
                  return right;
              }
              throw new RuntimeException("Unexpected SimpleName expression");
            });
    builder.setExpressionAppendable(expressionAppendable);

    final JSIdExpression result =
        (JSIdExpression) new InfixExpressionCFABuilder().append(builder, infixExpression);

    Truth.assertThat(result).isNotNull();
    Truth.assertThat(entryNode.getNumLeavingEdges()).isEqualTo(1);
    // assert expected side effect: var result
    final JSDeclarationEdge resultDeclarationEdge = (JSDeclarationEdge) entryNode.getLeavingEdge(0);
    final JSVariableDeclaration resultDeclaration =
        (JSVariableDeclaration) resultDeclarationEdge.getDeclaration();
    Truth.assertThat(resultDeclaration.getInitializer()).isEqualTo(null);

    // assert expected side effect:
    //    if (left) {
    //      result = left
    //    } else {
    //      result = right
    //    }
    final CFANode resultDeclarationEdgeSuccessor = resultDeclarationEdge.getSuccessor();
    Truth.assertThat(resultDeclarationEdgeSuccessor.getNumLeavingEdges()).isEqualTo(2);
    final JSAssumeEdge firstEdge = (JSAssumeEdge) resultDeclarationEdgeSuccessor.getLeavingEdge(0);
    final JSAssumeEdge secondEdge = (JSAssumeEdge) resultDeclarationEdgeSuccessor.getLeavingEdge(1);
    Truth.assertThat(firstEdge.getTruthAssumption()).isNotEqualTo(secondEdge.getTruthAssumption());
    final JSAssumeEdge thenEdge = firstEdge.getTruthAssumption() ? firstEdge : secondEdge;
    final JSAssumeEdge elseEdge = firstEdge.getTruthAssumption() ? secondEdge : firstEdge;

    // assert expected side effect:
    //    if (left) { ... } else { ... }
    Truth.assertThat(thenEdge.getTruthAssumption()).isTrue();
    Truth.assertThat(elseEdge.getTruthAssumption()).isFalse();
    Truth.assertThat(thenEdge.getExpression()).isEqualTo(left);
    Truth.assertThat(elseEdge.getExpression()).isEqualTo(left);

    // assert expected side effect:
    //      result = left
    final JSStatementEdge thenStatementEdge =
        (JSStatementEdge) thenEdge.getSuccessor().getLeavingEdge(0);
    final JSAssignment thenStatement = (JSAssignment) thenStatementEdge.getStatement();
    Truth.assertThat(((JSIdExpression) thenStatement.getLeftHandSide()).getDeclaration())
        .isEqualTo(resultDeclaration);
    Truth.assertThat(thenStatement.getRightHandSide()).isEqualTo(left);

    // assert expected side effect:
    //      result = right
    final JSStatementEdge elseStatementEdge =
        (JSStatementEdge) elseEdge.getSuccessor().getLeavingEdge(0);
    final JSAssignment elseStatement = (JSAssignment) elseStatementEdge.getStatement();
    Truth.assertThat(((JSIdExpression) elseStatement.getLeftHandSide()).getDeclaration())
        .isEqualTo(resultDeclaration);
    Truth.assertThat(elseStatement.getRightHandSide()).isEqualTo(right);

    // assert that conditional branches are joined in exit node
    final CFANode exitNode = builder.getExitNode();
    Truth.assertThat(exitNode.getNumEnteringEdges()).isEqualTo(2);
    final CFAEdge thenStatementExitEdge = thenStatementEdge.getSuccessor().getLeavingEdge(0);
    Truth.assertThat(thenStatementExitEdge.getSuccessor()).isEqualTo(exitNode);
    Truth.assertThat(thenStatementExitEdge.getDescription()).isEqualTo("end true || rhs");
    final CFAEdge elseStatementExitEdge = elseStatementEdge.getSuccessor().getLeavingEdge(0);
    Truth.assertThat(elseStatementExitEdge.getSuccessor()).isEqualTo(exitNode);
    Truth.assertThat(elseStatementExitEdge.getDescription()).isEqualTo("end false || rhs");

    // assert expected result
    Truth.assertThat(result.getDeclaration()).isEqualTo(resultDeclaration);
  }

  @Test
  public void testDivide() {
    testOperator("1 / 2", BinaryOperator.DIVIDE);
  }

  @Test
  public void testEquals() {
    testOperator("1 == 2", BinaryOperator.EQUALS);
  }

  @Test
  public void testEqualEqualEqual() {
    testOperator("1 === 1", BinaryOperator.EQUAL_EQUAL_EQUAL);
  }

  @Test
  public void testGreater() {
    testOperator("1 > 2", BinaryOperator.GREATER);
  }

  @Test
  public void testGreaterEquals() {
    testOperator("1 >= 2", BinaryOperator.GREATER_EQUALS);
  }

  @Test
  public void testIn() {
    testOperator("1 in []", BinaryOperator.IN);
  }

  @Test
  public void testInstanceOf() {
    testOperator("[] instanceof Object", BinaryOperator.INSTANCEOF);
  }

  @Test
  public void testLeftShift() {
    testOperator("8 << 2", BinaryOperator.LEFT_SHIFT);
  }

  @Test
  public void testLess() {
    testOperator("1 < 2", BinaryOperator.LESS);
  }

  @Test
  public void testLessEquals() {
    testOperator("1 <= 2", BinaryOperator.LESS_EQUALS);
  }

  @Test
  public void testMinus() {
    testOperator("1 - 2", BinaryOperator.MINUS);
  }

  @Test
  public void testNotEqualEqual() {
    testOperator("1 !== 2", BinaryOperator.NOT_EQUAL_EQUAL);
  }

  @Test
  public void testNotEquals() {
    testOperator("1 != 2", BinaryOperator.NOT_EQUALS);
  }

  @Test
  public void testOr() {
    testOperator("1 | 2", BinaryOperator.OR);
  }

  @Test
  public void testPlus() {
    testOperator("1 + 2", BinaryOperator.PLUS);
  }

  @Test
  public void testRemainder() {
    testOperator("1 % 2", BinaryOperator.REMAINDER);
  }

  @Test
  public void testRightShiftSigned() {
    testOperator("1 >> 2", BinaryOperator.RIGHT_SHIFT_SIGNED);
  }

  @Test
  public void testRightShiftUnsigned() {
    testOperator("1 >>> 2", BinaryOperator.RIGHT_SHIFT_UNSIGNED);
  }

  @Test
  public void testTimes() {
    testOperator("1 * 2", BinaryOperator.TIMES);
  }

  @Test
  public void testXor() {
    testOperator("1 ^ 2", BinaryOperator.XOR);
  }

  // shared code for operators
  private void testOperator(final String pCode, final BinaryOperator pExpectedOperator) {
    // infix expression:
    //    left + right
    // expected side effect:
    //    var tmpLeft = left
    //    var tmpRight = right
    // expected result:
    //    tmpLeft + tmpRight
    final InfixExpression infixExpression = parseInfixExpression(pCode);

    final JSExpression left = mock(JSExpression.class);
    final JSExpression right = mock(JSExpression.class);
    final ExpressionAppendable expressionAppendable = mock(ExpressionAppendable.class);
    when(expressionAppendable.append(any(), any())).thenReturn(left, right);
    builder.setExpressionAppendable(expressionAppendable);

    final JSBinaryExpression result =
        (JSBinaryExpression) new InfixExpressionCFABuilder().append(builder, infixExpression);

    Truth.assertThat(result).isNotNull();
    Truth.assertThat(entryNode.getNumLeavingEdges()).isEqualTo(1);
    // assert expected side effect: var tmpLeft = left
    final JSDeclarationEdge leftDeclarationEdge = (JSDeclarationEdge) entryNode.getLeavingEdge(0);
    final JSVariableDeclaration leftDeclaration =
        (JSVariableDeclaration) leftDeclarationEdge.getDeclaration();
    Truth.assertThat(((JSInitializerExpression) leftDeclaration.getInitializer()).getExpression())
        .isEqualTo(left);
    // assert expected side effect: var tmpRight = right
    Truth.assertThat(leftDeclarationEdge.getSuccessor().getNumLeavingEdges()).isEqualTo(1);
    final JSDeclarationEdge rightDeclarationEdge =
        (JSDeclarationEdge) leftDeclarationEdge.getSuccessor().getLeavingEdge(0);
    final JSVariableDeclaration rightDeclaration =
        (JSVariableDeclaration) rightDeclarationEdge.getDeclaration();
    Truth.assertThat(((JSInitializerExpression) rightDeclaration.getInitializer()).getExpression())
        .isEqualTo(right);
    Truth.assertThat(rightDeclarationEdge.getSuccessor().getNumLeavingEdges()).isEqualTo(0);
    // assert result: tmpLeft + tmpRight
    Truth.assertThat(leftDeclaration.getName())
        .isEqualTo(((JSIdExpression) result.getOperand1()).getName());
    Truth.assertThat(rightDeclaration.getName())
        .isEqualTo(((JSIdExpression) result.getOperand2()).getName());
    Truth.assertThat(result.getOperator()).isEqualTo(pExpectedOperator);
  }
}
