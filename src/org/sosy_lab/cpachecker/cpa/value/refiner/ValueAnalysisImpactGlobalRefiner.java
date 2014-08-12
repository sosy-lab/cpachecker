/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
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
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.MutableARGPath;
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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;

@Options(prefix="cpa.value.refinement")
public class ValueAnalysisImpactGlobalRefiner implements UnsoundRefiner, StatisticsProvider {

  @Option(description="whether or not to do lazy-abstraction", name="restart", toUppercase = true)
  private RestartStrategy restartStrategy = RestartStrategy.TOP;

  @Option(description="whether to use the top-down interpolation strategy or the bottom-up interpolation strategy")
  private boolean useTopDownInterpolationStrategy = true;

  @Option(description="when to export the interpolation tree"
      + "\nNEVER:   never export the interpolation tree"
      + "\nFINAL:   export the interpolation tree once after each refinement"
      + "\nALWAYD:  export the interpolation tree once after each interpolation, i.e. multiple times per refinmenet",
      values={"NEVER", "FINAL", "ALWAYS"})
  private String exportInterpolationTree = "NEVER";

  @Option(description="forceRestart")
  private int forceRestart = 0;

  ValueAnalysisInterpolationBasedRefiner interpolatingRefiner;
  ValueAnalysisFeasibilityChecker checker;

  private final LogManager logger;

  private Map<Integer, Integer> uniqueTargetTraceCounter = new HashMap<>();

  // statistics
  private int totalRefinements  = 0;
  private int totalTargetsFound = 0;
  private final Timer totalTime = new Timer();

  private ValueAnalysisPrecision globalPrecision = null;

  private Set<ARGState> strengthendStates = new HashSet<>();

  private static ARGCPA argCpa = null;

  public static ValueAnalysisImpactGlobalRefiner create(final ConfigurableProgramAnalysis pCpa) throws InvalidConfigurationException {
    final ValueAnalysisCPA valueAnalysisCpa = CPAs.retrieveCPA(pCpa, ValueAnalysisCPA.class);

    argCpa = CPAs.retrieveCPA(pCpa, ARGCPA.class);

    if (valueAnalysisCpa == null) {
      throw new InvalidConfigurationException(ValueAnalysisImpactGlobalRefiner.class.getSimpleName() + " needs a ValueAnalysisCPA");
    }

    valueAnalysisCpa.injectRefinablePrecision();

    ValueAnalysisImpactGlobalRefiner refiner = new ValueAnalysisImpactGlobalRefiner(valueAnalysisCpa.getConfiguration(),
                                    valueAnalysisCpa.getLogger(),
                                    valueAnalysisCpa.getShutdownNotifier(),
                                    valueAnalysisCpa.getCFA());

    valueAnalysisCpa.getStats().addRefiner(refiner);

    return refiner;
  }

  private ValueAnalysisImpactGlobalRefiner(final Configuration pConfig, final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier, final CFA pCfa)
          throws InvalidConfigurationException {

    pConfig.inject(this);

    logger                = pLogger;
    interpolatingRefiner  = new ValueAnalysisInterpolationBasedRefiner(pConfig, pLogger, pShutdownNotifier, pCfa);
    checker               = new ValueAnalysisFeasibilityChecker(pLogger, pCfa);
  }

