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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisPrefixProvider;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.refinement.GenericRefiner.RestartStrategy;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;


@Options(prefix="cpa.value.refinement")
public class ValueAnalysisImpactRefiner implements UnsoundRefiner, StatisticsProvider {

  @Option(secure=true, description="whether or not to do lazy-abstraction", name="restart", toUppercase = true)
  private RestartStrategy restartStrategy = RestartStrategy.ROOT;

  @Option(secure=true, description="whether to use the top-down interpolation strategy or the bottom-up interpolation strategy")
  private boolean useTopDownInterpolationStrategy = true;

  @Option(secure=true, description="globalPrec")
  private boolean useGlobalPrecision = false;

  @Option(secure=true, description="when to export the interpolation tree"
      + "\nNEVER:   never export the interpolation tree"
      + "\nFINAL:   export the interpolation tree once after each refinement"
      + "\nALWAYD:  export the interpolation tree once after each interpolation, i.e. multiple times per refinmenet",
      values={"NEVER", "FINAL", "ALWAYS"})
  private String exportInterpolationTree = "NEVER";

  @Option(secure=true, description="export interpolation trees to this file template")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate interpolationTreeExportFile = PathTemplate.ofFormatString("interpolationTree.%d-%d.dot");

  ValueAnalysisPathInterpolator interpolatingRefiner;
  ValueAnalysisFeasibilityChecker checker;

  private VariableTrackingPrecision globalPrecision = null;

  private final LogManager logger;

  // statistics
  private int refinementCounter  = 0;
  private int targetCounter = 0;
  private final Timer totalTime = new Timer();

  private final ARGCPA argCpa;

  public static ValueAnalysisImpactRefiner create(final ConfigurableProgramAnalysis pCpa) throws InvalidConfigurationException {
    final ValueAnalysisCPA valueAnalysisCpa = CPAs.retrieveCPA(pCpa, ValueAnalysisCPA.class);
    if (valueAnalysisCpa == null) {
      throw new InvalidConfigurationException(ValueAnalysisImpactRefiner.class.getSimpleName() + " needs a ValueAnalysisCPA");
    }

    valueAnalysisCpa.injectRefinablePrecision();

    ARGCPA argCpa;

    if (pCpa instanceof WrapperCPA) {
      argCpa = ((WrapperCPA) pCpa).retrieveWrappedCpa(ARGCPA.class);
    } else {
      throw new InvalidConfigurationException("ARG CPA needed for refinement");
    }
    if (argCpa == null) {
      throw new InvalidConfigurationException("ARG CPA needed for refinement");
    }

    final LogManager logger = valueAnalysisCpa.getLogger();
    final Configuration config = valueAnalysisCpa.getConfiguration();
    final CFA cfa = valueAnalysisCpa.getCFA();

    final StrongestPostOperator<ValueAnalysisState> strongestPostOperator =
        new ValueAnalysisStrongestPostOperator(logger, Configuration.builder().build(), cfa);

    final ValueAnalysisFeasibilityChecker feasibilityChecker =
        new ValueAnalysisFeasibilityChecker(strongestPostOperator, logger, cfa, config);

    return new ValueAnalysisImpactRefiner(
                                    feasibilityChecker,
                                    strongestPostOperator,
                                    config,
                                    logger,
                                    valueAnalysisCpa.getShutdownNotifier(),
                                    valueAnalysisCpa.getCFA(),
                                    argCpa);
  }

  private ValueAnalysisImpactRefiner(
      final ValueAnalysisFeasibilityChecker pFeasibilityChecker,
      final StrongestPostOperator<ValueAnalysisState> pStrongestPostOp,
      final Configuration pConfig, final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier, final CFA pCfa, final ARGCPA pArgCpa)
          throws InvalidConfigurationException {

    pConfig.inject(this);

    logger                = pLogger;
    argCpa                = pArgCpa;
    checker               = pFeasibilityChecker;
    interpolatingRefiner  =
        new ValueAnalysisPathInterpolator(checker,
                                          pStrongestPostOp,
                                          new ValueAnalysisPrefixProvider(logger, pCfa, pConfig),
                                          pConfig, pLogger, pShutdownNotifier, pCfa);
  }

