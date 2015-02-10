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

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
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
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.MemoryLocation;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ErrorPathClassifier;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ErrorPathClassifier.ErrorPathPrefixPreference;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Precisions;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

@Options(prefix = "cpa.value.refinement")
public class ValueAnalysisRefiner implements Refiner, StatisticsProvider {

  @Option(secure = true, description = "whether or not to do lazy-abstraction", name = "restart", toUppercase = true)
  private RestartStrategy restartStrategy = RestartStrategy.BOTTOM;

  @Option(
      secure = true,
      description = "whether to use the top-down interpolation strategy or the bottom-up interpolation strategy")
  private boolean useTopDownInterpolationStrategy = true;

  @Option(
      secure = true,
      description = "heuristic to sort targets based on the quality of interpolants deriveable from them")
  private boolean itpSortedTargets = false;

  @Option(secure = true, description = "when to export the interpolation tree"
      + "\nNEVER:   never export the interpolation tree"
      + "\nFINAL:   export the interpolation tree once after each refinement"
      + "\nALWAYD:  export the interpolation tree once after each interpolation, i.e. multiple times per refinmenet",
      values = { "NEVER", "FINAL", "ALWAYS" })
  private String exportInterpolationTree = "NEVER";

  @Option(secure = true, description = "export interpolation trees to this file template")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate interpolationTreeExportFile = PathTemplate.ofFormatString("interpolationTree.%d-%d.dot");

  private ValueAnalysisPathInterpolator pathInterpolator;

  private ValueAnalysisFeasibilityChecker checker;

  private final LogManager logger;

  private int previousErrorPathId = -1;

  private ErrorPathClassifier classifier;

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
    if (valueAnalysisCpa == null) { throw new InvalidConfigurationException(ValueAnalysisRefiner.class.getSimpleName()
        + " needs a ValueAnalysisCPA"); }

    valueAnalysisCpa.injectRefinablePrecision();

    ValueAnalysisRefiner refiner = new ValueAnalysisRefiner(valueAnalysisCpa.getConfiguration(),
        valueAnalysisCpa.getLogger(),
        valueAnalysisCpa.getShutdownNotifier(),
        valueAnalysisCpa.getCFA());

    valueAnalysisCpa.getStats().addRefiner(refiner);


