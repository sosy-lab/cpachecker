// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetDelta;
import org.sosy_lab.cpachecker.core.reachedset.TrackingForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics.DelegatingRefinerHeuristics;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * * This class provides a delegating refiner implementation for predicate analysis. It decides
 * which refiner to use in the next refinement iteration, baes on a set of core heuristics.
 */
public class DelegatingRefiner implements ARGBasedRefiner {

  private final Map<DelegatingRefinerHeuristics, ARGBasedRefiner> pRefiners;

  public DelegatingRefiner(Map<DelegatingRefinerHeuristics, ARGBasedRefiner> pRefinerMap) {
    this.pRefiners = pRefinerMap;
  }

  @Override
  public CounterexampleInfo performRefinementForPath(ARGReachedSet pReached, ARGPath pPath)
      throws CPAException, InterruptedException {

    UnmodifiableReachedSet reachedSet = pReached.asReachedSet();

    List<ReachedSetDelta> deltaSet;
    if (reachedSet instanceof TrackingForwardingReachedSet trackingForwardingReachedSet) {
      ReachedSetDelta delta = trackingForwardingReachedSet.getDelta();
      deltaSet = Collections.singletonList(delta);
    } else {
      deltaSet = Collections.emptyList();
    }

    for (Map.Entry<DelegatingRefinerHeuristics, ARGBasedRefiner> mapEntry : pRefiners.entrySet()) {
      if (mapEntry.getKey().fulfilled(reachedSet, deltaSet)) {
        return mapEntry.getValue().performRefinementForPath(pReached, pPath);
      }
    }

    throw new CPAException("No heuristic matched for refinement.");
  }
}
