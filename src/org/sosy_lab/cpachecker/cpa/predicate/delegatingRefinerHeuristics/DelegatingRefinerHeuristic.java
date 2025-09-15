// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetDelta;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

/**
 * Interface for the heuristics in the delegating refiner. The delegating refiner uses them to
 * decide which refiner to apply.
 */
public interface DelegatingRefinerHeuristic {
  /**
   * Function to check if the current ReachedSet satisfies a heuristic.
   *
   * @param pReached the current immutable ReachedSet
   * @param pDeltas the list of changes in the ReachedSet since the last refinement
   * @return true if heuristic conditions are satisfied, false otherwise
   */
  boolean fulfilled(UnmodifiableReachedSet pReached, ImmutableList<ReachedSetDelta> pDeltas);

  /** Default setting when none of the heuristics match. Use only as a safety net. */
  DelegatingRefinerHeuristic DEFAULT = (pReached, pDeltas) -> true;
}
