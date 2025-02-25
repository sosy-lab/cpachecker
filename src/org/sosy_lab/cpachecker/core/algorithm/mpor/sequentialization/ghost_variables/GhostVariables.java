// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.function.GhostFunctionVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.GhostPcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread.GhostThreadSimulationVariables;

public class GhostVariables {

  public final GhostFunctionVariables function;

  public final GhostPcVariables pc;

  public final GhostThreadSimulationVariables thread;

  public GhostVariables(
      GhostFunctionVariables pFunction,
      GhostPcVariables pPc,
      GhostThreadSimulationVariables pThread) {

    function = pFunction;
    pc = pPc;
    thread = pThread;
  }
}
