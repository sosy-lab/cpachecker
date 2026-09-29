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
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombinePreconditionsOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class CombineCompositeStatePreconditionsOperator implements CombinePreconditionsOperator {

  private final List<ConfigurableProgramAnalysis> wrapped;
  private final CFANode node;

  public CombineCompositeStatePreconditionsOperator(
      List<ConfigurableProgramAnalysis> pWrapped, CFANode pInitialNode) {
    wrapped = pWrapped;
    node = pInitialNode;
  }

  @Override
  public Optional<AbstractState> combineIfPossible(Collection<AbstractState> states)
      throws CPAException, InterruptedException {
    Preconditions.checkArgument(!states.isEmpty(), "States cannot be empty");
    ImmutableList.Builder<AbstractState> result = ImmutableList.builder();
    boolean hasDisjunctiveComponent = false;
    for (int i = 0; i < wrapped.size(); i++) {
      ImmutableList.Builder<AbstractState> components = ImmutableList.builder();
      for (AbstractState state : states) {
        CompositeState composite = (CompositeState) state;
        Preconditions.checkArgument(composite.getWrappedStates().size() == wrapped.size());
        components.add(composite.getWrappedStates().get(i));
      }
      ImmutableList<AbstractState> inputs = components.build();
      if (wrapped.get(i) instanceof DistributedConfigurableProgramAnalysis dcpa) {
        Optional<AbstractState> combined = dcpa.getCombineOperator().combineIfPossible(inputs);
        if (combined.isEmpty()) {
          return Optional.empty();
        }
        if (!dcpa.getCoverageOperator().isBasedOnEquality()) {
          for (AbstractState input : inputs) {
            if (!dcpa.getCoverageOperator().areStatesSyntacticallyEqual(inputs.getFirst(), input)) {
              // Component-wise unions of two varying domains would lose their correlation:
              // (a, b) or (c, d) must not become (a or c, b or d).
              if (hasDisjunctiveComponent) {
                return Optional.empty();
              }
              hasDisjunctiveComponent = true;
              break;
            }
          }
        }
        result.add(combined.orElseThrow());
      } else {
        // A component without a distributed operator may only stay unchanged.
        AbstractState first = inputs.getFirst();
        if (!inputs.stream().allMatch(first::equals)) {
          return Optional.empty();
        }
        result.add(first);
      }
    }
    return Optional.of(new CompositeState(result.build()));
  }

  @Override
  public AbstractState combinePreconditions(Collection<AbstractState> states)
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
        AbstractState combinedState =
            dcpa.getCombineOperator().combinePreconditions(statesToCombine.build());
        wrappedStates.add(combinedState);
      } else {
        wrappedStates.add(
            wrapped.get(i).getInitialState(node, StateSpacePartition.getDefaultPartition()));
      }
    }
    return new CompositeState(wrappedStates.build());
  }
}
