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

import static org.mockito.Mockito.mock;

import com.google.common.truth.Truth;
import java.math.BigInteger;
import org.eclipse.wst.jsdt.core.dom.PrefixExpression;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSAssignment;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.js.JSStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.js.JSAnyType;

public class PrefixExpressionCFABuilderTest extends CFABuilderTestBase {

  @Test
  public final void testOperatorsWithoutSideEffect() {
    testOperatorsWithoutSideEffect("!true", UnaryOperator.NOT);
    testOperatorsWithoutSideEffect("+1", UnaryOperator.PLUS);
    testOperatorsWithoutSideEffect("-1", UnaryOperator.MINUS);
    testOperatorsWithoutSideEffect("~1", UnaryOperator.COMPLEMENT);
  }

  private void testOperatorsWithoutSideEffect(
      final String pPrefixExpressionCode, final UnaryOperator pExpectedUnaryOperator) {
    final PrefixExpression prefixExpression =
        parseExpression(PrefixExpression.class, pPrefixExpressionCode);

    final JSExpression expectedOperand = mock(JSExpression.class);
    builder.setExpressionAppendable(
        (pBuilder, pExpression) -> {
          Truth.assertThat(pBuilder).isEqualTo(builder);
          return expectedOperand;
        });

    final JSUnaryExpression result =
        (JSUnaryExpression) new PrefixExpressionCFABuilder().append(builder, prefixExpression);

    Truth.assertThat(result).isNotNull();
    Truth.assertThat(result.getOperator()).isEqualTo(pExpectedUnaryOperator);
    Truth.assertThat(result.getOperand()).isEqualTo(expectedOperand);
    Truth.assertThat(entryNode.getNumLeavingEdges()).isEqualTo(0);
  }

  @Test
  public void testIncrement() {
    testOperatorWithSideEffect("++x", BinaryOperator.PLUS);
  }

  @Test
  public void testDecrement() {
    testOperatorWithSideEffect("--x", BinaryOperator.MINUS);
  }

  // shared code for increment and decrement
  private void testOperatorWithSideEffect(
      final String pCode, final BinaryOperator pExpectedOperator) {
    final PrefixExpression prefixExpression = parseExpression(PrefixExpression.class, pCode);

    final JSIdExpression variableId =
        new JSIdExpression(FileLocation.DUMMY, JSAnyType.ANY, "x", mock(JSSimpleDeclaration.class));
    builder.setExpressionAppendable(
        (pBuilder, pExpression) -> {
          Truth.assertThat(pBuilder).isEqualTo(builder);
          return variableId;
        });

    // prefix expression:
    //    ++x
    // expected side effect:
    //    x = x + 1
    // expected result:
    //    x

    final JSIdExpression result =
        (JSIdExpression) new PrefixExpressionCFABuilder().append(builder, prefixExpression);

    Truth.assertThat(result).isEqualTo(variableId);
    Truth.assertThat(entryNode.getNumLeavingEdges()).isEqualTo(1);
    final JSStatementEdge incrementStatementEdge = (JSStatementEdge) entryNode.getLeavingEdge(0);
    final JSAssignment incrementStatement = (JSAssignment) incrementStatementEdge.getStatement();
    Truth.assertThat(incrementStatement.getLeftHandSide()).isEqualTo(variableId);
    final JSBinaryExpression incrementExpression =
        (JSBinaryExpression) incrementStatement.getRightHandSide();
    Truth.assertThat(incrementExpression.getOperator()).isEqualTo(pExpectedOperator);
    Truth.assertThat(incrementExpression.getOperand1()).isEqualTo(variableId);
    Truth.assertThat(incrementExpression.getOperand2())
        .isEqualTo(new JSIntegerLiteralExpression(FileLocation.DUMMY, BigInteger.ONE));
    Truth.assertThat(incrementStatementEdge.getSuccessor().getNumLeavingEdges()).isEqualTo(0);
  }

}
