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
 * A heuristic which lets the default refiner do a fixed number of iterations so that enough data is
 * collected in order to judge refinement progress with other heuristics.
 */
public class DelegatingRefinerHeuristicRun_N_Times implements DelegatingRefinerHeuristic {

  private final int fixedRuns;
  private int currentRuns = 0;

  public DelegatingRefinerHeuristicRun_N_Times(int pFixedRuns) {
    this.fixedRuns = pFixedRuns;
  }

  @Override
  public boolean fulfilled(
      UnmodifiableReachedSet pReached, ImmutableList<ReachedSetDelta> pDeltas) {
    if (currentRuns < fixedRuns) {
      currentRuns++;
      return true;
    }
    return false;
  }

  public int getFixedRuns() {
    return fixedRuns;
  }
}
