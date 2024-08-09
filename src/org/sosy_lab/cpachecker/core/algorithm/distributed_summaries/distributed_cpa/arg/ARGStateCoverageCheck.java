// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.arg;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.fixpoint.CoverageCheck;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ARGStateCoverageCheck implements CoverageCheck {

  private final DistributedConfigurableProgramAnalysis wrapped;

  public ARGStateCoverageCheck(DistributedConfigurableProgramAnalysis pWrapped) {
    wrapped = pWrapped;
  }

  @Override
  public boolean isCovered(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    ARGState argState1 = (ARGState) state1;
    ARGState argState2 = (ARGState) state2;
    return wrapped
        .getCoverageCheck()
        .isCovered(argState1.getWrappedState(), argState2.getWrappedState());
  }
}
