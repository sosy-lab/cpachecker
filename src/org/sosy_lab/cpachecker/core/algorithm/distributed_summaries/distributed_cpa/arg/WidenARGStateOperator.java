// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.arg;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.widen.WidenOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class WidenARGStateOperator implements WidenOperator {

  private final DistributedConfigurableProgramAnalysis wrapped;

  public WidenARGStateOperator(DistributedConfigurableProgramAnalysis pWrapped) {
    wrapped = pWrapped;
  }

  @Override
  public AbstractState combine(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    Preconditions.checkArgument(state1 instanceof ARGState);
    Preconditions.checkArgument(state2 instanceof ARGState);
    return new ARGState(
        wrapped
            .getCombineOperator()
            .combine(((ARGState) state1).getWrappedState(), ((ARGState) state2).getWrappedState()),
        null);
  }
}
