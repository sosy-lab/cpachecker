// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.refiner;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisInterpolantManager;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisPrefixProvider;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.refinement.GenericPrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.GenericRefiner;
import org.sosy_lab.cpachecker.util.refinement.InterpolationTree;
import org.sosy_lab.cpachecker.util.refinement.PathExtractor;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class ValueAnalysisImpactRefiner extends AbstractARGBasedRefiner
    implements UnsoundRefiner, StatisticsProvider {

  // statistics
  private int restartCounter = 0;

  public static ValueAnalysisImpactRefiner create(final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {

    final ARGCPA argCpa =
        CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, ValueAnalysisImpactRefiner.class);
    final ValueAnalysisCPA valueAnalysisCpa =
        CPAs.retrieveCPAOrFail(pCpa, ValueAnalysisCPA.class, ValueAnalysisImpactRefiner.class);

    valueAnalysisCpa.injectRefinablePrecision();

    final LogManager logger = valueAnalysisCpa.getLogger();
    final Configuration config = valueAnalysisCpa.getConfiguration();
    final CFA cfa = valueAnalysisCpa.getCFA();

    final StrongestPostOperator<ValueAnalysisState> strongestPostOperator =
        new ValueAnalysisStrongestPostOperator(logger, Configuration.defaultConfiguration(), cfa);

    final PathExtractor pathExtractor = new PathExtractor(logger, config);

    final ValueAnalysisFeasibilityChecker checker =
        new ValueAnalysisFeasibilityChecker(strongestPostOperator, logger, cfa, config);

    final GenericPrefixProvider<ValueAnalysisState> prefixProvider =
        new ValueAnalysisPrefixProvider(
            logger, cfa, config, valueAnalysisCpa.getShutdownNotifier());

    ImpactDelegateRefiner delegate =
        new ImpactDelegateRefiner(
            checker,
            strongestPostOperator,
            pathExtractor,
            prefixProvider,
            config,
            logger,
            valueAnalysisCpa.getShutdownNotifier(),
            valueAnalysisCpa.getCFA());

    return new ValueAnalysisImpactRefiner(delegate, argCpa, logger);
  }

  ValueAnalysisImpactRefiner(
      final ImpactDelegateRefiner pDelegate, final ARGCPA pArgCpa, final LogManager pLogger) {
    super(pDelegate, pArgCpa, pLogger);
  }

  @Override
  public void forceRestart(ReachedSet pReached) throws InterruptedException {
    restartCounter++;
    ARGState firstChild =
        Iterables.getOnlyElement(((ARGState) pReached.getFirstState()).getChildren());

    ARGReachedSet reached = new ARGReachedSet(pReached);

    reached.removeSubtree(
        firstChild,
        mergeValuePrecisionsForSubgraph(firstChild, reached),
        VariableTrackingPrecision.isMatchingCPAClass(ValueAnalysisCPA.class));
  }

  private VariableTrackingPrecision mergeValuePrecisionsForSubgraph(
      final ARGState pRefinementRoot, final ARGReachedSet pReached) {
    // get all unique precisions from the subtree
    Set<VariableTrackingPrecision> uniquePrecisions = Sets.newIdentityHashSet();

    for (ARGState descendant : ARGUtils.getNonCoveredStatesInSubgraph(pRefinementRoot)) {
      if (pReached.asReachedSet().contains(descendant)) {
        uniquePrecisions.add(extractValuePrecision(pReached, descendant));
      }
    }

    if (uniquePrecisions.isEmpty()) {
      return null;
    }

    // join all unique precisions into a single precision
    VariableTrackingPrecision mergedPrecision = Iterables.getLast(uniquePrecisions);
    for (VariableTrackingPrecision precision : uniquePrecisions) {
      mergedPrecision = mergedPrecision.join(precision);
    }

    return mergedPrecision;
  }

  private static VariableTrackingPrecision extractValuePrecision(
      final ARGReachedSet pReached, ARGState state) {
    return (VariableTrackingPrecision)
        Precisions.asIterable(pReached.asReachedSet().getPrecision(state))
            .filter(VariableTrackingPrecision.isMatchingCPAClass(ValueAnalysisCPA.class))
            .get(0);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(
        new Statistics() {
          @Override
          public void printStatistics(
              PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
            pOut.println("Total number of restarts:      " + String.format("%9d", restartCounter));
          }

          @Nullable
          @Override
          public String getName() {
            return ValueAnalysisImpactRefiner.class.getSimpleName();
          }
        });
    super.collectStatistics(pStatsCollection);
  }

  private static class ImpactDelegateRefiner
      extends GenericRefiner<ValueAnalysisState, ValueAnalysisInterpolant> {

    // statistics
    private StatTimer timeStrengthen = new StatTimer("strengthen");
    private StatTimer timeCoverage = new StatTimer("coverage");
    private StatTimer timePrecision = new StatTimer("precision");
    private StatTimer timeRemove = new StatTimer("remove");

    ImpactDelegateRefiner(
        final ValueAnalysisFeasibilityChecker pFeasibilityChecker,
        final StrongestPostOperator<ValueAnalysisState> pStrongestPostOperator,
        final PathExtractor pPathExtractor,
        final GenericPrefixProvider<ValueAnalysisState> pPrefixProvider,
        final Configuration pConfig,
        final LogManager pLogger,
        final ShutdownNotifier pShutdownNotifier,
        final CFA pCfa)
        throws InvalidConfigurationException {

      super(
          pFeasibilityChecker,
          new ValueAnalysisPathInterpolator(
              pFeasibilityChecker,
              pStrongestPostOperator,
              pPrefixProvider,
              pConfig,
              pLogger,
              pShutdownNotifier,
              pCfa),
          ValueAnalysisInterpolantManager.getInstance(),
          pPathExtractor,
          pConfig,
          pLogger);
    }

    @Override
    protected void refineUsingInterpolants(
        final ARGReachedSet pReached,
        InterpolationTree<ValueAnalysisState, ValueAnalysisInterpolant> pInterpolationTree)
        throws InterruptedException {

      timeStrengthen.start();
      Set<ARGState> strengthenedStates = strengthenStates(pInterpolationTree);
      timeStrengthen.stop();

      // this works correctly for global-refinement, too, doesn't it?
      timeCoverage.start();
      for (ARGState interpolatedTarget :
          pInterpolationTree.getInterpolatedTargetsInSubtree(pInterpolationTree.getRoot())) {
        tryToCoverArg(strengthenedStates, pReached, interpolatedTarget);
      }
      timeCoverage.stop();

      CFANode dummyCfaNode = CFANode.newDummyCFANode();
      VariableTrackingPrecision previsousPrecision = null;
      SetMultimap<CFANode, MemoryLocation> previousIncrement = null;
      timePrecision.start();
      for (Map.Entry<ARGState, ValueAnalysisInterpolant> itp :
          pInterpolationTree.getInterpolantMapping()) {
        ARGState currentState = itp.getKey();

        if (pInterpolationTree.hasInterpolantForState(currentState)
            && pInterpolationTree.getInterpolantForState(currentState).isTrivial()) {
          continue;
        }

        if (strengthenedStates.contains(currentState)) {
          VariableTrackingPrecision currentPrecision =
              extractValuePrecision(pReached, currentState);

          SetMultimap<CFANode, MemoryLocation> increment = HashMultimap.create();
          for (MemoryLocation memoryLocation :
              pInterpolationTree.getInterpolantForState(currentState).getMemoryLocations()) {
            increment.put(dummyCfaNode, memoryLocation);
          }

          VariableTrackingPrecision newPrecision = currentPrecision;
          // precision or increment changed -> create new precision and apply
          if (previsousPrecision != currentPrecision || !increment.equals(previousIncrement)) {
            newPrecision = currentPrecision.withIncrement(increment);
          }

          // tried with readding to waitlist -> slower / less effective
          pReached.updatePrecisionForState(
              currentState,
              newPrecision,
              VariableTrackingPrecision.isMatchingCPAClass(ValueAnalysisCPA.class));

          // an option, that if a state has more than one child, the one child
          // will get a new precision in the next loop iteration, but also readd
          // all other children to the waitlist, with the new precision, which
          // should helps that the waitlist does not run dry too fast
          // -> did not help much

          ARGState parent = Iterables.getFirst(currentState.getParents(), null);
          if (parent != null) {
            // readdSiblings(pReached, parent, currentState, newPrecision);
          }

          previsousPrecision = currentPrecision;
          previousIncrement = increment;
        }
      }
      timePrecision.stop();

      timeRemove.start();
      removeInfeasiblePartsOfArg(pInterpolationTree, pReached);
      timeRemove.stop();
    }

    private Set<ARGState> strengthenStates(
        InterpolationTree<ValueAnalysisState, ValueAnalysisInterpolant> interpolationTree) {
      Set<ARGState> strengthenedStates = new HashSet<>();

      for (Map.Entry<ARGState, ValueAnalysisInterpolant> entry :
          interpolationTree.getInterpolantMapping()) {
        if (!entry.getValue().isTrivial()) {

          ARGState state = entry.getKey();
          ValueAnalysisInterpolant itp = entry.getValue();
          ValueAnalysisState valueState =
              AbstractStates.extractStateByType(state, ValueAnalysisState.class);

          if (itp.strengthen(valueState, state)) {
            strengthenedStates.add(state);
          }
        }
      }

      return strengthenedStates;
    }

    private void tryToCoverArg(
        Set<ARGState> strengthenedStates, ARGReachedSet reached, ARGState pTargetState)
        throws InterruptedException {
      ARGState coverageRoot = null;

      ARGPath errorPath = ARGUtils.getOnePathTo(pTargetState);

      for (ARGState state : errorPath.asStatesList()) {

        if (strengthenedStates.contains(state)) {
          try {
            // if it became (unsoundly!) covered in a previous iteration of another target path
            if (state.isCovered()
                // or if it is covered by now
                || reached.tryToCover(state, true)) {
              coverageRoot = state;
              break;
            }
          } catch (CPAException e) {
            throw new AssertionError(e); // TODO
          }
        }
      }

      if (coverageRoot != null) {
        for (ARGState children : ARGUtils.getNonCoveredStatesInSubgraph(coverageRoot)) {
          children.setCovered(coverageRoot);
        }
      }
    }

    private void removeInfeasiblePartsOfArg(
        InterpolationTree<ValueAnalysisState, ValueAnalysisInterpolant> interpolationTree,
        ARGReachedSet reached) {
      for (ARGState root : interpolationTree.obtainCutOffRoots()) {
        reached.cutOffSubtree(root);
      }
    }

    @Override
    protected void printAdditionalStatistics(
        PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      StatisticsWriter w = StatisticsWriter.writingStatisticsTo(pOut);
      w.beginLevel().put(timeStrengthen).put(timeCoverage).put(timePrecision).put(timeRemove);
    }
  }
}
