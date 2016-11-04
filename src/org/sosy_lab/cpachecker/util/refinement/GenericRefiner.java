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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.ForOverride;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

/**
 * A generic refiner using a {@link VariableTrackingPrecision}.
 *
 * @param <S> the type of the state the {@link StrongestPostOperator} and
 *    {@link Interpolant Interpolants} are based on
 * @param <I> the type of the interpolants used in refinement
 *
 * @see GenericFeasibilityChecker
 * @see GenericPathInterpolator
 */
@Options(prefix = "cpa.value.refinement")
public abstract class GenericRefiner<S extends ForgetfulState<?>, I extends Interpolant<S>>
    implements Refiner, StatisticsProvider {

  @Option(secure = true, description = "when to export the interpolation tree"
      + "\nNEVER:   never export the interpolation tree"
      + "\nFINAL:   export the interpolation tree once after each refinement"
      + "\nALWAYS:  export the interpolation tree once after each interpolation, i.e. multiple times per refinement",
      values = { "NEVER", "FINAL", "ALWAYS" })
  private String exportInterpolationTree = "NEVER";

  @Option(secure = true, description = "export interpolation trees to this file template")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate interpolationTreeExportFile = PathTemplate.ofFormatString("interpolationTree.%d-%d.dot");

  /* This option is broken with BAM, because BAM expects the refinementRoot belongs to origin path
   * (see ARGSubtreeRemover.removeSubtree)
   * If we here find a further path, refine it, try to remove a different subtree and fail*/
  @Option(secure = true, description="instead of reporting a repeated counter-example, "
      + "search and refine another error-path for the same target-state.")
  private boolean searchForFurtherErrorPaths = false;

  /* This option is useful for BAM configuration with Predicate analysis
   */
  @Option(secure = true, description="store all refined paths")
  private boolean storeAllRefinedPaths = false;

  @Option(secure = true, description="whether or not to add assumptions to counterexamples,"
      + " e.g., for supporting counterexample checks")
  private boolean addAssumptionsToCex = true;

  protected final LogManager logger;

  private final ARGCPA argCpa;

  private final PathInterpolator<I> interpolator;

  private final FeasibilityChecker<S> checker;

  private final InterpolantManager<S, I> interpolantManager;

  private final PathExtractor pathExtractor;

  private Set<Integer> previousErrorPathIds = Sets.newHashSet();

  // statistics
  private final StatCounter refinementCounter = new StatCounter("Number of refinements");
  private final StatInt numberOfTargets = new StatInt(StatKind.SUM, "Number of targets found");
  private final StatTimer refinementTime = new StatTimer("Time for completing refinement");

  public GenericRefiner(
      final ARGCPA pArgCpa,
      final FeasibilityChecker<S> pFeasibilityChecker,
      final PathInterpolator<I> pPathInterpolator,
      final InterpolantManager<S, I> pInterpolantManager,
      final PathExtractor pPathExtractor,
      final Configuration pConfig,
      final LogManager pLogger
  ) throws InvalidConfigurationException {

    pConfig.inject(this, GenericRefiner.class);

    logger = pLogger;
    argCpa = pArgCpa;
    interpolator = pPathInterpolator;
    interpolantManager = pInterpolantManager;
    checker = pFeasibilityChecker;
    pathExtractor = pPathExtractor;
  }

  /** retrieve the wrapped CPA or throw an exception. */
  protected static final <T extends ConfigurableProgramAnalysis> T retrieveCPA(
      ConfigurableProgramAnalysis pCpa, Class<T> retrieveCls)
          throws InvalidConfigurationException {
    final T extractedCPA = CPAs.retrieveCPA(pCpa, retrieveCls);
    if (extractedCPA == null) {
      throw new InvalidConfigurationException(retrieveCls.getSimpleName() + " cannot be retrieved.");
    }
    return extractedCPA;
  }

  private boolean madeProgress(ARGPath path) {
    boolean progress = (previousErrorPathIds.isEmpty() || !previousErrorPathIds.contains(obtainErrorPathId(path)));

    if (!storeAllRefinedPaths) {
      previousErrorPathIds.clear();
    }
    previousErrorPathIds.add(obtainErrorPathId(path));

    return progress;
  }

  @Override
  public boolean performRefinement(final ReachedSet pReached)
      throws CPAException, InterruptedException {
    return performRefinement(new ARGReachedSet(pReached, argCpa)).isSpurious();
  }

  private CounterexampleInfo performRefinement(final ARGReachedSet pReached)
      throws CPAException, InterruptedException {

    Collection<ARGState> targets = pathExtractor.getTargetStates(pReached);
    List<ARGPath> targetPaths = pathExtractor.getTargetPaths(targets);

    if (!madeProgress(targetPaths.get(0))) {
      throw new RefinementFailedException(Reason.RepeatedCounterexample,
          targetPaths.get(0));
    }

    return performRefinementForPaths(pReached, targets, targetPaths);
  }

  /**
   * Provide an adaptor from {@link GenericRefiner} (which implements {@link Refiner})
   * and provides global refinements) to {@link ARGBasedRefiner},
   * which provides path-specific refinements.
   */
  public ARGBasedRefiner asARGBasedRefiner() {
    class GenericRefinerToARGBasedRefinerAdaptor implements ARGBasedRefiner, StatisticsProvider {
      @Override
      public CounterexampleInfo performRefinementForPath(ARGReachedSet pReached, ARGPath pPath)
          throws CPAException, InterruptedException {
        return GenericRefiner.this.performRefinementForPath(pReached, pPath);
      }

      @Override
      public void collectStatistics(Collection<Statistics> pStatsCollection) {
        GenericRefiner.this.collectStatistics(pStatsCollection);
      }

      @Override
      public String toString() {
        return GenericRefiner.this.toString();
      }
    }
    return new GenericRefinerToARGBasedRefinerAdaptor();
  }

  private final CounterexampleInfo performRefinementForPath(
      final ARGReachedSet pReached, ARGPath targetPathToUse)
      throws CPAException, InterruptedException {
    Collection<ARGState> targets = Collections.singleton(targetPathToUse.getLastState());

    boolean repeatingCEX = !madeProgress(targetPathToUse);

    // if the target path is given from outside, do not fail hard on a repeated counterexample:
    // this can happen when the predicate-analysis refinement returns back-to-back target paths
    // that are feasible under predicate-analysis semantics and hands those into the value-analysis
    // refiner, where the in-between value-analysis refinement happens to only affect paths in a
    // (ABE) block, which may not be visible when constructing the target path in the next refinement.
    // Possible problem: alternating error-paths
    if (repeatingCEX && searchForFurtherErrorPaths) {
      for (ARGPath targetPath : pathExtractor.getTargetPaths(targets)) {
        if (madeProgress(targetPath)) {
          logger.log(Level.INFO, "The error path given to", getClass().getSimpleName(), "is a repeated counterexample,",
              "so instead, refiner uses a new error path extracted from the reachset.");
          targetPathToUse = targetPath;
          repeatingCEX = false;
          break;
        }
      }
    }

    if (repeatingCEX) {
      throw new RefinementFailedException(Reason.RepeatedCounterexample, targetPathToUse);
    }

    return performRefinementForPaths(pReached, targets, ImmutableList.of(targetPathToUse));
  }

  private CounterexampleInfo performRefinementForPaths(
      final ARGReachedSet pReached,
      final Collection<ARGState> pTargets,
      final List<ARGPath> pTargetPaths
  ) throws CPAException, InterruptedException {
    logger.log(Level.FINEST, "performing refinement ...");
    refinementTime.start();
    refinementCounter.inc();
    numberOfTargets.setNextValue(pTargets.size());

    CounterexampleInfo cex = isAnyPathFeasible(pReached, pTargetPaths);

    if (cex.isSpurious()) {
      refineUsingInterpolants(pReached, obtainInterpolants(pTargetPaths));
    }

    refinementTime.stop();
    logger.log(Level.FINEST, "refinement finished");
    return cex;
  }

  protected abstract void refineUsingInterpolants(
      final ARGReachedSet pReached,
      final InterpolationTree<S, I> pInterpolationTree
      ) throws InterruptedException;

  private InterpolationTree<S, I> obtainInterpolants(List<ARGPath> pTargetPaths)
      throws CPAException, InterruptedException {

    InterpolationTree<S, I> interpolationTree = createInterpolationTree(pTargetPaths);

    while (interpolationTree.hasNextPathForInterpolation()) {
      performPathInterpolation(interpolationTree);
    }

    exportTree(interpolationTree, "FINAL");
    return interpolationTree;
  }

  /**
   * This method creates the interpolation tree.
   */
  @ForOverride
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
    exportTree(interpolationTree, "ALWAYS");
  }

  private boolean isInitialInterpolantTooWeak(ARGState root, Interpolant<S> initialItp, ARGPath errorPath)
      throws CPAException, InterruptedException {

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
          madeProgress(currentPath);
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
      // merges which are done in the used CPAs, but if we (can) compute a path with assignments,
      // it is probably precise.
      CFAPathWithAssumptions assignments = addAssumptionsToCex
          ? createModel(feasiblePath)
          : CFAPathWithAssumptions.empty();
      if (!assignments.isEmpty()) {
        return CounterexampleInfo.feasiblePrecise(feasiblePath, assignments);
      } else {
        return CounterexampleInfo.feasibleImprecise(feasiblePath);
      }
    }

    return CounterexampleInfo.spurious();
  }

  @ForOverride
  protected boolean isErrorPathFeasible(final ARGPath errorPath)
      throws CPAException, InterruptedException {
    return checker.isFeasible(errorPath);
  }

  /**
   * This method creates a model for the given error path.
   *
   * @param errorPath the error path for which to create the model
   * @return the model for the given error path
   * @throws InterruptedException may be thrown in subclass
   * @throws CPAException may be thrown in subclass
   */
  protected CFAPathWithAssumptions createModel(ARGPath errorPath) throws InterruptedException, CPAException {
    return CFAPathWithAssumptions.empty();
  }

  @Override
  public void collectStatistics(final Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Statistics() {

      @Override
      public String getName() {
        return GenericRefiner.this.getClass().getSimpleName();
      }

      @Override
      public void printStatistics(final PrintStream pOut, final Result pResult, final UnmodifiableReachedSet pReached) {
        GenericRefiner.this.printStatistics(pOut, pResult, pReached);
      }
    });
  }

  private void printStatistics(final PrintStream pOut, final Result pResult, final UnmodifiableReachedSet pReached) {
    StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(pOut);
    writer.put(refinementCounter)
        .put(numberOfTargets)
        .put(refinementTime);

    pathExtractor.printStatistics(pOut, pResult, pReached);
    interpolator.printStatistics(pOut, pResult, pReached);
    printAdditionalStatistics(pOut, pResult, pReached); //hook
  }

  protected abstract void printAdditionalStatistics(final PrintStream out, final Result pResult, final UnmodifiableReachedSet pReached);

  private int obtainErrorPathId(ARGPath path) {
    return path.toString().hashCode();
  }

  /** export the interpolation-tree as dot-file, if necessary. */
  private void exportTree(InterpolationTree<S, I> interpolationTree, String level) {
    if (interpolationTreeExportFile != null && exportInterpolationTree.equals(level)) {
      interpolationTree.exportToDot(interpolationTreeExportFile, refinementCounter.getValue());
    }
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


