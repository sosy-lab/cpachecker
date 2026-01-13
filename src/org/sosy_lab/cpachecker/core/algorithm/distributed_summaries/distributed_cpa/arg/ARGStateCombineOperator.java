// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.arg;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ARGStateCombineOperator implements CombineOperator {

  private final DistributedConfigurableProgramAnalysis wrappedCpa;

  public ARGStateCombineOperator(DistributedConfigurableProgramAnalysis pWrappedCpa) {
    wrappedCpa = pWrappedCpa;
  }

  @Override
  public AbstractState combine(Collection<AbstractState> states)
      throws CPAException, InterruptedException {
    FluentIterable<@NonNull ARGState> argStates =
        FluentIterable.from(states).filter(ARGState.class);
    Preconditions.checkState(argStates.size() == states.size(), "All states must be ARGStates.");
    ImmutableList<@NonNull AbstractState> wrappedStates =
        argStates.transform(ARGState::getWrappedState).toList();
    AbstractState combined = wrappedCpa.getCombineOperator().combine(wrappedStates);
    return new ARGState(combined, null);
  }
}
