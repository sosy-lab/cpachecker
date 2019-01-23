/*
 *  CPAchecker is a tool for configurable software verification.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.eclipse.wst.jsdt.core.dom.ClassInstanceCreation;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFieldAccess;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.js.JSStatementEdge;

// TODO share code with FunctionInvocationCFABuilder

@SuppressWarnings("ResultOfMethodCallIgnored")
class ClassInstanceCreationCFABuilder implements ClassInstanceCreationAppendable {

  private final JSFunctionDeclaration unknownFunctionCallerDeclaration;
  private final JSIdExpression unknownFunctionCallerId;

  ClassInstanceCreationCFABuilder(
      final JSFunctionDeclaration pUnknownConstructorCallerDeclaration) {
    unknownFunctionCallerDeclaration = pUnknownConstructorCallerDeclaration;
    unknownFunctionCallerId =
        new JSIdExpression(FileLocation.DUMMY, unknownFunctionCallerDeclaration);
  }

  @Override
  public JSExpression append(
      final JavaScriptCFABuilder pBuilder, final ClassInstanceCreation pNode) {
    final JSExpression function =
        pNode.getMember() != null
            ? pNode.getMember() instanceof SimpleName
                ? pBuilder.resolve((SimpleName) pNode.getMember())
                : pBuilder.append(pNode.getMember()) // function expression
            : pBuilder.append(pNode.getExpression());
    final boolean isKnownFunctionDeclaration =
        (function instanceof JSIdExpression)
            && (((JSIdExpression) function).getDeclaration() instanceof JSFunctionDeclaration);
    final JSFunctionDeclaration declaration =
        isKnownFunctionDeclaration
            ? (JSFunctionDeclaration) ((JSIdExpression) function).getDeclaration()
            : unknownFunctionCallerDeclaration;
    final List<JSExpression> arguments = new ArrayList<>();
    if (!isKnownFunctionDeclaration) {
      arguments.add(function); // function is called by unknown function caller
    }
    appendArguments(pBuilder, pNode, arguments);
    final Optional<JSExpression> thisArg =
        (function instanceof JSFieldAccess)
            ? Optional.of(((JSFieldAccess) function).getObject())
            : Optional.empty();
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
                    Optional.empty(),
                    thisArg,
                    isKnownFunctionDeclaration))));
    return resultVariableId;
  }

  @SuppressWarnings("unchecked")
  private void appendArguments(
      final JavaScriptCFABuilder pBuilder,
      final ClassInstanceCreation pNode,
      final List<JSExpression> pArguments) {
    pArguments.addAll(pBuilder.append(pNode.arguments()));
  }
}
