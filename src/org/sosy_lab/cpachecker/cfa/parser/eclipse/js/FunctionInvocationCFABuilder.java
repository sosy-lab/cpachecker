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

import java.util.Collections;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.internal.core.dom.binding.FunctionBinding;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSUndefinedLiteralExpression;
import org.sosy_lab.cpachecker.cfa.model.js.JSStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.js.JSAnyType;

class FunctionInvocationCFABuilder implements FunctionInvocationAppendable {

  @Override
  public JSExpression append(final JavaScriptCFABuilder pBuilder, final FunctionInvocation pNode) {
    final ASTConverter astConverter = pBuilder.getAstConverter();
    final JSIdExpression function =
        pNode.getName() != null
            ? astConverter.convert(pNode.getName())
            : (JSIdExpression) pBuilder.append(pNode.getExpression());
    final JSFunctionDeclaration declaration =
        pNode.getName() != null
            ? astConverter.convert((FunctionBinding) pNode.getName().resolveBinding())
            : pBuilder.resolveFunctionDeclaration(function);
    final JSFunctionCallStatement functionCallStatement =
        new JSFunctionCallStatement(
            astConverter.getFileLocation(pNode),
            new JSFunctionCallExpression(
                astConverter.getFileLocation(pNode),
                JSAnyType.ANY,
                function,
                Collections.emptyList(),
                declaration));

    pBuilder.appendEdge(
        (pPredecessor, pSuccessor) ->
            new JSStatementEdge(
                functionCallStatement.toASTString(),
                functionCallStatement,
                functionCallStatement.getFileLocation(),
                pPredecessor,
                pSuccessor));

    // TODO create tmp variable for return value of function invocation and return its identifier
    return new JSUndefinedLiteralExpression(FileLocation.DUMMY);
  }

}
