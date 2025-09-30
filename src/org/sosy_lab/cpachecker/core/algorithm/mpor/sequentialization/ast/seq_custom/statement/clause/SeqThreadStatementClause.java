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
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
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

  private static int currentId = 0;

  /** This method is required, otherwise some checks fail. */
  private static int getNewId() {
    return currentId++;
  }

  /** The ID of a clause, used to find out which statements were linked already. */
  public final int id;

  public final int labelNumber;

  /**
   * The block e.g. {@code T0_0: fib(42); break;} and merged blocks that directly reachable due to
   * linking and atomic merging.
   */
  private final ImmutableList<SeqThreadStatementBlock> blocks;

  public SeqThreadStatementClause(SeqThreadStatementBlock pBlock) {
    id = getNewId();
    labelNumber = pBlock.getLabel().getNumber();
    blocks = ImmutableList.of(pBlock);
  }

  public SeqThreadStatementClause(ImmutableList<SeqThreadStatementBlock> pBlocks) {
    id = getNewId();
    labelNumber = pBlocks.getFirst().getLabel().getNumber();
    blocks = pBlocks;
  }

  /** Private constructor, only used during cloning process to keep the same id. */
  private SeqThreadStatementClause(
      int pId,
      int pLabelNumber,
      SeqThreadStatementBlock pBlock,
      ImmutableList<SeqThreadStatementBlock> pMergedBlocks) {

    id = pId;
    labelNumber = pLabelNumber;
    blocks = elementAndList(pBlock, pMergedBlocks);
  }

  /** Private constructor, only used during cloning process to keep the same id. */
  private SeqThreadStatementClause(
      int pId, int pLabelNumber, ImmutableList<SeqThreadStatementBlock> pBlocks) {

    id = pId;
    labelNumber = pLabelNumber;
    blocks = pBlocks;
  }

  public SeqThreadStatementBlock getFirstBlock() {
    return blocks.getFirst();
  }

  public ImmutableList<SeqThreadStatementBlock> getMergedBlocks() {
    return MPORUtil.withoutElement(blocks, getFirstBlock());
  }

  public ImmutableList<SeqThreadStatementBlock> getBlocks() {
    return blocks;
  }

  public ImmutableList<SeqThreadStatement> getAllStatements() {
    ImmutableList.Builder<SeqThreadStatement> rAll = ImmutableList.builder();
    for (SeqThreadStatementBlock block : blocks) {
      rAll.addAll(block.getStatements());
    }
    return rAll.build();
  }

  public SeqThreadStatementClause cloneWithFirstBlock(SeqThreadStatementBlock pBlock) {
    return new SeqThreadStatementClause(id, labelNumber, pBlock, getMergedBlocks());
  }

  public SeqThreadStatementClause cloneWithMergedBlocks(
      ImmutableList<SeqThreadStatementBlock> pMergedBlocks) {

    return new SeqThreadStatementClause(id, labelNumber, getFirstBlock(), pMergedBlocks);
  }

  public SeqThreadStatementClause cloneWithBlocks(
      ImmutableList<SeqThreadStatementBlock> pAllBlocks) {

    return new SeqThreadStatementClause(id, labelNumber, pAllBlocks);
  }

  public SeqThreadStatementClause cloneWithLabelNumber(int pLabelNumber) {
    return new SeqThreadStatementClause(id, pLabelNumber, blocks);
  }

  /**
   * Returns true if all statements in the first {@link SeqThreadStatementBlock} are blank, i.e.
   * they only update a pc.
   */
  public boolean onlyWritesPc() {
    for (SeqThreadStatement statement : getFirstBlock().getStatements()) {
      if (!statement.onlyWritesPc()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    StringBuilder rString = new StringBuilder();
    for (SeqThreadStatementBlock block : blocks) {
      rString.append(block.toASTString());
    }
    return rString.toString();
  }
}
