// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.ViolationConditionOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;

public class ValueViolationConditionOperator implements ViolationConditionOperator {
  public final MachineModel machineModel;

  public ValueViolationConditionOperator(MachineModel pMachineModel) {
    machineModel = pMachineModel;
  }

  @Override
  public Optional<AbstractState> computeViolationCondition(
      ARGPath pARGPath,
      Optional<ARGState> pPreviousCondition) {

    ImmutableList<ARGState> states = pARGPath.asStatesList();
    assert !states.isEmpty();

    AbstractState entry = states.getFirst().getWrappedState();
    if (!(entry instanceof CompositeState cS))
      return Optional.of(new ValueAnalysisState(machineModel));
    for (AbstractState state : cS.getWrappedStates()) {
      if (state instanceof ValueAnalysisState)
        return Optional.of(state);
    }
    return Optional.of(new ValueAnalysisState(machineModel));
  }
}
