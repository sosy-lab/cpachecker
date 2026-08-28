// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ReachedSetDeltaConsumer;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.TrackingForwardingReachedSet.ReachedSetDelta;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics.DelegatingRefinerHeuristic;
import org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics.HeuristicDelegatingRefinerRecord;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;

/**
 * A heuristic-driven refinement orchestrator for predicate analysis. The refiner delegates
 * refinement to one of several {@link Refiner} instances based on a set of core heuristics. Each
 * refiner is paired with a heuristic. During each refinement, the heuristics are evaluated in order
 * against the current reached set and the history of reached-set changes supplied by the CEGAR
 * algorithm. If all heuristics indicate likely divergence in the verification, the
 * PredicateDelegatingRefiner uses a {@link PredicateStopRefiner} to signal the CEGAR algorithm to
 * stop with refinement and end verification early.
 *
 * <p>This refiner is a pure consumer of the change history. The tracking itself is owned by the
 * CEGAR algorithm, which closes a tracking window and supplies the resulting history before every
 * refinement.
 */
public class PredicateDelegatingRefiner implements Refiner, ReachedSetDeltaConsumer {
  private final ImmutableList<HeuristicDelegatingRefinerRecord> heuristicRefinerRecords;
  private final LogManager logger;
  private ImmutableList<ReachedSetDelta> deltaHistory = ImmutableList.of();

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
   * Receives the history of reached-set changes from the CEGAR algorithm. Called before every
   * refinement, so the heuristics always evaluate against an up-to-date history.
   *
   * @param pDeltaHistory the deltas of the tracking windows closed so far, oldest first
   */
  @Override
  public void consumeDeltaHistory(ImmutableList<ReachedSetDelta> pDeltaHistory) {
    deltaHistory = checkNotNull(pDeltaHistory);
  }

  /**
   * Performs refinement by evaluating its internal heuristic-refiner map in order. It delegates the
   * refinement execution to the first refiner whose associated heuristic returns {@code true}.
   *
   * @param pReached the current reached Set
   * @return {@code true} refinement was successful, {@code false} otherwise
   * @throws CPAException if no heuristic matches
   * @throws InterruptedException if refinement is interrupted
   */
  @Override
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
    for (HeuristicDelegatingRefinerRecord pRecord : heuristicRefinerRecords) {
      DelegatingRefinerHeuristic pHeuristic = pRecord.pHeuristic();
      if (pHeuristic.fulfilled(pReached, deltaHistory)) {
        logger.logf(
            Level.FINER,
            "Heuristic %s matched for %s",
            pHeuristic.getClass().getSimpleName(),
            pRecord.pRefiner().getClass().getSimpleName());
        Refiner refiner = pRecord.pRefiner();
        return refiner.performRefinement(pReached);
      }
    }
    throw new CPAException("No heuristic matched for refinement.");
  }
}