  @Override
  public boolean performRefinement(final ReachedSet pReached) throws CPAException, InterruptedException {
    logger.log(Level.FINEST, "performing global refinement ...");

    totalTime.start();
    totalRefinements++;

    timerErrors.start();
    List<ARGState> targets  = getErrorStates(pReached);
    totalTargetsFound       = totalTargetsFound + targets.size();
    timerErrors.stop();
//System.out.println("number of targets: " + targets.size());
    // stop once any feasible counterexample is found
    if(isAnyPathFeasible(new ARGReachedSet(pReached), getErrorPaths(targets))) {
      totalTime.stop();
      return false;
    }

    logger.log(Level.FINEST, "-------------------------------- new refinement [" + totalRefinements + "] --------------------------------");

    timerItpTree.start();
    InterpolationTree interpolationTree = new InterpolationTree(logger, targets, useTopDownInterpolationStrategy);
    timerItpTree.stop();

    timerItp.start();
    int i = 0;
    MutableARGPath lastErrorPath = null;
    while(interpolationTree.hasNextPathForInterpolation()) {
      i++;

      MutableARGPath errorPath = interpolationTree.getNextPathForInterpolation();

//System.out.println(totalRefinements + " ->  errorPath |" + errorPath.size() + "|: " + errorPath.toString().hashCode() + "(" + uniqueTargetTraceCounter.containsKey(errorPath.toString().hashCode()) + ")");
      if(errorPath.isEmpty()) {
        logger.log(Level.FINEST, "skipping interpolation, error path is empty, because initial interpolant is already false");
        continue;
      }
//System.out.println(ARGUtils.getOnePathTo(errorPath.getLast().getFirst()).toString().hashCode());

      lastErrorPath = errorPath;

      if(i == 1) {
        incrementUniqueTargetTraceCounter(errorPath);
      }

      ValueAnalysisInterpolant initialItp = interpolationTree.getInitialInterpolantForPath(errorPath);

      if(initialInterpolantIsTooWeak(interpolationTree.root, initialItp, errorPath)) {
        errorPath   = ARGUtils.getOneMutablePathTo(errorPath.getLast().getFirst());
        initialItp  = ValueAnalysisInterpolant.createInitial();
      }

      logger.log(Level.FINEST, "performing interpolation, starting at ", errorPath.getFirst().getFirst().getStateId(), ", using interpolant ", initialItp);

      interpolationTree.addInterpolants(interpolatingRefiner.performInterpolation(errorPath, initialItp));

      if(exportInterpolationTree.equals("ALWAYS")) {
        interpolationTree.exportToDot(totalRefinements, i);
      }

      logger.log(Level.FINEST, "finished interpolation #", i);
    }

    timerItp.stop();

    if(exportInterpolationTree.equals("FINAL") && !exportInterpolationTree.equals("ALWAYS")) {
      interpolationTree.exportToDot(totalRefinements, i);
    }

    // construct a global precision, needed for full restart at initial node
    timerGlobalPrec.start();
    createGlobalPrecision(pReached, interpolationTree);
    timerGlobalPrec.stop();

    if(forceRestart != 0 && totalRefinements % forceRestart == 0) {
      new ARGReachedSet(pReached).removeSubtree(((ARGState)pReached.getFirstState()).getChildren().iterator().next(),
          globalPrecision, ValueAnalysisPrecision.class);

      totalTime.stop();
      return true;
    }

    // debugging only
    dumpArgToDot(pReached, "before", interpolationTree.getErrorPathEdges());

    timerStrengthen.start();
    strengthenArg(interpolationTree);
    timerStrengthen.stop();

    ARGReachedSet reached       = new ARGReachedSet(pReached, argCpa);
    Set<ARGState> weakSiblings  = new HashSet<>();

    tryToCoverArg(lastErrorPath, reached);

    timerObtainPrecision.start();

    for (Map.Entry<ARGState, ValueAnalysisInterpolant> itp : interpolationTree.interpolants.entrySet()) {
      ARGState currentState = itp.getKey();

      if (interpolationTree.interpolants.containsKey(currentState) && interpolationTree.interpolants.get(currentState).isTrivial()) {
        continue;
      }

      if(strengthendStates.contains(currentState) && currentState.getChildren().size() > 1) {
        ValueAnalysisPrecision currentPrecision = extractPrecision(pReached, currentState);

        Multimap<CFANode, MemoryLocation> increment = HashMultimap.create();
        for (MemoryLocation memoryLocation : interpolationTree.interpolants.get(currentState).getMemoryLocations()) {
          increment.put(new CFANode("dummy"), memoryLocation);
        }

        timerReaddToWaitlist.start();

        if(!currentState.isCovered()) {
          reached.readdToWaitlist(currentState, new ValueAnalysisPrecision(currentPrecision, increment), ValueAnalysisPrecision.class);
        }

        timerReaddToWaitlist.stop();

        weakSiblings.addAll(currentState.getChildren());
      }
    }
    timerObtainPrecision.stop();

    timerRemoveInfeasible.start();
    removeInfeasiblePartsOfArg(interpolationTree, reached);
    timerRemoveInfeasible.stop();

    for(ARGState leave : weakSiblings) {
      // do not remove the sibling that was strengthened, it's not weak after all
      if(strengthendStates.contains(leave)) {
        continue;
      }

      if(leave.isDestroyed()) {
        continue;
      }

      reached.cutOffSubtree(leave);
    }

    // debugging only
    dumpArgToDot(pReached, "after", interpolationTree.getErrorPathEdges());

    totalTime.stop();
    return true;
  }

