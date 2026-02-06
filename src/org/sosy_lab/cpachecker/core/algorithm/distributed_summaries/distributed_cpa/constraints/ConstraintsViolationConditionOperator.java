// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.constraints;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.ViolationConditionOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;

public class ConstraintsViolationConditionOperator implements ViolationConditionOperator {
  @Override
  public Optional<AbstractState> computeViolationCondition(
      ARGPath pARGPath, Optional<ARGState> pPreviousCondition) {
    ImmutableList<ARGState> states = pARGPath.asStatesList();
    assert !states.isEmpty();

    AbstractState violation = states.getLast().getWrappedState();
    if (!(violation instanceof CompositeState cS)) {
      return Optional.of(new ConstraintsState());
    }
    for (AbstractState state : cS.getWrappedStates()) {
      if (state instanceof ConstraintsState) {
        return Optional.of(state);
      }
    }
    return Optional.of(new ConstraintsState());
  }
}
