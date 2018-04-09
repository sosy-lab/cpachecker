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

import com.google.common.truth.Truth;
import org.eclipse.wst.jsdt.core.dom.ExpressionStatement;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.model.js.JSStatementEdge;
import org.sosy_lab.cpachecker.exceptions.ParserException;

public class FunctionInvocationCFABuilderTest extends CFABuilderTestBase {

  @Test
  public final void testFunctionInvocation() throws ParserException {
    final JavaScriptUnit ast = createAST("function foo() { /* stub */ }\nfoo()");
    final String expectedFunctionName = "foo";
    final FunctionDeclaration functionDeclaration = (FunctionDeclaration) ast.statements().get(0);
    final FunctionInvocation functionInvocation =
        (FunctionInvocation) ((ExpressionStatement) ast.statements().get(1)).getExpression();
    // expected CFA: entryNode -{foo()}-> ()

    // TODO check return value
    new FunctionInvocationCFABuilder().append(builder, functionInvocation);

    Truth.assertThat(entryNode.getNumLeavingEdges()).isEqualTo(1);
    final JSStatementEdge functionInvocationEdge = (JSStatementEdge) entryNode.getLeavingEdge(0);
    final JSFunctionCallStatement functionCallStatement =
        (JSFunctionCallStatement) functionInvocationEdge.getStatement();
    // TODO check function declaration of functionCallExpression
    final JSFunctionCallExpression functionCallExpression =
        functionCallStatement.getFunctionCallExpression();
    Truth.assertThat(functionCallExpression.getFunctionNameExpression())
        .isInstanceOf(JSIdExpression.class);
    Truth.assertThat(
            ((JSIdExpression) functionCallExpression.getFunctionNameExpression()).getName())
        .isEqualTo(expectedFunctionName);
    Truth.assertThat(functionInvocationEdge.getSuccessor().getNumLeavingEdges()).isEqualTo(0);
  }
}
