// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement;

import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqSwitchCaseGotoLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorReduction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
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

  public final int label;

  /** The goto label for the case, e.g. {@code case_t0_42;} */
  public final Optional<SeqSwitchCaseGotoLabelStatement> gotoLabel;

  /** The case block e.g. {@code fib(42); break;} */
  public final SeqThreadStatementBlock block;

  public SeqThreadStatementClause(
      MPOROptions pOptions,
      boolean pIsGlobal,
      boolean pIsLoopStart,
      Optional<Integer> pThreadId,
      int pLabel,
      SeqThreadStatementBlock pBlock) {

    id = getNewId();
    isGlobal = pIsGlobal;
    isLoopStart = pIsLoopStart;
    label = pLabel;
    gotoLabel =
        gotoLabelNecessary(pOptions, pThreadId)
            ? Optional.of(
                new SeqSwitchCaseGotoLabelStatement(
                    SeqNameUtil.buildSwitchCaseGotoLabelPrefix(pOptions, pThreadId.orElseThrow()),
                    pLabel))
            : Optional.empty();
    block = pBlock;
  }

  /** Private constructor, only used during cloning process to keep the same id. */
  private SeqThreadStatementClause(
      int pId,
      boolean pIsGlobal,
      boolean pIsLoopStart,
      int pLabel,
      Optional<SeqSwitchCaseGotoLabelStatement> pGotoLabel,
      SeqThreadStatementBlock pBlock) {

    id = pId;
    isGlobal = pIsGlobal;
    isLoopStart = pIsLoopStart;
    label = pLabel;
    gotoLabel = pGotoLabel;
    block = pBlock;
  }

  private boolean gotoLabelNecessary(MPOROptions pOptions, Optional<Integer> pThreadId) {
    return pThreadId.isPresent()
        && (!pOptions.porBitVectorReduction.equals(BitVectorReduction.NONE)
            || pOptions.threadLoops);
  }

  public SeqThreadStatementClause cloneWithLabelAndBlock(
      int pLabel, SeqThreadStatementBlock pBlock) {

    Optional<SeqSwitchCaseGotoLabelStatement> newGotoLabel = updateGotoLabel(pLabel);
    return new SeqThreadStatementClause(id, isGlobal, isLoopStart, pLabel, newGotoLabel, pBlock);
  }

  public SeqThreadStatementClause cloneWithLabel(int pLabel) {
    return new SeqThreadStatementClause(id, isGlobal, isLoopStart, pLabel, gotoLabel, block);
  }

  public SeqThreadStatementClause cloneWithBlock(SeqThreadStatementBlock pBlock) {
    return new SeqThreadStatementClause(id, isGlobal, isLoopStart, label, gotoLabel, pBlock);
  }

  /**
   * Returns an updated {@link SeqSwitchCaseGotoLabelStatement} based on the new {@code pCaseLabel}.
   */
  private Optional<SeqSwitchCaseGotoLabelStatement> updateGotoLabel(int pNewLabel) {

    if (gotoLabel.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(gotoLabel.orElseThrow().cloneWithLabelNumber(pNewLabel));
    }
  }

  /**
   * Returns true if all statements in the {@link SeqThreadStatementBlock} are blank, i.e. they only
   * update a pc.
   */
  public boolean onlyWritesPc() {
    for (SeqThreadStatement statement : block.statements) {
      if (!statement.onlyWritesPc()) {
        return false;
      }
    }
    return true;
  }

  /** Returns {@code true} if any statement in this case clause is a start to a critical section. */
  public boolean isCriticalSectionStart() {
    for (SeqThreadStatement statement : block.statements) {
      if (!statement.isCriticalSectionStart()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    String blockString = block.toASTString();
    String gotoLabelString =
        gotoLabel.isPresent()
            ? gotoLabel.orElseThrow().toASTString() + SeqSyntax.SPACE
            : SeqSyntax.EMPTY_STRING;
    return gotoLabelString + blockString;
  }
}
