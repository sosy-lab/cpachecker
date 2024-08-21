// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.List;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * The combine operator combines multiple preconditions at a block entry in summary-based block
 * analysis. This can be similar to the functionality of a {@link
 * org.sosy_lab.cpachecker.core.interfaces.MergeOperator}, but it aggregates each abstract state
 * that is deserialized from a precondition (with {@link
 * org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.DeserializeOperator})
 * into a single abstract state. Contrary, to {@link
 * org.sosy_lab.cpachecker.core.interfaces.MergeOperator} combine should be associative.
 *
 * @see org.sosy_lab.cpachecker.core.interfaces.MergeOperator
 * @see org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.BlockAnalysis
 */
public interface CombineOperator {

  List<AbstractState> combine(AbstractState pState1, AbstractState pState2, Precision pPrecision)
      throws CPAException, InterruptedException;

  default List<AbstractState> combine(
      List<AbstractState> pStates, AbstractState pTopElement, Precision pPrecision)
      throws InterruptedException, CPAException {
    if (pStates.isEmpty()) {
      return ImmutableList.of(pTopElement);
    }

    if (pStates.size() == 1) {
      return pStates;
    }

    AbstractState first = pStates.remove(0);

    for (AbstractState state : pStates) {
      first = Iterables.getOnlyElement(combine(first, state, pPrecision));
    }

    return ImmutableList.of(first);
  }
}
