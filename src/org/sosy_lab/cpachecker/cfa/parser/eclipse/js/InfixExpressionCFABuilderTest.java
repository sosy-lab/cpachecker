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
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.js.JSDeclarationEdge;
import org.sosy_lab.cpachecker.exceptions.ParserException;

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
  public void testAnd() throws ParserException {
    testOperator("2 & 7", BinaryOperator.AND);
  }

  @Test
  public void testConditionalAnd() throws ParserException {
    testOperator("true && false", BinaryOperator.CONDITIONAL_AND);
  }

  @Test
  public void testConditionalOr() throws ParserException {
    testOperator("true || false", BinaryOperator.CONDITIONAL_OR);
  }

  @Test
  public void testDivide() throws ParserException {
    testOperator("1 / 2", BinaryOperator.DIVIDE);
  }

  @Test
  public void testEquals() throws ParserException {
    testOperator("1 == 2", BinaryOperator.EQUALS);
  }

  @Test
  public void testEqualEqualEqual() throws ParserException {
    testOperator("1 === 1", BinaryOperator.EQUAL_EQUAL_EQUAL);
  }

  @Test
  public void testGreater() throws ParserException {
    testOperator("1 > 2", BinaryOperator.GREATER);
  }

  @Test
  public void testGreaterEquals() throws ParserException {
    testOperator("1 >= 2", BinaryOperator.GREATER_EQUALS);
  }

  @Test
  public void testIn() throws ParserException {
    testOperator("1 in []", BinaryOperator.IN);
  }

  @Test
  public void testInstanceOf() throws ParserException {
    testOperator("[] instanceof Object", BinaryOperator.INSTANCEOF);
  }

  @Test
  public void testLeftShift() throws ParserException {
    testOperator("8 << 2", BinaryOperator.LEFT_SHIFT);
  }

  @Test
  public void testLess() throws ParserException {
    testOperator("1 < 2", BinaryOperator.LESS);
  }

  @Test
  public void testLessEquals() throws ParserException {
    testOperator("1 <= 2", BinaryOperator.LESS_EQUALS);
  }

  @Test
  public void testMinus() throws ParserException {
    testOperator("1 - 2", BinaryOperator.MINUS);
  }

  @Test
  public void testNotEqualEqual() throws ParserException {
    testOperator("1 !== 2", BinaryOperator.NOT_EQUAL_EQUAL);
  }

  @Test
  public void testNotEquals() throws ParserException {
    testOperator("1 != 2", BinaryOperator.NOT_EQUALS);
  }

  @Test
  public void testOr() throws ParserException {
    testOperator("1 | 2", BinaryOperator.OR);
  }

  @Test
  public void testPlus() throws ParserException {
    testOperator("1 + 2", BinaryOperator.PLUS);
  }

  @Test
  public void testRemainder() throws ParserException {
    testOperator("1 % 2", BinaryOperator.REMAINDER);
  }

  @Test
  public void testRightShiftSigned() throws ParserException {
    testOperator("1 >> 2", BinaryOperator.RIGHT_SHIFT_SIGNED);
  }

  @Test
  public void testRightShiftUnsigned() throws ParserException {
    testOperator("1 >>> 2", BinaryOperator.RIGHT_SHIFT_UNSIGNED);
  }

  @Test
  public void testTimes() throws ParserException {
    testOperator("1 * 2", BinaryOperator.TIMES);
  }

  @Test
  public void testXor() throws ParserException {
    testOperator("1 ^ 2", BinaryOperator.XOR);
  }

  // shared code for operators
  private void testOperator(final String pCode, final BinaryOperator pExpectedOperator)
      throws ParserException {
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
