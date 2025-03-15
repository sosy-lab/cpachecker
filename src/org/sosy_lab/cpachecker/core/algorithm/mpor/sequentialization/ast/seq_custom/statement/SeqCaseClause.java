// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

/**
 * A case clause features a {@link SeqCaseLabel} and a {@link SeqCaseBlock}.
 *
 * <p>Example: {@code case 42: fib(42); break;}
 */
public class SeqCaseClause implements SeqStatement {

  private static int currentId = 0;

  /** This method is required, otherwise some checks fail. */
  private static int getNewId() {
    return currentId++;
  }

  public final int id;

  public final boolean isGlobal;

  public final boolean isLoopStart;

  /** The case label e.g. {@code case 42: ...} */
  public final SeqCaseLabel label;

  /** The case block e.g. {@code fib(42); break;} */
  public final SeqCaseBlock block;

  public SeqCaseClause(
      boolean pIsGlobal, boolean pIsLoopStart, int pLabelValue, SeqCaseBlock pBlock) {

    id = getNewId();
    isGlobal = pIsGlobal;
    isLoopStart = pIsLoopStart;
    label = new SeqCaseLabel(pLabelValue);
    block = pBlock;
  }

  /** Private constructor, only used during cloning process to keep the same id. */
  private SeqCaseClause(
      int pId, boolean pIsGlobal, boolean pIsLoopStart, SeqCaseLabel pLabel, SeqCaseBlock pBlock) {

    id = pId;
    isGlobal = pIsGlobal;
    isLoopStart = pIsLoopStart;
    label = pLabel;
    block = pBlock;
  }

  public SeqCaseClause cloneWithLabelAndBlock(SeqCaseLabel pLabel, SeqCaseBlock pBlock) {
    return new SeqCaseClause(id, isGlobal, isLoopStart, pLabel, pBlock);
  }

  public SeqCaseClause cloneWithLabel(SeqCaseLabel pLabel) {
    return new SeqCaseClause(id, isGlobal, isLoopStart, pLabel, block);
  }

  public SeqCaseClause cloneWithBlock(SeqCaseBlock pBlock) {
    return new SeqCaseClause(id, isGlobal, isLoopStart, label, pBlock);
  }

  /**
   * Returns true if all statements in the {@link SeqCaseBlock} are blank, i.e. they only update a
   * pc.
   */
  public boolean onlyWritesPc() {
    for (SeqCaseBlockStatement statement : block.statements) {
      if (!statement.onlyWritesPc()) {
        return false;
      }
    }
    return true;
  }

  /** Returns {@code true} if any statement in this case clause is a start to a critical section. */
  public boolean isCriticalSectionStart() {
    for (SeqCaseBlockStatement statement : block.statements) {
      if (!statement.isCriticalSectionStart()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toASTString() {
    String blockString = block.toASTString();
    // if the block starts with a newline, we don't want to add an unnecessary trailing space
    String separator =
        blockString.startsWith(SeqSyntax.NEWLINE) ? SeqSyntax.EMPTY_STRING : SeqSyntax.SPACE;
    return label.toASTString() + separator + blockString + SeqSyntax.NEWLINE;
  }
}
