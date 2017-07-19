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
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.model.js.JSStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.js.JSAnyType;

class FunctionInvocationCFABuilder {

  private final CFABuilder builder;

  FunctionInvocationCFABuilder(final CFABuilder pBuilder) {
    builder = pBuilder;
  }

  public void append(final FunctionInvocation node) {
    final ASTConverter astConverter = builder.getAstConverter();
    final JSFunctionCallStatement functionCallStatement =
        new JSFunctionCallStatement(
            astConverter.getFileLocation(node),
            new JSFunctionCallExpression(
                astConverter.getFileLocation(node),
                JSAnyType.ANY,
                astConverter.convert(node.getName()),
                Collections.emptyList(),
                astConverter.convert((FunctionBinding) node.getName().resolveBinding())));

    builder.appendEdge(
        (pPredecessor, pSuccessor) ->
            new JSStatementEdge(
                functionCallStatement.toASTString(),
                functionCallStatement,
                astConverter.getFileLocation(node),
                pPredecessor,
                pSuccessor));
  }

  public CFABuilder getBuilder() {
    return builder;
  }
}
