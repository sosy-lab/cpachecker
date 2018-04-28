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

import org.eclipse.wst.jsdt.core.dom.Block;
import org.eclipse.wst.jsdt.core.dom.EmptyStatement;
import org.eclipse.wst.jsdt.core.dom.ExpressionStatement;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.IfStatement;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;


@SuppressWarnings("ResultOfMethodCallIgnored")
class StatementCFABuilder implements StatementAppendable {

  private BlockStatementAppendable blockStatementAppendable;
  private EmptyStatementAppendable emptyStatementAppendable;
  private ExpressionStatementAppendable expressionStatementAppendable;
  private IfStatementAppendable ifStatementAppendable;
  private VariableDeclarationStatementAppendable variableDeclarationStatementAppendable;
  private FunctionDeclarationStatementAppendable functionDeclarationStatementAppendable;

  void setBlockStatementAppendable(final BlockStatementAppendable pBlockStatementAppendable) {
    blockStatementAppendable = pBlockStatementAppendable;
  }

  void setEmptyStatementAppendable(
      final EmptyStatementAppendable pEmptyStatementAppendable) {
    emptyStatementAppendable = pEmptyStatementAppendable;
  }

  void setExpressionStatementAppendable(
      final ExpressionStatementAppendable pExpressionStatementAppendable) {
    expressionStatementAppendable = pExpressionStatementAppendable;
  }

  void setIfStatementAppendable(final IfStatementAppendable pIfStatementAppendable) {
    ifStatementAppendable = pIfStatementAppendable;
  }

  void setVariableDeclarationStatementAppendable(
      final VariableDeclarationStatementAppendable pVariableDeclarationStatementAppendable) {
    variableDeclarationStatementAppendable = pVariableDeclarationStatementAppendable;
  }

  void setFunctionDeclarationStatementAppendable(
      final FunctionDeclarationStatementAppendable pFunctionDeclarationStatementAppendable) {
    functionDeclarationStatementAppendable = pFunctionDeclarationStatementAppendable;
  }

  @Override
  public void append(final JavaScriptCFABuilder pBuilder, final Statement pStatement) {
    if (pStatement instanceof Block) {
      blockStatementAppendable.append(pBuilder, (Block) pStatement);
    } else if (pStatement instanceof FunctionDeclarationStatement) {
      functionDeclarationStatementAppendable.append(
          pBuilder, (FunctionDeclarationStatement) pStatement);
    } else if (pStatement instanceof EmptyStatement) {
      emptyStatementAppendable.append(pBuilder, (EmptyStatement) pStatement);
    } else if (pStatement instanceof ExpressionStatement) {
      expressionStatementAppendable.append(pBuilder, (ExpressionStatement) pStatement);
    } else if (pStatement instanceof IfStatement) {
      ifStatementAppendable.append(pBuilder, (IfStatement) pStatement);
    } else if (pStatement instanceof VariableDeclarationStatement) {
      variableDeclarationStatementAppendable.append(
          pBuilder, (VariableDeclarationStatement) pStatement);
    } else {
      throw new CFAGenerationRuntimeException(
          "Unknown kind of statement (not handled yet): " + pStatement.getClass().getName(),
          pStatement);
    }
  }
}
