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
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetDelta;
import org.sosy_lab.cpachecker.core.reachedset.TrackingForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSetWrapper;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
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
  private ARGBasedRefiner currentRefiner = null;
  private final ARGBasedRefiner refiner;
  private final ARGCPA argCpa;

  public PredicateDelegatingRefiner(
      ARGBasedRefiner pRefiner,
      ARGCPA pArgCpa,
      LogManager pLogger,
      ImmutableList<HeuristicDelegatingRefinerRecord> pRefiners) {
    this.refiners = ImmutableList.copyOf(pRefiners);
    this.logger = pLogger;
    refiner = pRefiner;
    argCpa = pArgCpa;
  }

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

    ARGBasedRefiner refiner = factory.create(strategy);

    ImmutableList<HeuristicDelegatingRefinerRecord> refinerRecords = factory.getRefinerRecords();

    return new PredicateDelegatingRefiner(
        refiner, argcpa, predicateCpa.getLogger(), refinerRecords);
  }

  @Override
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
    CounterexampleInfo cex = null;
    currentRefiner = refiner;
    ARGReachedSet reached = new ARGReachedSet(pReached, argCpa);

    UnmodifiableReachedSet reachedSet = reached.asReachedSet();
    reachedSet = ((UnmodifiableReachedSetWrapper) reachedSet).getDelegate();

    final AbstractState last = reached.asReachedSet().getLastState();
    ARGState lastState = (ARGState) last;
    ARGPath errorPath = ARGUtils.getOnePathTo(lastState);

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
          Level.INFO,
          "Heuristic %s matched for %s",
          pHeuristic.getClass().getSimpleName(),
          pRecord.pRefiner().getClass().getSimpleName());
      if (pHeuristic.fulfilled(reachedSet, deltaSequence)) {
        currentRefiner = pRecord.pRefiner();

        cex = currentRefiner.performRefinementForPath(reached, errorPath);
        return cex.isSpurious();
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