// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause;

import static org.sosy_lab.common.collect.Collections3.elementAndList;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.CSeqThreadStatement;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportStatement;

/**
 * A clause features an {@code int} label and a list of {@link SeqThreadStatementBlock}. A clause is
 * reachable from outside a thread simulation via its {@code pc} label.
 *
 * <p>e.g. {@code case 42: fib(42); break;} when using switch cases.
 */
public class SeqThreadStatementClause implements CExportStatement {

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
    labelNumber = pBlock.getLabel().number();
    blocks = ImmutableList.of(pBlock);
  }

  public SeqThreadStatementClause(ImmutableList<SeqThreadStatementBlock> pBlocks) {
    id = getNewId();
    labelNumber = pBlocks.getFirst().getLabel().number();
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

  public ImmutableList<CSeqThreadStatement> getAllStatements() {
    ImmutableList.Builder<CSeqThreadStatement> rAll = ImmutableList.builder();
    for (SeqThreadStatementBlock block : blocks) {
      rAll.addAll(block.getStatements());
    }
    return rAll.build();
  }

  public SeqThreadStatementClause withFirstBlock(SeqThreadStatementBlock pBlock) {
    return new SeqThreadStatementClause(id, labelNumber, pBlock, getMergedBlocks());
  }

  public SeqThreadStatementClause withMergedBlocks(
      ImmutableList<SeqThreadStatementBlock> pMergedBlocks) {

    return new SeqThreadStatementClause(id, labelNumber, getFirstBlock(), pMergedBlocks);
  }

  public SeqThreadStatementClause withBlocks(ImmutableList<SeqThreadStatementBlock> pAllBlocks) {

    return new SeqThreadStatementClause(id, labelNumber, pAllBlocks);
  }

  public SeqThreadStatementClause withLabelNumber(int pLabelNumber) {
    return new SeqThreadStatementClause(id, pLabelNumber, blocks);
  }

  /** Returns true if all statements in all blocks are blank. */
  public boolean isBlank() {
    for (SeqThreadStatementBlock block : blocks) {
      for (CSeqThreadStatement statement : block.getStatements()) {
        if (!statement.isOnlyPcWrite()) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    StringBuilder rString = new StringBuilder();
    for (SeqThreadStatementBlock block : blocks) {
      rString.append(block.toASTString(pAAstNodeRepresentation));
    }
    return rString.toString();
  }
}
