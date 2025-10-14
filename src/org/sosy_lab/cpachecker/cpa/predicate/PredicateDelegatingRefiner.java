// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.collect.ImmutableList;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetDelta;
import org.sosy_lab.cpachecker.core.reachedset.TrackingForwardingReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics.DelegatingRefinerHeuristic;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics.HeuristicDelegatingRefinerRecord;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;

/**
 * A heuristic-driven refinement orchestrator for predicate analysis. The refiner delegates
 * refinement to one of several {@link ARGBasedRefiner} refiners bases on a set of core heuristics.
 * Each refiner is paired with a heuristic. During each refinement, the heuristics are evaluated in
 * order against the current {@link TrackingForwardingReachedSet} and its delta history. If all
 * heuristics indicate likely divergence in the verification, the DelegatingRefiner uses a {@link
 * PredicateStopRefiner} to signal the CEGAR algorithm to stop with refinement and end verification
 * early.
 */
public class PredicateDelegatingRefiner implements Refiner {

  private final ImmutableList<HeuristicDelegatingRefinerRecord> refiners;
  private final LogManager logger;
  private boolean shouldTerminateRefinement = false;

  public PredicateDelegatingRefiner(
      LogManager pLogger, ImmutableList<HeuristicDelegatingRefinerRecord> pRefiners) {
    this.refiners = ImmutableList.copyOf(pRefiners);
    this.logger = pLogger;
  }

  public static Refiner create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    ARGCPA argcpa = CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, PredicateDelegatingRefiner.class);

    PredicateCPARefinerFactory factory = new PredicateCPARefinerFactory(argcpa);

    ImmutableList<HeuristicDelegatingRefinerRecord> refinerRecords = factory.getRefinerRecords();

    return new PredicateDelegatingRefiner(argcpa.getLogger(), refinerRecords);
  }

  @Override
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
    // PredicateDelegatingRefiner only works with a TrackingForwardingReachedSet
    if (!(pReached instanceof TrackingForwardingReachedSet trackingForwardingReachedSet)) {
      throw new CPAException(
          "To use the Delegating Refiner, you need to enable tracking via"
              + " 'analysis.reachedSet.withTracking=true'");
    }

    ImmutableList<ReachedSetDelta> deltaSequence =
        ImmutableList.of(trackingForwardingReachedSet.getDelta());

    for (HeuristicDelegatingRefinerRecord pRecord : refiners) {
      DelegatingRefinerHeuristic pHeuristic = pRecord.pHeuristic();
      logger.logf(
          Level.INFO,
          "Heuristic %s matched for %s",
          pHeuristic.getClass().getSimpleName(),
          pRecord.pRefiner().getClass().getSimpleName());
      if (pHeuristic.fulfilled(trackingForwardingReachedSet.getDelegate(), deltaSequence)) {
        Refiner refiner = pRecord.pRefiner();
        shouldTerminateRefinement = refiner.shouldTerminateRefinement();
        return refiner.performRefinement(trackingForwardingReachedSet);
      }
    }
    throw new CPAException("No heuristic matched for refinement.");
  }

  @Override
  public boolean shouldTerminateRefinement() {
    return shouldTerminateRefinement;
  }
}
