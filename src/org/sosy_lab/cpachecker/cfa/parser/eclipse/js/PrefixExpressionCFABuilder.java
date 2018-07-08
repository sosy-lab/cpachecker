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

import java.math.BigInteger;
import org.eclipse.wst.jsdt.core.dom.PrefixExpression;
import org.eclipse.wst.jsdt.core.dom.PrefixExpression.Operator;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.js.JSStatementEdge;

@SuppressWarnings("ResultOfMethodCallIgnored")
class PrefixExpressionCFABuilder implements PrefixExpressionAppendable {

  @Override
  public JSExpression append(
      final JavaScriptCFABuilder pBuilder, final PrefixExpression pPrefixExpression) {
    final JSExpression operand = pBuilder.append(pPrefixExpression.getOperand());
    if (PrefixExpression.Operator.INCREMENT == pPrefixExpression.getOperator()
        || PrefixExpression.Operator.DECREMENT == pPrefixExpression.getOperator()) {
      final BinaryOperator binaryOperator =
          pPrefixExpression.getOperator() == Operator.INCREMENT
              ? BinaryOperator.PLUS
              : BinaryOperator.MINUS;
      final JSIdExpression variableToIncrement = (JSIdExpression) operand;
      final JSExpressionAssignmentStatement incrementStatement =
          new JSExpressionAssignmentStatement(
              FileLocation.DUMMY,
              variableToIncrement,
              new JSBinaryExpression(
                  FileLocation.DUMMY,
                  variableToIncrement,
                  new JSIntegerLiteralExpression(FileLocation.DUMMY, BigInteger.ONE),
                  binaryOperator));
      pBuilder.appendEdge(
          (pPredecessor, pSuccessor) ->
              new JSStatementEdge(
                  incrementStatement.toASTString(),
                  incrementStatement,
                  incrementStatement.getFileLocation(),
                  pPredecessor,
                  pSuccessor));
      return variableToIncrement;
    }
    final UnaryOperator operator = convert(pPrefixExpression.getOperator());
    switch (operator) {
      case NOT:
      case PLUS:
      case MINUS:
      case COMPLEMENT:
      case DELETE:
      case TYPE_OF:
      case VOID:
        return new JSUnaryExpression(
            pBuilder.getFileLocation(pPrefixExpression), operand, operator);
    }
    throw new CFAGenerationRuntimeException(
        "Unknown kind of unary operator (not handled yet): " + operator, pPrefixExpression);
  }

  private UnaryOperator convert(final PrefixExpression.Operator pOperator) {
    if (PrefixExpression.Operator.PLUS == pOperator) {
      return UnaryOperator.PLUS;
    } else if (PrefixExpression.Operator.MINUS == pOperator) {
      return UnaryOperator.MINUS;
    } else if (PrefixExpression.Operator.COMPLEMENT == pOperator) {
      return UnaryOperator.COMPLEMENT;
    } else if (PrefixExpression.Operator.NOT == pOperator) {
      return UnaryOperator.NOT;
    } else if (PrefixExpression.Operator.DELETE == pOperator) {
      return UnaryOperator.DELETE;
    } else if (PrefixExpression.Operator.TYPE_OF == pOperator) {
      return UnaryOperator.TYPE_OF;
    } else if (PrefixExpression.Operator.VOID == pOperator) {
      return UnaryOperator.VOID;
    }
    throw new CFAGenerationRuntimeException(
        "Unknown kind of unary operator (not handled yet): " + pOperator);
  }
}
