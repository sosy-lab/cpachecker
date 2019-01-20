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

  Hoisting(
      final FunctionScope pFunctionScope,
      final FunctionDeclarationCFABuilder pFunctionDeclarationCFABuilder) {
    functionScope = pFunctionScope;
    functionDeclarationCFABuilder = pFunctionDeclarationCFABuilder;
  }

  void append(final JavaScriptCFABuilder pBuilder, final Statement pStatement) {
    createStatementBuilder().append(pBuilder, pStatement);
  }

  void append(final JavaScriptCFABuilder pBuilder, final JavaScriptUnit pUnit) {
    final StatementAppendable builder = createStatementBuilder();
    for (final ASTNode node : pUnit.statements()) {
      if (node instanceof Statement) {
        builder.append(pBuilder, (Statement) node);
      } else if (node instanceof FunctionDeclaration) {
        declareFunction(pBuilder, (FunctionDeclaration) node);
      } else {
        throw new CFAGenerationRuntimeException(
            "Unknown kind of node (not handled yet): " + node.getClass().getSimpleName(), node);
      }
    }
  }

  private StatementAppendable createStatementBuilder() {
    final StatementCFABuilder builder = new StatementCFABuilder();

    // statements that do not contain (declaration) statements
    builder.setBreakStatementAppendable((pBuilder, pStatement) -> {});
    builder.setContinueStatementAppendable((pBuilder, pStatement) -> {});
    builder.setEmptyStatementAppendable((pBuilder, pStatement) -> {});
    builder.setExpressionStatementAppendable((pBuilder, pStatement) -> null);
    builder.setReturnStatementAppendable((pBuilder, pStatement) -> {});

    // visit (sub-) statements of control flow statements
    builder.setBlockStatementAppendable(
        (pBuilder, pStatement) -> {
          for (final Object statement : pStatement.statements()) {
            builder.append(pBuilder, (Statement) statement);
          }
        });
    builder.setDoWhileStatementAppendable(
        (pBuilder, pStatement) -> builder.append(pBuilder, pStatement.getBody()));
    builder.setForStatementAppendable(
        (pBuilder, pStatement) -> builder.append(pBuilder, pStatement.getBody()));
    builder.setIfStatementAppendable(
        (pBuilder, pStatement) -> {
          builder.append(pBuilder, pStatement.getThenStatement());
          if (pStatement.getElseStatement() != null) {
            builder.append(pBuilder, pStatement.getElseStatement());
          }
        });
    builder.setLabeledStatementAppendable(
        (pBuilder, pStatement) -> builder.append(pBuilder, pStatement.getBody()));
    builder.setSwitchStatementAppendable(
        (pBuilder, pStatement) -> {
          for (final Object statement : pStatement.statements()) {
            if (!(statement instanceof SwitchCase)) {
              builder.append(pBuilder, (Statement) statement);
            }
          }
        });
    builder.setWhileStatementAppendable(
        (pBuilder, pStatement) -> builder.append(pBuilder, pStatement.getBody()));

    // statements that may lead to a hoisted declaration
    builder.setVariableDeclarationStatementAppendable(this::declareVariable);
    builder.setFunctionDeclarationStatementAppendable(this::declareFunction);
    return builder;
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
