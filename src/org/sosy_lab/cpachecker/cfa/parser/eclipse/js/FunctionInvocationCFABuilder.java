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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.js.JSStatementEdge;

@SuppressWarnings("ResultOfMethodCallIgnored")
class FunctionInvocationCFABuilder implements FunctionInvocationAppendable {

  private final JSFunctionDeclaration unknownFunctionCallerDeclaration;
  private final JSIdExpression unknownFunctionCallerId;

  FunctionInvocationCFABuilder(final JSFunctionDeclaration pUnknownFunctionCallerDeclaration) {
    unknownFunctionCallerDeclaration = pUnknownFunctionCallerDeclaration;
    unknownFunctionCallerId =
        new JSIdExpression(FileLocation.DUMMY, unknownFunctionCallerDeclaration);
  }

  @Override
  public JSExpression append(final JavaScriptCFABuilder pBuilder, final FunctionInvocation pNode) {
    final JSIdExpression function =
        pNode.getName() != null
            ? pBuilder.resolve(pNode.getName())
            : (JSIdExpression) pBuilder.append(pNode.getExpression());
    final boolean isKnownFunctionDeclaration =
        function.getDeclaration() instanceof JSFunctionDeclaration;
    final JSFunctionDeclaration declaration =
        isKnownFunctionDeclaration
            ? (JSFunctionDeclaration) function.getDeclaration()
            : unknownFunctionCallerDeclaration;
    final List<JSExpression> arguments = new ArrayList<>();
    if (!isKnownFunctionDeclaration) {
      arguments.add(function); // function is called by unknown function caller
    }
    appendArguments(pBuilder, pNode, arguments);
    final JSVariableDeclaration resultVariableDeclaration = pBuilder.declareVariable();
    final JSIdExpression resultVariableId =
        new JSIdExpression(FileLocation.DUMMY, resultVariableDeclaration);
    pBuilder.appendEdge(
        JSStatementEdge.of(
            new JSFunctionCallAssignmentStatement(
                pBuilder.getFileLocation(pNode),
                resultVariableId,
                new JSFunctionCallExpression(
                    pBuilder.getFileLocation(pNode),
                    isKnownFunctionDeclaration ? function : unknownFunctionCallerId,
                    arguments,
                    declaration,
                    Optional.empty()))));
    return resultVariableId;
  }

  @SuppressWarnings("unchecked")
  private void appendArguments(
      final JavaScriptCFABuilder pBuilder,
      final FunctionInvocation pNode,
      final List<JSExpression> pArguments) {
    ((List<Expression>) pNode.arguments())
        .stream()
        .map(argument -> pBuilder.append(argument))
        .forEachOrdered(pArguments::add);
  }
}
