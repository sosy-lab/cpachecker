// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.location;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineViolationConditionsOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;

public class LocationStateCombineViolationConditionOperator
    implements CombineViolationConditionsOperator {

  @Override
  public AbstractState combineViolationConditionsAtSameProgramHash(
      Optional<AbstractState> origin, Collection<AbstractState> states) {
    ImmutableSet<CFANode> locations =
        FluentIterable.from(states)
            .filter(LocationState.class)
            .transform(LocationState::getLocationNode)
            .toSet();
    Preconditions.checkState(locations.size() == 1, "All states must have the same location");
    return Iterables.getFirst(states, null);
  }
}
