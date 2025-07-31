// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Map;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class CombineCompositeStateOperator implements CombineOperator {

  private final Map<ConfigurableProgramAnalysis, DistributedConfigurableProgramAnalysis> registered;

  public CombineCompositeStateOperator(
      Map<ConfigurableProgramAnalysis, DistributedConfigurableProgramAnalysis> pRegistered) {
    registered = pRegistered;
  }

  @Override
  public AbstractState combine(Collection<AbstractState> states)
      throws CPAException, InterruptedException {
    Preconditions.checkArgument(!states.isEmpty(), "states cannot be empty");
    Preconditions.checkArgument(
        states.stream().allMatch(CompositeState.class::isInstance),
        "All states must be of type CallstackState");

    // we need the reference to know the ordering of states in the wrapped list.
    CompositeState reference = (CompositeState) Iterables.get(states, 0);
    Preconditions.checkNotNull(reference, "Reference composite state cannot be null");
    if (states.size() == 1) {
      return reference;
    }

    // find the collection of states per state class
    Multimap<Class<? extends AbstractState>, AbstractState> statesPerClass =
        ArrayListMultimap.create(states.size(), reference.getWrappedStates().size());
    for (AbstractState state : states) {
      CompositeState compositeState = (CompositeState) state;
      for (AbstractState wrappedState : compositeState.getWrappedStates()) {
        statesPerClass.put(wrappedState.getClass(), wrappedState);
      }
    }

    // combine the grouped states
    ImmutableList.Builder<AbstractState> combinedStates = ImmutableList.builder();
    for (AbstractState wrappedState : reference.getWrappedStates()) {
      boolean found = false;
      for (DistributedConfigurableProgramAnalysis dcpa : registered.values()) {
        if (dcpa.doesOperateOn(wrappedState.getClass())) {
          found = true;
          AbstractState combined =
              dcpa.getCombineOperator().combine(statesPerClass.get(wrappedState.getClass()));
          combinedStates.add(combined);
          break;
        }
      }
      if (!found) {
        throw new UnregisteredDistributedCpaError(
            "Cannot find combination of " + wrappedState.getClass());
      }
    }
    return new CompositeState(combinedStates.build());
  }
}
