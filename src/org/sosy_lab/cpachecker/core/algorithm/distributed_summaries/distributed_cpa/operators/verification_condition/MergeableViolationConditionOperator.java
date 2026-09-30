// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssARGPathGraph;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * A violation-condition operator that propagates a condition backwards along the edges of a path,
 * starting at the violation. The composite operator runs these steps for all components together
 * over a {@link DssARGPathGraph} and merges the conditions of paths that meet at the same state,
 * instead of computing one condition per path.
 *
 * @param <T> the condition while it is propagated; its representation is private to the operator
 */
public interface MergeableViolationConditionOperator<T> extends ViolationConditionOperator {

  /** The condition at {@code pTarget}, the state the violation was found in. */
  T initialCondition(ARGState pTarget, Optional<ARGState> pPreviousCondition)
      throws InterruptedException;

  /**
   * The condition before {@code pEdges}, or empty if no state there leads to {@code pCondition}.
   */
  Optional<T> prepend(T pCondition, List<CFAEdge> pEdges)
      throws CPATransferException, InterruptedException;

  /**
   * Merges the conditions of two paths that meet at the same state, or returns empty if they have
   * to stay apart.
   *
   * <p>The composite operator merges two paths only if every component merges them, and conjoins
   * the merged components. This is exact only if at most one component merges conditions that
   * differ in meaning. All others must merge only equal conditions, or information that merely
   * restricts where the condition is searched for, like the witness of the block.
   */
  Optional<T> merge(T pFirst, T pSecond) throws InterruptedException;

  /**
   * The violation condition at the first state of {@code pPath}, the path or graph {@code
   * pCondition} was propagated along, or empty if the operator finds it unsatisfiable.
   */
  Optional<AbstractState> finish(ARGPath pPath, Optional<ARGState> pPreviousCondition, T pCondition)
      throws InterruptedException, SolverException;

  @Override
  default List<AbstractState> computeViolationConditions(
      ARGPath pARGPath, Optional<ARGState> pPreviousCondition)
      throws InterruptedException, CPATransferException, SolverException {
    Preconditions.checkArgument(
        !(pARGPath instanceof DssARGPathGraph), "Component operators expect a single path");
    Optional<T> condition =
        prepend(
            initialCondition(pARGPath.getLastState(), pPreviousCondition), pARGPath.getFullPath());
    if (condition.isEmpty()) {
      return ImmutableList.of();
    }
    return finish(pARGPath, pPreviousCondition, condition.orElseThrow()).stream().toList();
  }
}
