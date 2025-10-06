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
 * * This class provides a delegating refiner implementation for predicate analysis. It decides
 * which refiner to use in the next refinement iteration, based on a set of core heuristics.
 */
public class PredicateDelegatingRefiner implements ARGBasedRefiner {

  private final ImmutableList<HeuristicDelegatingRefinerRecord> pRefiners;
  private final LogManager logger;
  private boolean shouldTerminate;

  public PredicateDelegatingRefiner(
      ImmutableList<HeuristicDelegatingRefinerRecord> pHeuristicRefinerRecords,
      final LogManager pLogger) {
    this.pRefiners = ImmutableList.copyOf(pHeuristicRefinerRecords);
    this.logger = pLogger;
    shouldTerminate = false;
  }

  @Override
  public CounterexampleInfo performRefinementForPath(ARGReachedSet pReached, ARGPath pPath)
      throws CPAException, InterruptedException {
    if (shouldTerminate) {
      return CounterexampleInfo.giveUp(pPath);
    }

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

    for (HeuristicDelegatingRefinerRecord pRecord : pRefiners) {
      DelegatingRefinerHeuristic pHeuristic = pRecord.pHeuristic();

      if (pHeuristic.fulfilled(reachedSet, deltaSequence)) {
        logger.logf(
            Level.INFO,
            "Heuristic %s matched for refiner %s.",
            pHeuristic.getClass().getSimpleName(),
            pRecord.pRefiner().getClass().getSimpleName());

        CounterexampleInfo refinementResult =
            pRecord.pRefiner().performRefinementForPath(pReached, pPath);

        if (refinementResult.shouldTerminateRefinement()) {
          shouldTerminate = true;
          return refinementResult;
        }
        return refinementResult;
      }
    }

    throw new CPAException("No heuristic matched for refinement.");
  }
}
