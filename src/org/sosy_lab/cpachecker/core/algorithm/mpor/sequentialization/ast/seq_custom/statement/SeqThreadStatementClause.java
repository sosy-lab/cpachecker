// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement;

import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqSwitchCaseGotoLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
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

  public final int labelNumber;

  /**
   * The goto label for the case, e.g. {@code T0_42;}. It is mandatory for all statements, but may
   * not actually be targeted with a {@code goto}.
   */
  public final SeqSwitchCaseGotoLabelStatement gotoLabel;

  /** The case block e.g. {@code fib(42); break;} */
  public final SeqThreadStatementBlock block;

  public SeqThreadStatementClause(
      MPOROptions pOptions,
      boolean pIsGlobal,
      boolean pIsLoopStart,
      int pThreadId,
      int pLabelNumber,
      SeqThreadStatementBlock pBlock) {

    id = getNewId();
    isGlobal = pIsGlobal;
    isLoopStart = pIsLoopStart;
    labelNumber = pLabelNumber;
    gotoLabel =
        new SeqSwitchCaseGotoLabelStatement(
            SeqNameUtil.buildSwitchCaseGotoLabelPrefix(pOptions, pThreadId), pLabelNumber);
    block = pBlock;
  }

  /** Private constructor, only used during cloning process to keep the same id. */
  private SeqThreadStatementClause(
      int pId,
      boolean pIsGlobal,
      boolean pIsLoopStart,
      int pLabelNumber,
      SeqSwitchCaseGotoLabelStatement pGotoLabel,
      SeqThreadStatementBlock pBlock) {

    id = pId;
    isGlobal = pIsGlobal;
    isLoopStart = pIsLoopStart;
    labelNumber = pLabelNumber;
    gotoLabel = pGotoLabel;
    block = pBlock;
  }

  public SeqThreadStatementClause cloneWithLabelAndBlock(
      int pLabel, SeqThreadStatementBlock pBlock) {

    SeqSwitchCaseGotoLabelStatement newGotoLabel = updateGotoLabel(pLabel);
    return new SeqThreadStatementClause(id, isGlobal, isLoopStart, pLabel, newGotoLabel, pBlock);
  }

  public SeqThreadStatementClause cloneWithLabel(int pLabel) {
    return new SeqThreadStatementClause(id, isGlobal, isLoopStart, pLabel, gotoLabel, block);
  }

  public SeqThreadStatementClause cloneWithBlock(SeqThreadStatementBlock pBlock) {
    return new SeqThreadStatementClause(id, isGlobal, isLoopStart, labelNumber, gotoLabel, pBlock);
  }

  /**
   * Returns an updated {@link SeqSwitchCaseGotoLabelStatement} based on the new {@code pCaseLabel}.
   */
  private SeqSwitchCaseGotoLabelStatement updateGotoLabel(int pNewLabel) {
    return gotoLabel.cloneWithLabelNumber(pNewLabel);
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
      if (statement.isCriticalSectionStart()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    return gotoLabel.toASTString() + SeqSyntax.SPACE + block.toASTString();
  }
}
