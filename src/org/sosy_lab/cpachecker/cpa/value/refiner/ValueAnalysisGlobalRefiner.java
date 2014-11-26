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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.MutableARGPath;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Precisions;

import com.google.common.collect.Iterables;

@Options(prefix="cpa.value.refinement")
public class ValueAnalysisGlobalRefiner implements Refiner, StatisticsProvider {

  @Option(secure=true, description="whether or not to do lazy-abstraction", name="restart", toUppercase = true)
  private RestartStrategy restartStrategy = RestartStrategy.TOP;

  @Option(secure=true, description="whether to use the top-down interpolation strategy or the bottom-up interpolation strategy")
  private boolean useTopDownInterpolationStrategy = true;

  @Option(secure=true, description="when to export the interpolation tree"
      + "\nNEVER:   never export the interpolation tree"
      + "\nFINAL:   export the interpolation tree once after each refinement"
      + "\nALWAYD:  export the interpolation tree once after each interpolation, i.e. multiple times per refinmenet",
      values={"NEVER", "FINAL", "ALWAYS"})
  private String exportInterpolationTree = "NEVER";

  @Option(secure=true, description="export interpolation trees to this file template")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate interpolationTreeExportFile = PathTemplate.ofFormatString("interpolationTree.%d-%d.dot");

  ValueAnalysisPathInterpolator pathInterpolator;
  ValueAnalysisFeasibilityChecker checker;

  private final LogManager logger;

  // statistics
  private int totalRefinements  = 0;
  private int totalTargetsFound = 0;
  private final Timer totalTime = new Timer();

  public static ValueAnalysisGlobalRefiner create(final ConfigurableProgramAnalysis pCpa) throws InvalidConfigurationException {
    final ValueAnalysisCPA valueAnalysisCpa = CPAs.retrieveCPA(pCpa, ValueAnalysisCPA.class);
    if (valueAnalysisCpa == null) {
      throw new InvalidConfigurationException(ValueAnalysisGlobalRefiner.class.getSimpleName() + " needs a ValueAnalysisCPA");
    }

    valueAnalysisCpa.injectRefinablePrecision();

    ValueAnalysisGlobalRefiner refiner = new ValueAnalysisGlobalRefiner(valueAnalysisCpa.getConfiguration(),
                                    valueAnalysisCpa.getLogger(),
                                    valueAnalysisCpa.getShutdownNotifier(),
                                    valueAnalysisCpa.getCFA());

    valueAnalysisCpa.getStats().addRefiner(refiner);


    return refiner;
  }

  private ValueAnalysisGlobalRefiner(final Configuration pConfig, final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier, final CFA pCfa)
          throws InvalidConfigurationException {

    pConfig.inject(this);

    logger = pLogger;
    pathInterpolator = new ValueAnalysisPathInterpolator(pConfig, pLogger, pShutdownNotifier, pCfa);
    checker = new ValueAnalysisFeasibilityChecker(pLogger, pCfa, pConfig);
  }

  @Override
  public boolean performRefinement(final ReachedSet pReached) throws CPAException, InterruptedException {
    return performRefinement(new ARGReachedSet(pReached));
  }

  public boolean performRefinement(final ARGReachedSet pReached) throws CPAException, InterruptedException {
    logger.log(Level.FINEST, "performing global refinement ...");
    totalTime.start();
    totalRefinements++;

    Collection<ARGState> targets = getTargetStates(pReached);

    // stop once any feasible counterexample is found
    if (isAnyPathFeasible(pReached, getTargetPaths(targets))) {
      totalTime.stop();
      return false;
    }

    ValueAnalysisInterpolationTree interpolationTree = new ValueAnalysisInterpolationTree(logger, targets, useTopDownInterpolationStrategy);

    int i = 0;
    while (interpolationTree.hasNextPathForInterpolation()) {
      populateInterpolationTree(interpolationTree, ++i);
    }

    if (interpolationTreeExportFile != null && exportInterpolationTree.equals("FINAL") && !exportInterpolationTree.equals("ALWAYS")) {
      interpolationTree.exportToDot(interpolationTreeExportFile.getPath(totalRefinements, i));
    }

    Map<ARGState, VariableTrackingPrecision> refinementInformation = new HashMap<>();
    for (ARGState root : interpolationTree.obtainRefinementRoots(restartStrategy)) {
      Collection<ARGState> targetsReachableFromRoot = interpolationTree.getTargetsInSubtree(root);

      // join the precisions of the subtree of this roots into a single precision
      final VariableTrackingPrecision subTreePrecision = joinSubtreePrecisions(pReached, targetsReachableFromRoot);

      refinementInformation.put(root, subTreePrecision.withIncrement(interpolationTree.extractPrecisionIncrement(root)));
    }

    for (Map.Entry<ARGState, VariableTrackingPrecision> info : refinementInformation.entrySet()) {
      pReached.removeSubtree(info.getKey(), info.getValue(), VariableTrackingPrecision.isMatchingCPAClass(ValueAnalysisCPA.class));
    }

    totalTime.stop();
    return true;
  }

