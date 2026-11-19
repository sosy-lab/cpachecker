// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements;

public enum SeqInstrumentationType {
  BIT_VECTOR_UPDATE(false, true, true),
  BLOCK_LABEL(false, false, false),
  EXECUTE_UNTIL_CONFLICT_REDUCTION(false, true, false),
  GOTO_BLOCK_LABEL(false, false, false),
  GUARDED_GOTO(false, true, true),
  ONCE_UNLOCK(false, false, false),
  PREV_BIT_VECTOR_UPDATE(false, true, true),
  PREV_THREAD_UPDATE(false, true, true),
  PROGRAM_COUNTER_UPDATE(false, false, false),
  RESET_DANGLING_POINTER(true, false, false),
  RESET_OUT_OF_SCOPE_POINTER(false, false, false),
  THREAD_COUNT_UPDATE(false, false, false),
  THREAD_SYNC_UPDATE(false, true, false);

  /**
   * Whether this instrumentation type is placed before statements from the input program. This can
   * be useful when the input statement invalidates a check in the instrumentation, e.g., when a
   * variable is assigned a new value.
   */
  public final boolean isPlacedBeforeStatements;

  /**
   * Whether this instrumentation type can be pruned from its owning {@link SeqThreadStatement} if
   * it contains a target {@code goto} instead of a target {@code pc}.
   *
   * <p>If a target {@code goto} is present, then the simulation stays in the same thread. Some
   * {@link SeqInstrumentation}s update e.g. ghost variables that are utilized by other threads. But
   * if no context-switch occurs due to the {@code goto}, then the ghost variable updates are
   * unnecessary and can be pruned.
   */
  public final boolean isPrunedWithTargetGoto;

  /**
   * Whether this instrumentation type can be pruned from its owning {@link SeqThreadStatement} when
   * at least one {@link SeqInstrumentation} contains an empty bit vector evaluation expression.
   */
  public final boolean isPrunedWithEmptyBitVectorEvaluation;

  SeqInstrumentationType(
      boolean pIsPlacedBeforeStatements,
      boolean pIsPrunedWithTargetGoto,
      boolean pIsPrunedWithEmptyBitVectorEvaluation) {

    isPlacedBeforeStatements = pIsPlacedBeforeStatements;
    isPrunedWithTargetGoto = pIsPrunedWithTargetGoto;
    isPrunedWithEmptyBitVectorEvaluation = pIsPrunedWithEmptyBitVectorEvaluation;
  }
}
