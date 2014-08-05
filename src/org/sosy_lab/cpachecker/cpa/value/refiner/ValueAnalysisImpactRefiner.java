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

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
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
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisPrecision;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.MemoryLocation;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisInterpolationBasedRefiner.ValueAnalysisInterpolant;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.ValueAnalysisFeasibilityChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Precisions;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;


@Options(prefix="cpa.value.refinement")
public class ValueAnalysisImpactRefiner implements UnsoundRefiner, StatisticsProvider {

  @Option(description="whether or not to do lazy-abstraction", name="restart", toUppercase = true)
  private RestartStrategy restartStrategy = RestartStrategy.TOP;

  @Option(description="whether to use the top-down interpolation strategy or the bottom-up interpolation strategy")
  private boolean useTopDownInterpolationStrategy = true;

  @Option(description="globalPrec")
  private boolean useGlobalPrecision = false;

  @Option(description="when to export the interpolation tree"
      + "\nNEVER:   never export the interpolation tree"
      + "\nFINAL:   export the interpolation tree once after each refinement"
      + "\nALWAYD:  export the interpolation tree once after each interpolation, i.e. multiple times per refinmenet",
      values={"NEVER", "FINAL", "ALWAYS"})
  private String exportInterpolationTree = "NEVER";

  ValueAnalysisInterpolationBasedRefiner interpolatingRefiner;
  ValueAnalysisFeasibilityChecker checker;

  private ValueAnalysisPrecision globalPrecision = null;

  private final LogManager logger;

  // statistics
  private int totalRefinements  = 0;
  private int totalTargetsFound = 0;
  private final Timer totalTime = new Timer();

  private final ARGCPA argCpa;

  public static ValueAnalysisImpactRefiner create(final ConfigurableProgramAnalysis pCpa) throws InvalidConfigurationException {
    final ValueAnalysisCPA valueAnalysisCpa = CPAs.retrieveCPA(pCpa, ValueAnalysisCPA.class);
    if (valueAnalysisCpa == null) {
      throw new InvalidConfigurationException(ValueAnalysisImpactRefiner.class.getSimpleName() + " needs a ValueAnalysisCPA");
    }

    valueAnalysisCpa.injectRefinablePrecision();

    ARGCPA argCpa = null;

    if (pCpa instanceof WrapperCPA) {
      argCpa = ((WrapperCPA) pCpa).retrieveWrappedCpa(ARGCPA.class);
    } else {
      throw new InvalidConfigurationException("ARG CPA needed for refinement");
    }
    if (argCpa == null) {
      throw new InvalidConfigurationException("ARG CPA needed for refinement");
    }

    ValueAnalysisImpactRefiner refiner = new ValueAnalysisImpactRefiner(valueAnalysisCpa.getConfiguration(),
                                    valueAnalysisCpa.getLogger(),
                                    valueAnalysisCpa.getShutdownNotifier(),
                                    valueAnalysisCpa.getCFA(),
                                    argCpa);

    valueAnalysisCpa.getStats().addRefiner(refiner);

    return refiner;
  }

  private ValueAnalysisImpactRefiner(final Configuration pConfig, final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier, final CFA pCfa, final ARGCPA pArgCpa)
          throws InvalidConfigurationException {

    pConfig.inject(this);

    logger                = pLogger;
    argCpa                = pArgCpa;
    interpolatingRefiner  = new ValueAnalysisInterpolationBasedRefiner(pConfig, pLogger, pShutdownNotifier, pCfa);
    checker               = new ValueAnalysisFeasibilityChecker(pLogger, pCfa);
  }