  private void tryToCoverArg(MutableARGPath pLastErrorPath, ARGReachedSet reached) {
    ARGState coverageRoot = null;

    // traverse path top-to-bottom, trying to find a state covering a strengthened state in the error path ...
    for(int i = 0; i < pLastErrorPath.size(); i++) {
      Pair<ARGState, CFAEdge> elem = pLastErrorPath.get(i);

      ARGState state = elem.getFirst();

      if(strengthendStates.contains(state)) {
        try {

          if(reached.tryToCover(state, true)) {
//System.out.println("detected coverage for state " + state.getStateId());
            coverageRoot = state;
            break;
          }
        } catch (CPAException | InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }

    // ... and if one was found, also set its subtree as covered
    if(coverageRoot != null) {
      for(ARGState toCover : coverageRoot.getSubgraph()) {
        if(!toCover.isCovered()) {
          toCover.setCovered(coverageRoot);
        }
      }
    }
  }

  @Override
  public ValueAnalysisPrecision getGlobalPrecision() {
    return globalPrecision;
  }

  // debugging/stats only
  private void incrementUniqueTargetTraceCounter(MutableARGPath errorPath) {
    Integer hash = errorPath.toString().hashCode();

    if(!uniqueTargetTraceCounter.containsKey(hash)) {
      uniqueTargetTraceCounter.put(hash, 0);
    }
    uniqueTargetTraceCounter.put(hash, uniqueTargetTraceCounter.get(hash) + 1);
  }

  Timer timerItp = new Timer();
  Timer timerGlobalPrec = new Timer();
  Timer timerRemoveInfeasible = new Timer();
  Timer timerStrengthen = new Timer();
  Timer timerObtainPrecision = new Timer();
  Timer timerReaddToWaitlist = new Timer();
  Timer timerItpTree = new Timer();
  Timer timerErrors = new Timer();

  private void dumpArgToDot(final ReachedSet pReached, String currentPhase, Collection<Pair<ARGState, ARGState>> errorPaths) {
    if(exportInterpolationTree.equals("ALWAYS")) {
      try (Writer w = Files.openOutputFile(Paths.get(currentPhase + "_" + totalRefinements + ".dot"))) {
        //ARGUtils.writeARGAsDot(w, (ARGState)pReached.getFirstState(), Predicates.in(errorPaths));
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write ARG to file");
      }
    }
  }

  private void strengthenArg(InterpolationTree interpolationTree) {
    strengthendStates.clear();

    for (Map.Entry<ARGState, ValueAnalysisInterpolant> entry : interpolationTree.interpolants.entrySet()) {
      if (!entry.getValue().isTrivial()) {

        ARGState state                = entry.getKey();
        ValueAnalysisInterpolant itp  = entry.getValue();
        ValueAnalysisState valueState = AbstractStates.extractStateByType(state, ValueAnalysisState.class);

        if(itp.strengthen(valueState, state)) {
          strengthendStates.add(state);
        }
      }
    }
  }

  private void removeInfeasiblePartsOfArg(InterpolationTree interpolationTree, ARGReachedSet reached) {
    for (ARGState root : interpolationTree.obtainCutOffRoots()) {
      reached.cutOffSubtree(root);
    }
  }

  private void createGlobalPrecision(final ReachedSet pReached, InterpolationTree interpolationTree) {
    for (ARGState root : interpolationTree.obtainRefinementRoots()) {
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

  private boolean initialInterpolantIsTooWeak(ARGState root, ValueAnalysisInterpolant initialItp, MutableARGPath errorPath)
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

  private boolean isAnyPathFeasible(final ARGReachedSet pReached, final Collection<MutableARGPath> errorPaths)
      throws CPAException, InterruptedException {

    MutableARGPath feasiblePath = null;
    for(MutableARGPath currentPath : errorPaths) {
      if(isErrorPathFeasible(currentPath)) {
        feasiblePath = currentPath;
      }
    }

    // remove all other target states, so that only one is left (for CEX-checker)
    if(feasiblePath != null) {
      for(MutableARGPath others : errorPaths) {
        if(others != feasiblePath) {
          pReached.removeSubtree(others.getLast().getFirst());
        }
      }
      return true;
    }

    return false;
  }

  private boolean isErrorPathFeasible(final MutableARGPath errorPath)
      throws CPAException, InterruptedException {
    if(checker.isFeasible(errorPath)) {
      logger.log(Level.FINEST, "found a feasible cex - returning from refinement");

      return true;
    }

    return false;
  }

  private Collection<MutableARGPath> getErrorPaths(final Collection<ARGState> targetStates) {
    Set<MutableARGPath> errorPaths = new TreeSet<>(new Comparator<MutableARGPath>() {
      @Override
      public int compare(MutableARGPath path1, MutableARGPath path2) {
        if(path1.size() == path2.size()) {
          return 1;
        }

        else {
          return (path1.size() < path2.size()) ? -1 : 1;
        }
      }
    });

    for(ARGState target : targetStates) {
      MutableARGPath p = ARGUtils.getOneMutablePathTo(target);
      errorPaths.add(p);
    }

    return errorPaths;
  }

  private List<ARGState> getErrorStates(final ReachedSet pReached) {
    if(((ARGState)pReached.getLastState()).isTarget()) {
      return Lists.newArrayList(((ARGState)pReached.getLastState()));
    }

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
        return "ValueAnalysisImpactGlobalRefiner";
      }

      @Override
      public void printStatistics(final PrintStream pOut, final Result pResult, final ReachedSet pReached) {
        ValueAnalysisImpactGlobalRefiner.this.printStatistics(pOut, pResult, pReached);
      }
    });
  }

  private void printStatistics(final PrintStream out, final Result pResult, final ReachedSet pReached) {
    if (totalRefinements > 0) {
      out.println("Total number of refinements:      " + String.format(Locale.US, "%9d", totalRefinements));
      out.println("Total number of targets found:    " + String.format(Locale.US, "%9d", totalTargetsFound));
      out.println("Total time for global refinement:     " + totalTime);

      out.println("timerItp: " + timerItp);
      out.println("timerGlobalPrec: " + timerGlobalPrec);
      out.println("timerRemoveInfeasible: " + timerRemoveInfeasible);
      out.println("timerStrengthen: " + timerStrengthen);
      out.println("timerObtainPrecision: " + timerObtainPrecision);
      out.println("timerReaddToWaitlist: " + timerReaddToWaitlist);

      out.println("timerItpTree: " + timerItpTree);
      out.println("timerErrors: " + timerErrors);

      out.println("numberOfUniqueTargets: " + uniqueTargetTraceCounter.size());

      int max = 0;
      for(Integer i : uniqueTargetTraceCounter.values()) {
        max = Math.max(max, i);
      }
      out.println("MaxNumberOfIdenticalPaths: " + max);

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

    public Collection<Pair<ARGState, ARGState>> getErrorPathEdges() {
      Set<Pair<ARGState, ARGState>> edges = new HashSet<>();

      for(Map.Entry<ARGState, ARGState> entry : successorRelation.entries()) {
        edges.add(Pair.<ARGState, ARGState>getPairFomMapEntry().apply(entry));
      }

      return edges;
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

          sb.append(interpolants.get(current.getKey()));

          result.append(current.getKey().getStateId() + " [label=\"" + (current.getKey().getStateId() + " / " + AbstractStates.extractLocation(current.getKey())) + " has itp " + (sb.toString()) + "\"]" + "\n");
          result.append(current.getKey().getStateId() + " -> " + current.getValue().getStateId() + " [label=\"" + current.getKey().getEdgeToChild(current.getValue()).getRawStatement().replace("\n", "").replace("\"", "'") + "\"]\n");
        }

        else {
          result.append(current.getKey().getStateId() + " [label=\"" + current.getKey().getStateId() + " has itp NA\"]" + "\n");
          result.append(current.getKey().getStateId() + " -> " + current.getValue().getStateId() + " [label=\"" + current.getKey().getEdgeToChild(current.getValue()).getRawStatement().replace("\n", "").replace("\"", "'") + "\"]\n");
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
    private MutableARGPath getNextPathForInterpolation() {
      return strategy.getNextPathForInterpolation();
    }

    /**
     * This method returns the interpolant to be used for interpolation of the given path.
     *
     * @param errorPath the path for which to obtain the initial interpolant
     * @return the initial interpolant for the given path
     */
    private ValueAnalysisInterpolant getInitialInterpolantForPath(MutableARGPath errorPath) {
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
        }

        else {
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

    private Collection<ARGState> obtainRefinementRoots() {
      Collection<ARGState> refinementRoots = new HashSet<>();

      Deque<ARGState> todo = new ArrayDeque<>(Collections.singleton(root));
      while (!todo.isEmpty()) {
        final ARGState currentState = todo.removeFirst();

        if (isNonTrivialInterpolantAvailable(currentState)) {
          refinementRoots.add(currentState);
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

        if (isFalseInterpolantAvailable(currentState)) {
          refinementRoots.add(currentState);
          continue;

        }

        Set<ARGState> successors = successorRelation.get(currentState);
        todo.addAll(successors);
      }

      return refinementRoots;
    }

    private boolean isFalseInterpolantAvailable(final ARGState currentState) {
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

      public MutableARGPath getNextPathForInterpolation();

      public boolean hasNextPathForInterpolation();

      public ValueAnalysisInterpolant getInitialInterpolantForRoot(ARGState root);
    }

    private class TopDownInterpolationStrategy implements InterpolationStrategy {

      /**
       * the states that are the sources for obtaining (partial) error paths
       */
      private Deque<ARGState> sources = new ArrayDeque<>(Collections.singleton(root));

      /**
       * a flag to distinguish the initial interpolation from subsequent ones
       */
      private boolean isInitialInterpolation = true;

      @Override
      public MutableARGPath getNextPathForInterpolation() {
        MutableARGPath errorPath = new MutableARGPath();

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
          assert isInitialInterpolation : "initial interpolant was null after initial interpolation!";
        }

        isInitialInterpolation = false;

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
      public MutableARGPath getNextPathForInterpolation() {
        ARGState current = sources.remove(0);

        assert current.isTarget() : "current element is not a target";

        MutableARGPath errorPath = new MutableARGPath();

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


