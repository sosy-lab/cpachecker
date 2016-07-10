/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.refiner;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.smg.SMGCPA;
import org.sosy_lab.cpachecker.cpa.smg.SMGPredicateManager;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGInterpolant.SMGPrecisionIncrement;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.refinement.InterpolationTree;
import org.sosy_lab.cpachecker.util.refinement.PathExtractor;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;


@Options(prefix = "cpa.smg.refinement")
public class SMGRefiner implements Refiner {

  private final LogManager logger;

  private final SMGFeasibilityChecker checker;
  private final ARGCPA argCpa;
  private final SMGCPA smgCpa;
  private final PathExtractor pathExtractor;
  private final SMGInterpolantManager interpolantManager;
  private final SMGPathInterpolator interpolator;

  private final StatCounter refinementCounter = new StatCounter("Number of refinements");
  private final StatInt numberOfTargets = new StatInt(StatKind.SUM, "Number of targets found");

  private final ShutdownNotifier shutdownNotifier;

  /**
   * keep log of previous refinements to identify repeated one
   */
  private final Set<Integer> previousRefinementIds = new HashSet<>();

  private Set<Integer> previousErrorPathIds = Sets.newHashSet();

  @Option(secure = true, description = "export interpolation trees to this file template")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate interpolationTreeExportFile = PathTemplate.ofFormatString("interpolationTree.%d-%d.dot");

  @Option(secure = true, description = "export interpolant smgs for every path interpolation to this path template")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate exportInterpolantSMGs = PathTemplate.ofFormatString("smg/interpolation-%d/%s");

  @Option(secure = true, description = "export interpolant smgs for every path interpolation to this path template")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate exportRefinmentSMGs = PathTemplate.ofFormatString("smg/refinment-%d/smg-%s");

  @Option(secure = true, description = "when to export the interpolation tree"
      + "\nNEVER:   never export the interpolation tree"
      + "\nFINAL:   export the interpolation tree once after each refinement"
      + "\nALWAYS:  export the interpolation tree once after each interpolation, i.e. multiple times per refinement",
      values = { "NEVER", "FINAL", "ALWAYS" })
  private String exportInterpolationTree = "NEVER";

  private SMGRefiner(SMGCPA pSmgCpa, CFA pCfa, LogManager pLogger, ARGCPA pArgCpa,
      PathExtractor pPathExtractor,
      ShutdownNotifier pShutdownNotifier,
      Configuration pConfig, Set<ControlAutomatonCPA> automatonCpas) throws InvalidConfigurationException {
    pConfig.inject(this);

    logger = pLogger;
    argCpa = pArgCpa;
    smgCpa = pSmgCpa;
    pathExtractor = pPathExtractor;
    SMGPredicateManager predicateManager = smgCpa.getPredicateManager();
    BlockOperator blockOperator = smgCpa.getBlockOperator();

    smgCpa.injectRefinablePrecision();
    smgCpa.setTransferRelationToRefinment(exportRefinmentSMGs);

    SMGStrongestPostOperator strongestPostOpForCEX =
        SMGStrongestPostOperator.getSMGStrongestPostOperatorForCEX(pLogger, pConfig, pCfa, predicateManager, blockOperator);

    SMGState initialState = smgCpa.getInitialState(pCfa.getMainFunction());

    checker =
        new SMGFeasibilityChecker(strongestPostOpForCEX, logger, pCfa, initialState, automatonCpas, smgCpa.getBlockOperator());

    interpolantManager = new SMGInterpolantManager(smgCpa.getMachineModel(), logger,
        pCfa, smgCpa.getTrackPredicates(), smgCpa.getExternalAllocationSize());

    SMGStrongestPostOperator strongestPostOpForInterpolation =
        SMGStrongestPostOperator.getSMGStrongestPostOperatorForInterpolation(pLogger, pConfig, pCfa,
            predicateManager, blockOperator);

    SMGFeasibilityChecker checkerForInterpolation =
        new SMGFeasibilityChecker(strongestPostOpForInterpolation, logger, pCfa, initialState, automatonCpas, smgCpa.getBlockOperator());

    SMGEdgeInterpolator edgeInterpolator =
        new SMGEdgeInterpolator(checkerForInterpolation, strongestPostOpForInterpolation,
            interpolantManager,
            smgCpa.getShutdownNotifier(), logger, smgCpa.getBlockOperator());

    interpolator =
        new SMGPathInterpolator(smgCpa.getShutdownNotifier(), interpolantManager,
            edgeInterpolator, logger, exportInterpolantSMGs, smgCpa.getExportSMGLevel(), checkerForInterpolation);

    shutdownNotifier = pShutdownNotifier;
  }

