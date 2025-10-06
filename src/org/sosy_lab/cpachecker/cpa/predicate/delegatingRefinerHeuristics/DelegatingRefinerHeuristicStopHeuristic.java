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
 * A simple, stateful heuristic that ensures the stop refiner stops refinement. Should always be
 * added as the last heuristic, intended for use with the PredicateStopRefiner.
 */
public class DelegatingRefinerHeuristicStopHeuristic implements DelegatingRefinerHeuristic {
  private boolean terminationTriggered = false;

  @Override
  public boolean fulfilled(
      UnmodifiableReachedSet pReached, ImmutableList<ReachedSetDelta> pDeltas) {
    if (terminationTriggered) {
      return false;
    }
    terminationTriggered = true;
    return true;
  }
}
