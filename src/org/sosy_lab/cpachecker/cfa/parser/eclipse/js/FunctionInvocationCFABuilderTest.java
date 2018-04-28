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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.truth.Truth;
import org.eclipse.wst.jsdt.core.dom.ExpressionStatement;
import org.eclipse.wst.jsdt.core.dom.FunctionInvocation;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.ParenthesizedExpression;
import org.eclipse.wst.jsdt.core.dom.SimpleName;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.model.js.JSStatementEdge;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class FunctionInvocationCFABuilderTest extends CFABuilderTestBase {

  @Test
  public final void testFunctionInvocation() {
    final JavaScriptUnit ast = createAST("function foo() { /* stub */ }\nfoo()");
    final String expectedFunctionName = "foo";
    final FunctionInvocation functionInvocation =
        (FunctionInvocation) ((ExpressionStatement) ast.statements().get(1)).getExpression();
    // expected CFA: entryNode -{foo()}-> ()

    final JSFunctionDeclaration functionDeclaration = mock(JSFunctionDeclaration.class);
    final JSIdExpression functionId =
        new JSIdExpression(FileLocation.DUMMY, "foo", functionDeclaration);
    final ExpressionAppendable expressionAppendable = mock(ExpressionAppendable.class);
    when(expressionAppendable.append(any(), any(SimpleName.class))).thenReturn(functionId);
    builder.setExpressionAppendable(expressionAppendable);

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

  @Test
  public final void testImmediatelyInvokedFunctionExpression() {
    final FunctionInvocation functionInvocation =
        parseExpression(FunctionInvocation.class, "(function () {})()");

    final JSFunctionDeclaration functionDeclaration = mock(JSFunctionDeclaration.class);
    final JSIdExpression functionId =
        new JSIdExpression(
            FileLocation.DUMMY,
            "__CPAChecker_ANONYMOUS_FUNCTION_0",
            functionDeclaration);
    final ExpressionAppendable expressionAppendable = mock(ExpressionAppendable.class);
    when(expressionAppendable.append(any(), any(ParenthesizedExpression.class)))
        .thenReturn(functionId);
    builder.setExpressionAppendable(expressionAppendable);

    // TODO check return value
    new FunctionInvocationCFABuilder().append(builder, functionInvocation);

    Truth.assertThat(entryNode.getNumLeavingEdges()).isEqualTo(1);
    final JSStatementEdge functionInvocationEdge = (JSStatementEdge) entryNode.getLeavingEdge(0);
    final JSFunctionCallStatement functionCallStatement =
        (JSFunctionCallStatement) functionInvocationEdge.getStatement();
    final JSFunctionCallExpression functionCallExpression =
        functionCallStatement.getFunctionCallExpression();
    Truth.assertThat(functionCallExpression.getFunctionNameExpression()).isEqualTo(functionId);
    Truth.assertThat(functionCallExpression.getDeclaration()).isEqualTo(functionDeclaration);
    Truth.assertThat(functionInvocationEdge.getSuccessor().getNumLeavingEdges()).isEqualTo(0);
  }
}
