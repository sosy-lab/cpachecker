// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements.FunctionStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_synchronization.ThreadSynchronizationVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class GhostElements {

  private final ImmutableMap<MPORThread, FunctionStatements> functionStatements;
  // TODO make private
  public final ProgramCounterVariables programCounterVariables;

  public final ThreadSynchronizationVariables threadSynchronizationVariables;

  public GhostElements(
      ImmutableMap<MPORThread, FunctionStatements> pFunctionStatements,
      ProgramCounterVariables pProgramCounterVariables,
      ThreadSynchronizationVariables pThreadSynchronizationVariables) {

    functionStatements = pFunctionStatements;
    programCounterVariables = pProgramCounterVariables;
    threadSynchronizationVariables = pThreadSynchronizationVariables;
  }

  public ImmutableMap<MPORThread, FunctionStatements> getFunctionStatements() {
    return functionStatements;
  }

  public FunctionStatements getFunctionStatementsByThread(MPORThread pThread) {
    assert functionStatements.containsKey(pThread)
        : "functionStatements does not contain pThread key";
    return functionStatements.get(pThread);
  }
}
