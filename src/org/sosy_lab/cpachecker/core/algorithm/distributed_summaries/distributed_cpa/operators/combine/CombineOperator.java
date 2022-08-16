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
import java.util.ArrayList;
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
 * @see
 *     org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.ForwardBlockAnalysis
 * @see
 *     org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.BackwardBlockAnalysis
 */
public interface CombineOperator {

  List<AbstractState> combine(AbstractState pState1, AbstractState pState2, Precision pPrecision)
      throws CPAException, InterruptedException;

  /**
   * Combine a list of states to a list of states that over-approximates all elements of the list.
   * Since combination should be associative, the order does not matter.
   *
   * <p>Combined states are commonly used as the initial {@link AbstractState} for a {@link
   * org.sosy_lab.cpachecker.core.reachedset.ReachedSet}.
   *
   * <p>By convention, the number of {@link AbstractState}s in the returned list is less than or
   * equal to the number of abstract states in {@code pStates}.
   *
   * @param pStates list of abstract states that will be over-approximated by the returned states
   * @param pTopElement top element that over-approximates all states by definition (supremum of
   *     lattice). Can be null and will be ignored if pStates is not empty.
   * @param pPrecision Initial precision for returned abstract states
   * @return List of abstract states that over-approximate {@code pStates}.
   */
  default List<AbstractState> combine(
      ImmutableList<AbstractState> pStates, AbstractState pTopElement, Precision pPrecision)
      throws InterruptedException, CPAException {
    List<AbstractState> states = new ArrayList<>(pStates);
    if (states.isEmpty()) {
      return ImmutableList.of(pTopElement);
    }

    if (states.size() == 1) {
      return states;
    }

    AbstractState first = states.remove(0);

    for (AbstractState state : states) {
      first = Iterables.getOnlyElement(combine(first, state, pPrecision));
    }

    return ImmutableList.of(first);
  }
}
