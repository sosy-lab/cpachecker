// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.function_statements.FunctionStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.thread_simulation.ThreadSimulationVariables;

public class GhostVariables {

  public final FunctionStatements function;

  public final PcVariables pc;

  public final ThreadSimulationVariables thread;

  public GhostVariables(
      FunctionStatements pFunction, PcVariables pPc, ThreadSimulationVariables pThread) {

    function = pFunction;
    pc = pPc;
    thread = pThread;
  }
}
