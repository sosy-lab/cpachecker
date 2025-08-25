// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements.FunctionStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_synchronization.ThreadSynchronizationVariables;

public class GhostVariables {

  public final FunctionStatements function;

  public final ProgramCounterVariables pc;

  public final ThreadSynchronizationVariables thread;

  public GhostVariables(
      FunctionStatements pFunction, ProgramCounterVariables pPc, ThreadSynchronizationVariables pThread) {

    function = pFunction;
    pc = pPc;
    thread = pThread;
  }
}
