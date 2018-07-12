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
import org.eclipse.wst.jsdt.core.dom.PostfixExpression;
import org.eclipse.wst.jsdt.core.dom.PostfixExpression.Operator;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.js.JSDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.js.JSStatementEdge;

@SuppressWarnings("ResultOfMethodCallIgnored")
class PostfixExpressionCFABuilder implements PostfixExpressionAppendable {

  @Override
  public JSExpression append(
      final JavaScriptCFABuilder pBuilder, final PostfixExpression pPostfixExpression) {
    // postfix expression:
    //    x++
    // side effect:
    //    var tmp = x
    //    x = x + 1
    // result:
    //    tmp
    final BinaryOperator binaryOperator = getBinaryOperator(pPostfixExpression);
    final JSIdExpression variableToIncrement =
        (JSIdExpression) pBuilder.append(pPostfixExpression.getOperand());
    final JSVariableDeclaration oldValueVariableDeclaration =
        pBuilder.declareVariable(
            new JSInitializerExpression(FileLocation.DUMMY, variableToIncrement));
    final JSExpressionAssignmentStatement incrementStatement =
        new JSExpressionAssignmentStatement(
            FileLocation.DUMMY,
            variableToIncrement,
            new JSBinaryExpression(
                FileLocation.DUMMY,
                variableToIncrement,
                new JSIntegerLiteralExpression(FileLocation.DUMMY, BigInteger.ONE),
                binaryOperator));
    pBuilder
        .appendEdge(JSDeclarationEdge.of(oldValueVariableDeclaration))
        .appendEdge(
            (pPredecessor, pSuccessor) ->
                new JSStatementEdge(
                    incrementStatement.toASTString(),
                    incrementStatement,
                    incrementStatement.getFileLocation(),
                    pPredecessor,
                    pSuccessor));
    return new JSIdExpression(FileLocation.DUMMY, oldValueVariableDeclaration);
  }

  private BinaryOperator getBinaryOperator(final PostfixExpression pPostfixExpression) {
    final Operator postfixOperator = pPostfixExpression.getOperator();
    if (postfixOperator.equals(Operator.INCREMENT)) {
      return BinaryOperator.PLUS;
    } else if (postfixOperator.equals(Operator.DECREMENT)) {
      return BinaryOperator.MINUS;
    }
    throw new CFAGenerationRuntimeException(
        "Unknown kind of postfix operator (not handled yet): " + postfixOperator,
        pPostfixExpression);
  }
}
