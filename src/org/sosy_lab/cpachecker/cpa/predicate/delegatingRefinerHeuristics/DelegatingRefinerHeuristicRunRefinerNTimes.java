// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetDelta;

/**
 * A simple heuristic that runs a configurable number of times. To mirror the default predicate
 * abstraction functionality, this heuristic should be set with N = 1 and used as the first
 * heuristic in the PredicateDelegatingRefiner, paired with a PredicateStaticRefiner.
 */
public class DelegatingRefinerHeuristicRunRefinerNTimes implements DelegatingRefinerHeuristic {

  private int totalCount;
  private int currentCount = 0;

  public DelegatingRefinerHeuristicRunRefinerNTimes(int pTotalCount)
      throws InvalidConfigurationException {
    if (pTotalCount < 0) {
      throw new InvalidConfigurationException(
          "Number of times DelegatingRefinerHeuristicStaticRefinement should run must not be"
              + " negative");
    }
    this.totalCount = pTotalCount;
  }

  /**
   * Evaluates if the heuristic has already run the configured number of times.
   *
   * @param pReached the current ReachedSet (not used directly)
   * @param pDeltas the list of changes in the ReachedSet (not used directly)
   * @return {@code true}, if the heuristic has not yet run the configured number of times, {@code
   *     false} otherwise
   */
  @Override
  public boolean fulfilled(ReachedSet pReached, ImmutableList<ReachedSetDelta> pDeltas) {
    if (currentCount < totalCount) {
      currentCount++;
      return true;
    }
    return false;
  }
}
