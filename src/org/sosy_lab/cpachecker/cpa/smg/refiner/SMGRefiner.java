// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.refiner;

import static org.sosy_lab.cpachecker.util.Precisions.extractPrecisionByType;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
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
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.smg.SMGCPA;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGPredicateManager;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelationKind;
import org.sosy_lab.cpachecker.cpa.smg.UnmodifiableSMGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.refinement.InterpolationTree;
import org.sosy_lab.cpachecker.util.refinement.PathExtractor;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;

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

  @Option(secure = true, description = "export interpolation trees to this file template")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate interpolationTreeExportFile =
      PathTemplate.ofFormatString("interpolationTree.%d-%d.dot");

  @Option(
      secure = true,
      description = "export interpolant smgs for every path interpolation to this path template")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate exportInterpolantSMGs =
      PathTemplate.ofFormatString("smg/interpolation-%d/%s");

  @Option(
      secure = true,
      description = "export interpolant smgs for every path interpolation to this path template")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate exportRefinementSMGs =
      PathTemplate.ofFormatString("smg/refinement-%d/smg-%s");

  @Option(
      secure = true,
      description =
          "when to export the interpolation tree\n"
              + "NEVER:   never export the interpolation tree\n"
              + "FINAL:   export the interpolation tree once after each refinement\n"
              + "ALWAYS:  export the interpolation tree once after each interpolation, i.e."
              + " multiple times per refinement",
      values = {"NEVER", "FINAL", "ALWAYS"})
  private String exportInterpolationTree = "NEVER";

  private SMGRefiner(SMGCPA pSmgCpa, ARGCPA pArgCpa, Set<ControlAutomatonCPA> automatonCpas)
      throws InvalidConfigurationException, SMGInconsistentException {
    pSmgCpa.getConfiguration().inject(this);

    argCpa = pArgCpa;
    smgCpa = pSmgCpa;
    logger = pSmgCpa.getLogger();
    CFA cfa = pSmgCpa.getCFA();
    shutdownNotifier = pSmgCpa.getShutdownNotifier();

    smgCpa.enableRefinement(exportRefinementSMGs);

    pathExtractor = new PathExtractor(logger, pSmgCpa.getConfiguration());
    SMGPredicateManager predicateManager = smgCpa.getPredicateManager();

    SMGStrongestPostOperator strongestPostOpForCEX =
        new SMGStrongestPostOperator(
            logger,
            cfa,
            predicateManager,
            smgCpa.getOptions(),
            SMGTransferRelationKind.STATIC,
            shutdownNotifier);

    UnmodifiableSMGState initialState =
        smgCpa.getInitialState(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());

    checker =
        new SMGFeasibilityChecker(strongestPostOpForCEX, logger, cfa, initialState, automatonCpas);

    interpolantManager = new SMGInterpolantManager(logger, cfa, smgCpa.getOptions());

    SMGStrongestPostOperator strongestPostOpForInterpolation =
        new SMGStrongestPostOperator(
            logger,
            cfa,
            predicateManager,
            smgCpa.getOptions(),
            SMGTransferRelationKind.REFINEMENT,
            shutdownNotifier);

    SMGFeasibilityChecker checkerForInterpolation =
        new SMGFeasibilityChecker(
            strongestPostOpForInterpolation, logger, cfa, initialState, automatonCpas);

    SMGEdgeInterpolator edgeInterpolator =
        new SMGEdgeInterpolator(
            checkerForInterpolation,
            strongestPostOpForInterpolation,
            interpolantManager,
            shutdownNotifier,
            logger,
            smgCpa.getBlockOperator());

    interpolator =
        new SMGPathInterpolator(
            shutdownNotifier,
            interpolantManager,
            edgeInterpolator,
            logger,
            exportInterpolantSMGs,
            smgCpa.getOptions().getExportSMGLevel(),
            checkerForInterpolation);
  }

  public static SMGRefiner create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException, SMGInconsistentException {

    ARGCPA argCpa = CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, SMGRefiner.class);
    SMGCPA smgCpa = CPAs.retrieveCPAOrFail(pCpa, SMGCPA.class, SMGRefiner.class);
    Set<ControlAutomatonCPA> automatonCpas =
        CPAs.asIterable(pCpa).filter(ControlAutomatonCPA.class).toSet();

    return new SMGRefiner(smgCpa, argCpa, automatonCpas);
  }

  @Override
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
    ARGReachedSet reached = new ARGReachedSet(pReached, argCpa);
    Collection<ARGState> targets = pathExtractor.getTargetStates(reached);
    List<ARGPath> targetPaths = pathExtractor.getTargetPaths(targets);
    CounterexampleInfo cexInfo = performRefinementForPaths(reached, targets, targetPaths);

    boolean isSpuriousCEX = cexInfo.isSpurious();
    if (isSpuriousCEX) {
      // just increment counters for exporting SMGs
      smgCpa.nextRefinement();
    }
    return isSpuriousCEX;
  }

  private CounterexampleInfo performRefinementForPaths(
      ARGReachedSet pReached, Collection<ARGState> pTargets, List<ARGPath> pTargetPaths)
      throws CPAException, InterruptedException {
    Preconditions.checkState(pTargets.size() == pTargetPaths.size());
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

  private List<ARGPath> getFeasibleErrorPaths(Collection<ARGPath> pErrorPaths)
      throws CPAException, InterruptedException {
    List<ARGPath> lst = new ArrayList<>();
    for (ARGPath currentPath : pErrorPaths) {
      if (checker.isFeasible(currentPath)) {
        lst.add(currentPath);
      }
    }
    return lst;
  }

  private CounterexampleInfo isAnyPathFeasible(
      final ARGReachedSet pReached, final Collection<ARGPath> pErrorPaths)
      throws CPAException, InterruptedException {

    // get all feasible error paths
    List<ARGPath> feasibleErrorPaths = getFeasibleErrorPaths(pErrorPaths);
    for (ARGPath feasibleErrorPath : feasibleErrorPaths) {
      pathExtractor.addFeasibleTarget(feasibleErrorPath.getLastState());
    }

    // choose one path and remove all other target states,
    // so that only one is left (for CEX-checker)
    ARGPath feasiblePath = Iterables.getFirst(feasibleErrorPaths, null);
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
      CFAPathWithAssumptions assignments = createModel();
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
   * @return the model for the given error path
   */
  private CFAPathWithAssumptions createModel() {

    // TODO Fix creating a model.
    return CFAPathWithAssumptions.empty();
  }

  private SMGInterpolationTree obtainInterpolants(
      List<ARGPath> pTargetPaths, ARGReachedSet pReachedSet)
      throws CPAException, InterruptedException {

    SMGInterpolationTree interpolationTree = createInterpolationTree(pTargetPaths);

    while (interpolationTree.hasNextPathForInterpolation()) {
      performPathInterpolation(interpolationTree, pReachedSet);
    }

    exportTree(interpolationTree, "FINAL");
    return interpolationTree;
  }

  private void performPathInterpolation(
      SMGInterpolationTree interpolationTree, ARGReachedSet pReachedSet)
      throws CPAException, InterruptedException {
    ARGPath errorPath = interpolationTree.getNextPathForInterpolation();

    if (errorPath == InterpolationTree.EMPTY_PATH) {
      logger.log(
          Level.FINEST,
          "skipping interpolation," + " because false interpolant on path to target state");
      return;
    }

    SMGInterpolant initialItp = interpolationTree.getInitialInterpolantForPath(errorPath);

    if (isInitialInterpolantTooWeak(interpolationTree.getRoot(), initialItp, errorPath)) {
      errorPath = ARGUtils.getOnePathTo(errorPath.getLastState());
      initialItp = interpolantManager.createInitialInterpolant();
    }

    logger.log(
        Level.FINEST,
        "performing interpolation, starting at ",
        errorPath.getFirstState().getStateId(),
        ", using interpolant ",
        initialItp);

    interpolationTree.addInterpolants(
        interpolator.performInterpolation(errorPath, initialItp, pReachedSet));
    exportTree(interpolationTree, "ALWAYS");
  }

  private boolean isInitialInterpolantTooWeak(
      ARGState root, SMGInterpolant initialItp, ARGPath errorPath)
      throws CPAException, InterruptedException {

    // if the first state of the error path is the root, the interpolant cannot be to weak
    if (Objects.equals(errorPath.getFirstState(), root)) {
      return false;
    }

    // for all other cases, check if the path is feasible when using the interpolant
    // (or any of its reconstructed states) as initial state
    for (UnmodifiableSMGState start : initialItp.reconstructState()) {
      if (checker.isFeasible(errorPath, start)) {
        return true;
      }
    }
    return false;
  }

  private SMGInterpolationTree createInterpolationTree(List<ARGPath> pTargetPaths) {
    return new SMGInterpolationTree(interpolantManager, logger, pTargetPaths, true);
  }

  private void refineUsingInterpolants(
      ARGReachedSet pReached, SMGInterpolationTree pInterpolationTree) throws InterruptedException {

    Map<ARGState, List<Precision>> refinementInformation = new HashMap<>();
    Collection<ARGState> refinementRoots = pInterpolationTree.obtainRefinementRoots();

    for (ARGState root : refinementRoots) {
      shutdownNotifier.shutdownIfNecessary();
      List<Precision> precisions = new ArrayList<>(2);
      // merge the value precisions of the subtree, and refine it
      precisions.add(
          mergeSMGPrecisionsForSubgraph(root, pReached)
              .withIncrement(pInterpolationTree.extractPrecisionIncrement(root)));

      refinementInformation.put(root, precisions);
    }

    for (Entry<ARGState, List<Precision>> info : refinementInformation.entrySet()) {
      shutdownNotifier.shutdownIfNecessary();
      List<Predicate<? super Precision>> precisionTypes =
          Lists.newArrayList(Predicates.instanceOf(SMGPrecision.class));
      pReached.removeSubtree(info.getKey(), info.getValue(), precisionTypes);
    }
  }

  private SMGPrecision mergeSMGPrecisionsForSubgraph(
      final ARGState pRefinementRoot, final ARGReachedSet pReached) {
    // get all unique precisions from the subtree
    Set<SMGPrecision> uniquePrecisions = Sets.newIdentityHashSet();
    for (ARGState descendant : ARGUtils.getNonCoveredStatesInSubgraph(pRefinementRoot)) {
      uniquePrecisions.add(
          extractPrecisionByType(
              pReached.asReachedSet().getPrecision(descendant), SMGPrecision.class));
    }

    // join all unique precisions into a single precision
    SMGPrecision mergedPrecision = Iterables.getLast(uniquePrecisions);
    for (SMGPrecision precision : uniquePrecisions) {
      mergedPrecision = mergedPrecision.join(precision);
    }

    return mergedPrecision;
  }

  /** export the interpolation-tree as dot-file, if necessary. */
  private void exportTree(SMGInterpolationTree interpolationTree, String level) {
    if (interpolationTreeExportFile != null && exportInterpolationTree.equals(level)) {
      interpolationTree.exportToDot(interpolationTreeExportFile, refinementCounter.getValue());
    }
  }
}
