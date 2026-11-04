// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer;

import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineViolationConditionsOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.EqualityCombinePreconditionsOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState;

/** Exact combination requires equal pointer assignments and equal exit-handler stacks. */
final class FunctionPointerStateCombinePreconditionsOperator
    extends EqualityCombinePreconditionsOperator implements CombineViolationConditionsOperator {

  FunctionPointerStateCombinePreconditionsOperator() {
    super(new FunctionPointerStateCoverageOperator(), FunctionPointerState.class);
  }

  @Override
  public AbstractState combineViolationConditionsAtSameProgramHash(
      Collection<AbstractState> states) {
    return combineIfPossible(states)
        .orElseThrow(
            () -> new IllegalArgumentException("Cannot combine different function-pointer states"));
  }

  @Override
  public Optional<AbstractState> combineIfPossible(Collection<AbstractState> states) {
    FunctionPointerState first = (FunctionPointerState) Iterables.get(states, 0);
    return states.stream().allMatch(first::equals) ? Optional.of(first) : Optional.empty();
  }
}
