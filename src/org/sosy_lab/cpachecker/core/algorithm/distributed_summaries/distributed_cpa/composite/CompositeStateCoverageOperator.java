// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import com.google.common.base.Preconditions;
import java.util.List;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.constraints.DistributedConstraintsCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.CoverageOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class CompositeStateCoverageOperator implements CoverageOperator {

  private final List<ConfigurableProgramAnalysis> wrapped;

  public CompositeStateCoverageOperator(List<ConfigurableProgramAnalysis> pWrapped) {
    wrapped = pWrapped;
  }

  @Override
  public boolean isSubsumed(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    CompositeState compositeState1 = (CompositeState) state1;
    CompositeState compositeState2 = (CompositeState) state2;
    Preconditions.checkArgument(
        compositeState1.getWrappedStates().size() == compositeState2.getWrappedStates().size()
            && compositeState1.getWrappedStates().size() == wrapped.size(),
        "Composite states must have the same number of wrapped states for coverage check.");
    for (int i = 0; i < wrapped.size(); i++) {
      AbstractState wrappedState1 = compositeState1.getWrappedStates().get(i);
      AbstractState wrappedState2 = compositeState2.getWrappedStates().get(i);
      ConfigurableProgramAnalysis wrappedAnalysis = wrapped.get(i);
      if (wrappedAnalysis instanceof DistributedConfigurableProgramAnalysis dcpa) {
        Preconditions.checkState(
            dcpa.doesOperateOn(wrappedState1.getClass())
                && dcpa.doesOperateOn(wrappedState2.getClass()),
            "Wrapped states must be compatible with the corresponding CPA.");

        if (dcpa instanceof DistributedConstraintsCPA) {
          wrappedState1 = compositeState1;
          wrappedState2 = compositeState2;
        }

        if (!dcpa.getCoverageOperator().isSubsumed(wrappedState1, wrappedState2)) {
          return false;
        }
      }
      // TODO: Handle cases where the wrapped analysis does not implement CoverageOperator
    }
    return true;
  }

  @Override
  public boolean isBasedOnEquality() {
    return false;
  }
}
