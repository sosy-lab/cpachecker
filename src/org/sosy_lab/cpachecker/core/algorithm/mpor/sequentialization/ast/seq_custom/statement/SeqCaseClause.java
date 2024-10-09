// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqBlankStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;

/** Represents a case clause, i.e. a case label and its case block. */
public class SeqCaseClause implements SeqStatement {

  private static long currentId = 0;

  public final long id;

  public final int originPc;

  public final ImmutableList<SeqCaseBlockStatement> caseBlock;

  public SeqCaseClause(int pOriginPc, ImmutableList<SeqCaseBlockStatement> pStatements) {
    id = createNewId();
    originPc = pOriginPc;
    caseBlock = pStatements;
  }

  /** Private constructor, only used during cloning process to keep the same id. */
  private SeqCaseClause(long pId, int pOriginPc, ImmutableList<SeqCaseBlockStatement> pStatements) {
    id = pId;
    originPc = pOriginPc;
    caseBlock = pStatements;
  }

  public SeqCaseClause cloneWithOriginPc(int pOriginPc) {
    return new SeqCaseClause(id, pOriginPc, caseBlock);
  }

  private static long createNewId() {
    return currentId++;
  }

  public boolean isPrunable() {
    for (SeqCaseBlockStatement stmt : caseBlock) {
      if (!(stmt instanceof SeqBlankStatement)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toASTString() {
    StringBuilder stmts = new StringBuilder();
    for (SeqCaseBlockStatement stmt : caseBlock) {
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
