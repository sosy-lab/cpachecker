// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements.SeqFunctionStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.SeqProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.SeqThreadSyncFlags;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.util.cwriter.export.CLabelStatement;

/**
 * Contains ghost elements not present in the input program, e.g. to simulate threads or functions.
 */
public record SeqGhostElements(
    Optional<SeqBitVectorVariables> bitVectorVariables,
    ImmutableMap<MPORThread, SeqFunctionStatements> functionStatements,
    SeqProgramCounterVariables programCounterVariables,
    ImmutableMap<MPORThread, CLabelStatement> threadLabels,
    SeqThreadSyncFlags threadSyncFlags) {

  public SeqFunctionStatements getFunctionStatementsByThread(MPORThread pThread) {
    return Objects.requireNonNull(functionStatements.get(pThread));
  }

  public SeqProgramCounterVariables getPcVariables() {
    return programCounterVariables;
  }

  /**
   * Returns the {@link CLabelStatement} of the next thread relative to {@code pThread}, i.e. the
   * one with ID {@code pThread.id() + 1}. Returns {@link Optional#empty()} if {@code pThread} is
   * the last thread, or if there are no {@link CLabelStatement}s at all.
   */
  public Optional<CLabelStatement> tryGetNextThreadLabel(MPORThread pThread) {
    // shortcut if there are no thread labels (because they are unnecessary due to the options)
    if (threadLabels.isEmpty()) {
      return Optional.empty();
    }
    return threadLabels.entrySet().stream()
        .filter(entry -> entry.getKey().id() == pThread.id() + 1)
        .map(Map.Entry::getValue)
        .findFirst();
  }
}
