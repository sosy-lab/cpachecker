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

  public CombineCompositeStateOperator(
      Map<Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
          pRegistered) {
    registered = pRegistered;
  }

  @Override
  public List<AbstractState> combine(
      AbstractState pState1, AbstractState pState2, Precision pPrecision)
      throws InterruptedException, CPAException {

    CompositeState compositeState1 = (CompositeState) pState1;
    CompositeState compositeState2 = (CompositeState) pState2;
    if (compositeState1.getWrappedStates().size() != compositeState2.getWrappedStates().size()) {
      throw new AssertionError("CompositeStates have to have the same size");
    }
    for (int i = 0; i < compositeState1.getWrappedStates().size(); i++) {
      AbstractState state1I = compositeState1.get(i);
      AbstractState state2I = compositeState2.get(i);
      if (!state1I.getClass().equals(state2I.getClass())) {
        throw new AssertionError(
            "All states have to work on equally structured composite states. Mismatch for classes "
                + state1I.getClass()
                + " and "
                + state2I.getClass());
      }
    }

    List<AbstractState> combined = new ArrayList<>();
    for (int i = 0; i < compositeState1.getWrappedStates().size(); i++) {
      boolean found = false;
      AbstractState state1I = compositeState1.get(i);
      AbstractState state2I = compositeState2.get(i);
      for (DistributedConfigurableProgramAnalysis value : registered.values()) {
        if (value.doesOperateOn(state1I.getClass()) && value.doesOperateOn(state2I.getClass())) {
          combined.addAll(value.getCombineOperator().combine(state1I, state2I, pPrecision));
          found = true;
        }
      }
      // merge sep
      if (!found) {
        combined.add(state2I);
      }
    }
    return ImmutableList.of(new CompositeState(combined));
  }
}
