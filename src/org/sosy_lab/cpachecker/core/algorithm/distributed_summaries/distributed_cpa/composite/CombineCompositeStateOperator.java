// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class CombineCompositeStateOperator implements CombineOperator {

  private final List<ConfigurableProgramAnalysis> wrapped;
  private final CFANode node;

  public CombineCompositeStateOperator(
      List<ConfigurableProgramAnalysis> pWrapped, CFANode pInitialNode) {
    wrapped = pWrapped;
    node = pInitialNode;
  }

  @Override
  public AbstractState combine(Collection<AbstractState> states)
      throws CPAException, InterruptedException {
    Preconditions.checkArgument(!states.isEmpty(), "States cannot be empty");
    Preconditions.checkArgument(
        states.stream().allMatch(CompositeState.class::isInstance),
        "All states must be of type CompositeState");
    Preconditions.checkArgument(
        states.stream()
            .allMatch(c -> ((CompositeState) c).getWrappedStates().size() == wrapped.size()),
        "All states must have the same number of wrapped states");
    ImmutableList.Builder<AbstractState> wrappedStates = ImmutableList.builder();
    for (int i = 0; i < wrapped.size(); i++) {
      ImmutableList.Builder<AbstractState> statesToCombine =
          ImmutableList.builderWithExpectedSize(states.size());
      for (AbstractState state : states) {
        CompositeState compositeState = (CompositeState) state;
        AbstractState wrappedState = compositeState.getWrappedStates().get(i);
        statesToCombine.add(wrappedState);
      }
      if (wrapped.get(i) instanceof DistributedConfigurableProgramAnalysis dcpa) {
        AbstractState combinedState = dcpa.getCombineOperator().combine(statesToCombine.build());
        wrappedStates.add(combinedState);
      } else {
        wrappedStates.add(
            wrapped.get(i).getInitialState(node, StateSpacePartition.getDefaultPartition()));
      }
    }
    return new CompositeState(wrappedStates.build());
  }
}
