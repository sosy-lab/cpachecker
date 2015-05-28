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

import static com.google.common.collect.FluentIterable.from;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisPrefixProvider;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.refinement.InfeasiblePrefix;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector;
import org.sosy_lab.cpachecker.util.refinement.PrefixSelector.PrefixPreference;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

@Options(prefix = "cpa.value.refinement")
public class ValueAnalysisRefiner implements Refiner, StatisticsProvider {

  @Option(secure = true, description = "whether or not to do lazy-abstraction", name = "restart", toUppercase = true)
  private RestartStrategy restartStrategy = RestartStrategy.PIVOT;

  @Option(
      secure = true,
      description = "heuristic to sort targets based on the quality of interpolants deriveable from them")
  private boolean itpSortedTargets = false;

  @Option(
      secure = true,
      description = "whether or not to use heuristic to avoid similar, repeated refinements")
  private boolean avoidSimilarRepeatedRefinement = false;

  @Option(secure = true, description = "when to export the interpolation tree"
      + "\nNEVER:   never export the interpolation tree"
      + "\nFINAL:   export the interpolation tree once after each refinement"
      + "\nALWAYD:  export the interpolation tree once after each interpolation, i.e. multiple times per refinmenet",
      values = { "NEVER", "FINAL", "ALWAYS" })
  private String exportInterpolationTree = "NEVER";

  @Option(secure = true, description = "export interpolation trees to this file template")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate interpolationTreeExportFile = PathTemplate.ofFormatString("interpolationTree.%d-%d.dot");

  protected final LogManager logger;

  private ValueAnalysisPathInterpolator interpolator;

  private ValueAnalysisFeasibilityChecker checker;

  private ValueAnalysisPrefixProvider prefixProvider;

  private ValueAnalysisConcreteErrorPathAllocator concreteErrorPathAllocator;

  private PrefixSelector classifier;

  private int previousErrorPathId = -1;

  /**
   * keep log of feasible targets that were already found
   */
  private final Set<ARGState> feasibleTargets = new HashSet<>();

  /**
   * keep log of previous refinements to identify repeated one
   */
  private final Set<Integer> previousRefinementIds = new HashSet<>();

  // statistics
  private int refinementCounter = 0;
  private int targetCounter = 0;
  private final Timer totalTime = new Timer();
  private int timesRootRelocated = 0;
  private int timesRepeatedRefinements = 0;

