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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.eclipse.wst.jsdt.core.dom.Assignment;
import org.eclipse.wst.jsdt.core.dom.Assignment.Operator;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.model.js.JSStatementEdge;

@SuppressWarnings("ResultOfMethodCallIgnored")
class AssignmentCFABuilder implements AssignmentAppendable {

  @Override
  public JSExpression append(final JavaScriptCFABuilder pBuilder, final Assignment pAssignment) {
    // TODO handle other left hand side expressions like field access
    final JSIdExpression varId = (JSIdExpression) pBuilder.append(pAssignment.getLeftHandSide());
    final JSExpression value = pBuilder.append(pAssignment.getRightHandSide());
    final Optional<BinaryOperator> operator = binaryOperatorOf(pAssignment.getOperator());
    final JSExpression newValue =
        operator.isPresent()
            ? new JSBinaryExpression(FileLocation.DUMMY, varId, value, operator.get())
            : value;
    final JSExpressionAssignmentStatement assignmentStatement =
        new JSExpressionAssignmentStatement(FileLocation.DUMMY, varId, newValue);
    pBuilder.appendEdge(
        (pPredecessor, pSuccessor) ->
            new JSStatementEdge(
                assignmentStatement.toASTString(),
                assignmentStatement,
                assignmentStatement.getFileLocation(),
                pPredecessor,
                pSuccessor));
    return varId;
  }

  private static final @Nonnull Map<Operator, BinaryOperator> assignmentOperatorToBinaryOperator;

  static {
    final Map<Operator, BinaryOperator> m = new HashMap<>();
    m.put(Operator.TIMES_ASSIGN, BinaryOperator.TIMES);
    m.put(Operator.DIVIDE_ASSIGN, BinaryOperator.DIVIDE);
    m.put(Operator.REMAINDER_ASSIGN, BinaryOperator.REMAINDER);
    m.put(Operator.PLUS_ASSIGN, BinaryOperator.PLUS);
    m.put(Operator.MINUS_ASSIGN, BinaryOperator.MINUS);
    m.put(Operator.LEFT_SHIFT_ASSIGN, BinaryOperator.LEFT_SHIFT);
    m.put(Operator.RIGHT_SHIFT_SIGNED_ASSIGN, BinaryOperator.RIGHT_SHIFT_SIGNED);
    m.put(Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN, BinaryOperator.RIGHT_SHIFT_UNSIGNED);
    m.put(Operator.BIT_AND_ASSIGN, BinaryOperator.AND);
    m.put(Operator.BIT_XOR_ASSIGN, BinaryOperator.XOR);
    m.put(Operator.BIT_OR_ASSIGN, BinaryOperator.OR);
    assignmentOperatorToBinaryOperator = Collections.unmodifiableMap(m);
  }

  private static Optional<BinaryOperator> binaryOperatorOf(final Operator pOperator) {
    if (pOperator.equals(Operator.ASSIGN)) {
      return Optional.empty();
    }
    assert assignmentOperatorToBinaryOperator.containsKey(pOperator)
        : "Unhandled assignment operator " + pOperator;
    return Optional.of(assignmentOperatorToBinaryOperator.get(pOperator));
  }
}