  private void populateInterpolationTree(ValueAnalysisInterpolationTree interpolationTree, int iterationCount) throws CPAException, InterruptedException {
    MutableARGPath errorPath = interpolationTree.getNextPathForInterpolation();

    if (errorPath.isEmpty()) {
      logger.log(Level.FINEST, "skipping interpolation, error path is empty, because initial interpolant is already false");
      return;
    }

    ValueAnalysisInterpolant initialItp = interpolationTree.getInitialInterpolantForPath(errorPath);

    if (isInitialInterpolantTooWeak(interpolationTree.getRoot(), initialItp, errorPath)) {
      errorPath   = ARGUtils.getOneMutablePathTo(errorPath.getLast().getFirst());
      initialItp  = ValueAnalysisInterpolant.createInitial();
    }

    logger.log(Level.FINEST, "performing interpolation, starting at ", errorPath.getFirst().getFirst().getStateId(), ", using interpolant ", initialItp);

    interpolationTree.addInterpolants(pathInterpolator.performInterpolation(errorPath, initialItp));

    if (interpolationTreeExportFile != null && exportInterpolationTree.equals("ALWAYS")) {
      interpolationTree.exportToDot(interpolationTreeExportFile.getPath(totalRefinements, iterationCount));
    }

    logger.log(Level.FINEST, "finished interpolation #", iterationCount);
  }

  private boolean isInitialInterpolantTooWeak(ARGState root, ValueAnalysisInterpolant initialItp, MutableARGPath errorPath)
      throws CPAException, InterruptedException {

    // if the first state of the error path is the root, the interpolant cannot be to weak
    if (errorPath.getFirst().getFirst() == root) {
      return false;
    }

    // for all other cases, check if the path is feasible when using the interpolant as initial state
    return checker.isFeasible(errorPath, initialItp.createValueAnalysisState());
  }

  private VariableTrackingPrecision joinSubtreePrecisions(final ARGReachedSet pReached,
      Collection<ARGState> targetsReachableFromRoot) {

    VariableTrackingPrecision precision = extractPrecision(pReached, Iterables.getLast(targetsReachableFromRoot));
    // join precisions of all target states
    for (ARGState target : targetsReachableFromRoot) {
      VariableTrackingPrecision precisionOfTarget = extractPrecision(pReached, target);
      precision = precision.join(precisionOfTarget);
    }

    return precision;
  }

  private VariableTrackingPrecision extractPrecision(final ARGReachedSet pReached,
      ARGState state) {
    return (VariableTrackingPrecision) Precisions.asIterable(pReached.asReachedSet().getPrecision(state))
        .filter(VariableTrackingPrecision.isMatchingCPAClass(ValueAnalysisCPA.class))
        .get(0);
  }

  private boolean isAnyPathFeasible(final ARGReachedSet pReached, final Collection<ARGPath> errorPaths)
      throws CPAException, InterruptedException {

    ARGPath feasiblePath = null;
    for (ARGPath currentPath : errorPaths) {
      if (isErrorPathFeasible(currentPath)) {
        feasiblePath = currentPath;
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
    if (checker.isFeasible(errorPath.mutableCopy())) {
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

    // sort the list, as shorter paths are cheaper during interpolation
    // TODO: does this matter? Any other cost-measures, i.e., quality of
    // interpolants, etc. worth trying?
    Collections.sort(errorPaths, new Comparator<ARGPath>(){
      @Override
      public int compare(ARGPath path1, ARGPath path2) {
        return path1.size() - path2.size();
      }
    });

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
    Set<ARGState> targets = from(pReached.asReachedSet())
        .transform(AbstractStates.toState(ARGState.class))
        .filter(AbstractStates.IS_TARGET_STATE).toSet();

    assert !targets.isEmpty();
    logger.log(Level.FINEST, "number of targets found: " + targets.size());

    totalTargetsFound = totalTargetsFound + targets.size();

    return targets;
  }

  @Override
  public void collectStatistics(final Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Statistics() {

      @Override
      public String getName() {
        return "ValueAnalysisGlobalRefiner";
      }

      @Override
      public void printStatistics(final PrintStream pOut, final Result pResult, final ReachedSet pReached) {
        ValueAnalysisGlobalRefiner.this.printStatistics(pOut, pResult, pReached);
      }
    });
  }

  private void printStatistics(final PrintStream out, final Result pResult, final ReachedSet pReached) {
    if (totalRefinements > 0) {
      out.println("Total number of refinements:      " + String.format(Locale.US, "%9d", totalRefinements));
      out.println("Total number of targets found:    " + String.format(Locale.US, "%9d", totalTargetsFound));
      out.println("Total time for global refinement:     " + totalTime);

      pathInterpolator.printStatistics(out, pResult, pReached);
    }
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
