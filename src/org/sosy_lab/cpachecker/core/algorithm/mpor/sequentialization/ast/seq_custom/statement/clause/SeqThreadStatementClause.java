// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause;

import static org.sosy_lab.common.collect.Collections3.elementAndList;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * A clause features an {@code int} label and a {@link SeqThreadStatementBlock}.
 *
 * <p>e.g. {@code case 42: fib(42); break;} when using switch cases.
 */
public class SeqThreadStatementClause implements SeqStatement {

  // TODO this is not used?
  private static int currentId = 0;

  /** This method is required, otherwise some checks fail. */
  private static int getNewId() {
    return currentId++;
  }

  public final int id;

  public final boolean isLoopStart;

  public final int labelNumber;

  // TODO why not merge block and mergedBlocks into just blocks? should make for better architecture

  /** The case block e.g. {@code fib(42); break;} */
  public final SeqThreadStatementBlock block;

  /**
   * The list of merged blocks, that are not directly reachable due to linking and atomic merging.
   */
  public final ImmutableList<SeqThreadStatementBlock> mergedBlocks;

  public SeqThreadStatementClause(boolean pIsLoopStart, SeqThreadStatementBlock pBlock) {
    id = getNewId();
    isLoopStart = pIsLoopStart;
    labelNumber = pBlock.getLabel().labelNumber;
    block = pBlock;
    mergedBlocks = ImmutableList.of();
  }

  /** Private constructor, only used during cloning process to keep the same id. */
  private SeqThreadStatementClause(
      int pId,
      boolean pIsLoopStart,
      int pLabelNumber,
      SeqThreadStatementBlock pBlock,
      ImmutableList<SeqThreadStatementBlock> pMergedBlocks) {

    id = pId;
    isLoopStart = pIsLoopStart;
    labelNumber = pLabelNumber;
    block = pBlock;
    mergedBlocks = pMergedBlocks;
  }

  public ImmutableList<SeqThreadStatementBlock> getAllBlocks() {
    return elementAndList(block, mergedBlocks);
  }

  public ImmutableList<SeqThreadStatement> getAllStatements() {
    ImmutableList.Builder<SeqThreadStatement> rAll = ImmutableList.builder();
    rAll.addAll(block.getStatements());
    for (SeqThreadStatementBlock mergedBlock : mergedBlocks) {
      rAll.addAll(mergedBlock.getStatements());
    }
    return rAll.build();
  }

  public SeqThreadStatementClause cloneWithBlock(SeqThreadStatementBlock pBlock) {
    return new SeqThreadStatementClause(id, isLoopStart, labelNumber, pBlock, mergedBlocks);
  }

  public SeqThreadStatementClause cloneWithMergedBlocks(
      ImmutableList<SeqThreadStatementBlock> pMergedBlocks) {

    return new SeqThreadStatementClause(id, isLoopStart, labelNumber, block, pMergedBlocks);
  }

  public SeqThreadStatementClause cloneWithLabelNumber(int pLabelNumber) {
    return new SeqThreadStatementClause(id, isLoopStart, pLabelNumber, block, mergedBlocks);
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

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    StringBuilder rString = new StringBuilder();
    rString.append(block.toASTString());
    for (SeqThreadStatementBlock mergedBlock : mergedBlocks) {
      rString.append(mergedBlock.toASTString());
    }
    return rString.toString();
  }
}
