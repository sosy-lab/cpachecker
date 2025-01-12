// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqBlankStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

/**
 * A case clause features a {@link SeqCaseLabel}, a {@link SeqCaseBlock} and a {@link
 * SeqCaseBlock.Terminator}.
 *
 * <p>Example: {@code case 42: fib(42); break;}
 */
public class SeqCaseClause implements SeqStatement {

  private static long currentId = 0;

  public final long id;

  public final boolean isGlobal;

  public final SeqCaseLabel caseLabel;

  public final SeqCaseBlock caseBlock;

  public SeqCaseClause(boolean pIsGlobal, int pCaseLabelValue, SeqCaseBlock pCaseBlock) {

    id = createNewId();
    isGlobal = pIsGlobal;
    caseLabel = new SeqCaseLabel(pCaseLabelValue);
    caseBlock = pCaseBlock;
  }

  /** Private constructor, only used during cloning process to keep the same id. */
  private SeqCaseClause(
      long pId, boolean pIsGlobal, SeqCaseLabel pCaseLabel, SeqCaseBlock pCaseBlock) {

    id = pId;
    isGlobal = pIsGlobal;
    caseLabel = pCaseLabel;
    caseBlock = pCaseBlock;
  }

  public SeqCaseClause cloneWithCaseLabel(SeqCaseLabel pCaseLabel) {
    return new SeqCaseClause(id, isGlobal, pCaseLabel, caseBlock);
  }

  public SeqCaseClause cloneWithCaseBlock(SeqCaseBlock pCaseBlock) {
    // id is not imported at this stage of pruning case clauses
    return new SeqCaseClause(isGlobal, caseLabel.value, pCaseBlock);
  }

  private static long createNewId() {
    return currentId++;
  }

  /**
   * Returns true if all statements in the {@link SeqCaseBlock} are blank, i.e. they only update a
   * pc.
   */
  public boolean isPrunable() {
    for (SeqCaseBlockStatement stmt : caseBlock.statements) {
      if (!(stmt instanceof SeqBlankStatement)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns {@code true} if the {@code pc} of the thread this case clause belongs to is guaranteed
   * to be updated when this case clause is executed.
   *
   * <p>E.g. {@code pthread_mutex_lock} simulation code only updates the pc if the mutex is
   * unlocked.
   */
  public boolean alwaysUpdatesPc() {
    for (SeqCaseBlockStatement stmt : caseBlock.statements) {
      if (!stmt.alwaysUpdatesPc()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toASTString() {
    return caseLabel.toASTString() + SeqSyntax.SPACE + caseBlock.toASTString() + SeqSyntax.NEWLINE;
  }
}
