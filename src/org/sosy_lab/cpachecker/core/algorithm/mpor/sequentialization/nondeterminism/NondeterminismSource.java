// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism;

public enum NondeterminismSource {
  /** When the next thread executing a (single) statement is chosen non-deterministically. */
  NEXT_THREAD(false, true),
  /** When the number of executed statements is chosen non-deterministically. */
  NUM_STATEMENTS(true, false),
  /**
   * When both the next thread executing a (single) statement and the number of executed statements
   * is chosen non-deterministically.
   */
  NEXT_THREAD_AND_NUM_STATEMENTS(true, true);

  private final boolean isNumStatementsNondeterministic;

  private final boolean isNextThreadNondeterministic;

  NondeterminismSource(
      boolean pIsNumStatementsNondeterministic, boolean pIsNextThreadNondeterministic) {

    isNumStatementsNondeterministic = pIsNumStatementsNondeterministic;
    isNextThreadNondeterministic = pIsNextThreadNondeterministic;
  }

  public boolean isNumStatementsNondeterministic() {
    return isNumStatementsNondeterministic;
  }

  public boolean isNextThreadNondeterministic() {
    return isNextThreadNondeterministic;
  }
}
