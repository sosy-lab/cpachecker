// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineViolationConditionsOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class CombineCompositeStateViolationConditionOperator
    implements CombineViolationConditionsOperator {

  private final List<ConfigurableProgramAnalysis> wrapped;
  private final CFANode node;

  public CombineCompositeStateViolationConditionOperator(
      List<ConfigurableProgramAnalysis> pWrapped, CFANode pInitialNode) {
    wrapped = pWrapped;
    node = pInitialNode;
  }

  @Override
  public AbstractState combineViolationConditionsAtSameProgramHash(
      Optional<AbstractState> origin, Collection<AbstractState> states)
      throws InterruptedException, CPAException {
    ImmutableList.Builder<AbstractState> wrappedStates = ImmutableList.builder();
    for (int i = 0; i < wrapped.size(); i++) {
      ImmutableList.Builder<AbstractState> statesToCombine =
          ImmutableList.builderWithExpectedSize(states.size());
      for (AbstractState state : states) {
        CompositeState compositeState = (CompositeState) state;
        AbstractState wrappedState = compositeState.getWrappedStates().get(i);
        statesToCombine.add(wrappedState);
      }
      ImmutableList<AbstractState> cpaStates = statesToCombine.build();
      Class<? extends AbstractState> currClass =
          Iterables.getOnlyElement(transformedImmutableSetCopy(cpaStates, s -> s.getClass()));
      if (wrapped.get(i) instanceof DistributedConfigurableProgramAnalysis dcpa) {
        AbstractState combinedState =
            dcpa.getCombineViolationConditionsOperator()
                .combineViolationConditionsAtSameProgramHash(
                    origin.map(s -> AbstractStates.extractStateByType(s, currClass)), cpaStates);
        wrappedStates.add(combinedState);
      } else {
        wrappedStates.add(
            wrapped.get(i).getInitialState(node, StateSpacePartition.getDefaultPartition()));
      }
    }
    return new CompositeState(wrappedStates.build());
  }
}
