// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.CoverageOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class CompositeStateCoverageOperator implements CoverageOperator {

  private final Map<
          Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
      registered;

  public CompositeStateCoverageOperator(
      Map<Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
          pRegistered) {
    registered = pRegistered;
  }

  @Override
  public boolean covers(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    CompositeState compositeState1 = (CompositeState) state1;
    CompositeState compositeState2 = (CompositeState) state2;
    Preconditions.checkArgument(
        compositeState1.getWrappedStates().size() == compositeState2.getWrappedStates().size(),
        "Composite states must have the same number of wrapped states for coverage check.");
    ImmutableMap.Builder<Class<? extends AbstractState>, AbstractState> firstState =
        ImmutableMap.builder();
    for (AbstractState wrapped : compositeState1.getWrappedStates()) {
      firstState.put(wrapped.getClass(), wrapped);
    }
    // TODO: ignores states of cpas that are not registered
    Map<Class<? extends AbstractState>, AbstractState> firstStateMap = firstState.buildOrThrow();
    for (AbstractState wrapped : compositeState2.getWrappedStates()) {
      for (DistributedConfigurableProgramAnalysis dcpa : registered.values()) {
        if (dcpa.doesOperateOn(wrapped.getClass())) {
          if (!dcpa.getCoverageOperator().covers(firstStateMap.get(wrapped.getClass()), wrapped)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  @Override
  public boolean isBasedOnEquality() {
    return false;
  }
}
