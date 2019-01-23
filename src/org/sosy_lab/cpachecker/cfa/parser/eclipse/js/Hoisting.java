/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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

import org.eclipse.wst.jsdt.core.dom.ASTNode;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclaration;
import org.eclipse.wst.jsdt.core.dom.FunctionDeclarationStatement;
import org.eclipse.wst.jsdt.core.dom.JavaScriptUnit;
import org.eclipse.wst.jsdt.core.dom.Statement;
import org.eclipse.wst.jsdt.core.dom.SwitchCase;
import org.eclipse.wst.jsdt.core.dom.VariableDeclarationStatement;
import org.sosy_lab.cpachecker.cfa.ast.js.JSFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.js.JSDeclarationEdge;

class Hoisting {

  private final FunctionScope functionScope;
  private final FunctionDeclarationCFABuilder functionDeclarationCFABuilder;
  private final JavaScriptCFABuilder builder;

  Hoisting(
      final FunctionScope pFunctionScope,
      final FunctionDeclarationCFABuilder pFunctionDeclarationCFABuilder,
      final JavaScriptCFABuilder pBuilder) {
    functionScope = pFunctionScope;
    functionDeclarationCFABuilder = pFunctionDeclarationCFABuilder;
    builder = pBuilder;
  }

  void append(final Statement pStatement) {
    createStatementBuilder().append(builder, pStatement);
  }

  void append(final JavaScriptUnit pUnit) {
    final StatementAppendable stmtBuilder = createStatementBuilder();
    for (final ASTNode node : pUnit.statements()) {
      if (node instanceof Statement) {
        stmtBuilder.append(builder, (Statement) node);
      } else if (node instanceof FunctionDeclaration) {
        declareFunction(builder, (FunctionDeclaration) node);
      } else {
        throw new CFAGenerationRuntimeException(
            "Unknown kind of node (not handled yet): " + node.getClass().getSimpleName(), node);
      }
    }
  }

  private StatementAppendable createStatementBuilder() {
    final StatementCFABuilder stmtBuilder = new StatementCFABuilder();

    // statements that do not contain (declaration) statements
    stmtBuilder.setBreakStatementAppendable((pBuilder, pStatement) -> {});
    stmtBuilder.setContinueStatementAppendable((pBuilder, pStatement) -> {});
    stmtBuilder.setEmptyStatementAppendable((pBuilder, pStatement) -> {});
    stmtBuilder.setExpressionStatementAppendable((pBuilder, pStatement) -> null);
    stmtBuilder.setReturnStatementAppendable((pBuilder, pStatement) -> {});

    // visit (sub-) statements of control flow statements
    stmtBuilder.setBlockStatementAppendable(
        (pBuilder, pStatement) -> {
          for (final Object statement : pStatement.statements()) {
            stmtBuilder.append(pBuilder, (Statement) statement);
          }
        });
    stmtBuilder.setDoWhileStatementAppendable(
        (pBuilder, pStatement) -> stmtBuilder.append(pBuilder, pStatement.getBody()));
    stmtBuilder.setForStatementAppendable(
        (pBuilder, pStatement) -> stmtBuilder.append(pBuilder, pStatement.getBody()));
    stmtBuilder.setIfStatementAppendable(
        (pBuilder, pStatement) -> {
          stmtBuilder.append(pBuilder, pStatement.getThenStatement());
          if (pStatement.getElseStatement() != null) {
            stmtBuilder.append(pBuilder, pStatement.getElseStatement());
          }
        });
    stmtBuilder.setLabeledStatementAppendable(
        (pBuilder, pStatement) -> stmtBuilder.append(pBuilder, pStatement.getBody()));
    stmtBuilder.setSwitchStatementAppendable(
        (pBuilder, pStatement) -> {
          for (final Object statement : pStatement.statements()) {
            if (!(statement instanceof SwitchCase)) {
              stmtBuilder.append(pBuilder, (Statement) statement);
            }
          }
        });
    stmtBuilder.setWhileStatementAppendable(
        (pBuilder, pStatement) -> stmtBuilder.append(pBuilder, pStatement.getBody()));

    // statements that may lead to a hoisted declaration
    stmtBuilder.setVariableDeclarationStatementAppendable(this::declareVariable);
    stmtBuilder.setFunctionDeclarationStatementAppendable(this::declareFunction);
    return stmtBuilder;
  }

  private void declareVariable(
      final JavaScriptCFABuilder pBuilder,
      final VariableDeclarationStatement pVariableDeclarationStatement) {
    // TODO declare hoisted variable
  }

  private void declareFunction(
      final JavaScriptCFABuilder pBuilder,
      final FunctionDeclarationStatement pFunctionDeclarationStatement) {
    final FunctionDeclaration functionDeclaration = pFunctionDeclarationStatement.getDeclaration();
    declareFunction(pBuilder, functionDeclaration);
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private void declareFunction(
      final JavaScriptCFABuilder pBuilder, final FunctionDeclaration pFunctionDeclaration) {
    final JSFunctionDeclaration jsFunctionDeclaration =
        functionDeclarationCFABuilder.getJSFunctionDeclaration(pBuilder, pFunctionDeclaration);
    functionScope.addDeclaration(jsFunctionDeclaration);
    functionDeclarationCFABuilder.addDeclarationMapping(
        pFunctionDeclaration, jsFunctionDeclaration);
    pBuilder.appendEdge(JSDeclarationEdge.of(jsFunctionDeclaration));
  }
}
