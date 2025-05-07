// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqAtomicStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * A clause features an {@code int} label and a {@link SeqThreadStatementBlock}.
 *
 * <p>e.g. {@code case 42: fib(42); break;} when using switch cases.
 */
public class SeqThreadStatementClause implements SeqStatement {

  private static int currentId = 0;

  /** This method is required, otherwise some checks fail. */
  private static int getNewId() {
    return currentId++;
  }

  public final int id;

  public final boolean isGlobal;

  public final boolean isLoopStart;

  public final int labelNumber;

  /** The case block e.g. {@code fib(42); break;} */
  public final SeqStatementBlock block;

  public SeqThreadStatementClause(
      boolean pIsGlobal, boolean pIsLoopStart, int pLabelNumber, SeqStatementBlock pBlock) {

    id = getNewId();
    isGlobal = pIsGlobal;
    isLoopStart = pIsLoopStart;
    labelNumber = pLabelNumber;
    block = pBlock;
  }

  /** Private constructor, only used during cloning process to keep the same id. */
  private SeqThreadStatementClause(
      int pId,
      boolean pIsGlobal,
      boolean pIsLoopStart,
      int pLabelNumber,
      SeqStatementBlock pBlock) {

    id = pId;
    isGlobal = pIsGlobal;
    isLoopStart = pIsLoopStart;
    labelNumber = pLabelNumber;
    block = pBlock;
  }

  public SeqThreadStatementClause cloneWithLabel(int pLabelNumber) {
    return new SeqThreadStatementClause(id, isGlobal, isLoopStart, pLabelNumber, block);
  }

  public SeqThreadStatementClause cloneWithAtomicBlock(SeqAtomicStatementBlock pAtomicBlock) {
    return new SeqThreadStatementClause(id, isGlobal, isLoopStart, labelNumber, pAtomicBlock);
  }

  public SeqThreadStatementClause cloneWithBlockStatements(
      ImmutableList<SeqThreadStatement> pStatements) {
    return new SeqThreadStatementClause(
        id, isGlobal, isLoopStart, labelNumber, block.cloneWithStatements(pStatements));
  }

  public SeqThreadStatementClause cloneWithLabelAndBlockStatements(
      int pLabelNumber, ImmutableList<SeqThreadStatement> pStatements) {
    return new SeqThreadStatementClause(
        id, isGlobal, isLoopStart, pLabelNumber, block.cloneWithStatements(pStatements));
  }

  /**
   * Returns true if all statements in the {@link SeqThreadStatementBlock} are blank, i.e. they only
   * update a pc.
   */
  public boolean onlyWritesPc() {
    for (SeqThreadStatement statement : block.getStatements()) {
      if (!statement.onlyWritesPc()) {
        return false;
      }
    }
    return true;
  }

  /** Returns {@code true} if any statement in this case clause is a start to a critical section. */
  public boolean isCriticalSectionStart() {
    for (SeqThreadStatement statement : block.getStatements()) {
      if (statement.isCriticalSectionStart()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    return block.toASTString();
  }
}