  public static ValueAnalysisRefiner create(final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {

    final ValueAnalysisCPA valueAnalysisCpa = CPAs.retrieveCPA(pCpa, ValueAnalysisCPA.class);
    if (valueAnalysisCpa == null) {
      throw new InvalidConfigurationException(ValueAnalysisRefiner.class.getSimpleName() + " needs a ValueAnalysisCPA");
    }

    valueAnalysisCpa.injectRefinablePrecision();

    ValueAnalysisRefiner refiner = new ValueAnalysisRefiner(valueAnalysisCpa.getConfiguration(),
        valueAnalysisCpa.getLogger(),
        valueAnalysisCpa.getShutdownNotifier(),
        valueAnalysisCpa.getCFA());

    return refiner;
  }

  ValueAnalysisRefiner(final Configuration pConfig, final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier, final CFA pCfa)
      throws InvalidConfigurationException {

    pConfig.inject(this);

    logger = pLogger;

    interpolator   = new ValueAnalysisPathInterpolator(pConfig, pLogger, pShutdownNotifier, pCfa);
    checker        = new ValueAnalysisFeasibilityChecker(pLogger, pCfa, pConfig);
    prefixProvider = new ValueAnalysisPrefixProvider(pLogger, pCfa, pConfig);
    classifier     = new PrefixSelector(pCfa.getVarClassification(), pCfa.getLoopStructure());

    concreteErrorPathAllocator = new ValueAnalysisConcreteErrorPathAllocator(logger, pShutdownNotifier, pCfa.getMachineModel());
  }

  @Override
  public boolean performRefinement(final ReachedSet pReached)
      throws CPAException, InterruptedException {
    return performRefinement(new ARGReachedSet(pReached)).isSpurious();
  }

  public CounterexampleInfo performRefinement(final ARGReachedSet pReached)
      throws CPAException, InterruptedException {
    Collection<ARGState> targets = getTargetStates(pReached);
    List<ARGPath> targetPaths = getTargetPaths(targets);

    // fail hard on a repeated counterexample, this is most definitively a bug
    if (!madeProgress(targetPaths.get(0))) {
      throw new RefinementFailedException(Reason.RepeatedCounterexample, targetPaths.get(0));
    }

    return performRefinement(pReached, targets, targetPaths);
  }

  public CounterexampleInfo performRefinement(final ARGReachedSet pReached, ARGPath targetPath)
      throws CPAException, InterruptedException {
    Collection<ARGState> targets = Collections.singleton(targetPath.getLastState());

    // if the target path is given from outside, do not fail hard on a repeated counterexample:
    // this can happen when the predicate-analysis refinement returns back-to-back target paths
    // that are feasible under predicate-analysis semantics and hands those into the value-analysis
    // refiner, where the in-between value-analysis refinement happens to only affect paths in a
    // (ABE) block, which may not be visible when constructing the target path in the next refinement.
    if (!madeProgress(targetPath)) {
      logger.log(Level.INFO, "The error path given to", getClass().getSimpleName(), "is a repeated counterexample,",
          "so instead, refiner uses an error path extracted from the reachset.");
      targetPath = getTargetPaths(targets).get(0);
    }

    return performRefinement(pReached, targets, Lists.newArrayList(targetPath));
  }

  private CounterexampleInfo performRefinement(final ARGReachedSet pReached,
      Collection<ARGState> targets,
      List<ARGPath> targetPaths)
          throws RefinementFailedException, CPAException, InterruptedException {

    logger.log(Level.FINEST, "performing refinement ...");

    totalTime.start();
    refinementCounter++;
    targetCounter = targetCounter + targets.size();

    CounterexampleInfo cex = isAnyPathFeasible(pReached, targetPaths);

    if (cex.isSpurious()) {
      refineUsingInterpolants(pReached, obtainInterpolants(targetPaths));
    }

    totalTime.stop();

    return cex;
  }

  private void refineUsingInterpolants(final ARGReachedSet pReached, ValueAnalysisInterpolationTree interpolationTree) {

    final boolean predicatePrecisionIsAvailable = isPredicatePrecisionAvailable(pReached);

    Map<ARGState, List<Precision>> refinementInformation = new HashMap<>();
    Collection<ARGState> refinementRoots = interpolationTree.obtainRefinementRoots(restartStrategy);

    for (ARGState root : refinementRoots) {
      root = relocateRefinementRoot(root, predicatePrecisionIsAvailable);

      if (refinementRoots.size() == 1 && isSimilarRepeatedRefinement(interpolationTree.extractPrecisionIncrement(root).values())) {
        root = relocateRepeatedRefinementRoot(root);
      }

      List<Precision> precisions = new ArrayList<>(2);
      // merge the value precisions of the subtree, and refine it
      precisions.add(mergeValuePrecisionsForSubgraph(root, pReached)
          .withIncrement(interpolationTree.extractPrecisionIncrement(root)));

      // merge the predicate precisions of the subtree, if available
      if (predicatePrecisionIsAvailable) {
        precisions.add(mergePredictePrecisionsForSubgraph(root, pReached));
      }

      refinementInformation.put(root, precisions);
    }

    for (Map.Entry<ARGState, List<Precision>> info : refinementInformation.entrySet()) {
      List<Predicate<? super Precision>> precisionTypes = new ArrayList<>(2);

      precisionTypes.add(VariableTrackingPrecision.isMatchingCPAClass(ValueAnalysisCPA.class));
      if (predicatePrecisionIsAvailable) {
        precisionTypes.add(Predicates.instanceOf(PredicatePrecision.class));
      }

      pReached.removeSubtree(info.getKey(), info.getValue(), precisionTypes);
    }
  }

  private ValueAnalysisInterpolationTree obtainInterpolants(List<ARGPath> targetsPaths) throws CPAException,
      InterruptedException {
    ValueAnalysisInterpolationTree interpolationTree = createInterpolationTree(targetsPaths);

    while (interpolationTree.hasNextPathForInterpolation()) {
      performPathInterpolation(interpolationTree);
    }

    if (interpolationTreeExportFile != null && exportInterpolationTree.equals("FINAL")
        && !exportInterpolationTree.equals("ALWAYS")) {
      interpolationTree.exportToDot(interpolationTreeExportFile, refinementCounter);
    }
    return interpolationTree;
  }

  /**
   * This method creates the interpolation tree. As there is only a single target, it is irrelevant
   * whether to use top-down or bottom-up interpolation, as the tree is degenerated to a list.
   */
  protected ValueAnalysisInterpolationTree createInterpolationTree(List<ARGPath> targetsPaths) {
    return new ValueAnalysisInterpolationTree(logger, targetsPaths, true);
  }

  private boolean isPredicatePrecisionAvailable(final ARGReachedSet pReached) {
    return Precisions.extractPrecisionByType(pReached.asReachedSet()
        .getPrecision(pReached.asReachedSet().getFirstState()), PredicatePrecision.class) != null;
  }

  private void performPathInterpolation(ValueAnalysisInterpolationTree interpolationTree) throws CPAException,
      InterruptedException {
    ARGPath errorPath = interpolationTree.getNextPathForInterpolation();

    if (errorPath == ValueAnalysisInterpolationTree.EMPTY_PATH) {
      logger.log(Level.FINEST, "skipping interpolation,"
          + " because false interpolant on path to target state");
      return;
    }

    ValueAnalysisInterpolant initialItp = interpolationTree.getInitialInterpolantForPath(errorPath);

    if (isInitialInterpolantTooWeak(interpolationTree.getRoot(), initialItp, errorPath)) {
      errorPath = ARGUtils.getOnePathTo(errorPath.getLastState());
      initialItp = ValueAnalysisInterpolant.createInitial();
    }

    logger.log(Level.FINEST, "performing interpolation, starting at ", errorPath.getFirstState().getStateId(),
        ", using interpolant ", initialItp);

    interpolationTree.addInterpolants(interpolator.performInterpolation(errorPath, initialItp));

    if (interpolationTreeExportFile != null && exportInterpolationTree.equals("ALWAYS")) {
      interpolationTree.exportToDot(interpolationTreeExportFile, refinementCounter);
    }
  }

  private boolean isInitialInterpolantTooWeak(ARGState root, ValueAnalysisInterpolant initialItp, ARGPath errorPath)
      throws CPAException, InterruptedException {

    // if the first state of the error path is the root, the interpolant cannot be to weak
    if (errorPath.getFirstState() == root) {
      return false;
    }

    // for all other cases, check if the path is feasible when using the interpolant as initial state
    return checker.isFeasible(errorPath, initialItp.createValueAnalysisState());
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
  private PredicatePrecision mergePredictePrecisionsForSubgraph(final ARGState pRefinementRoot,
      final ARGReachedSet pReached) {

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
    return (VariableTrackingPrecision) Precisions.asIterable(pReached.asReachedSet().getPrecision(state))
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

  private boolean madeProgress(ARGPath path) {
    boolean progress = (previousErrorPathId == -1 || previousErrorPathId != obtainErrorPathId(path));

    previousErrorPathId = obtainErrorPathId(path);

    return progress;
  }

  private CounterexampleInfo isAnyPathFeasible(final ARGReachedSet pReached, final Collection<ARGPath> errorPaths)
      throws CPAException, InterruptedException {

    ARGPath feasiblePath = null;
    for (ARGPath currentPath : errorPaths) {

      if (isErrorPathFeasible(currentPath)) {
        if(feasiblePath == null) {
          previousErrorPathId = obtainErrorPathId(currentPath);
          feasiblePath = currentPath;
        }

        feasibleTargets.add(currentPath.getLastState());
      }
    }

    // remove all other target states, so that only one is left (for CEX-checker)
    if (feasiblePath != null) {
      for (ARGPath others : errorPaths) {
        if (others != feasiblePath) {
          pReached.removeSubtree(others.getLastState());
        }
      }

      logger.log(Level.FINEST, "found a feasible counterexample");
      return CounterexampleInfo.feasible(feasiblePath, createModel(feasiblePath));
    }

    return CounterexampleInfo.spurious();
  }

  boolean isErrorPathFeasible(final ARGPath errorPath)
      throws CPAException, InterruptedException {
    return checker.isFeasible(errorPath);
  }

  /**
   * This method creates a model for the given error path.
   *
   * @param errorPath the error path for which to create the model
   * @return the model for the given error path
   * @throws InvalidConfigurationException
   * @throws InterruptedException
   * @throws CPAException
   */
  private Model createModel(ARGPath errorPath) throws InterruptedException, CPAException {
    return concreteErrorPathAllocator.allocateAssignmentsToPath(checker.evaluate(errorPath));
  }

  /**
   * This method returns the list of paths to the target states, sorted by the
   * length of the paths, in ascending order.
   *
   * @param targetStates the target states for which to get the target paths
   * @return the list of paths to the target states
   */
  protected List<ARGPath> getTargetPaths(final Collection<ARGState> targetStates) {
    List<ARGPath> errorPaths = new ArrayList<>(targetStates.size());

    for (ARGState target : targetStates) {
      errorPaths.add(ARGUtils.getOnePathTo(target));
    }

    return errorPaths;
  }

  /**
   * This method returns an unsorted, non-empty collection of target states
   * found during the analysis.
   *
   * @param pReached the set of reached states
   * @return the target states
   * @throws RefinementFailedException
   */
  private Collection<ARGState> getTargetStates(final ARGReachedSet pReached) throws RefinementFailedException {

    // sort the list, to either favor shorter paths or better interpolants
    Comparator<ARGState> comparator = new Comparator<ARGState>() {
      @Override
      public int compare(ARGState target1, ARGState target2) {
        try {
          ARGPath path1 = ARGUtils.getOnePathTo(target1);
          ARGPath path2 = ARGUtils.getOnePathTo(target2);

          if(itpSortedTargets) {
            List<InfeasiblePrefix> prefixes1 = prefixProvider.extractInfeasiblePrefixes(path1);
            List<InfeasiblePrefix> prefixes2 = prefixProvider.extractInfeasiblePrefixes(path2);

            int score1 = classifier.obtainScoreForPrefixes(prefixes1, PrefixPreference.DOMAIN_GOOD_SHORT);
            int score2 = classifier.obtainScoreForPrefixes(prefixes2, PrefixPreference.DOMAIN_GOOD_SHORT);

            if(score1 == score2) {
              return 0;
            }

            else if(score1 < score2) {
              return -1;
            }

            else {
              return 1;
            }
          }

          else {
            return path1.size() - path2.size();
          }
        } catch (CPAException e) {
          throw new AssertionError(e);
        }
      }
    };

    // extract target locations from and exclude those found to be feasible before,
    // e.g., when analysis.stopAfterError is set to false
    List<ARGState> targets = extractTargetStatesFromArg(pReached)
        .filter(Predicates.not(Predicates.in(feasibleTargets))).toSortedList(comparator);

    // set of targets may only be empty, if all of them were found feasible previously
    if(targets.isEmpty()) {
      assert feasibleTargets.containsAll(extractTargetStatesFromArg(pReached).toSet());

      throw new RefinementFailedException(Reason.RepeatedCounterexample,
          ARGUtils.getOnePathTo(Iterables.getLast(feasibleTargets)));
    }

    logger.log(Level.FINEST, "number of targets found: " + targets.size());

    return targets;
  }

  /**
   * This method extracts the last state from the ARG, which has to be a target state.
   */
  protected FluentIterable<ARGState> extractTargetStatesFromArg(final ARGReachedSet pReached) {
    ARGState lastState = ((ARGState)pReached.asReachedSet().getLastState());

    assert (lastState.isTarget()) : "Last state is not a target state";

    return from(Collections.singleton(lastState));
  }

  @Override
  public void collectStatistics(final Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Statistics() {

      @Override
      public String getName() {
        return ValueAnalysisRefiner.this.getClass().getSimpleName();
      }

      @Override
      public void printStatistics(final PrintStream pOut, final Result pResult, final ReachedSet pReached) {
        ValueAnalysisRefiner.this.printStatistics(pOut, pResult, pReached);
      }
    });
  }

  private void printStatistics(final PrintStream out, final Result pResult, final ReachedSet pReached) {
    out.println("Total number of refinements:      " + String.format(Locale.US, "%9d", refinementCounter));
    out.println("Total number of targets found:    " + String.format(Locale.US, "%9d", targetCounter));
    out.println("Time for completing refinement:       " + totalTime);
    interpolator.printStatistics(out, pResult, pReached);
    out.println("Total number of root relocations: " + String.format(Locale.US, "%9d", timesRootRelocated));
    out.println("Total number of similar, repeated refinements: " + String.format(Locale.US, "%9d", timesRepeatedRefinements));
  }

  private int obtainErrorPathId(ARGPath path) {
    return path.toString().hashCode();
  }

  /**
   * The strategy to determine where to restart the analysis after a successful refinement.
   * {@link #ROOT} means that the analysis is restarted from the root of the ARG
   * {@link #PIVOT} means that the analysis is restarted from the lowest possible refinement root, i.e.,
   *  the first ARGNode associated with a non-trivial interpolant (cf. Lazy Abstraction, 2002)
   * {@link #COMMON} means that the analysis is restarted from lowest ancestor common to all refinement roots, if more
   * than two refinement roots where identified
   */
  public enum RestartStrategy {
    ROOT,
    PIVOT,
    COMMON
  }
}

