// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements;

import static org.sosy_lab.common.collect.Collections3.elementAndList;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.util.cwriter.export.CBreakStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatementElement;
import org.sosy_lab.cpachecker.util.cwriter.export.CContinueStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CGotoStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CReturnStatementWrapper;

/**
 * A clause features an {@code int} label and a list of {@link SeqThreadStatementBlock}. A clause is
 * reachable from outside a thread simulation via its {@code pc} label. Initially each {@link
 * SeqThreadStatementClause} has only one {@link SeqThreadStatementBlock}, but after merging e.g.
 * atomic blocks or linking local statements a {@link SeqThreadStatementClause} can contain multiple
 * {@link SeqThreadStatementBlock}.
 *
 * <p>e.g. {@code case 42: fib(42); break;} for {@link MultiSelectionStatementEncoding#SWITCH_CASE}
 * with {@code fib(42);} as the only {@link SeqThreadStatementBlock}.
 */
public final class SeqThreadStatementClause implements SeqExportStatement {

  private static int currentId = 0;

  /** This method is required, otherwise some checks fail. */
  private static int getNewId() {
    return currentId++;
  }

  /** The ID of a clause, used to find out which statements were linked already. */
  public final int id;

  private final MPOROptions options;

  public final int labelNumber;

  /**
   * The block e.g. {@code T0_0: fib(42); break;} and merged blocks that directly reachable due to
   * linking and atomic merging.
   */
  private final ImmutableList<SeqThreadStatementBlock> blocks;

  public SeqThreadStatementClause(MPOROptions pOptions, SeqThreadStatementBlock pBlock) {
    id = getNewId();
    options = pOptions;
    labelNumber = pBlock.getLabelNumber();
    blocks = ImmutableList.of(pBlock);
  }

  public SeqThreadStatementClause(
      MPOROptions pOptions, ImmutableList<SeqThreadStatementBlock> pBlocks) {

    id = getNewId();
    options = pOptions;
    labelNumber = pBlocks.getFirst().getLabelNumber();
    blocks = pBlocks;
  }

  /** Private constructor, only used during cloning process to keep the same id. */
  private SeqThreadStatementClause(
      int pId,
      MPOROptions pOptions,
      int pLabelNumber,
      SeqThreadStatementBlock pBlock,
      ImmutableList<SeqThreadStatementBlock> pMergedBlocks) {

    id = pId;
    options = pOptions;
    labelNumber = pLabelNumber;
    blocks = elementAndList(pBlock, pMergedBlocks);
  }

  /** Private constructor, only used during cloning process to keep the same id. */
  private SeqThreadStatementClause(
      int pId,
      MPOROptions pOptions,
      int pLabelNumber,
      ImmutableList<SeqThreadStatementBlock> pBlocks) {

    id = pId;
    options = pOptions;
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

  public SeqThreadStatementClause withFirstBlock(SeqThreadStatementBlock pBlock) {
    return new SeqThreadStatementClause(id, options, labelNumber, pBlock, getMergedBlocks());
  }

  public SeqThreadStatementClause withMergedBlocks(
      ImmutableList<SeqThreadStatementBlock> pMergedBlocks) {

    return new SeqThreadStatementClause(id, options, labelNumber, getFirstBlock(), pMergedBlocks);
  }

  public SeqThreadStatementClause withBlocks(ImmutableList<SeqThreadStatementBlock> pAllBlocks) {
    return new SeqThreadStatementClause(id, options, labelNumber, pAllBlocks);
  }

  public SeqThreadStatementClause withLabelNumber(int pLabelNumber) {
    return new SeqThreadStatementClause(id, options, pLabelNumber, blocks);
  }

  /** Returns true if all statements in all blocks are blank. */
  public boolean isBlank() {
    for (SeqThreadStatementBlock block : blocks) {
      for (SeqThreadStatement statement : block.getStatements()) {
        if (!statement.isOnlyPcWrite()) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public ImmutableList<CCompoundStatementElement> toCExportStatements() {
    ImmutableList.Builder<CCompoundStatementElement> exportedStatements = ImmutableList.builder();
    for (int i = 0; i < blocks.size(); i++) {
      SeqThreadStatementBlock block = blocks.get(i);
      exportedStatements.addAll(block.toCExportStatements());
      tryBuildBlockSuffix(block, i == blocks.size() - 1).ifPresent(s -> exportedStatements.add(s));
    }
    return exportedStatements.build();
  }

  private Optional<CExportStatement> tryBuildBlockSuffix(
      SeqThreadStatementBlock pBlock, boolean pIsLastBlock) {

    // if all statements have a 'goto', then the suffix is never reached
    if (SeqThreadStatementUtil.allHaveTargetGoto(pBlock.getStatements())) {
      return Optional.empty();
    }

    // if the bit vector evaluation is empty, 'abort();' is called and the suffix is never reached
    if (SeqThreadStatementUtil.anyContainsEmptyBitVectorEvaluationExpression(
        pBlock.getStatements())) {
      return Optional.empty();
    }

    // use control encoding of the statement since we append the suffix to the statement
    return switch (options.controlEncodingStatement()) {
      case NONE ->
          throw new IllegalArgumentException(
              "cannot build suffix for control encoding " + options.controlEncodingStatement());
      case BINARY_SEARCH_TREE, IF_ELSE_CHAIN -> {
        if (options.loopUnrolling()) {
          // with loop unrolling (and separate thread functions) enabled, always return to main()
          yield Optional.of(new CReturnStatementWrapper(Optional.empty()));
        }
        // if this is not the last thread, add "goto T{next_thread_ID};"
        if (pBlock.getNextThreadLabel().isPresent()) {
          yield Optional.of(new CGotoStatement(pBlock.getNextThreadLabel().orElseThrow()));
        }
        // otherwise, continue i.e. go to next loop iteration
        yield Optional.of(new CContinueStatement());
      }
      case SWITCH_CASE -> {
        // do not add 'break;' for the last block, because CSwitchStatement will add it anyway
        if (pIsLastBlock) {
          yield Optional.empty();
        }
        // for all other blocks, add additional 'break;' in between
        yield Optional.of(new CBreakStatement());
      }
    };
  }
}