    return refiner;
  }

  ValueAnalysisRefiner(final Configuration pConfig, final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier, final CFA pCfa)
      throws InvalidConfigurationException {

    pConfig.inject(this);

    logger = pLogger;
    pathInterpolator = new ValueAnalysisPathInterpolator(pConfig, pLogger, pShutdownNotifier, pCfa);
    checker = new ValueAnalysisFeasibilityChecker(pLogger, pCfa, pConfig);

    classifier = new ErrorPathClassifier(pCfa.getVarClassification(), pCfa.getLoopStructure());

    for(CFANode node : pCfa.getAllNodes()) {
      for(int i = 0; i < node.getNumLeavingEdges(); i++) {
        CFAEdge edge = node.getLeavingEdge(i);

        if(edge.getEdgeType() == CFAEdgeType.StatementEdge) {
          processStatementEdge((CStatementEdge)edge, extendedCounterVariables);
        }

        else if (edge.getEdgeType() == CFAEdgeType.MultiEdge) {
          for(CFAEdge singleEdge : ((MultiEdge)edge).getEdges()) {
            if(singleEdge.getEdgeType() == CFAEdgeType.StatementEdge) {
              processStatementEdge((CStatementEdge)singleEdge, extendedCounterVariables);
            }
          }
        }
      }
    }
  }

  public static Set<String> extendedCounterVariables = new HashSet<>();

  private Set<String> processStatementEdge(CStatementEdge stmtEdge, Set<String> incDecVars) {

    if (stmtEdge.getStatement() instanceof CAssignment) {
      CAssignment assign = (CAssignment) stmtEdge.getStatement();

      if (assign.getLeftHandSide() instanceof CIdExpression) {
        CIdExpression assignementToId = (CIdExpression) assign.getLeftHandSide();
        String assignToVar = assignementToId.getDeclaration().getQualifiedName();

        if (assign.getRightHandSide() instanceof CBinaryExpression) {
          CBinaryExpression binExpr = (CBinaryExpression) assign.getRightHandSide();
          BinaryOperator op = binExpr.getOperator();

          if (op == BinaryOperator.PLUS || op == BinaryOperator.MINUS) {

            if (binExpr.getOperand1() instanceof CLiteralExpression
                || binExpr.getOperand2() instanceof CLiteralExpression) {
              CIdExpression operandId = null;

              if (binExpr.getOperand1() instanceof CIdExpression) {
                operandId = (CIdExpression) binExpr.getOperand1();
              }
              if (binExpr.getOperand2() instanceof CIdExpression) {
                operandId = (CIdExpression) binExpr.getOperand2();
              }

              if (operandId != null) {
                String operandVar = operandId.getDeclaration().getQualifiedName();
                if (assignToVar.equals(operandVar)) {
                  incDecVars.add(assignToVar);
                }
              }
            }
          }
        }
      }
    }

    return incDecVars;
  }

  private boolean madeProgress(ARGPath path) {
    boolean progress = (previousErrorPathId == -1 || previousErrorPathId != obtainErrorPathId(path));

    previousErrorPathId = obtainErrorPathId(path);

    return progress;
  }

  @Override
  public boolean performRefinement(final ReachedSet pReached) throws CPAException, InterruptedException {
    return performRefinement(new ARGReachedSet(pReached));
  }

  public boolean performRefinement(final ARGReachedSet pReached) throws CPAException, InterruptedException {
    logger.log(Level.FINEST, "performing global refinement ...");
    totalTime.start();
    refinementCounter++;

    Collection<ARGState> targets = getTargetStates(pReached);
    List<ARGPath> targetPaths = getTargetPaths(targets);

    if (!madeProgress(targetPaths.get(0))) {
      throw new RefinementFailedException(Reason.RepeatedCounterexample,
        targetPaths.get(0));
    }

    // stop once any feasible counterexample is found
    if (isAnyPathFeasible(pReached, targetPaths)) {
      totalTime.stop();
      return false;
    }

    ValueAnalysisInterpolationTree interpolationTree = obtainInterpolants(targets);

    refineUsingInterpolants(pReached, interpolationTree);

    totalTime.stop();
    return true;
  }

  private void refineUsingInterpolants(final ARGReachedSet pReached, ValueAnalysisInterpolationTree interpolationTree) {

    final boolean predicatePrecisionIsAvailable = isPredicatePrecisionAvailable(pReached);

    Map<ARGState, List<Precision>> refinementInformation = new HashMap<>();
    Collection<ARGState> refinementRoots = interpolationTree.obtainRefinementRoots(restartStrategy);

    for (ARGState root : refinementRoots) {
      root = relocateRefinementRoot(root, predicatePrecisionIsAvailable);

      if (isSimilarRepeatedRefinement(interpolationTree.extractPrecisionIncrement(root).values())) {
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

  private ValueAnalysisInterpolationTree obtainInterpolants(Collection<ARGState> targets) throws CPAException,
      InterruptedException {
    ValueAnalysisInterpolationTree interpolationTree =
        new ValueAnalysisInterpolationTree(logger, targets, useTopDownInterpolationStrategy);

    while (interpolationTree.hasNextPathForInterpolation()) {
      performPathInterpolation(interpolationTree);
    }

    if (interpolationTreeExportFile != null && exportInterpolationTree.equals("FINAL")
        && !exportInterpolationTree.equals("ALWAYS")) {
      interpolationTree.exportToDot(interpolationTreeExportFile, refinementCounter);
    }
    return interpolationTree;
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

    interpolationTree.addInterpolants(pathInterpolator.performInterpolation(errorPath, initialItp));

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
    return !previousRefinementIds.add(new TreeSet<>(currentIncrement).hashCode());
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

    ARGPath path = ARGUtils.getOnePathTo(Iterables.getOnlyElement(currentRoot.getParents()));
    for (ARGState currentState : path.asStatesList().reverse()) {
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
    if(restartStrategy == RestartStrategy.TOP) {
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

  private boolean isAnyPathFeasible(final ARGReachedSet pReached, final Collection<ARGPath> errorPaths)
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
      return true;
    }

    return false;
  }

  private boolean isErrorPathFeasible(final ARGPath errorPath)
      throws CPAException, InterruptedException {
    if (checker.isFeasible(errorPath)) {
      logger.log(Level.FINEST, "found a feasible cex - returning from refinement");

      return true;
    }

    return false;
  }

  /**
   * This method returns the list of paths to the target states, sorted by the
   * length of the paths, in ascending order.
   *
   * @param targetStates the target states for which to get the target paths
   * @return the list of paths to the target states
   */
  private List<ARGPath> getTargetPaths(final Collection<ARGState> targetStates) {
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
   */
  private Collection<ARGState> getTargetStates(final ARGReachedSet pReached) {

    // sort the list, to either favor shorter paths or better interpolants
    Comparator<ARGState> comparator = new Comparator<ARGState>() {
      @Override
      public int compare(ARGState target1, ARGState target2) {
        try {
          ARGPath path1 = ARGUtils.getOnePathTo(target1);
          ARGPath path2 = ARGUtils.getOnePathTo(target2);

          if(itpSortedTargets) {
            List<ARGPath> prefixes1 = checker.getInfeasilbePrefixes(path1, new ValueAnalysisState());
            List<ARGPath> prefixes2 = checker.getInfeasilbePrefixes(path2, new ValueAnalysisState());

            Long score1 = classifier.obtainScoreForPrefixes(prefixes1, ErrorPathPrefixPreference.DOMAIN_BEST_BOUNDED);
            Long score2 = classifier.obtainScoreForPrefixes(prefixes2, ErrorPathPrefixPreference.DOMAIN_BEST_BOUNDED);

            if(score1.equals(score2)) {
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
        } catch (CPAException | InterruptedException e) {
          throw new AssertionError(e);
        }
      }
    };

    // obtain all target locations, excluding feasible ones
    // this filtering is needed to distinguish between multiple targets being available
    // because of stopAfterError=false (feasible) versus globalRefinement=true (new)
    List<ARGState> targets = from(pReached.asReachedSet())
        .transform(AbstractStates.toState(ARGState.class))
        .filter(AbstractStates.IS_TARGET_STATE)
        .filter(Predicates.not(Predicates.in(feasibleTargets))).toSortedList(comparator);
    assert !targets.isEmpty();

    logger.log(Level.FINEST, "number of targets found: " + targets.size());

    targetCounter = targetCounter + targets.size();

    return targets;
  }

  @Override
  public void collectStatistics(final Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Statistics() {

      @Override
      public String getName() {
        return ValueAnalysisRefiner.class.getSimpleName();
      }

      @Override
      public void printStatistics(final PrintStream pOut, final Result pResult, final ReachedSet pReached) {
        ValueAnalysisRefiner.this.printStatistics(pOut, pResult, pReached);
      }
    });
  }

  private void printStatistics(final PrintStream out, final Result pResult, final ReachedSet pReached) {
    if (refinementCounter > 0) {
      out.println("Total number of refinements:      " + String.format(Locale.US, "%9d", refinementCounter));
      out.println("Total number of targets found:    " + String.format(Locale.US, "%9d", targetCounter));
      out.println("Time for completing refinement:       " + totalTime);
      pathInterpolator.printStatistics(out, pResult, pReached);
      out.println("Total number of root relocations: " + String.format(Locale.US, "%9d", timesRootRelocated));
      out.println("Total number of similar, repeated refinements: " + String.format(Locale.US, "%9d", timesRepeatedRefinements));

    }
  }

  private int obtainErrorPathId(ARGPath path) {
    return path.toString().hashCode();
  }

  /**
   * The strategy to determine where to restart the analysis after a successful refinement.
   * {@link #TOP} means that the analysis is restarted from the root of the ARG
   * {@link #BOTTOM} means that the analysis is restarted from the individual refinement roots identified
   * {@link #COMMON} means that the analysis is restarted from lowest ancestor common to all refinement roots, if more
   * than two refinement roots where identified
   */
  public enum RestartStrategy {
    TOP,
    BOTTOM,
    COMMON
  }
}

