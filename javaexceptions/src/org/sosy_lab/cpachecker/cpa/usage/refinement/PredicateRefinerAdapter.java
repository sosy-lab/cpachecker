// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage.refinement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.BAMBlockFormulaStrategy;
import org.sosy_lab.cpachecker.cpa.predicate.BAMPredicateAbstractionRefinementStrategy;
import org.sosy_lab.cpachecker.cpa.predicate.BAMPredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.BAMPredicateRefiner;
import org.sosy_lab.cpachecker.cpa.predicate.BlockFormulaStrategy;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPARefinerFactory;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class PredicateRefinerAdapter extends GenericSinglePathRefiner {
  ARGBasedRefiner refiner;
  LogManager logger;

  private final UsageStatisticsRefinementStrategy strategy;
  private ARGReachedSet ARGReached;

  private final Map<Set<CFAEdge>, PredicatePrecision> falseCache = new HashMap<>();
  private final Map<Set<CFAEdge>, PredicatePrecision> falseCacheForCurrentIteration =
      new HashMap<>();
  // private final Multimap<SingleIdentifier, Set<CFAEdge>> idCached = LinkedHashMultimap.create();
  private final Set<Set<CFAEdge>> trueCache = new HashSet<>();

  private final Set<Set<CFAEdge>> potentialLoopTraces = new HashSet<>();
  // Statistics
  private StatCounter solverFailures = new StatCounter("Solver failures");
  private StatCounter numberOfrepeatedPaths = new StatCounter("Number of repeated paths");
  private StatCounter numberOfrefinedPaths = new StatCounter("Number of refined paths");
  private StatCounter numberOfBAMupdates = new StatCounter("Number of BAM updates");

  public PredicateRefinerAdapter(
      ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>> wrapper,
      ConfigurableProgramAnalysis pCpa,
      LogManager pLogger)
      throws InvalidConfigurationException {
    super(wrapper);

    if (!(pCpa instanceof WrapperCPA)) {
      throw new InvalidConfigurationException(
          BAMPredicateRefiner.class.getSimpleName() + " could not find the PredicateCPA");
    }

    @SuppressWarnings("resource")
    BAMPredicateCPA predicateCpa = ((WrapperCPA) pCpa).retrieveWrappedCpa(BAMPredicateCPA.class);
    if (predicateCpa == null) {
      throw new InvalidConfigurationException(
          BAMPredicateRefiner.class.getSimpleName() + " needs an BAMPredicateCPA");
    }

    logger = pLogger;
    PathFormulaManager pfmgr = predicateCpa.getPathFormulaManager();

    BlockFormulaStrategy blockFormulaStrategy = new BAMBlockFormulaStrategy(pfmgr);

    strategy =
        new UsageStatisticsRefinementStrategy(
            predicateCpa.getConfiguration(),
            logger,
            predicateCpa.getSolver(),
            predicateCpa.getPredicateManager());

    refiner =
        new PredicateCPARefinerFactory(pCpa)
            .setBlockFormulaStrategy(blockFormulaStrategy)
            .create(strategy);
  }

  @Override
  public RefinementResult call(ExtendedARGPath pInput) throws CPAException, InterruptedException {
    RefinementResult result;

    Set<CFAEdge> currentPath = new HashSet<>(pInput.getInnerEdges());

    if (trueCache.contains(currentPath)) {
      // Somewhen we have already refined this path as true
      result = RefinementResult.createTrue();
    } else {
      Set<CFAEdge> edgeSet = new HashSet<>(currentPath);
      if (falseCache.containsKey(edgeSet)) {
        PredicatePrecision previousPreds = falseCache.get(edgeSet);
        Precision currentPrecision = getCurrentPrecision();
        PredicatePrecision currentPreds =
            Precisions.extractPrecisionByType(currentPrecision, PredicatePrecision.class);

        if (previousPreds.calculateDifferenceTo(currentPreds) == 0) {
          if (potentialLoopTraces.contains(edgeSet)) {
            // Second time, we obtain it
            numberOfrepeatedPaths.inc();
            logger.log(Level.WARNING, "Path is repeated, BAM is looped");
            pInput.getUsageInfo().setAsLooped();
            result = RefinementResult.createTrue();
            potentialLoopTraces.remove(edgeSet);
          } else {
            result = performPredicateRefinement(pInput);
            logger.log(Level.WARNING, "Path is repeated, hope BAM can handle it itself");
            // BAM can refine with updated predicate refiner, congratulate him.
            numberOfBAMupdates.inc();
            potentialLoopTraces.add(edgeSet);
          }
        } else {
          // rerefine it to obtain new states
          logger.log(Level.WARNING, "Path is repeated, but predicates are missed");
          result = performPredicateRefinement(pInput);
          // We expect the same result
          // but in case of loop the transformation path -> set is not correct, so, there can be a
          // true result
          // assert result.isFalse() : "Current result is " + result;
        }
        // pInput.failureFlag = true;
      } else {
        if (falseCacheForCurrentIteration.containsKey(edgeSet)) {
          // We refined it for other usage
          // just return the result;
          result = RefinementResult.createFalse();
        } else {
          /*if (!totalARGCleaning) {
            subtreesRemover.addStateForRemoving((ARGState)target.getKeyState());
            for (ARGState state : strategy.lastAffectedStates) {
              subtreesRemover.addStateForRemoving(state);
            }
          }*/
          result = performPredicateRefinement(pInput);
        }
      }
    }
    return result;
  }

  private RefinementResult performPredicateRefinement(ExtendedARGPath path)
      throws CPAException, InterruptedException {
    RefinementResult result;
    try {
      numberOfrefinedPaths.inc();
      CounterexampleInfo cex = refiner.performRefinementForPath(ARGReached, path);
      Set<CFAEdge> edgeSet = new HashSet<>(path.getInnerEdges());

      if (!cex.isSpurious()) {
        trueCache.add(edgeSet);
        result = RefinementResult.createTrue();
      } else {
        result = RefinementResult.createFalse();
        result.addInfo(PredicateRefinerAdapter.class, getLastAffectedStates());
        result.addPrecision(getLastPrecision());
        falseCacheForCurrentIteration.put(edgeSet, getLastPrecision());
      }

    } catch (IllegalStateException e) {
      // msat_solver return -1 <=> unknown
      // consider its as true;
      logger.logUserException(Level.WARNING, e, "Solver exception");
      solverFailures.inc();
      result = RefinementResult.createUnknown();
    }
    return result;
  }

  @Override
  protected void handleUpdateSignal(
      Class<? extends RefinementInterface> pCallerClass, Object pData) {
    if (pCallerClass.equals(IdentifierIterator.class)) {
      if (pData instanceof ReachedSet) {
        // Updating new reached set
        updateReachedSet((ReachedSet) pData);
      }
    }
  }

  @Override
  protected void handleFinishSignal(Class<? extends RefinementInterface> pCallerClass) {
    if (pCallerClass.equals(IdentifierIterator.class)) {
      // false cache may contain other precision
      // It happens if we clean it for other Id and rerefine it now
      // Just replace old precision
      falseCacheForCurrentIteration.forEach(falseCache::put);
      falseCacheForCurrentIteration.clear();
      ARGReached = null;
      strategy.lastAffectedStates.clear();
      strategy.lastAddedPrecision = null;
    }
  }

  @Override
  protected void printAdditionalStatistics(StatisticsWriter pOut) {
    pOut.beginLevel()
        .put(numberOfrefinedPaths)
        .put(numberOfrepeatedPaths)
        .put(solverFailures)
        .put(numberOfBAMupdates)
        .put("Size of false cache", falseCache.size());
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStats) {
    if (refiner instanceof StatisticsProvider) {
      ((StatisticsProvider) refiner).collectStatistics(pStats);
    }
    super.collectStatistics(pStats);
  }

  private List<ARGState> getLastAffectedStates() {
    return strategy.lastAffectedStates;
  }

  private PredicatePrecision getLastPrecision() {
    return strategy.lastAddedPrecision;
  }

  private Precision getCurrentPrecision() {
    return ARGReached.asReachedSet().getPrecision(ARGReached.asReachedSet().getFirstState());
  }

  private void updateReachedSet(ReachedSet pReached) {
    ARGReached = new ARGReachedSet(pReached);
  }

  protected static class UsageStatisticsRefinementStrategy
      extends BAMPredicateAbstractionRefinementStrategy {

    private List<ARGState> lastAffectedStates = new ArrayList<>();
    private PredicatePrecision lastAddedPrecision;

    public UsageStatisticsRefinementStrategy(
        final Configuration config,
        final LogManager logger,
        final Solver pSolver,
        final PredicateAbstractionManager pPredAbsMgr)
        throws InvalidConfigurationException {
      super(config, logger, pSolver, pPredAbsMgr);
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

      lastAffectedStates.clear();
      lastAffectedStates.addAll(pAffectedStates);
    }

    @Override
    protected PredicatePrecision addPredicatesToPrecision(PredicatePrecision basePrecision) {
      PredicatePrecision newPrecision = super.addPredicatesToPrecision(basePrecision);
      lastAddedPrecision = (PredicatePrecision) newPrecision.subtract(basePrecision);
      return newPrecision;
    }

    @Override
    protected void updateARG(
        PredicatePrecision pNewPrecision, ARGState pRefinementRoot, ARGReachedSet pReached)
        throws InterruptedException {
      // Do not update ARG for race analysis
    }
  }
}
