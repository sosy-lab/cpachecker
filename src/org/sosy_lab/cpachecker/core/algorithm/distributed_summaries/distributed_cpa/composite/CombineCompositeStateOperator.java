// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.BlockAnalysisStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class CombineCompositeStateOperator implements CombineOperator {

  private final Map<
          Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
      registered;

  private final BlockAnalysisStatistics stats;

  public CombineCompositeStateOperator(
      Map<Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
          pRegistered,
      BlockAnalysisStatistics pStats) {
    registered = pRegistered;
    stats = pStats;
  }

  @Override
  public List<AbstractState> combine(
      AbstractState pState1, AbstractState pState2, Precision pPrecision)
      throws InterruptedException, CPAException {
    try {
      stats.getCombineCount().inc();
      stats.getCombineTime().start();
    CompositeState compositeState1 = (CompositeState) pState1;
    CompositeState compositeState2 = (CompositeState) pState2;
      if (compositeState1.getWrappedStates().size() != compositeState2.getWrappedStates().size()) {
        throw new AssertionError("CompositeStates have to have the same size");
      }
      for (int i = 0; i < compositeState1.getWrappedStates().size(); i++) {
        AbstractState state1 = compositeState1.get(i);
        AbstractState state2 = compositeState2.get(i);
        if (!state1.getClass().equals(state2.getClass())) {
          throw new AssertionError(
              "All states have to work on equally structured composite states. Mismatch for classes "
                  + state1.getClass()
                  + " and "
                  + state2.getClass());
        }
      }

      List<AbstractState> combined = new ArrayList<>();
      for (int i = 0; i < compositeState1.getWrappedStates().size(); i++) {
        boolean found = false;
        AbstractState state1 = compositeState1.get(i);
        AbstractState state2 = compositeState2.get(i);
        for (DistributedConfigurableProgramAnalysis value : registered.values()) {
          if (value.doesOperateOn(state1.getClass()) && value.doesOperateOn(state2.getClass())) {
            combined.addAll(value.getCombineOperator().combine(state1, state2, pPrecision));
            found = true;
          }
        }
        // merge sep
        if (!found) {
          combined.add(state2);
        }
      }
      return ImmutableList.of(new CompositeState(combined));
    } finally {
      stats.getCombineTime().stop();
    }
  }
}
