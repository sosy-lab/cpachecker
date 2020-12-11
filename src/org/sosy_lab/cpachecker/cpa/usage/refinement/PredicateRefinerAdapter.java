// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage.refinement;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionGlobalRefinementStrategy;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class PredicateRefinerAdapter extends GenericSinglePathRefiner {
  ARGBasedRefiner refiner;
  LogManager logger;

  private final UsageStatisticsRefinementStrategy strategy;
  private ARGReachedSet ARGReached;

  // Statistics
  private StatTimer externalRefinement = new StatTimer("Time for predicate refinement");
  private StatCounter solverFailures = new StatCounter("Solver failures");
  // Number of refined and repeated paths are calculated in generic refiner
  private StatCounter numberOfBAMupdates = new StatCounter("Number of BAM updates");

  public PredicateRefinerAdapter(
      ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>> wrapper,
      LogManager pLogger,
      UsageStatisticsRefinementStrategy pStrategy,
      ARGBasedRefiner pRefiner) {
    super(wrapper);
    logger = pLogger;
    strategy = pStrategy;
    refiner = pRefiner;
  }

  @Override
  public RefinementResult call(ExtendedARGPath pInput) throws CPAException, InterruptedException {
    RefinementResult result = RefinementResult.createTrue();

    strategy.initializeGlobalRefinement();
    CounterexampleInfo cex = null;

    externalRefinement.start();
    try {
      cex = refiner.performRefinementForPath(ARGReached, pInput);

    } catch (IllegalStateException e) {
      // msat_solver return -1 <=> unknown
      // consider its as true;
      logger.log(Level.WARNING, "Solver exception: " + e.getMessage());
      solverFailures.inc();
    } catch (RefinementFailedException e) {
      logger.log(Level.WARNING, "Path is repeated, BAM is looped");
      pInput.getUsageInfo().setAsLooped();
    } catch (AssertionError e) {
      // Sometimes the assertion is inside the solver
      logger.log(Level.WARNING, "Assertion error in the solver: " + e.getMessage());
      pInput.getUsageInfo().setAsLooped();
    } finally {
      externalRefinement.stop();
    }

    if (cex != null && cex.isSpurious()) {
      result = RefinementResult.createFalse();
      List<ARGState> affectedStates = strategy.getLastAffectedStates();
      if (!affectedStates.isEmpty()) {
        // it may be, if there are no valuable interpolants: ..., true, false, ...
        result.addInfo(PredicateRefinerAdapter.class, affectedStates);
        PredicatePrecision lastPrecision = strategy.getNewPrecision();
        assert (lastPrecision != null);
        assert (!lastPrecision.isEmpty());
        result.addPrecision(lastPrecision);
      }
    }

    // We update the precision later
    strategy.resetGlobalRefinement();
    return result;
  }

  @Override
  protected void handleUpdateSignal(Class<? extends RefinementInterface> pCallerClass, Object pData) {
    if (pCallerClass.equals(IdentifierIterator.class)) {
      if (pData instanceof ReachedSet) {
        //Updating new reached set
        updateReachedSet((ReachedSet)pData);
      }
    }
  }

  @Override
  protected void handleFinishSignal(Class<? extends RefinementInterface> pCallerClass) {
    if (pCallerClass.equals(IdentifierIterator.class)) {
      ARGReached = null;
      strategy.resetGlobalRefinement();
    }
  }

  @Override
  protected void printAdditionalStatistics(StatisticsWriter pOut) {
    pOut.put(externalRefinement)
        .put(solverFailures)
        .put(numberOfBAMupdates);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStats) {
    if (refiner instanceof StatisticsProvider) {
      ((StatisticsProvider)refiner).collectStatistics(pStats);
    }
    super.collectStatistics(pStats);
  }

  private void updateReachedSet(ReachedSet pReached) {
    ARGReached = new ARGReachedSet(pReached);
  }

  public static class UsageStatisticsRefinementStrategy
      extends PredicateAbstractionGlobalRefinementStrategy {

    private List<ARGState> affectedStates = new ArrayList<>();
    private Function<ARGState, ARGState> transformer;

    public UsageStatisticsRefinementStrategy(
        final Configuration config,
        final LogManager logger,
        final Solver pSolver,
        final PredicateAbstractionManager pPredAbsMgr, Function<ARGState, ARGState> pTransformer) throws InvalidConfigurationException {
      super(config, logger, pPredAbsMgr, pSolver);
      transformer = pTransformer;
    }

    @Override
    protected void finishRefinementOfPath(
            ARGState pUnreachableState,
            List<ARGState> pAffectedStates,
            ARGReachedSet pReached,
            List<ARGState> abstractionStatesTrace,
            boolean pRepeatedCounterexample)
        throws CPAException, InterruptedException {

      super.finishRefinementOfPath(
          pUnreachableState,
          pAffectedStates,
          pReached,
          abstractionStatesTrace,
          pRepeatedCounterexample);

        from(pAffectedStates)
          .transform(transformer)
          .forEach(affectedStates::add);
    }

    public PredicatePrecision getNewPrecision() {
      PredicatePrecision newPrecision = PredicatePrecision.empty();
      return newPrecision.addLocalPredicates(newPredicates.entries());
    }

    @Override
    public void resetGlobalRefinement() {
      super.resetGlobalRefinement();
      affectedStates.clear();
    }

    @Override
    protected void updateARG(PredicatePrecision pNewPrecision, ARGState pRefinementRoot)
        throws InterruptedException {
      // Do not update ARG for race analysis
    }

    @Override
    public List<ARGState> filterAbstractionStates(ARGPath pPath) {
      List<ARGState> result =
          from(pPath.asStatesList())
              .skip(1)
              .filter(PredicateAbstractState::containsAbstractionState)
              .toList();

      if (pPath.getLastState() != result.get(result.size() - 1)) {
        List<ARGState> newResult = new ArrayList<>(result);
        newResult.add(pPath.getLastState());
        return ImmutableList.copyOf(newResult);
      }

      return result;
    }

    public List<ARGState> getLastAffectedStates() {
      return ImmutableList.copyOf(affectedStates);
    }
  }
}
