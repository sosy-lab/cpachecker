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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.eclipse.wst.jsdt.core.dom.ExpressionStatement;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.IfStatement;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.junit.Before;
import org.junit.Test;

public class StatementCFABuilderTest {

  private JavaScriptCFABuilder builder;

  @Before
  public void init() {
    builder = JavaScriptCFABuilderFactory.createTestJavaScriptCFABuilder();
  }

  @Test
  public final void testExpressionStatement() {
    final ExpressionStatement expressionStatement = mock(ExpressionStatement.class);
    final ExpressionStatementAppendable expressionStatementAppendable =
        mock(ExpressionStatementAppendable.class);

    final StatementCFABuilder statementCFABuilder = new StatementCFABuilder();
    statementCFABuilder.setExpressionStatementAppendable(expressionStatementAppendable);
    statementCFABuilder.append(builder, expressionStatement);

    verify(expressionStatementAppendable, times(1)).append(builder, expressionStatement);
  }

  @Test
  public final void testIfStatement() {
    final IfStatement ifStatement = mock(IfStatement.class);
    final IfStatementAppendable ifStatementAppendable = mock(IfStatementAppendable.class);

    final StatementCFABuilder statementCFABuilder = new StatementCFABuilder();
    statementCFABuilder.setIfStatementAppendable(ifStatementAppendable);
    statementCFABuilder.append(builder, ifStatement);

    verify(ifStatementAppendable, times(1)).append(builder, ifStatement);
  }

  @Test
  public final void testVariableDeclarationStatement() {
    final VariableDeclarationStatement variableDeclarationStatement =
        mock(VariableDeclarationStatement.class);
    final VariableDeclarationStatementAppendable variableDeclarationStatementAppendable =
        mock(VariableDeclarationStatementAppendable.class);

    final StatementCFABuilder statementCFABuilder = new StatementCFABuilder();
    statementCFABuilder.setVariableDeclarationStatementAppendable(
        variableDeclarationStatementAppendable);
    statementCFABuilder.append(builder, variableDeclarationStatement);

    verify(variableDeclarationStatementAppendable, times(1))
        .append(builder, variableDeclarationStatement);
  }

  @Test
  public final void testFunctionDeclarationStatement() {
    final FunctionDeclarationStatement functionDeclarationStatement =
        mock(FunctionDeclarationStatement.class);
    final FunctionDeclarationStatementAppendable functionDeclarationStatementAppendable =
        mock(FunctionDeclarationStatementAppendable.class);

    final StatementCFABuilder statementCFABuilder = new StatementCFABuilder();
    statementCFABuilder.setFunctionDeclarationStatementAppendable(
        functionDeclarationStatementAppendable);
    statementCFABuilder.append(builder, functionDeclarationStatement);

    verify(functionDeclarationStatementAppendable, times(1))
        .append(builder, functionDeclarationStatement);
  }
}
