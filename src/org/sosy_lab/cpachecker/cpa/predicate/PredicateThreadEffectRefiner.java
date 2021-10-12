// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import static org.sosy_lab.cpachecker.util.statistics.StatisticsWriter.writingStatisticsTo;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.predicates.PathChecker;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.refinement.PrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class PredicateThreadEffectRefiner extends PredicateCPARefiner {

  private final StatTimer mainCheckTime = new StatTimer("Time for checking the main path");
  private final StatTimer effectCheckTime =
      new StatTimer("Time for checking the paths with effects");
  private final StatCounter pathsWithEffects =
      new StatCounter("How many paths with effects were checked");
  private final StatCounter mainPathChecks = new StatCounter("How many main paths were checked");
  private final StatCounter spuriousCount = new StatCounter("Spurious contrexamples found");

  public PredicateThreadEffectRefiner(
      final Configuration pConfig,
      final LogManager pLogger,
      final Optional<LoopStructure> pLoopStructure,
      final BlockFormulaStrategy pBlockFormulaStrategy,
      final Solver pSolver,
      final PathFormulaManager pPfgmr,
      final InterpolationManager pInterpolationManager,
      final PathChecker pPathChecker,
      final PrefixProvider pPrefixProvider,
      final PrefixSelector pPrefixSelector,
      final PredicateCPAInvariantsManager pInvariantsManager,
      final RefinementStrategy pStrategy)
      throws InvalidConfigurationException {
    super(
        pConfig,
        pLogger,
        pLoopStructure,
        pBlockFormulaStrategy,
        pSolver,
        pPfgmr,
        pInterpolationManager,
        pPathChecker,
        pPrefixProvider,
        pPrefixSelector,
        pInvariantsManager,
        pStrategy);
  }

  @Override
  public CounterexampleInfo
      performRefinementForPath(final ARGReachedSet pReached, final ARGPath allStatesTrace)
          throws CPAException, InterruptedException {

    mainCheckTime.start();

    ((GlobalRefinementStrategy) strategy).initializeGlobalRefinement();
    // checking main path
    // TODO Add stat timers for all basic blocks: main path check, formula preparation, effect
    // checks
    mainPathChecks.inc();
    CounterexampleInfo cexInfo = super.performRefinementForPath(pReached, allStatesTrace);
    mainCheckTime.stop();

    if (cexInfo.isSpurious()) {
      ((GlobalRefinementStrategy) strategy).updatePrecisionAndARG();
      return cexInfo;
    }

    // list of all states in main path from beginning to the current
    List<ARGState> statesInMainPath = new ArrayList<>();

    effectCheckTime.start();
    for (ARGState state : allStatesTrace.asStatesList()) {
      if (state.getAppliedFrom() != null) {
        boolean feasibleCexFound = false;
        for (ARGState effect : state.getAppliedFrom().getSecond().getProjectedFrom()) {
          pathsWithEffects.inc();
          // extracting formulas from path to current state
          // Note, not getOnePathTo, to be sure, the sequence is the same
          ARGPath pathToEffect = ARGUtils.getOnePathTo(effect);

          List<ARGState> mainAndEffect = new ArrayList<>(statesInMainPath);
          mainAndEffect.addAll(pathToEffect.asStatesList());
          ARGPath newPath = new ARGPath(mainAndEffect);
          CounterexampleInfo counterexampleInfo = super.performRefinementForPath(pReached, newPath);

          mainAndEffect = new ArrayList<>(pathToEffect.asStatesList());
          mainAndEffect.addAll(statesInMainPath);
          newPath = new ARGPath(mainAndEffect);
          CounterexampleInfo counterexampleInfo2 =
              super.performRefinementForPath(pReached, newPath);

          if (counterexampleInfo.isSpurious()) {
            spuriousCount.inc();
          } else {
            feasibleCexFound = true;
          }
          if (counterexampleInfo2.isSpurious()) {
            spuriousCount.inc();
          } else {
            feasibleCexFound = true;
          }
        }
        if (!feasibleCexFound) {
          ((GlobalRefinementStrategy) strategy).updatePrecisionAndARG();
          effectCheckTime.stop();
          return CounterexampleInfo.spurious();
        }
      }
      statesInMainPath.add(state);
    }

    // TODO Update in case of spurious counterexample
    ((GlobalRefinementStrategy) strategy).resetGlobalRefinement();
    effectCheckTime.stop();
    return cexInfo;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Stats());
    super.collectStatistics(pStatsCollection);
  }

  private class Stats implements Statistics {

    @Override
    public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
      StatisticsWriter w0 = writingStatisticsTo(out);
      w0.put(pathsWithEffects)
          .put(mainPathChecks)
          .put(spuriousCount)
          .put(mainCheckTime)
          .put(effectCheckTime);
    }

    @Override
    public String getName() {
      return "PredicateThreadEffect Refiner";
    }
  }
}
