// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics;

import java.util.List;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetDelta;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

/**
 * A heuristic which lets the {@link org.sosy_lab.cpachecker.cpa.predicate.PredicateCPARefiner} do a
 * fixed number of iterations so that enough data is collected in order to judge refinement progress
 * with other heuristics.
 */
public class DelegatingRefinerHeuristicRunDefaultNTimes implements DelegatingRefinerHeuristic {

  private final int fixedRuns;
  private int currentRuns = 0;

  public DelegatingRefinerHeuristicRunDefaultNTimes(int pFixedRuns) {
    this.fixedRuns = pFixedRuns;
  }

  @Override
  public boolean fulfilled(UnmodifiableReachedSet pReached, List<ReachedSetDelta> pDeltas) {
    if (currentRuns < fixedRuns) {
      currentRuns++;
      return true;
    }
    return false;
  }
}
