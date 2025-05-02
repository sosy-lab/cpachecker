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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.SeqCaseBlockStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqSwitchCaseGotoLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorReduction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

/**
 * A case clause features a {@link SeqSwitchCaseLabel} and a {@link SeqCaseBlock}.
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
  public final SeqSwitchCaseLabel caseLabel;

  /** The goto label for the case, e.g. {@code case_t0_42;} */
  public final Optional<SeqSwitchCaseGotoLabelStatement> gotoLabel;

  /** The case block e.g. {@code fib(42); break;} */
  public final SeqCaseBlock block;

  public SeqCaseClause(
      MPOROptions pOptions,
      boolean pIsGlobal,
      boolean pIsLoopStart,
      Optional<Integer> pThreadId,
      int pLabelValue,
      SeqCaseBlock pBlock) {

    id = getNewId();
    isGlobal = pIsGlobal;
    isLoopStart = pIsLoopStart;
    caseLabel = new SeqSwitchCaseLabel(pLabelValue);
    gotoLabel =
        pThreadId.isPresent() && !pOptions.porBitVectorReduction.equals(BitVectorReduction.NONE)
            ? Optional.of(
                new SeqSwitchCaseGotoLabelStatement(
                    SeqNameUtil.buildSwitchCaseGotoLabelPrefix(pOptions, pThreadId.orElseThrow()),
                    pLabelValue))
            : Optional.empty();
    block = pBlock;
  }

  /** Private constructor, only used during cloning process to keep the same id. */
  private SeqCaseClause(
      int pId,
      boolean pIsGlobal,
      boolean pIsLoopStart,
      SeqSwitchCaseLabel pCaseLabel,
      Optional<SeqSwitchCaseGotoLabelStatement> pGotoLabel,
      SeqCaseBlock pBlock) {

    id = pId;
    isGlobal = pIsGlobal;
    isLoopStart = pIsLoopStart;
    caseLabel = pCaseLabel;
    gotoLabel = pGotoLabel;
    block = pBlock;
  }

  public SeqCaseClause cloneWithCaseLabelAndBlock(
      SeqSwitchCaseLabel pCaseLabel, SeqCaseBlock pBlock) {

    Optional<SeqSwitchCaseGotoLabelStatement> newGotoLabel = updateGotoLabel(pCaseLabel);
    return new SeqCaseClause(id, isGlobal, isLoopStart, pCaseLabel, newGotoLabel, pBlock);
  }

  public SeqCaseClause cloneWithSwitchLabel(SeqSwitchCaseLabel pCaseLabel) {
    return new SeqCaseClause(id, isGlobal, isLoopStart, pCaseLabel, gotoLabel, block);
  }

  public SeqCaseClause cloneWithBlock(SeqCaseBlock pBlock) {
    return new SeqCaseClause(id, isGlobal, isLoopStart, caseLabel, gotoLabel, pBlock);
  }

  /**
   * Returns an updated {@link SeqSwitchCaseGotoLabelStatement} based on the new {@code pCaseLabel}.
   */
  private Optional<SeqSwitchCaseGotoLabelStatement> updateGotoLabel(
      SeqSwitchCaseLabel pNewCaseLabel) {

    if (gotoLabel.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(gotoLabel.orElseThrow().cloneWithLabelNumber(pNewCaseLabel.value));
    }
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
    String gotoLabelString =
        gotoLabel.isPresent() ? gotoLabel.orElseThrow().toASTString() : SeqSyntax.EMPTY_STRING;
    return caseLabel.toASTString()
        + SeqSyntax.SPACE
        + gotoLabelString
        + SeqSyntax.SPACE
        + blockString
        + SeqSyntax.NEWLINE;
  }
}
