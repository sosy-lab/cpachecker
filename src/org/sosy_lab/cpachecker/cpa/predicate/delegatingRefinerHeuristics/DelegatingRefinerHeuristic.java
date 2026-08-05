// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetDelta;

/**
 * Interface for the heuristics in the PredicateDelegatingRefiner. The delegating refiner uses them
 * to decide which refiner to apply.
 *
 * <p>Each heuristic is paired with one subordinate refiner. The delegating refiner evaluates the
 * configured heuristics in order and delegates to the refiner of the first heuristic that is
 * fulfilled; the remaining heuristics are not evaluated. Consequently, a fulfilled heuristic means
 * that under its specific aspect, refinement progress looks promising enough to apply its refiner.
 * A heuristic that is not fulfilled implies that under its specific aspect, refinement progress
 * does not look promising, so its refiner is skipped and another heuristic evaluates the progress
 * next.
 *
 * <p>Because a fulfilled heuristic always leads to its refiner being applied, the configured chain
 * of heuristics is expected to end in a heuristic that is always fulfilled and is paired with a
 * refiner signalling early termination of CEGAR. Otherwise, all heuristics may reject a refinement
 * and the delegating refiner cannot make progress.
 */
public interface DelegatingRefinerHeuristic {
  /**
   * Checks whether the current refinement progress satisfies this heuristic, i.e., whether the
   * subordinate refiner associated with this heuristic should perform the next refinement.
   *
   * @param pReached the current immutable ReachedSet
   * @param pDeltas the list of changes in the ReachedSet since the last refinement
   * @return {@code true} if the conditions of this heuristic are satisfied and its refiner shall be
   *     applied, {@code false} if the delegating refiner shall continue with the next heuristic
   */
  boolean fulfilled(ReachedSet pReached, ImmutableList<ReachedSetDelta> pDeltas);
}
