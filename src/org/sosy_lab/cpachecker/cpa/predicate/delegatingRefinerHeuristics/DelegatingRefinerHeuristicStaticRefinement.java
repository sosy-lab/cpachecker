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
 * A simple heuristic that chooses a static refinement strategy in one refinement iteration. To
 * mirror the default predicate abstraction functionality, this heuristic should be used as the
 * first heuristic in the PredicateDelegatingRefiner, paired with a PredicateStaticRefiner.
 */
public class DelegatingRefinerHeuristicStaticRefinement implements DelegatingRefinerHeuristic {

  private boolean staticRefinerUsed = false;

  /**
   * Evaluates if the static refinement strategy has been applied.
   *
   * @param pReached the current ReachedSet (not used directly)
   * @param pDeltas the list of changes in the ReachedSet (not used directly)
   * @return {@code true}, if static refinement has not yet been used, {@code false} otherwise
   */
  @Override
  public boolean fulfilled(ReachedSet pReached, ImmutableList<ReachedSetDelta> pDeltas) {

    if (!staticRefinerUsed) {
      staticRefinerUsed = true;
      return true;
    }
    return false;
  }
}