  @Override
  public boolean performRefinement(final ReachedSet pReached) throws CPAException, InterruptedException {
    logger.log(Level.FINEST, "performing global refinement ...");
    totalTime.start();
    totalRefinements++;

    List<ARGState> targets  = getErrorStates(pReached);
    totalTargetsFound       = totalTargetsFound + targets.size();

    // stop once any feasible counterexample is found
    if(isAnyPathFeasible(new ARGReachedSet(pReached), getErrorPaths(targets))) {
      System.out.println("path is feasible !!!");
      totalTime.stop();
      return false;
    }

    InterpolationTree interpolationTree = new InterpolationTree(logger, targets, useTopDownInterpolationStrategy);

    Set<ARGState> interpolatedTargets = new HashSet<>();
//System.out.println("----------------------- ref: " + totalRefinements + "--------------------------------------");
    int i = 0;
    while(interpolationTree.hasNextPathForInterpolation()) {
      i++;

      ARGPath errorPath = interpolationTree.getNextPathForInterpolation();
      if(errorPath.isEmpty()) {
        logger.log(Level.FINEST, "skipping interpolation, error path is empty, because initial interpolant is already false");
        continue;
      }
//System.out.println(ARGUtils.getOnePathTo(errorPath.getLast().getFirst()).toString().hashCode());
      ValueAnalysisInterpolant initialItp = interpolationTree.getInitialInterpolantForPath(errorPath);

      if(initialInterpolantIsTooWeak(interpolationTree.root, initialItp, errorPath)) {
        errorPath   = ARGUtils.getOnePathTo(errorPath.getLast().getFirst());
        initialItp  = ValueAnalysisInterpolant.createInitial();
      }

      logger.log(Level.FINEST, "performing interpolation, starting at ", errorPath.getFirst().getFirst().getStateId(), ", using interpolant ", initialItp);

      interpolatedTargets.add(errorPath.getLast().getFirst());

      interpolationTree.addInterpolants(interpolatingRefiner.performInterpolation(errorPath, initialItp));

      if(exportInterpolationTree.equals("ALWAYS")) {
        interpolationTree.exportToDot(totalRefinements, i);
      }

      logger.log(Level.FINEST, "finished interpolation #", i);
    }

    if(exportInterpolationTree.equals("FINAL") && !exportInterpolationTree.equals("ALWAYS")) {
      interpolationTree.exportToDot(totalRefinements, i);
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
    for(ARGState interpolatedTarget : interpolatedTargets) {
      tryToCoverArg(strengthenedStates, argReachedSet, interpolatedTarget);
    }

    Set<ARGState> weakStates = new HashSet<>();
    for (Map.Entry<ARGState, ValueAnalysisInterpolant> itp : interpolationTree.interpolants.entrySet()) {
      ARGState currentState = itp.getKey();

      if (interpolationTree.interpolants.containsKey(currentState) && interpolationTree.interpolants.get(currentState).isTrivial()) {
        continue;
      }

      if (strengthenedStates.contains(currentState) && currentState.getChildren().size() > 1) {
        ValueAnalysisPrecision currentPrecision = extractPrecision(pReached, currentState);

        Multimap<CFANode, MemoryLocation> increment = HashMultimap.create();
        for (MemoryLocation memoryLocation : interpolationTree.interpolants.get(currentState).getMemoryLocations()) {
          increment.put(new CFANode("dummy"), memoryLocation);
        }

        // does not clear waitlist
        if(!currentState.isCovered()) {
          if(useGlobalPrecision) {
            argReachedSet.readdToWaitlist(currentState, globalPrecision, ValueAnalysisPrecision.class);
          }
          else {
            argReachedSet.readdToWaitlist(currentState, new ValueAnalysisPrecision(currentPrecision, increment), ValueAnalysisPrecision.class);
          }
        }

        weakStates.addAll(currentState.getChildren());
      }
    }


    removeInfeasiblePartsOfArg(interpolationTree, argReachedSet);

    weakStates.removeAll(strengthenedStates);
    for(ARGState leave : weakStates) {
      if(leave.isDestroyed()) {
        continue;
      }

      // remove subtree of non-strengthened state, it will be rediscovered anyway
      // because parent was readded to waitlist before
      argReachedSet.cutOffSubtree(leave);
    }

    totalTime.stop();
    return true;
  }

  private void createGlobalPrecision(final ReachedSet pReached, InterpolationTree interpolationTree) {
    for (ARGState root : interpolationTree.obtainRefinementRoots(RestartStrategy.BOTTOM)) {
      Collection<ARGState> targetsReachableFromRoot = interpolationTree.getTargetsInSubtree(root);

      // join the precisions of the subtree of this roots into a single precision
      final ValueAnalysisPrecision subTreePrecision = joinSubtreePrecisions(pReached, targetsReachableFromRoot);

      Multimap<CFANode, MemoryLocation> extractPrecisionIncrement = interpolationTree.extractPrecisionIncrement(root);
//System.out.println(new TreeSet<>(extractPrecisionIncrement.values()));
      ValueAnalysisPrecision currentPrecision = new ValueAnalysisPrecision(subTreePrecision, extractPrecisionIncrement);

      if (globalPrecision != null) {
        currentPrecision.getRefinablePrecision().join(globalPrecision.getRefinablePrecision());
      }

      globalPrecision = currentPrecision;
    }
  }

  //////////////////////// IMPACT STUFF ///////////////////////////

  @Override
  public ValueAnalysisPrecision getGlobalPrecision() {
    return globalPrecision;
  }

  private Set<ARGState> strengthenStates(InterpolationTree interpolationTree) {
    Set<ARGState> strengthenedStates = new HashSet<>();

    for (Map.Entry<ARGState, ValueAnalysisInterpolant> entry : interpolationTree.interpolants.entrySet()) {
      if (!entry.getValue().isTrivial()) {

        ARGState state                = entry.getKey();
        ValueAnalysisInterpolant itp  = entry.getValue();
        ValueAnalysisState valueState = AbstractStates.extractStateByType(state, ValueAnalysisState.class);

        if(itp.strengthen(valueState, state)) {
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

    for(Pair<ARGState, CFAEdge> currentElement : errorPath) {
      ARGState state = currentElement.getFirst();

      if(strengthenedStates.contains(state)) {
        try {
          // it might became (unsoundly!) covered in a previous iteration of another target path
          if(state.isCovered()
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

    if(coverageRoot != null) {
      for(ARGState children : coverageRoot.getSubgraph()) {
        if(!children.isCovered()) {
          children.setCovered(coverageRoot);
          //System.out.println("set coverage for " + children.getStateId());
        }
      }
    }
  }

  private void removeInfeasiblePartsOfArg(InterpolationTree interpolationTree, ARGReachedSet reached) {
    for (ARGState root : interpolationTree.obtainCutOffRoots()) {
      reached.cutOffSubtree(root);
    }
  }

  private boolean initialInterpolantIsTooWeak(ARGState root, ValueAnalysisInterpolant initialItp, ARGPath errorPath)
      throws CPAException, InterruptedException {

    // if the first state of the error path is the root, the interpolant cannot be to weak
    if(errorPath.getFirst().getFirst() == root) {
      return false;
    }

    // for all other cases, check if the path is feasible when using the interpolant as initial state
    return checker.isFeasible(errorPath, initialItp.createValueAnalysisState());
  }

  private ValueAnalysisPrecision joinSubtreePrecisions(final ReachedSet pReached,
      Collection<ARGState> targetsReachableFromRoot) {

    final ValueAnalysisPrecision precision = extractPrecision(pReached, Iterables.getLast(targetsReachableFromRoot));
    // join precisions of all target states
    for(ARGState target : targetsReachableFromRoot) {
      ValueAnalysisPrecision precisionOfTarget = extractPrecision(pReached, target);
      precision.getRefinablePrecision().join(precisionOfTarget.getRefinablePrecision());
    }

    return precision;
  }

  private ValueAnalysisPrecision extractPrecision(final ReachedSet pReached,
      ARGState state) {
    return Precisions.extractPrecisionByType(pReached.getPrecision(state), ValueAnalysisPrecision.class);
  }

  private boolean isAnyPathFeasible(final ARGReachedSet pReached, final Collection<ARGPath> errorPaths)
      throws CPAException, InterruptedException {

    ARGPath feasiblePath = null;
    for(ARGPath currentPath : errorPaths) {
      if(isErrorPathFeasible(currentPath)) {
        feasiblePath = currentPath;
      }
    }

    // remove all other target states, so that only one is left (for CEX-checker)
    if(feasiblePath != null) {
      for(ARGPath others : errorPaths) {
        if(others != feasiblePath) {
          pReached.removeSubtree(others.getLast().getFirst());
        }
      }
      return true;
    }

    return false;
  }

  private boolean isErrorPathFeasible(final ARGPath errorPath)
      throws CPAException, InterruptedException {
    if(checker.isFeasible(errorPath)) {
      logger.log(Level.FINEST, "found a feasible cex - returning from refinement");

      return true;
    }

    return false;
  }

  private Collection<ARGPath> getErrorPaths(final Collection<ARGState> targetStates) {
    Set<ARGPath> errorPaths = new TreeSet<>(new Comparator<ARGPath>() {
      @Override
      public int compare(ARGPath path1, ARGPath path2) {
        if(path1.size() == path2.size()) {
          return 1;
        }

        else {
          return (path1.size() < path2.size()) ? -1 : 1;
        }
      }
    });

    for(ARGState target : targetStates) {
      ARGPath p = ARGUtils.getOnePathTo(target);
      errorPaths.add(p);
    }

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
    if (totalRefinements > 0) {
      out.println("Total number of refinements:      " + String.format(Locale.US, "%9d", totalRefinements));
      out.println("Total number of targets found:    " + String.format(Locale.US, "%9d", totalTargetsFound));
      out.println("Total time for global refinement:     " + totalTime);

      interpolatingRefiner.printStatistics(out, pResult, pReached);
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

  /**
   * This class represents an interpolation tree, i.e. a set of states connected through a successor-predecessor-relation.
   * The tree is built from traversing backwards from error states. It can be used to retrieve paths from the root of the
   * tree to error states, in a way, that only path not yet excluded by previous path interpolation need to be interpolated.
   */
  private static class InterpolationTree {
    /**
     * the logger in use
     */
    private final LogManager logger;

    /**
     * the predecessor relation of the states contained in this tree
     */
    private final Map<ARGState, ARGState> predecessorRelation = Maps.newHashMap();

    /**
     * the successor relation of the states contained in this tree
     */
    private final SetMultimap<ARGState, ARGState> successorRelation = LinkedHashMultimap.create();

    /**
     * the mapping from state to the identified interpolants
     */
    private final Map<ARGState, ValueAnalysisInterpolant> interpolants = new LinkedHashMap<>();

    /**
     * the root of the tree
     */
    private final ARGState root;

    /**
     * the target states to build the tree from
     */
    private final Collection<ARGState> targets;

    /**
     * the strategy on how to select paths for interpolation
     */
    private final InterpolationStrategy strategy;

    /**
     * This method acts as constructor of the interpolation tree.
     *
     * @param pLogger the logger to use
     * @param pTargets the set of target states from which to build the interpolation tree
     * @param useTopDownInterpolationStrategy the flag to choose the strategy to apply
     */
    private InterpolationTree(final LogManager pLogger, final Collection<ARGState> pTargets,
        final boolean useTopDownInterpolationStrategy) {
      logger    = pLogger;

      targets   = pTargets;
      root      = buildTree();

      if(useTopDownInterpolationStrategy) {
        strategy = new TopDownInterpolationStrategy();
      } else {
        strategy = new BottomUpInterpolationStrategy();
      }
    }

    /**
     * This method decides whether or not there are more paths left for interpolation.
     *
     * @return true if there are more paths left for interpolation, else false
     */
    public boolean hasNextPathForInterpolation() {
      return strategy.hasNextPathForInterpolation();
    }

    /**
     * This method creates the successor and predecessor relations, which make up the interpolation tree,
     * from the target states given as input.
     *
     * @return the root of the tree
     */
    private ARGState buildTree() {
      Deque<ARGState> todo = new ArrayDeque<>(targets);
      ARGState itpTreeRoot = null;

      // build the tree, bottom-up, starting from the target states
      while (!todo.isEmpty()) {
        final ARGState currentState = todo.removeFirst();

        if (currentState.getParents().iterator().hasNext()) {
          ARGState parentState = currentState.getParents().iterator().next();
          todo.add(parentState);
          predecessorRelation.put(currentState, parentState);
          successorRelation.put(parentState, currentState);
        }

        else if (itpTreeRoot == null) {
          itpTreeRoot = currentState;
        }
      }

      return itpTreeRoot;
    }

    /**
     * This method exports the current representation to a *.dot file.
     *
     * @param refinementCnt the current refinement counter
     * @param iteration the current iteration of the current refinement
     */
    private void exportToDot(int refinementCnt, int iteration) {
      StringBuilder result = new StringBuilder().append("digraph tree {" + "\n");
      for(Map.Entry<ARGState, ARGState> current : successorRelation.entries()) {
        if(interpolants.containsKey(current.getKey())) {
          StringBuilder sb = new StringBuilder();

          sb.append("itp is " + interpolants.get(current.getKey()));

          result.append(current.getKey().getStateId() + " [label=\"" + (current.getKey().getStateId() + " / " + AbstractStates.extractLocation(current.getKey())) + " has itp " + (sb.toString()) + "\"]" + "\n");
          result.append(current.getKey().getStateId() + " -> " + current.getValue().getStateId() + "\n");// + " [label=\"" + current.getKey().getEdgeToChild(current.getValue()).getRawStatement().replace("\n", "") + "\"]\n");
        }

        else {
          result.append(current.getKey().getStateId() + " [label=\"" + current.getKey().getStateId() + " has itp NA\"]" + "\n");
          result.append(current.getKey().getStateId() + " -> " + current.getValue().getStateId() + "\n");// + " [label=\"" + current.getKey().getEdgeToChild(current.getValue()).getRawStatement().replace("\n", "") + "\"]\n");
        }

        if(current.getValue().isTarget()) {
          result.append(current.getValue().getStateId() + " [style=filled, fillcolor=\"red\"]" + "\n");
        }

        assert(!current.getKey().isTarget());
      }
      result.append("}");

      try {
        Files.writeFile(Paths.get("itpTree_" + refinementCnt + "_" + iteration + ".dot"), result.toString());
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e,
            "Could not write interpolation tree to file");
      }
    }

    /**
     * This method returns the next error path for interpolation.
     *
     * @param current the current root of the error path to retrieve for a subsequent interpolation
     * @param interpolationRoots the mutable stack of interpolation roots, which might be added to within this method
     * @return the next error path for a subsequent interpolation
     */
    private ARGPath getNextPathForInterpolation() {
      return strategy.getNextPathForInterpolation();
    }

    /**
     * This method returns the interpolant to be used for interpolation of the given path.
     *
     * @param errorPath the path for which to obtain the initial interpolant
     * @return the initial interpolant for the given path
     */
    private ValueAnalysisInterpolant getInitialInterpolantForPath(ARGPath errorPath) {
      return strategy.getInitialInterpolantForRoot(errorPath.getFirst().getFirst());
    }

    /**
     * This method updates the mapping from states to interpolants.
     *
     * @param newItps the new mapping to add
     */
    private void addInterpolants(Map<ARGState, ValueAnalysisInterpolant> newItps) {
      for (Map.Entry<ARGState, ValueAnalysisInterpolant> entry : newItps.entrySet()) {
        ARGState state                = entry.getKey();
        ValueAnalysisInterpolant itp  = entry.getValue();

        if(interpolants.containsKey(state)) {
          interpolants.put(state, interpolants.get(state).join(itp));
        } else {
          interpolants.put(state, itp);
        }
      }
    }

    /**
     * This method extracts the precision increment for the given refinement root.
     * It does so by collection all non-trivial interpolants in the subtree of the given refinement root.
     *
     * @return the precision increment for the given refinement root
     */
    private Multimap<CFANode, MemoryLocation> extractPrecisionIncrement(ARGState refinmentRoot) {
      Multimap<CFANode, MemoryLocation> increment = HashMultimap.create();

      Deque<ARGState> todo = new ArrayDeque<>(Collections.singleton(predecessorRelation.get(refinmentRoot)));
      while (!todo.isEmpty()) {
        final ARGState currentState = todo.removeFirst();

        if (isNonTrivialInterpolantAvailable(currentState) && !currentState.isTarget()) {
          ValueAnalysisInterpolant itp = interpolants.get(currentState);
          for (MemoryLocation memoryLocation : itp.getMemoryLocations()) {
            increment.put(AbstractStates.extractLocation(currentState), memoryLocation);
          }
        }

        Set<ARGState> successors = successorRelation.get(currentState);
        todo.addAll(successors);
      }
//System.out.println(new TreeSet<>(increment.values()));
      return increment;
    }

    /**
     * This method obtains the refinement roots, i.e., for each disjunct path from target states
     * to the root, it collects the highest state that has a non-trivial interpolant associated.
     * With non-lazy abstraction, the root of the interpolation tree is used as refinement root.
     *
     * @param whether to perform lazy abstraction or not
     * @return the set of refinement roots
     */
    private Collection<ARGState> obtainRefinementRoots(RestartStrategy strategy) {
      if (strategy == RestartStrategy.TOP) {
        assert successorRelation.get(root).size() == 1 : "ARG root has more than one successor";
        return new HashSet<>(Collections.singleton(successorRelation.get(root).iterator().next()));
      }

      ARGState commonRoot = null;

      Collection<ARGState> refinementRoots = new HashSet<>();

      Deque<ARGState> todo = new ArrayDeque<>(Collections.singleton(root));
      while (!todo.isEmpty()) {
        final ARGState currentState = todo.removeFirst();

        // determine the first branching point, which is the lowest node common to all refinement roots
        if (commonRoot == null && successorRelation.get(currentState).size() > 1) {
          commonRoot = currentState;
        }

        if (isNonTrivialInterpolantAvailable(currentState)) {
          refinementRoots.add(currentState);

          if(strategy == RestartStrategy.COMMON && refinementRoots.size() > 2) {
            assert commonRoot != null: "common root not yet set";
            return new HashSet<>(Collections.singleton(commonRoot));
          }
          continue;
        }

        Set<ARGState> successors = successorRelation.get(currentState);
        todo.addAll(successors);
      }

      return refinementRoots;
    }

    private Collection<ARGState> obtainCutOffRoots() {
      Collection<ARGState> refinementRoots = new HashSet<>();

      Deque<ARGState> todo = new ArrayDeque<>(Collections.singleton(root));
      while (!todo.isEmpty()) {
        final ARGState currentState = todo.removeFirst();

        if (hasFalseInterpolant(currentState)) {
          refinementRoots.add(currentState);
          continue;

        }

        Set<ARGState> successors = successorRelation.get(currentState);
        todo.addAll(successors);
      }

      return refinementRoots;
    }

    private boolean hasFalseInterpolant(final ARGState currentState) {
      return interpolants.containsKey(currentState) && interpolants.get(currentState).isFalse();
    }

    /**
     * This method returns the target states in the subtree of the given state.
     *
     * @param state the state for which to collect the target states in its subtree.
     * @return target states in the subtree of the given state
     */
    private Collection<ARGState> getTargetsInSubtree(ARGState state) {
      Collection<ARGState> targetStates = new HashSet<>();

      Deque<ARGState> todo = new ArrayDeque<>(Collections.singleton(state));
      while (!todo.isEmpty()) {
        final ARGState currentState = todo.removeFirst();

        if (currentState.isTarget()) {
          targetStates.add(currentState);
          continue;
        }

        Set<ARGState> successors = successorRelation.get(currentState);
        todo.addAll(successors);
      }

      return targetStates;
    }

    /**
     * This method checks if for the given state a non-trivial interpolant is present.
     *
     * @param currentState the state for which to check
     * @return true, if a non-trivial interpolant is present, else false
     */
    private boolean isNonTrivialInterpolantAvailable(final ARGState currentState) {
      return interpolants.containsKey(currentState) && !interpolants.get(currentState).isTrivial();
    }



    private interface InterpolationStrategy {

      public ARGPath getNextPathForInterpolation();

      public boolean hasNextPathForInterpolation();

      public ValueAnalysisInterpolant getInitialInterpolantForRoot(ARGState root);
    }

    private class TopDownInterpolationStrategy implements InterpolationStrategy {

      /**
       * the states that are the sources for obtaining (partial) error paths
       */
      private Deque<ARGState> sources = new ArrayDeque<>(Collections.singleton(root));

      @Override
      public ARGPath getNextPathForInterpolation() {
        ARGPath errorPath = new ARGPath();

        ARGState current = sources.pop();

        if(!isValidInterpolationRoot(predecessorRelation.get(current))) {
          logger.log(Level.FINEST, "interpolant of predecessor of ", current.getStateId(), " is already false ... return empty path");
          return errorPath;
        }

        // if the current state is not the root, it is a child of a branch , however, the path should not start with the
        // child, but with the branching node (children are stored on the stack because this needs less book-keeping)
        if(current != root) {
          errorPath.add(Pair.of(predecessorRelation.get(current), predecessorRelation.get(current).getEdgeToChild(current)));
        }

        while(successorRelation.get(current).iterator().hasNext()) {
          Iterator<ARGState> children = successorRelation.get(current).iterator();
          ARGState child = children.next();
          errorPath.add(Pair.of(current, current.getEdgeToChild(child)));

          // push all other children of the current state, if any, onto the stack for later interpolations
          if(children.hasNext()) {
            ARGState sibling = children.next();
            logger.log(Level.FINEST, "\tpush new root ", sibling.getStateId(), " onto stack for parent ", predecessorRelation.get(sibling).getStateId());
            sources.push(sibling);
          }

          current = child;

          // add out-going edges of final state, too (just for compatibility reasons to compare to DelegatingRefiner)
          if(!successorRelation.get(current).iterator().hasNext()) {
            errorPath.add(Pair.of(current, CFAUtils.leavingEdges(AbstractStates.extractLocation(current)).first().orNull()));
          }
        }

        return errorPath;
      }

      /**
       * The given state is not a valid interpolation root if it is associated with a interpolant representing "false"
       */
      public boolean isValidInterpolationRoot(ARGState root) {
        if(!interpolants.containsKey(root)) {
          return true;
        }

        if(!interpolants.get(root).isFalse()) {
          return true;
        }

        return false;
      }

      @Override
      public ValueAnalysisInterpolant getInitialInterpolantForRoot(ARGState root) {
        ValueAnalysisInterpolant initialInterpolant = interpolants.get(root);

        if(initialInterpolant == null) {
          initialInterpolant = ValueAnalysisInterpolant.createInitial();

          assert (interpolants.size() == 0) : "initial interpolant was null after initial interpolation!";
        }

        return initialInterpolant;
      }

      @Override
      public boolean hasNextPathForInterpolation() {
        return !sources.isEmpty();
      }
    }

    private class BottomUpInterpolationStrategy implements InterpolationStrategy {

      /**
       * the states that are the sources for obtaining error paths
       */
      private List<ARGState> sources = new ArrayList<>(targets);

      @Override
      public ARGPath getNextPathForInterpolation() {
        ARGState current = sources.remove(0);

        assert current.isTarget() : "current element is not a target";

        ARGPath errorPath = new ARGPath();

        errorPath.addFirst(Pair.of(current, CFAUtils.leavingEdges(AbstractStates.extractLocation(current)).first().orNull()));

        while(predecessorRelation.get(current) != null) {

          ARGState parent = predecessorRelation.get(current);

          errorPath.addFirst(Pair.of(parent, parent.getEdgeToChild(current)));

          current = parent;
        }

        return errorPath;
      }

      @Override
      public ValueAnalysisInterpolant getInitialInterpolantForRoot(ARGState root) {
        return ValueAnalysisInterpolant.createInitial();
      }

      @Override
      public boolean hasNextPathForInterpolation() {
        return !sources.isEmpty();
      }
    }
  }
}
