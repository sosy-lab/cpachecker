// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;

/** A case block follows a {@link SeqCaseLabel} and has a list of {@link SeqCaseBlockStatement}s. */
public class SeqCaseBlock implements SeqStatement {

  public final ImmutableList<SeqCaseBlockStatement> statements;

  public SeqCaseBlock(ImmutableList<SeqCaseBlockStatement> pStatements) {
    statements = pStatements;
  }

  public SeqCaseBlockStatement getFirstStatement() {
    checkArgument(!statements.isEmpty(), "there are no statements, cannot get first");
    return statements.get(0);
  }

  @Override
  public String toASTString() {
    StringBuilder statementsString = new StringBuilder();
    for (SeqCaseBlockStatement statement : this.statements) {
      statementsString.append(statement.toASTString()).append(SeqSyntax.SPACE);
    }
    // tests showed that using break is more efficient than continue, despite the loop
    return statementsString + SeqToken._break + SeqSyntax.SEMICOLON;
  }
}