  public static final SMGRefiner create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {

    ARGCPA argCpa = retrieveCPA(pCpa, ARGCPA.class);
    SMGCPA smgCpa = retrieveCPA(pCpa, SMGCPA.class);
    Set<ControlAutomatonCPA> automatonCpas =
        CPAs.asIterable(pCpa).filter(ControlAutomatonCPA.class).toSet();

    LogManager logger = smgCpa.getLogger();
    Configuration config = smgCpa.getConfiguration();
    CFA cfa = smgCpa.getCFA();

    PathExtractor pathExtractor = new PathExtractor(logger, config);

    return new SMGRefiner(smgCpa, cfa, logger, argCpa, pathExtractor,
        smgCpa.getShutdownNotifier(), config, automatonCpas);
  }

  @Override
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
    ARGReachedSet reached = new ARGReachedSet(pReached, argCpa);
    CounterexampleInfo cexInfo = performRefinement(reached);

    boolean isSpuriousCEX = cexInfo.isSpurious();

    if (isSpuriousCEX) {
      smgCpa.nextRefinment();
    }

    return isSpuriousCEX;
  }

  private CounterexampleInfo performRefinement(ARGReachedSet pReached) throws CPAException, InterruptedException {

    Collection<ARGState> targets = pathExtractor.getTargetStates(pReached);
    List<ARGPath> targetPaths = pathExtractor.getTargetPaths(targets);

    if (!madeProgress(targetPaths.get(0))) {
      throw new RefinementFailedException(Reason.RepeatedCounterexample,
          targetPaths.get(0));
    }

    return performRefinementForPaths(pReached, targets, targetPaths);
  }

  private boolean madeProgress(ARGPath path) {
    Integer pathId = obtainErrorPathId(path);
    boolean progress = (previousErrorPathIds.isEmpty() || !previousErrorPathIds.contains(pathId));

    previousErrorPathIds.add(pathId);

    return progress;
  }

  private int obtainErrorPathId(ARGPath path) {

    Predicate<? super AutomatonState> automatonStateIsTarget = (AutomatonState state) -> {
      return state.isTarget() ? true : false;
    };

    Function<AutomatonState, String> toNameFunction = (AutomatonState state) -> {
      return state.getOwningAutomatonName();
    };

    Set<String> automatonNames =
        AbstractStates.asIterable(path.getLastState()).filter(AutomatonState.class)
            .filter(automatonStateIsTarget).transform(toNameFunction).toSet();

    int id = path.toString().hashCode() + automatonNames.hashCode();

    return id;
  }

  private CounterexampleInfo performRefinementForPaths(ARGReachedSet pReached,
      Collection<ARGState> pTargets,
      List<ARGPath> pTargetPaths) throws CPAException, InterruptedException {
    logger.log(Level.FINEST, "performing refinement ...");
    refinementCounter.inc();
    numberOfTargets.setNextValue(pTargets.size());

    CounterexampleInfo cex = isAnyPathFeasible(pReached, pTargetPaths);

    if (cex.isSpurious()) {
      SMGInterpolationTree interpolationTree = obtainInterpolants(pTargetPaths, pReached);
      refineUsingInterpolants(pReached, interpolationTree);
    }

    logger.log(Level.FINEST, "refinement finished");
    return cex;
  }

  private CounterexampleInfo isAnyPathFeasible(
      final ARGReachedSet pReached,
      final Collection<ARGPath> pErrorPaths
  ) throws CPAException, InterruptedException {

    ARGPath feasiblePath = null;
    for (ARGPath currentPath : pErrorPaths) {

      if (isErrorPathFeasible(currentPath)) {
        if(feasiblePath == null) {
          feasiblePath = currentPath;
        }

        pathExtractor.addFeasibleTarget(currentPath.getLastState());
      }
    }

    // remove all other target states, so that only one is left (for CEX-checker)
    if (feasiblePath != null) {
      for (ARGPath others : pErrorPaths) {
        if (others != feasiblePath) {
          pReached.removeSubtree(others.getLastState());
        }
      }

      logger.log(Level.FINEST, "found a feasible counterexample");
      // we use the imprecise version of the CounterexampleInfo, due to the possible
      // merges which are done in the used CPAs, but if we can compute a path with assignments,
      // it is probably precise.
      CFAPathWithAssumptions assignments = createModel(feasiblePath);
      if (!assignments.isEmpty()) {
        return CounterexampleInfo.feasiblePrecise(feasiblePath, assignments);
      } else {
        return CounterexampleInfo.feasibleImprecise(feasiblePath);
      }
    }

    return CounterexampleInfo.spurious();
  }

  /**
   * This method creates a model for the given error path.
   *
   * @param errorPath the error path for which to create the model
   * @return the model for the given error path
   * @throws InterruptedException may be thrown in subclass
   * @throws CPAException may be thrown in subclass
   */
  private CFAPathWithAssumptions createModel(ARGPath errorPath)
      throws InterruptedException, CPAException {

    //TODO Fix creating a model.
    return CFAPathWithAssumptions.empty();
  }

  private SMGInterpolationTree obtainInterpolants(List<ARGPath> pTargetPaths, ARGReachedSet pReachedSet)
      throws CPAException, InterruptedException {

    SMGInterpolationTree interpolationTree = createInterpolationTree(pTargetPaths);

    while (interpolationTree.hasNextPathForInterpolation()) {
      performPathInterpolation(interpolationTree, pReachedSet);
    }

    exportTree(interpolationTree, "FINAL");
    return interpolationTree;
  }

  private void performPathInterpolation(SMGInterpolationTree interpolationTree, ARGReachedSet pReachedSet)
      throws CPAException, InterruptedException {
    ARGPath errorPath = interpolationTree.getNextPathForInterpolation();

    if (errorPath == InterpolationTree.EMPTY_PATH) {
      logger.log(Level.FINEST, "skipping interpolation,"
          + " because false interpolant on path to target state");
      return;
    }

    SMGInterpolant initialItp = interpolationTree.getInitialInterpolantForPath(errorPath);

    if (isInitialInterpolantTooWeak(interpolationTree.getRoot(), initialItp, errorPath)) {
      errorPath = ARGUtils.getOnePathTo(errorPath.getLastState());
      initialItp = interpolantManager.createInitialInterpolant();
    }

    logger.log(Level.FINEST, "performing interpolation, starting at ",
        errorPath.getFirstState().getStateId(),
        ", using interpolant ", initialItp);

    interpolationTree.addInterpolants(interpolator.performInterpolation(errorPath, initialItp, pReachedSet));
    exportTree(interpolationTree, "ALWAYS");
  }

  private boolean isInitialInterpolantTooWeak(ARGState root, SMGInterpolant initialItp, ARGPath errorPath)
      throws CPAException, InterruptedException {

    // if the first state of the error path is the root, the interpolant cannot be to weak
    if (errorPath.getFirstState() == root) {
      return false;
    }

    // for all other cases, check if the path is feasible when using the interpolant as initial state
    return checker.isFeasible(errorPath, initialItp.reconstructStates());
  }

  private SMGInterpolationTree createInterpolationTree(List<ARGPath> pTargetPaths) {
    return new SMGInterpolationTree(interpolantManager, logger, pTargetPaths, true);
  }

  private void refineUsingInterpolants(ARGReachedSet pReached, SMGInterpolationTree pInterpolationTree) throws InterruptedException {

    Map<ARGState, List<Precision>> refinementInformation = new HashMap<>();
    Collection<ARGState> refinementRoots = pInterpolationTree.obtainRefinementRoots();

    for (ARGState root : refinementRoots) {
      shutdownNotifier.shutdownIfNecessary();

      if (refinementRoots.size() == 1 && isSimilarRepeatedRefinement(
          pInterpolationTree.extractPrecisionIncrement(root).values())) {
        root = relocateRepeatedRefinementRoot(root);
      }

      List<Precision> precisions = new ArrayList<>(2);
      // merge the value precisions of the subtree, and refine it
      precisions.add(mergeSMGPrecisionsForSubgraph(root, pReached)
          .withIncrement(pInterpolationTree.extractPrecisionIncrement(root)));

      refinementInformation.put(root, precisions);
    }

    for (Entry<ARGState, List<Precision>> info : refinementInformation.entrySet()) {
      shutdownNotifier.shutdownIfNecessary();
      List<Predicate<? super Precision>> precisionTypes = new ArrayList<>(2);

      precisionTypes.add(new Predicate<Precision>() {

        @Override
        public boolean apply(Precision pPrecision) {
          return pPrecision instanceof SMGPrecision;
        }
      });

      pReached.removeSubtree(info.getKey(), info.getValue(), precisionTypes);
    }
  }

  /**
   * A simple heuristic to detect similar repeated refinements.
   */
  private boolean isSimilarRepeatedRefinement(Collection<SMGPrecisionIncrement> currentIncrement) {

    boolean isSimilar = false;
    int currentRefinementId = new TreeSet<>(currentIncrement).hashCode();

    previousRefinementIds.add(currentRefinementId);

    return isSimilar;
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

  /** retrieve the wrapped CPA or throw an exception. */
  private static final <T extends ConfigurableProgramAnalysis> T retrieveCPA(
      ConfigurableProgramAnalysis pCpa, Class<T> retrieveCls)
          throws InvalidConfigurationException {
    final T extractedCPA = CPAs.retrieveCPA(pCpa, retrieveCls);
    if (extractedCPA == null) {
      throw new InvalidConfigurationException(retrieveCls.getSimpleName() + " cannot be retrieved.");
    }
    return extractedCPA;
  }

  private SMGPrecision mergeSMGPrecisionsForSubgraph(
      final ARGState pRefinementRoot,
      final ARGReachedSet pReached
  ) {
    // get all unique precisions from the subtree
    Set<SMGPrecision> uniquePrecisions = Sets.newIdentityHashSet();
    for (ARGState descendant : getNonCoveredStatesInSubgraph(pRefinementRoot)) {
      uniquePrecisions.add(SMGCEGARUtils.extractSMGPrecision(pReached, descendant));
    }

    // join all unique precisions into a single precision
    SMGPrecision mergedPrecision = Iterables.getLast(uniquePrecisions);
    for (SMGPrecision precision : uniquePrecisions) {
      mergedPrecision = mergedPrecision.join(precision);
    }

    return mergedPrecision;
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

  private boolean isErrorPathFeasible(ARGPath pErrorPath)
      throws CPAException, InterruptedException {
    return checker.isFeasible(pErrorPath);
  }

  /** export the interpolation-tree as dot-file, if necessary. */
  private void exportTree(SMGInterpolationTree interpolationTree, String level) {
    if (interpolationTreeExportFile != null && exportInterpolationTree.equals(level)) {
      interpolationTree.exportToDot(interpolationTreeExportFile, refinementCounter.getValue());
    }
  }
}