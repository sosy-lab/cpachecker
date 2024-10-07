// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.loop_case;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqElement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;

/** Represents a case in the sequentialization while loop. */
public class SeqLoopCase implements SeqElement {

  private static long currentId = 0;

  public final long id;

  public final int originPc;

  public final ImmutableList<SeqLoopCaseStmt> statements;

  public final ImmutableSet<Integer> targetPcs;

  public SeqLoopCase(int pOriginPc, ImmutableList<SeqLoopCaseStmt> pStatements) {
    id = createNewId();
    originPc = pOriginPc;
    statements = pStatements;
    targetPcs = initTargetPcs(statements);
  }

  /** Private constructor, only used during cloning process to keep the same id. */
  private SeqLoopCase(long pId, int pOriginPc, ImmutableList<SeqLoopCaseStmt> pStatements) {
    id = pId;
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

  public SeqLoopCase cloneWithOriginPc(int pOriginPc) {
    return new SeqLoopCase(id, pOriginPc, statements);
  }

  private static long createNewId() {
    return currentId++;
  }

  // TODO it is very confusing to have statements that can be empty, and the statements list which
  //  can be empty too -> rename
  public boolean allStatementsEmpty() {
    for (SeqLoopCaseStmt stmt : statements) {
      if (stmt.statement.isPresent()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toASTString() {
    StringBuilder stmts = new StringBuilder();
    for (SeqLoopCaseStmt stmt : statements) {
      stmts.append(stmt.toASTString()).append(SeqSyntax.SPACE);
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
