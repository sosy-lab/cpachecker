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
import org.sosy_lab.cpachecker.cfa.ast.c.export.CLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements.FunctionStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.ThreadSyncFlags;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

/**
 * Contains ghost elements not present in the input program, e.g. to simulate threads or functions.
 */
public record GhostElements(
    Optional<BitVectorVariables> bitVectorVariables,
    ImmutableMap<MPORThread, FunctionStatements> functionStatements,
    ProgramCounterVariables programCounterVariables,
    ImmutableMap<MPORThread, CLabelStatement> threadLabels,
    ThreadSyncFlags threadSyncFlags) {

  public FunctionStatements getFunctionStatementsByThread(MPORThread pThread) {
    return Objects.requireNonNull(functionStatements.get(pThread));
  }

  public ProgramCounterVariables getPcVariables() {
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
