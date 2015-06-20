/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.value.refiner;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.counterexample.RichModel;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisInformation;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.SortingPathExtractor;
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
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

@Options(prefix = "cpa.value.refinement")
public class ValueAnalysisRefiner
    extends GenericRefiner<ValueAnalysisState, ValueAnalysisInterpolant> {

  @Option(secure = true, description = "whether or not to do lazy-abstraction", name = "restart", toUppercase = true)
  private RestartStrategy restartStrategy = RestartStrategy.PIVOT;

  @Option(
      secure = true,
      description = "whether or not to use heuristic to avoid similar, repeated refinements")
  private boolean avoidSimilarRepeatedRefinement = false;

  /**
   * keep log of previous refinements to identify repeated one
   */
  private final Set<Integer> previousRefinementIds = new HashSet<>();

  private final ValueAnalysisFeasibilityChecker checker;

  private ValueAnalysisConcreteErrorPathAllocator concreteErrorPathAllocator;

  // Statistics
  private int timesRootRelocated = 0;
  private int timesRepeatedRefinements = 0;

  public static ValueAnalysisRefiner create(final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {

    final ValueAnalysisCPA valueAnalysisCpa = CPAs.retrieveCPA(pCpa, ValueAnalysisCPA.class);
    if (valueAnalysisCpa == null) {
      throw new InvalidConfigurationException(ValueAnalysisRefiner.class.getSimpleName() + " needs a ValueAnalysisCPA");
    }

    valueAnalysisCpa.injectRefinablePrecision();

    final LogManager logger = valueAnalysisCpa.getLogger();
    final Configuration config = valueAnalysisCpa.getConfiguration();
    final CFA cfa = valueAnalysisCpa.getCFA();

    final StrongestPostOperator<ValueAnalysisState> strongestPostOp =
        new ValueAnalysisStrongestPostOperator(logger, Configuration.builder().build(), cfa);

    final ValueAnalysisFeasibilityChecker checker =
        new ValueAnalysisFeasibilityChecker(strongestPostOp, logger, cfa, config);

    final GenericPrefixProvider<ValueAnalysisState> prefixProvider =
        new ValueAnalysisPrefixProvider(logger, cfa, config);
    final PrefixSelector prefixSelector = new PrefixSelector(cfa.getVarClassification(),
                                                             cfa.getLoopStructure());

    ValueAnalysisRefiner refiner = new ValueAnalysisRefiner(
        checker,
        strongestPostOp,
        new SortingPathExtractor(prefixProvider,
                                 prefixSelector,
                                 logger, config),
        prefixProvider,
        config,
        logger,
        valueAnalysisCpa.getShutdownNotifier(),
        cfa);

    return refiner;
  }

  ValueAnalysisRefiner(
      final ValueAnalysisFeasibilityChecker pFeasibilityChecker,
      final StrongestPostOperator<ValueAnalysisState> pStrongestPostOperator,
      final PathExtractor pPathExtractor,
      final GenericPrefixProvider<ValueAnalysisState> pPrefixProvider,
      final Configuration pConfig, final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier, final CFA pCfa)
      throws InvalidConfigurationException {

    super(pFeasibilityChecker,
        new ValueAnalysisPathInterpolator(pFeasibilityChecker,
            pStrongestPostOperator,
            pPrefixProvider,
            pConfig, pLogger, pShutdownNotifier, pCfa),
        ValueAnalysisInterpolantManager.getInstance(),
        pPathExtractor,
        pConfig,
        pLogger,
        pShutdownNotifier,
        pCfa);

    pConfig.inject(this);

    checker = pFeasibilityChecker;

    concreteErrorPathAllocator = new ValueAnalysisConcreteErrorPathAllocator(pLogger, pShutdownNotifier, pCfa.getMachineModel());
  }

  @Override
  protected void refineUsingInterpolants(
      final ARGReachedSet pReached,
      final InterpolationTree<ValueAnalysisState, ValueAnalysisInterpolant> pInterpolationTree
  ) {
    final boolean predicatePrecisionIsAvailable = isPredicatePrecisionAvailable(pReached);

    Map<ARGState, List<Precision>> refinementInformation = new HashMap<>();
    Collection<ARGState> refinementRoots = pInterpolationTree.obtainRefinementRoots(restartStrategy);

    for (ARGState root : refinementRoots) {
      root = relocateRefinementRoot(root, predicatePrecisionIsAvailable);

      if (refinementRoots.size() == 1 && isSimilarRepeatedRefinement(
          pInterpolationTree.extractPrecisionIncrement(root).values())) {
        root = relocateRepeatedRefinementRoot(root);
      }

      List<Precision> precisions = new ArrayList<>(2);
      // merge the value precisions of the subtree, and refine it
      precisions.add(mergeValuePrecisionsForSubgraph(root, pReached)
          .withIncrement(pInterpolationTree.extractPrecisionIncrement(root)));

      // merge the predicate precisions of the subtree, if available
      if (predicatePrecisionIsAvailable) {
        precisions.add(mergePredicatePrecisionsForSubgraph(root, pReached));
      }

      refinementInformation.put(root, precisions);
    }

    for (Entry<ARGState, List<Precision>> info : refinementInformation.entrySet()) {
      List<Predicate<? super Precision>> precisionTypes = new ArrayList<>(2);

      precisionTypes.add(VariableTrackingPrecision.isMatchingCPAClass(ValueAnalysisCPA.class));
      if (predicatePrecisionIsAvailable) {
        precisionTypes.add(Predicates.instanceOf(PredicatePrecision.class));
      }

      pReached.removeSubtree(info.getKey(), info.getValue(), precisionTypes);
    }
  }

  private boolean isPredicatePrecisionAvailable(final ARGReachedSet pReached) {
    return Precisions.extractPrecisionByType(pReached.asReachedSet()
        .getPrecision(pReached.asReachedSet().getFirstState()), PredicatePrecision.class) != null;
  }

  private VariableTrackingPrecision mergeValuePrecisionsForSubgraph(final ARGState pRefinementRoot,
      final ARGReachedSet pReached) {
    // get all unique precisions from the subtree
    Set<VariableTrackingPrecision> uniquePrecisions = Sets.newIdentityHashSet();
    for (ARGState descendant : getNonCoveredStatesInSubgraph(pRefinementRoot)) {
      uniquePrecisions.add(extractValuePrecision(pReached, descendant));
    }

    // join all unique precisions into a single precision
    VariableTrackingPrecision mergedPrecision = Iterables.getLast(uniquePrecisions);
    for (VariableTrackingPrecision precision : uniquePrecisions) {
      mergedPrecision = mergedPrecision.join(precision);
    }

    return mergedPrecision;
  }

  /**
   * Merge all predicate precisions in the subgraph below the refinement root
   * into a new predicate precision
   *
   * @return a new predicate precision containing all predicate precision from
   * the subgraph below the refinement root.
   */
  private PredicatePrecision mergePredicatePrecisionsForSubgraph(
      final ARGState pRefinementRoot,
      final ARGReachedSet pReached
  ) {

    PredicatePrecision mergedPrecision = PredicatePrecision.empty();

    // find all distinct precisions to merge them
    Set<PredicatePrecision> uniquePrecisions = Sets.newIdentityHashSet();
    for (ARGState descendant : getNonCoveredStatesInSubgraph(pRefinementRoot)) {
      uniquePrecisions.add(extractPredicatePrecision(pReached, descendant));
    }

    for (PredicatePrecision precision : uniquePrecisions) {
      mergedPrecision = mergedPrecision.mergeWith(precision);
    }

    return mergedPrecision;
  }

  private VariableTrackingPrecision extractValuePrecision(final ARGReachedSet pReached,
      ARGState state) {
    return (VariableTrackingPrecision) Precisions
        .asIterable(pReached.asReachedSet().getPrecision(state))
        .filter(VariableTrackingPrecision.isMatchingCPAClass(ValueAnalysisCPA.class))
        .get(0);
  }

  protected final PredicatePrecision extractPredicatePrecision(final ARGReachedSet pReached,
      ARGState state) {
    return (PredicatePrecision) Precisions.asIterable(pReached.asReachedSet().getPrecision(state))
        .filter(Predicates.instanceOf(PredicatePrecision.class))
        .get(0);
  }

  private Collection<ARGState> getNonCoveredStatesInSubgraph(ARGState pRoot) {
    Collection<ARGState> subgraph = new HashSet<>();
    for (ARGState state : pRoot.getSubgraph()) {
      if (!state.isCovered()) {
        subgraph.add(state);
      }
    }
    return subgraph;
  }

  /**
   * A simple heuristic to detect similar repeated refinements.
   */
  private boolean isSimilarRepeatedRefinement(Collection<MemoryLocation> currentIncrement) {
    // a refinement is a similar, repeated refinement
    // if the (sorted) precision increment was already added in a previous refinement
    return avoidSimilarRepeatedRefinement && !previousRefinementIds.add(new TreeSet<>(currentIncrement).hashCode());
  }

  /**
   * This method chooses a new refinement root, in a bottom-up fashion along the error path.
   * It either picks the next state on the path sharing the same CFA location, or the (only)
   * child of the ARG root, what ever comes first.
   *
   * @param currentRoot the current refinement root
   * @return the relocated refinement root
   */
  private ARGState relocateRepeatedRefinementRoot(final ARGState currentRoot) {
    timesRepeatedRefinements++;
    int currentRootNumber = AbstractStates.extractLocation(currentRoot).getNodeNumber();

    ARGPath path = ARGUtils.getOnePathTo(currentRoot);
    for (ARGState currentState : path.asStatesList().reverse()) {
      // skip identity, because a new root has to be found
      if (currentState == currentRoot) {
        continue;
      }

      if (currentRootNumber == AbstractStates.extractLocation(currentState).getNodeNumber()) {
        return currentState;
      }
    }

    return Iterables.getOnlyElement(path.getFirstState().getChildren());
  }

  private ARGState relocateRefinementRoot(final ARGState pRefinementRoot,
      final boolean  predicatePrecisionIsAvailable) {

    // no relocation needed if only running value analysis,
    // because there, this does slightly degrade performance
    // when running VA+PA, merging/covering and refinements
    // of both CPAs could lead to the state, where in two
    // subsequent refinements, two identical error paths
    // were found, through different parts of the ARG
    // So now, when running VA+PA, the refinement root
    // is set to the lowest common ancestor of those states
    // that are covered by the states in the subtree of the
    // original refinement root
    if(!predicatePrecisionIsAvailable) {
      return pRefinementRoot;
    }

    // no relocation needed if restart at top
    if(restartStrategy == RestartStrategy.ROOT) {
      return pRefinementRoot;
    }

    Set<ARGState> descendants = pRefinementRoot.getSubgraph();
    Set<ARGState> coveredStates = new HashSet<>();
    for (ARGState descendant : descendants) {
      coveredStates.addAll(descendant.getCoveredByThis());
    }
    coveredStates.add(pRefinementRoot);

    // no relocation needed if set of descendants is closed under coverage
    if(descendants.containsAll(coveredStates)) {
      return pRefinementRoot;
    }

    Map<ARGState, ARGState> predecessorRelation = Maps.newHashMap();
    SetMultimap<ARGState, ARGState> successorRelation = LinkedHashMultimap.create();

    Deque<ARGState> todo = new ArrayDeque<>(coveredStates);
    ARGState coverageTreeRoot = null;

    // build the coverage tree, bottom-up, starting from the covered states
    while (!todo.isEmpty()) {
      final ARGState currentState = todo.removeFirst();

      if (currentState.getParents().iterator().hasNext()) {
        ARGState parentState = currentState.getParents().iterator().next();
        todo.add(parentState);
        predecessorRelation.put(currentState, parentState);
        successorRelation.put(parentState, currentState);

      } else if (coverageTreeRoot == null) {
        coverageTreeRoot = currentState;
      }
    }

    // starting from the root of the coverage tree,
    // the new refinement root is either the first node
    // having two or more children, or the original
    // refinement root, what ever comes first
    ARGState newRefinementRoot = coverageTreeRoot;
    while (successorRelation.get(newRefinementRoot).size() == 1 && newRefinementRoot != pRefinementRoot) {
      newRefinementRoot = Iterables.getOnlyElement(successorRelation.get(newRefinementRoot));
    }

    timesRootRelocated++;
    return newRefinementRoot;
  }

  /**
   * This method creates a model for the given error path.
   *
   * @param errorPath the error path for which to create the model
   * @return the model for the given error path
   * @throws InterruptedException
   * @throws CPAException
   */
  @Override
  protected RichModel createModel(ARGPath errorPath) throws InterruptedException, CPAException {
    return concreteErrorPathAllocator.allocateAssignmentsToPath(checker.evaluate(errorPath));
  }

  @Override
  protected void printAdditionalStatistics(PrintStream out, Result pResult, ReachedSet pReached) {
    out.println("Total number of root relocations: " + String.format(Locale.US, "%9d", timesRootRelocated));
    out.println("Total number of similar, repeated refinements: " + String.format(Locale.US, "%9d", timesRepeatedRefinements));
  }
}