  @Override
  public boolean performRefinement(final ReachedSet pReached) throws CPAException, InterruptedException {
    logger.log(Level.FINEST, "performing global refinement ...");
    totalTime.start();
    refinementCounter++;

    List<ARGState> targets = getErrorStates(pReached);
    targetCounter = targetCounter + targets.size();

    // stop once any feasible counterexample is found
    if (isAnyPathFeasible(new ARGReachedSet(pReached), getTargetPaths(targets))) {
      totalTime.stop();
      return false;
    }

    ValueAnalysisInterpolationTree interpolationTree = new ValueAnalysisInterpolationTree(logger, getTargetPaths(targets), useTopDownInterpolationStrategy);

    Set<ARGState> interpolatedTargets = new HashSet<>();
    while (interpolationTree.hasNextPathForInterpolation()) {
      ARGPath errorPath = interpolationTree.getNextPathForInterpolation();
      if (errorPath == null) {
        logger.log(Level.FINEST, "skipping interpolation, error path is empty, because initial interpolant is already false");
        continue;
      }
      ValueAnalysisInterpolant initialItp = interpolationTree.getInitialInterpolantForPath(errorPath);

      if (initialInterpolantIsTooWeak(interpolationTree.getRoot(), initialItp, errorPath)) {
        errorPath   = ARGUtils.getOnePathTo(errorPath.getLastState());
        initialItp  = ValueAnalysisInterpolant.createInitial();
      }

      logger.log(Level.FINEST, "performing interpolation, starting at ", errorPath.getFirstState().getStateId(), ", using interpolant ", initialItp);

      interpolatedTargets.add(errorPath.getLastState());

      interpolationTree.addInterpolants(interpolatingRefiner.performInterpolation(errorPath, initialItp));

      if (interpolationTreeExportFile != null && exportInterpolationTree.equals("ALWAYS")) {
        interpolationTree.exportToDot(interpolationTreeExportFile, refinementCounter);
      }

      logger.log(Level.FINEST, "finished interpolation #");
    }

    if (interpolationTreeExportFile != null && exportInterpolationTree.equals("FINAL") && !exportInterpolationTree.equals("ALWAYS")) {
      interpolationTree.exportToDot(interpolationTreeExportFile, refinementCounter);
    }
/*
    try (Writer w = Files.openOutputFile(Paths.get("output/ARG_" + totalRefinements + ".dot"))) {
      ARGUtils.writeARGAsDot(w, (ARGState)pReached.getFirstState());
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write ARG to file");
    }
*/
    createGlobalPrecision(pReached, interpolationTree);

    Set<ARGState> strengthenedStates = strengthenStates(interpolationTree);

    ARGReachedSet argReachedSet = new ARGReachedSet(pReached, argCpa);

    // this only works correctly for non-global-refinement for now, doesn't it?
    for (ARGState interpolatedTarget : interpolatedTargets) {
      tryToCoverArg(strengthenedStates, argReachedSet, interpolatedTarget);
    }

    Set<ARGState> weakStates = new HashSet<>();
    for (Map.Entry<ARGState, ValueAnalysisInterpolant> itp : interpolationTree.getInterpolantMapping()) {
      ARGState currentState = itp.getKey();

      if (interpolationTree.hasInterpolantForState(currentState) && interpolationTree.getInterpolantForState(currentState).isTrivial()) {
        continue;
      }

      if (strengthenedStates.contains(currentState) && currentState.getChildren().size() > 1) {
        VariableTrackingPrecision currentPrecision = extractPrecision(pReached, currentState);

        Multimap<CFANode, MemoryLocation> increment = HashMultimap.create();
        for (MemoryLocation memoryLocation : interpolationTree.getInterpolantForState(currentState).getMemoryLocations()) {
          increment.put(new CFANode("dummy"), memoryLocation);
        }

        // does not clear waitlist
        if (!currentState.isCovered()) {
          if (useGlobalPrecision) {
            argReachedSet.readdToWaitlist(currentState, globalPrecision, VariableTrackingPrecision.isMatchingCPAClass(ValueAnalysisCPA.class));
          } else {
            argReachedSet.readdToWaitlist(currentState, currentPrecision.withIncrement(increment), VariableTrackingPrecision.isMatchingCPAClass(ValueAnalysisCPA.class));
          }
        }

        weakStates.addAll(currentState.getChildren());
      }
    }


    removeInfeasiblePartsOfArg(interpolationTree, argReachedSet);

    weakStates.removeAll(strengthenedStates);
    for (ARGState leave : weakStates) {
      if (leave.isDestroyed()) {
        continue;
      }

      // remove subtree of non-strengthened state, it will be rediscovered anyway
      // because parent was readded to waitlist before
      argReachedSet.cutOffSubtree(leave);
    }

    totalTime.stop();
    return true;
  }

  private void createGlobalPrecision(final ReachedSet pReached, ValueAnalysisInterpolationTree interpolationTree) {
    for (ARGState root : interpolationTree.obtainRefinementRoots(RestartStrategy.PIVOT)) {
      Collection<ARGState> targetsReachableFromRoot = interpolationTree.getTargetsInSubtree(root);

      // join the precisions of the subtree of this roots into a single precision
      final VariableTrackingPrecision subTreePrecision = joinSubtreePrecisions(pReached, targetsReachableFromRoot);

      Multimap<CFANode, MemoryLocation> extractPrecisionIncrement = interpolationTree.extractPrecisionIncrement(root);
      VariableTrackingPrecision currentPrecision = subTreePrecision.withIncrement(extractPrecisionIncrement);

      if (globalPrecision != null) {
        currentPrecision = currentPrecision.join(globalPrecision);
      }

      globalPrecision = currentPrecision;
    }
  }

