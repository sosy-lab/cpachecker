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
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics.DelegatingRefinerHeuristic;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics.HeuristicDelegatingRefinerRecord;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;

/**
 * A heuristic-driven refinement orchestrator for predicate analysis. The refiner delegates
 * refinement to one of several {@link Refiner} instances based on a set of core heuristics. Each
 * refiner is paired with a heuristic. During each refinement, the heuristics are evaluated in order
 * against the current {@link TrackingForwardingReachedSet} and its delta history. If all heuristics
 * indicate likely divergence in the verification, the PredicateDelegatingRefiner uses a {@link
 * PredicateStopRefiner} to signal the CEGAR algorithm to stop with refinement and end verification
 * early.
 */
public class PredicateDelegatingRefiner implements Refiner {
  private final ImmutableList<HeuristicDelegatingRefinerRecord> heuristicRefinerRecords;
  private final LogManager logger;
  private boolean shouldTerminateRefinement = false;
  private final ImmutableList.Builder<ReachedSetDelta> deltaSequenceBuilder =
      ImmutableList.builder();

  public PredicateDelegatingRefiner(
      LogManager pLogger,
      ImmutableList<HeuristicDelegatingRefinerRecord> pHeuristicRefinerRecords) {
    this.heuristicRefinerRecords = ImmutableList.copyOf(pHeuristicRefinerRecords);
    this.logger = pLogger;
  }

  /**
   * Factory method to create a PredicateDelegatingRefiner from the given CPA configuration and
   * initialize its internal map of heuristic-refiner records.
   *
   * @param pCpa the CPA configuration needed to retrieve the ARGCPA and the PredicateCPA
   * @return a configured PredicateDelegatingRefiner
   * @throws InvalidConfigurationException if predicateCPA required for initializing the
   *     heuristic-refiner map is missing
   */
  public static Refiner create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    ARGCPA argcpa = CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, PredicateDelegatingRefiner.class);
    PredicateCPA predicateCpa =
        CPAs.retrieveCPAOrFail(pCpa, PredicateCPA.class, PredicateDelegatingRefiner.class);
    if (predicateCpa == null) {
      throw new InvalidConfigurationException(
          PredicateDelegatingRefiner.class.getSimpleName() + " needs a PredicateCPA");
    }

    RefinementStrategy strategy =
        new PredicateAbstractionRefinementStrategy(
            predicateCpa.getConfiguration(),
            predicateCpa.getLogger(),
            predicateCpa.getPredicateManager(),
            predicateCpa.getSolver());

    PredicateCPARefinerFactory factory = new PredicateCPARefinerFactory(argcpa);

    // to create refiners and populate the ImmutableList<HeuristicDelegatingRefinerRecord>
    // refinerRecords, it is necessary to call factory.create(strategy) but the resulting refiner is
    // not needed for DelegatingRefiner functionality
    factory.create(strategy);

    ImmutableList<HeuristicDelegatingRefinerRecord> availableHeuristicRefinerRecords =
        factory.getRefinerRecords();

    return new PredicateDelegatingRefiner(argcpa.getLogger(), availableHeuristicRefinerRecords);
  }

  /**
   * Performs refinement by evaluating its internal heuristic-refiner map in order. It delegates the
   * refinement execution to the first refiner whose associated heuristic returns {@code true}.
   * Requires a {@link TrackingForwardingReachedSet}.
   *
   * @param pReached the current reached Set
   * @return {@code true} refinement was successful, {@code false} otherwise
   * @throws CPAException if no heuristic matches or if {@link TrackingForwardingReachedSet} is
   *     disabled
   * @throws InterruptedException if refinement is interrupted
   */
  @Override
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
    // PredicateDelegatingRefiner only works with a TrackingForwardingReachedSet
    if (!(pReached instanceof TrackingForwardingReachedSet trackingForwardingReachedSet)) {
      throw new CPAException(
          "To use the Delegating Refiner, you need to enable tracking via"
              + " 'analysis.reachedSet.withTracking=true'");
    }

    deltaSequenceBuilder.add(trackingForwardingReachedSet.getDelta());
    trackingForwardingReachedSet.resetTracking();
    ImmutableList<ReachedSetDelta> deltaSequence = deltaSequenceBuilder.build();

    for (HeuristicDelegatingRefinerRecord pRecord : heuristicRefinerRecords) {
      DelegatingRefinerHeuristic pHeuristic = pRecord.pHeuristic();
      if (pHeuristic.fulfilled(trackingForwardingReachedSet.getDelegate(), deltaSequence)) {
        logger.logf(
            Level.FINER,
            "Heuristic %s matched for %s",
            pHeuristic.getClass().getSimpleName(),
            pRecord.pRefiner().getClass().getSimpleName());
        Refiner refiner = pRecord.pRefiner();
        shouldTerminateRefinement = refiner.shouldTerminateRefinement();
        return refiner.performRefinement(trackingForwardingReachedSet);
      }
    }
    throw new CPAException("No heuristic matched for refinement.");
  }

  /**
   * Termination signal used to signal CEGAR to terminate refinement early if all heuristics
   * indicate divergence
   *
   * @return {@code true}, if refinement should terminate, {@code false} otherwise
   */
  @Override
  public boolean shouldTerminateRefinement() {
    return shouldTerminateRefinement;
  }
}
