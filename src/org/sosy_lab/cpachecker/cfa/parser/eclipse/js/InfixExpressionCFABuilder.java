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

import org.eclipse.wst.jsdt.core.dom.InfixExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.js.JSDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.js.JSAnyType;

class InfixExpressionCFABuilder implements InfixExpressionAppendable {

  @Override
  public JSExpression append(
      final JavaScriptCFABuilder pBuilder, final InfixExpression pInfixExpression) {
    // infix expression:
    //    left + right
    // expected side effect:
    //    var tmpLeft = left
    //    var tmpRight = right
    // expected result:
    //    tmpLeft + tmpRight
    final ASTConverter converter = pBuilder.getAstConverter();
    final JSExpression leftOperand = pBuilder.append(pInfixExpression.getLeftOperand());
    final String tmpLeftVariableName = "tmpLeft"; // TODO generate
    final JSVariableDeclaration tmpLeftVariableDeclaration =
        new JSVariableDeclaration(
            FileLocation.DUMMY,
            false,
            JSAnyType.ANY,
            tmpLeftVariableName,
            tmpLeftVariableName,
            tmpLeftVariableName,
            new JSInitializerExpression(FileLocation.DUMMY, leftOperand));
    final JSExpression rightOperand = pBuilder.append(pInfixExpression.getRightOperand());
    final String tmpRightVariableName = "tmpRight"; // TODO generate
    final JSVariableDeclaration tmpRightVariableDeclaration =
        new JSVariableDeclaration(
            FileLocation.DUMMY,
            false,
            JSAnyType.ANY,
            tmpRightVariableName,
            tmpRightVariableName,
            tmpRightVariableName,
            new JSInitializerExpression(FileLocation.DUMMY, rightOperand));
    pBuilder
        .appendEdge(
            (pPredecessor, pSuccessor) ->
                new JSDeclarationEdge(
                    tmpLeftVariableDeclaration.toASTString(),
                    FileLocation.DUMMY,
                    pPredecessor,
                    pSuccessor,
                    tmpLeftVariableDeclaration))
        .appendEdge(
            (pPredecessor, pSuccessor) ->
                new JSDeclarationEdge(
                    tmpRightVariableDeclaration.toASTString(),
                    FileLocation.DUMMY,
                    pPredecessor,
                    pSuccessor,
                    tmpRightVariableDeclaration));
    final BinaryOperator binaryOperator = converter.convert(pInfixExpression.getOperator());
    return new JSBinaryExpression(
        FileLocation.DUMMY,
        JSAnyType.ANY,
        JSAnyType.ANY,
        new JSIdExpression(
            FileLocation.DUMMY, JSAnyType.ANY, tmpLeftVariableName, tmpLeftVariableDeclaration),
        new JSIdExpression(
            FileLocation.DUMMY, JSAnyType.ANY, tmpRightVariableName, tmpRightVariableDeclaration),
        binaryOperator);
  }
}
