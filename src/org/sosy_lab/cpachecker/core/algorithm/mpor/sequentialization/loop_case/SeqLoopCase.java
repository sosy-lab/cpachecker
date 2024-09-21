// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.loop_case;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqElement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;

/** Represents a case in the sequentialization while loop. */
public class SeqLoopCase implements SeqElement {

  // TODO boolean isEmpty (prune later)

  public final int originPc;

  public final ImmutableList<SeqLoopCaseStmt> statements;

  public SeqLoopCase(int pOriginPc, ImmutableList<SeqLoopCaseStmt> pStatements) {
    originPc = pOriginPc;
    statements = pStatements;
  }

  @Override
  public String createString() {
    StringBuilder stmts = new StringBuilder();
    for (SeqLoopCaseStmt stmt : statements) {
      stmts.append(stmt.createString());
    }
    return SeqToken.CASE
        + SeqSyntax.SPACE
        + originPc
        + SeqSyntax.COLON
        + SeqSyntax.SPACE
        + stmts
        + SeqToken.CONTINUE
        + SeqSyntax.SEMICOLON
        + SeqSyntax.NEWLINE;
  }
}
