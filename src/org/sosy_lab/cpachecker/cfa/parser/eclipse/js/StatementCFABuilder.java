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
import org.eclipse.wst.jsdt.core.dom.ContinueStatement;
import org.eclipse.wst.jsdt.core.dom.DoStatement;
import org.eclipse.wst.jsdt.core.dom.EmptyStatement;
import org.eclipse.wst.jsdt.core.dom.ExpressionStatement;
import org.eclipse.wst.jsdt.core.dom.ForStatement;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.IfStatement;
import org.eclipse.wst.jsdt.core.dom.LabeledStatement;
import org.eclipse.wst.jsdt.core.dom.ReturnStatement;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.WhileStatement;

@SuppressWarnings("ResultOfMethodCallIgnored")
class StatementCFABuilder implements StatementAppendable {

  private BlockStatementAppendable blockStatementAppendable;
  private ContinueStatementAppendable continueStatementAppendable;
  private DoWhileStatementAppendable doWhileStatementAppendable;
  private EmptyStatementAppendable emptyStatementAppendable;
  private ExpressionStatementAppendable expressionStatementAppendable;
  private ForStatementAppendable forStatementAppendable;
  private IfStatementAppendable ifStatementAppendable;
  private LabeledStatementAppendable labeledStatementAppendable;
  private VariableDeclarationStatementAppendable variableDeclarationStatementAppendable;
  private FunctionDeclarationStatementAppendable functionDeclarationStatementAppendable;
  private ReturnStatementAppendable returnStatementAppendable;
  private WhileStatementAppendable whileStatementAppendable;

  void setBlockStatementAppendable(final BlockStatementAppendable pBlockStatementAppendable) {
    blockStatementAppendable = pBlockStatementAppendable;
  }

  void setContinueStatementAppendable(
      final ContinueStatementAppendable pContinueStatementCFABuilder) {
    continueStatementAppendable = pContinueStatementCFABuilder;
  }

  void setDoWhileStatementAppendable(final DoWhileStatementAppendable pDoWhileStatementAppendable) {
    doWhileStatementAppendable = pDoWhileStatementAppendable;
  }

  void setEmptyStatementAppendable(
      final EmptyStatementAppendable pEmptyStatementAppendable) {
    emptyStatementAppendable = pEmptyStatementAppendable;
  }

  void setExpressionStatementAppendable(
      final ExpressionStatementAppendable pExpressionStatementAppendable) {
    expressionStatementAppendable = pExpressionStatementAppendable;
  }

  void setForStatementAppendable(final ForStatementAppendable pForStatementAppendable) {
    forStatementAppendable = pForStatementAppendable;
  }

  void setIfStatementAppendable(final IfStatementAppendable pIfStatementAppendable) {
    ifStatementAppendable = pIfStatementAppendable;
  }

  void setLabeledStatementAppendable(final LabeledStatementAppendable pLabeledStatementAppendable) {
    labeledStatementAppendable = pLabeledStatementAppendable;
  }

  void setVariableDeclarationStatementAppendable(
      final VariableDeclarationStatementAppendable pVariableDeclarationStatementAppendable) {
    variableDeclarationStatementAppendable = pVariableDeclarationStatementAppendable;
  }

  void setWhileStatementAppendable(final WhileStatementAppendable pWhileStatementAppendable) {
    whileStatementAppendable = pWhileStatementAppendable;
  }

  void setFunctionDeclarationStatementAppendable(
      final FunctionDeclarationStatementAppendable pFunctionDeclarationStatementAppendable) {
    functionDeclarationStatementAppendable = pFunctionDeclarationStatementAppendable;
  }

  void setReturnStatementAppendable(final ReturnStatementAppendable pReturnStatementAppendable) {
    returnStatementAppendable = pReturnStatementAppendable;
  }

  @Override
  public void append(final JavaScriptCFABuilder pBuilder, final Statement pStatement) {
    if (pStatement instanceof Block) {
      blockStatementAppendable.append(pBuilder, (Block) pStatement);
    } else if (pStatement instanceof ContinueStatement) {
      continueStatementAppendable.append(pBuilder, (ContinueStatement) pStatement);
    } else if (pStatement instanceof DoStatement) {
      doWhileStatementAppendable.append(pBuilder, (DoStatement) pStatement);
    } else if (pStatement instanceof FunctionDeclarationStatement) {
      functionDeclarationStatementAppendable.append(
          pBuilder, (FunctionDeclarationStatement) pStatement);
    } else if (pStatement instanceof EmptyStatement) {
      emptyStatementAppendable.append(pBuilder, (EmptyStatement) pStatement);
    } else if (pStatement instanceof ExpressionStatement) {
      expressionStatementAppendable.append(pBuilder, (ExpressionStatement) pStatement);
    } else if (pStatement instanceof ForStatement) {
      forStatementAppendable.append(pBuilder, (ForStatement) pStatement);
    } else if (pStatement instanceof IfStatement) {
      ifStatementAppendable.append(pBuilder, (IfStatement) pStatement);
    } else if (pStatement instanceof LabeledStatement) {
      labeledStatementAppendable.append(pBuilder, (LabeledStatement) pStatement);
    } else if (pStatement instanceof ReturnStatement) {
      returnStatementAppendable.append(pBuilder, (ReturnStatement) pStatement);
    } else if (pStatement instanceof VariableDeclarationStatement) {
      variableDeclarationStatementAppendable.append(
          pBuilder, (VariableDeclarationStatement) pStatement);
    } else if (pStatement instanceof WhileStatement) {
      whileStatementAppendable.append(pBuilder, (WhileStatement) pStatement);
    } else {
      throw new CFAGenerationRuntimeException(
          "Unknown kind of statement (not handled yet): " + pStatement.getClass().getName(),
          pStatement);
    }
  }
}
