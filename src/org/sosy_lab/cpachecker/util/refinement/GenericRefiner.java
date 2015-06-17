/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.util.refinement;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
import org.sosy_lab.cpachecker.core.counterexample.RichModel;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;

import com.google.common.collect.Lists;

/**
 * A generic refiner using a {@link VariableTrackingPrecision}.
 *
 * @param <S> the type of the state the {@link StrongestPostOperator} and
 *    {@link Interpolant Interpolants} are based on
 * @param <T> the type <code>S</code> uses for returning forgotten information
 * @param <I> the type of the interpolants used in refinement
 *
 * @see GenericFeasibilityChecker
 * @see GenericPathInterpolator
 */
@Options(prefix = "cpa.value.refinement")
public abstract class GenericRefiner<S extends ForgetfulState<T>, T, I extends Interpolant<S>>
    implements Refiner, StatisticsProvider {

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

  private final PathInterpolator<I> interpolator;

  private final FeasibilityChecker<S> checker;

  private final InterpolantManager<S, I> interpolantManager;

  private final PathExtractor pathExtractor;

  private int previousErrorPathId = -1;

  // statistics
  private int refinementCounter = 0;
  private int targetCounter = 0;
  private final Timer totalTime = new Timer();

  public GenericRefiner(
      final FeasibilityChecker<S> pFeasibilityChecker,
      final PathInterpolator<I> pPathInterpolator,
      final InterpolantManager<S, I> pInterpolantManager,
      final PathExtractor pPathExtractor,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa
  ) throws InvalidConfigurationException {

    pConfig.inject(this);

    logger = pLogger;
    interpolator = pPathInterpolator;
    interpolantManager = pInterpolantManager;
    checker = pFeasibilityChecker;
    pathExtractor = pPathExtractor;
  }

  private boolean madeProgress(ARGPath path) {
    boolean progress = (previousErrorPathId == -1 || previousErrorPathId != obtainErrorPathId(path));

    previousErrorPathId = obtainErrorPathId(path);

    return progress;
  }

  @Override
  public boolean performRefinement(final ReachedSet pReached)
      throws CPAException, InterruptedException {
    return performRefinement(new ARGReachedSet(pReached)).isSpurious();
  }

  public CounterexampleInfo performRefinement(final ARGReachedSet pReached)
      throws CPAException, InterruptedException {

    Collection<ARGState> targets = pathExtractor.getTargetStates(pReached);
    List<ARGPath> targetPaths = pathExtractor.getTargetPaths(targets);

    if (!madeProgress(targetPaths.get(0))) {
      throw new RefinementFailedException(Reason.RepeatedCounterexample,
          targetPaths.get(0));
    }

    return performRefinement(pReached, targets, targetPaths);
  }

  public CounterexampleInfo performRefinement(
      final ARGReachedSet pReached,
      final ARGPath pTargetPath
  ) throws CPAException, InterruptedException {
    Collection<ARGState> targets = Collections.singleton(pTargetPath.getLastState());
    ARGPath targetPathToUse = pTargetPath;

    // if the target path is given from outside, do not fail hard on a repeated counterexample:
    // this can happen when the predicate-analysis refinement returns back-to-back target paths
    // that are feasible under predicate-analysis semantics and hands those into the value-analysis
    // refiner, where the in-between value-analysis refinement happens to only affect paths in a
    // (ABE) block, which may not be visible when constructing the target path in the next refinement.
    if (!madeProgress(pTargetPath)) {
      logger.log(Level.INFO, "The error path given to", getClass().getSimpleName(), "is a repeated counterexample,",
          "so instead, refiner uses an error path extracted from the reachset.");
      targetPathToUse = pathExtractor.getTargetPaths(targets).get(0);
    }

    return performRefinement(pReached, targets, Lists.newArrayList(targetPathToUse));
  }

  private CounterexampleInfo performRefinement(
      final ARGReachedSet pReached,
      final Collection<ARGState> pTargets,
      final List<ARGPath> pTargetPaths
  ) throws CPAException, InterruptedException {
    logger.log(Level.FINEST, "performing refinement ...");
    totalTime.start();
    refinementCounter++;
    targetCounter = targetCounter + pTargets.size();

    CounterexampleInfo cex = isAnyPathFeasible(pReached, pTargetPaths);

    if (cex.isSpurious()) {
      refineUsingInterpolants(pReached, obtainInterpolants(pTargetPaths));
    }

    totalTime.stop();

    return cex;
  }

  protected abstract void refineUsingInterpolants(
      final ARGReachedSet pReached,
      final InterpolationTree<S, I> pInterpolationTree
  );

  protected InterpolationTree<S, I> obtainInterpolants(List<ARGPath> pTargetPaths)
      throws CPAException, InterruptedException {

    InterpolationTree<S, I> interpolationTree = createInterpolationTree(pTargetPaths);

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
  protected InterpolationTree<S, I> createInterpolationTree(List<ARGPath> targets) {
    return new InterpolationTree<>(interpolantManager, logger, targets, true);
  }

  private void performPathInterpolation(InterpolationTree<S, I> interpolationTree) throws CPAException,
      InterruptedException {
    ARGPath errorPath = interpolationTree.getNextPathForInterpolation();

    if (errorPath == InterpolationTree.EMPTY_PATH) {
      logger.log(Level.FINEST, "skipping interpolation,"
          + " because false interpolant on path to target state");
      return;
    }

    I initialItp = interpolationTree.getInitialInterpolantForPath(errorPath);

    if (isInitialInterpolantTooWeak(interpolationTree.getRoot(), initialItp, errorPath)) {
      errorPath = ARGUtils.getOnePathTo(errorPath.getLastState());
      initialItp = interpolantManager.createInitialInterpolant();
    }

    logger.log(Level.FINEST, "performing interpolation, starting at ", errorPath.getFirstState().getStateId(),
        ", using interpolant ", initialItp);

    interpolationTree.addInterpolants(interpolator.performInterpolation(errorPath, initialItp));

    if (interpolationTreeExportFile != null && exportInterpolationTree.equals("ALWAYS")) {
      interpolationTree.exportToDot(interpolationTreeExportFile, refinementCounter);
    }
  }

  private boolean isInitialInterpolantTooWeak(ARGState root, Interpolant<S> initialItp, ARGPath errorPath)
      throws CPAException {

    // if the first state of the error path is the root, the interpolant cannot be to weak
    if (errorPath.getFirstState() == root) {
      return false;
    }

    // for all other cases, check if the path is feasible when using the interpolant as initial state
    return checker.isFeasible(errorPath, initialItp.reconstructState());
  }

  private CounterexampleInfo isAnyPathFeasible(
      final ARGReachedSet pReached,
      final Collection<ARGPath> pErrorPaths
  ) throws CPAException, InterruptedException {

    ARGPath feasiblePath = null;
    for (ARGPath currentPath : pErrorPaths) {

      if (isErrorPathFeasible(currentPath)) {
        if(feasiblePath == null) {
          previousErrorPathId = obtainErrorPathId(currentPath);
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
      return CounterexampleInfo.feasible(feasiblePath, createModel(feasiblePath));
    }

    return CounterexampleInfo.spurious();
  }

  public boolean isErrorPathFeasible(final ARGPath errorPath)
      throws CPAException {
    return checker.isFeasible(errorPath);
  }

  /**
   * This method creates a model for the given error path.
   *
   * @param errorPath the error path for which to create the model
   * @return the model for the given error path
   * @throws InterruptedException
   * @throws CPAException
   */
  protected RichModel createModel(ARGPath errorPath) throws InterruptedException, CPAException {
    return RichModel.empty();
  }

  @Override
  public void collectStatistics(final Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Statistics() {

      @Override
      public String getName() {
        return GenericRefiner.this.getClass().getSimpleName();
      }

      @Override
      public void printStatistics(final PrintStream pOut, final Result pResult, final ReachedSet pReached) {
        GenericRefiner.this.printStatistics(pOut, pResult, pReached);
      }
    });
  }

  private void printStatistics(final PrintStream out, final Result pResult, final ReachedSet pReached) {
    out.println("Total number of refinements:      " + String.format(Locale.US, "%9d", refinementCounter));
    pathExtractor.printStatistics(out, pResult, pReached);
    out.println("Time for completing refinement:       " + totalTime);
    interpolator.printStatistics(out, pResult, pReached);
    printAdditionalStatistics(out, pResult, pReached); //hook
  }

  protected abstract void printAdditionalStatistics(final PrintStream out, final Result pResult, final ReachedSet pReached);

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

  /**
   * This method resets the current error path id, which is needed when using another refiner,
   * such as a refiner from the predicate domain, in parallel to this refiner.
   */
  public final void resetPreviousErrorPathId() {
    previousErrorPathId = -1;
  }
}

