/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSAssignment;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.js.JSStatementEdge;

public class AssignmentCFABuilderTest extends CFABuilderTestBase {

  @Test
  public void testVariableAssignment() {
    final Assignment assignmentExpression = parseExpression(Assignment.class, "x = 42");

    final String expectedVariableName = "x";
    final JSIdExpression variableId =
        new JSIdExpression(
            FileLocation.DUMMY,
            expectedVariableName,
            mock(JSSimpleDeclaration.class));

    final JSIntegerLiteralExpression expectedInitializerExpression =
        new JSIntegerLiteralExpression(FileLocation.DUMMY, BigInteger.valueOf(42));
    // expected CFA: entryNode -{x = 42}-> ()

    builder.setExpressionAppendable(
        (pBuilder, pExpression) ->
            pExpression instanceof SimpleName ? variableId : expectedInitializerExpression);

    final JSIdExpression result =
        (JSIdExpression) new AssignmentCFABuilder().append(builder, assignmentExpression);

    Truth.assertThat(result).isEqualTo(variableId);
    final JSStatementEdge assignmentEdge = (JSStatementEdge) entryNode.getLeavingEdge(0);
    final JSAssignment variableAssignment = (JSAssignment) assignmentEdge.getStatement();
    Truth.assertThat(variableAssignment.getLeftHandSide()).isEqualTo(variableId);
    Truth.assertThat(variableAssignment.getRightHandSide())
        .isEqualTo(expectedInitializerExpression);
    Truth.assertThat(assignmentEdge.getSuccessor().getNumLeavingEdges()).isEqualTo(0);
  }

  @Test
  public void testVariableTimesOperatorAssignment() {
    testVariableOperatorAssignment("x *= 42", BinaryOperator.TIMES);
  }

  @Test
  public void testVariableDivideOperatorAssignment() {
    testVariableOperatorAssignment("x /= 42", BinaryOperator.DIVIDE);
  }

  @Test
  public void testVariableRemainderOperatorAssignment() {
    testVariableOperatorAssignment("x %= 42", BinaryOperator.REMAINDER);
  }

  @Test
  public void testVariablePlusOperatorAssignment() {
    testVariableOperatorAssignment("x += 42", BinaryOperator.PLUS);
  }

  @Test
  public void testVariableMinusOperatorAssignment() {
    testVariableOperatorAssignment("x -= 42", BinaryOperator.MINUS);
  }

  @Test
  public void testVariableLeftShiftOperatorAssignment() {
    testVariableOperatorAssignment("x <<= 42", BinaryOperator.LEFT_SHIFT);
  }

  @Test
  public void testVariableRightShiftSignedOperatorAssignment() {
    testVariableOperatorAssignment("x >>= 42", BinaryOperator.RIGHT_SHIFT_SIGNED);
  }

  @Test
  public void testVariableRightShiftUnsignedOperatorAssignment() {
    testVariableOperatorAssignment("x >>>= 42", BinaryOperator.RIGHT_SHIFT_UNSIGNED);
  }

  @Test
  public void testVariableAndOperatorAssignment() {
    testVariableOperatorAssignment("x &= 42", BinaryOperator.AND);
  }

  @Test
  public void testVariableXorOperatorAssignment() {
    testVariableOperatorAssignment("x ^= 42", BinaryOperator.XOR);
  }

  @Test
  public void testVariableOrOperatorAssignment() {
    testVariableOperatorAssignment("x |= 42", BinaryOperator.OR);
  }

  private void testVariableOperatorAssignment(
      final String pCode, final BinaryOperator pExpectedOperator) {
    final Assignment assignmentExpression = parseExpression(Assignment.class, pCode);

    final String expectedVariableName = "x";
    final JSIdExpression variableId =
        new JSIdExpression(
            FileLocation.DUMMY, expectedVariableName, mock(JSSimpleDeclaration.class));

    final JSExpression assignmentInitializerExpression =
        new JSIntegerLiteralExpression(FileLocation.DUMMY, BigInteger.valueOf(42));
    // expected CFA: entryNode -{x = x + 42}-> ()

    builder.setExpressionAppendable(
        (pBuilder, pExpression) ->
            pExpression instanceof SimpleName ? variableId : assignmentInitializerExpression);

    final JSIdExpression result =
        (JSIdExpression) new AssignmentCFABuilder().append(builder, assignmentExpression);

    Truth.assertThat(result).isEqualTo(variableId);
    final JSStatementEdge assignmentEdge = (JSStatementEdge) entryNode.getLeavingEdge(0);
    final JSAssignment variableAssignment = (JSAssignment) assignmentEdge.getStatement();
    Truth.assertThat(variableAssignment.getLeftHandSide()).isEqualTo(variableId);
    Truth.assertThat(variableAssignment.getRightHandSide())
        .isEqualTo(
            new JSBinaryExpression(
                FileLocation.DUMMY,
                variableId,
                assignmentInitializerExpression,
                pExpectedOperator));
    Truth.assertThat(assignmentEdge.getSuccessor().getNumLeavingEdges()).isEqualTo(0);
  }
}
