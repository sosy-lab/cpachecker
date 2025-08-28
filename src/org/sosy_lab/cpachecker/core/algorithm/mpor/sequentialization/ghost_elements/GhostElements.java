// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements.FunctionStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_synchronization.ThreadSynchronizationVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

/**
 * Contains ghost elements not present in the input program, e.g. to simulate threads or functions.
 */
public class GhostElements {

  private final Optional<BitVectorVariables> bitVectorVariables;

  private final ImmutableMap<MPORThread, FunctionStatements> functionStatements;

  private final ProgramCounterVariables programCounterVariables;

  private final ThreadSynchronizationVariables threadSynchronizationVariables;

  public GhostElements(
      Optional<BitVectorVariables> pBitVectorVariables,
      ImmutableMap<MPORThread, FunctionStatements> pFunctionStatements,
      ProgramCounterVariables pProgramCounterVariables,
      ThreadSynchronizationVariables pThreadSynchronizationVariables) {

    bitVectorVariables = pBitVectorVariables;
    functionStatements = pFunctionStatements;
    programCounterVariables = pProgramCounterVariables;
    threadSynchronizationVariables = pThreadSynchronizationVariables;
  }

  public Optional<BitVectorVariables> getBitVectorVariables() {
    return bitVectorVariables;
  }

  public ImmutableMap<MPORThread, FunctionStatements> getFunctionStatements() {
    return functionStatements;
  }

  public FunctionStatements getFunctionStatementsByThread(MPORThread pThread) {
    assert functionStatements.containsKey(pThread)
        : "functionStatements does not contain pThread key";
    return functionStatements.get(pThread);
  }

  public ProgramCounterVariables getPcVariables() {
    return programCounterVariables;
  }

  public ThreadSynchronizationVariables getThreadSynchronizationVariables() {
    return threadSynchronizationVariables;
  }
}
