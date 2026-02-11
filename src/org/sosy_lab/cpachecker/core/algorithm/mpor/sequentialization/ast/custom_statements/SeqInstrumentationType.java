// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements;

public enum SeqInstrumentationType {
  BIT_VECTOR_UPDATE(true, true),
  BLOCK_LABEL(false, false),
  GOTO_BLOCK_LABEL(false, false),
  GUARDED_GOTO(true, true),
  LAST_BIT_VECTOR_UPDATE(true, true),
  LAST_THREAD_UPDATE(true, true),
  PROGRAM_COUNTER_UPDATE(false, false),
  THREAD_COUNT_UPDATE(false, false),
  THREAD_SYNC_UPDATE(true, false),
  UNTIL_CONFLICT_REDUCTION(true, false);

  /**
   * Whether this statement type can be pruned from its owning {@link SeqThreadStatement} if it
   * contains a target {@code goto} instead of a target {@code pc}.
   *
   * <p>If a target {@code goto} is present, then the simulation stays in the same thread. Some
   * {@link SeqInstrumentation}s update e.g. ghost variables that are utilized by other threads. But
   * if no context-switch occurs due to the {@code goto}, then the ghost variable updates are
   * unnecessary and can be pruned.
   */
  public final boolean isPrunedWithTargetGoto;

  /**
   * Whether this statement type can be pruned from its owning {@link SeqThreadStatement} when at
   * least one {@link SeqInstrumentation} contains an empty bit vector evaluation expression.
   */
  public final boolean isPrunedWithEmptyBitVectorEvaluation;

  SeqInstrumentationType(
      boolean pIsPrunedWithTargetGoto, boolean pIsPrunedWithEmptyBitVectorEvaluation) {

    isPrunedWithTargetGoto = pIsPrunedWithTargetGoto;
    isPrunedWithEmptyBitVectorEvaluation = pIsPrunedWithEmptyBitVectorEvaluation;
  }
}
