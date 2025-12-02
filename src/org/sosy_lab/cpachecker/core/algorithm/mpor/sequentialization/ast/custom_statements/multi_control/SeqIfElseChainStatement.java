// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.StringJoiner;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control.SeqBranchStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public record SeqIfElseChainStatement(
    ImmutableList<CStatement> precedingStatements,
    ImmutableMap<CExpression, ? extends SeqStatement> statements)
    implements SeqMultiControlStatement {

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    StringJoiner ifElseChain = new StringJoiner(SeqSyntax.NEWLINE);

    // first add preceding statements
    precedingStatements.forEach(statement -> ifElseChain.add(statement.toASTString()));

    // then add all statements via if ... else { if ... } from the bottom up
    ImmutableList<Entry<CExpression, ? extends SeqStatement>> statementList =
        ImmutableList.copyOf(statements.entrySet());
    SeqBranchStatement currentBranch = null;
    for (int i = statementList.size() - 1; i >= 0; i--) {
      Entry<CExpression, ? extends SeqStatement> currentStatement = statementList.get(i);
      // on last statement: no else branch
      if (i == statementList.size() - 1) {
        currentBranch =
            new SeqBranchStatement(
                currentStatement.getKey().toASTString(),
                ImmutableList.of(currentStatement.getValue().toASTString()));
      } else {
        currentBranch =
            new SeqBranchStatement(
                currentStatement.getKey().toASTString(),
                ImmutableList.of(currentStatement.getValue().toASTString()),
                Objects.requireNonNull(currentBranch));
      }
    }
    ifElseChain.add(Objects.requireNonNull(currentBranch).toASTString());
    return ifElseChain.toString();
  }

  @Override
  public MultiControlStatementEncoding getEncoding() {
    return MultiControlStatementEncoding.IF_ELSE_CHAIN;
  }
}
