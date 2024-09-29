// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.loop_case;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqElement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;

/** Represents a case in the sequentialization while loop. */
public class SeqLoopCase implements SeqElement {

  public final int originPc;

  public final ImmutableList<SeqLoopCaseStmt> statements;

  public final ImmutableSet<Integer> targetPcs;

  public SeqLoopCase(int pOriginPc, ImmutableList<SeqLoopCaseStmt> pStatements) {
    originPc = pOriginPc;
    statements = pStatements;
    targetPcs = initTargetPcs(statements);
  }

  private ImmutableSet<Integer> initTargetPcs(ImmutableList<SeqLoopCaseStmt> pStatements) {
    ImmutableSet.Builder<Integer> rPcs = ImmutableSet.builder();
    for (SeqLoopCaseStmt caseStmt : pStatements) {
      if (caseStmt.targetPc.isPresent()) {
        rPcs.add(caseStmt.targetPc.orElseThrow());
      }
    }
    return rPcs.build();
  }

  public SeqLoopCase cloneWithTargetPc(int pTargetPc) {
    checkArgument(statements.size() == 1, "multiple statements - cloning not possible");

    ImmutableList.Builder<SeqLoopCaseStmt> cloneStmt = ImmutableList.builder();
    cloneStmt.add(statements.get(0).cloneWithTargetPc(pTargetPc));

    return new SeqLoopCase(originPc, cloneStmt.build());
  }

  @Override
  public String toString() {
    StringBuilder stmts = new StringBuilder();
    for (SeqLoopCaseStmt stmt : statements) {
      stmts.append(stmt.toString());
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