  //////////////////////// IMPACT STUFF ///////////////////////////

  @Override
  public void forceRestart(ReachedSet reached) {
    ARGState firstChild = Iterables.getOnlyElement(((ARGState)reached.getFirstState()).getChildren());

    new ARGReachedSet(reached).removeSubtree(firstChild,
        globalPrecision,
        VariableTrackingPrecision.isMatchingCPAClass(ValueAnalysisCPA.class));
  }

  private Set<ARGState> strengthenStates(ValueAnalysisInterpolationTree interpolationTree) {
    Set<ARGState> strengthenedStates = new HashSet<>();

    for (Map.Entry<ARGState, ValueAnalysisInterpolant> entry : interpolationTree.getInterpolantMapping()) {
      if (!entry.getValue().isTrivial()) {

        ARGState state                = entry.getKey();
        ValueAnalysisInterpolant itp  = entry.getValue();
        ValueAnalysisState valueState = AbstractStates.extractStateByType(state, ValueAnalysisState.class);

        if (itp.strengthen(valueState, state)) {
          strengthenedStates.add(state);
        }
      }
    }

    return strengthenedStates;
  }

  private void tryToCoverArg(Set<ARGState> strengthenedStates, ARGReachedSet reached,
      ARGState lastState) {
    ARGState coverageRoot = null;

    ARGPath errorPath = ARGUtils.getOnePathTo(lastState);

    for (ARGState state : errorPath.asStatesList()) {

      if (strengthenedStates.contains(state)) {
        try {
          // it might became (unsoundly!) covered in a previous iteration of another target path
          if (state.isCovered()
              // or it might be covered by now
              || reached.tryToCover(state, true)) {
            coverageRoot = state;
            break;
          }
        }

        catch (CPAException | InterruptedException e) {
          throw new Error();
        }
      }
    }

    if (coverageRoot != null) {
      for (ARGState children : coverageRoot.getSubgraph()) {
        if (!children.isCovered()) {
          children.setCovered(coverageRoot);
        }
      }
    }
  }

  private void removeInfeasiblePartsOfArg(ValueAnalysisInterpolationTree interpolationTree, ARGReachedSet reached) {
    for (ARGState root : interpolationTree.obtainCutOffRoots()) {
      reached.cutOffSubtree(root);
    }
  }

  private boolean initialInterpolantIsTooWeak(ARGState root, ValueAnalysisInterpolant initialItp, ARGPath errorPath)
      throws CPAException, InterruptedException {

    // if the first state of the error path is the root, the interpolant cannot be to weak
    if (errorPath.getFirstState() == root) {
      return false;
    }

    // for all other cases, check if the path is feasible when using the interpolant as initial state
    return checker.isFeasible(errorPath, initialItp.reconstructState());
  }

  private VariableTrackingPrecision joinSubtreePrecisions(final ReachedSet pReached,
      Collection<ARGState> targetsReachableFromRoot) {

    VariableTrackingPrecision precision = extractPrecision(pReached, Iterables.getLast(targetsReachableFromRoot));
    // join precisions of all target states
    for (ARGState target : targetsReachableFromRoot) {
      VariableTrackingPrecision precisionOfTarget = extractPrecision(pReached, target);
      precision = precision.join(precisionOfTarget);
    }

    return precision;
  }

  private VariableTrackingPrecision extractPrecision(final ReachedSet pReached,
      ARGState state) {
    return (VariableTrackingPrecision) Precisions.asIterable(pReached.getPrecision(state)).filter(VariableTrackingPrecision.isMatchingCPAClass(ValueAnalysisCPA.class)).get(0);
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

  private List<ARGState> getErrorStates(final ReachedSet pReached) {
    List<ARGState> targets = from(pReached)
        .transform(AbstractStates.toState(ARGState.class))
        .filter(AbstractStates.IS_TARGET_STATE)
        .toList();

    assert !targets.isEmpty();
    logger.log(Level.FINEST, "number of targets found: " + targets.size());

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
        ValueAnalysisImpactRefiner.this.printStatistics(pOut, pResult, pReached);
      }
    });
  }

  private void printStatistics(final PrintStream out, final Result pResult, final ReachedSet pReached) {
    if (refinementCounter > 0) {
      out.println("Total number of refinements:      " + String.format(Locale.US, "%9d", refinementCounter));
      out.println("Total number of targets found:    " + String.format(Locale.US, "%9d", targetCounter));
      out.println("Total time for global refinement:     " + totalTime);

      interpolatingRefiner.printStatistics(out, pResult, pReached);
    }
  }
}
