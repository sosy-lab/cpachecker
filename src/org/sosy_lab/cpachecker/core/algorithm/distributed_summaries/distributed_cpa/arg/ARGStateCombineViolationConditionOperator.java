// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.arg;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineViolationConditionsOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ARGStateCombineViolationConditionOperator
    implements CombineViolationConditionsOperator {

  private final DistributedConfigurableProgramAnalysis wrappedCpa;

  public ARGStateCombineViolationConditionOperator(
      DistributedConfigurableProgramAnalysis pWrappedCpa) {
    wrappedCpa = pWrappedCpa;
  }

  @Override
  public AbstractState combineViolationConditionsAtSameProgramHash(Collection<AbstractState> states)
      throws InterruptedException, CPAException {
    return combineIfPossible(states)
        .orElseThrow(
            () -> new IllegalArgumentException("Cannot combine incompatible violation conditions"));
  }

  @Override
  public Optional<AbstractState> combineIfPossible(Collection<AbstractState> states)
      throws InterruptedException, CPAException {
    FluentIterable<@NonNull ARGState> argStates =
        FluentIterable.from(states).filter(ARGState.class);
    Preconditions.checkState(argStates.size() == states.size(), "All states must be ARGStates.");
    ImmutableList<@NonNull AbstractState> wrappedStates =
        argStates.transform(ARGState::getWrappedState).toList();
    Optional<AbstractState> combined =
        wrappedCpa.getCombineViolationConditionsOperator().combineIfPossible(wrappedStates);
    return combined.map(state -> new ARGState(state, null));
  }
}
