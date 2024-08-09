// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.widen.WidenOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class WidenCompositeStateOperator implements WidenOperator {

  private final CompositeCPA compositeCPA;
  private final ImmutableMap<
          Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
      analyses;

  public WidenCompositeStateOperator(
      CompositeCPA pCompositeCPA,
      ImmutableMap<
              Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
          pAnalyses) {
    compositeCPA = pCompositeCPA;
    analyses = pAnalyses;
  }

  @Override
  public AbstractState combine(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    CompositeState compositeState1 = (CompositeState) state1;
    CompositeState compositeState2 = (CompositeState) state2;
    Preconditions.checkArgument(
        compositeState1.getWrappedStates().size() == compositeState2.getWrappedStates().size());
    CFANode location = AbstractStates.extractLocation(compositeState1);
    ImmutableList.Builder<AbstractState> combined = ImmutableList.builder();
    for (ConfigurableProgramAnalysis analysis : compositeCPA.getWrappedCPAs()) {
      if (!analyses.containsKey(analysis.getClass())) {
        combined.add(analysis.getInitialState(location, StateSpacePartition.getDefaultPartition()));
        continue;
      }
      DistributedConfigurableProgramAnalysis dcpa = analyses.get(analysis.getClass());
      AbstractState combinedState =
          dcpa.getCombineOperator()
              .combine(
                  Objects.requireNonNull(
                      AbstractStates.extractStateByType(
                          compositeState1, dcpa.getAbstractStateClass()),
                      "State not found: "
                          + dcpa.getAbstractStateClass()
                          + " in "
                          + compositeState1),
                  Objects.requireNonNull(
                      AbstractStates.extractStateByType(
                          compositeState2, dcpa.getAbstractStateClass()),
                      "State not found: "
                          + dcpa.getAbstractStateClass()
                          + " in "
                          + compositeState2));
      if (combinedState.getClass() != dcpa.getAbstractStateClass()) {
        combinedState =
            analysis.getInitialState(location, StateSpacePartition.getDefaultPartition());
      }
      combined.add(combinedState);
    }
    return new CompositeState(combined.build());
  }
}
