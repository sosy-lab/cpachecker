// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;

/** A case block follows a {@link SeqCaseLabel} and has a list of {@link SeqCaseBlockStatement}s. */
public class SeqCaseBlock implements SeqStatement {

  /** The suffix that ends the case block. */
  public enum Terminator {
    BREAK(SeqToken._break),
    CONTINUE(SeqToken._continue);

    private final String asString;

    Terminator(String pAsString) {
      asString = pAsString;
    }
  }

  public final ImmutableList<SeqCaseBlockStatement> statements;

  private final Terminator terminator;

  public SeqCaseBlock(ImmutableList<SeqCaseBlockStatement> pStatements, Terminator pTerminator) {
    statements = pStatements;
    terminator = pTerminator;
  }

  public SeqCaseBlock cloneWithStatements(ImmutableList<SeqCaseBlockStatement> pStatements) {
    return new SeqCaseBlock(pStatements, terminator);
  }

  @Override
  public String toASTString() {
    StringBuilder statementsString = new StringBuilder();
    for (SeqCaseBlockStatement statement : this.statements) {
      statementsString.append(statement.toASTString()).append(SeqSyntax.SPACE);
    }
    return statementsString + terminator.asString + SeqSyntax.SEMICOLON;
  }
}
