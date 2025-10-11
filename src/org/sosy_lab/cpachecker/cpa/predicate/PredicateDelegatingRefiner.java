// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetDelta;
import org.sosy_lab.cpachecker.core.reachedset.TrackingForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSetWrapper;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics.DelegatingRefinerHeuristic;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics.HeuristicDelegatingRefinerRecord;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * A heuristic-driven refinement orchestrator for predicate analysis. The refiner delegates
 * refinement to one of several {@link ARGBasedRefiner} refiners bases on a set of core heuristics.
 * Each refiner is paired with a heuristic. During each refinement, the heuristics are evaluated in
 * order against the current {@link TrackingForwardingReachedSet} and its delta history. If all
 * heuristics indicate likely divergence in the verification, the DelegatingRefiner uses a {@link
 * PredicateStopRefiner} to signal the CEGAR algorithm to stop with refinement and end verification
 * early.
 */
public class PredicateDelegatingRefiner implements ARGBasedRefiner {

  private final ImmutableList<HeuristicDelegatingRefinerRecord> refiners;
  private final LogManager logger;
  private ARGBasedRefiner currentRefiner;

  public PredicateDelegatingRefiner(
      ImmutableList<HeuristicDelegatingRefinerRecord> pHeuristicRefinerRecords,
      final LogManager pLogger)
      throws InvalidConfigurationException {
    this.refiners = ImmutableList.copyOf(pHeuristicRefinerRecords);
    this.logger = pLogger;
    this.currentRefiner = null;
  }

  @Override
  public CounterexampleInfo performRefinementForPath(ARGReachedSet pReached, ARGPath pPath)
      throws CPAException, InterruptedException {

    UnmodifiableReachedSet reachedSet = pReached.asReachedSet();
    // The reachedSet comes as a UnmodifiableReachedSetWrapper and needs to be unwrapped to expose
    // the delegate class in order for Verify to recognize the TrackingForwardingReachedSet
    while (reachedSet instanceof UnmodifiableReachedSetWrapper) {
      reachedSet = ((UnmodifiableReachedSetWrapper) reachedSet).getDelegate();
    }

    // PredicateDelegatingRefiner only works with a TrackingForwardingReachedSet
    Verify.verify(
        reachedSet instanceof TrackingForwardingReachedSet,
        "To use the Delegating Refiner, you need to enable tracking via"
            + " 'analysis.reachedSet.withTracking=true'");

    TrackingForwardingReachedSet trackingForwardingReachedSet =
        (TrackingForwardingReachedSet) reachedSet;

    ImmutableList<ReachedSetDelta> deltaSequence =
        ImmutableList.of(trackingForwardingReachedSet.getDelta());

    for (HeuristicDelegatingRefinerRecord pRecord : refiners) {
      DelegatingRefinerHeuristic pHeuristic = pRecord.pHeuristic();
      logger.logf(
          Level.FINEST,
          "Heuristic %s matched for %s",
          pHeuristic.getClass().getSimpleName(),
          pRecord.pRefiner().getClass().getSimpleName());
      if (pHeuristic.fulfilled(reachedSet, deltaSequence)) {
        currentRefiner = pRecord.pRefiner();
        return currentRefiner.performRefinementForPath(pReached, pPath);
      }
    }

    throw new CPAException("No heuristic matched for refinement.");
  }

  @Override
  public boolean shouldTerminateRefinement() {
    if (currentRefiner != null) {
      return currentRefiner.shouldTerminateRefinement();
    }
    return false;
  }
}
