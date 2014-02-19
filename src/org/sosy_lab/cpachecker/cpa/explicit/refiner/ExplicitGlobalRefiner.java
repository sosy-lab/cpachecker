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
package org.sosy_lab.cpachecker.cpa.explicit.refiner;

import static com.google.common.collect.FluentIterable.from;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitCPA;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitPrecision;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitState.MemoryLocation;
import org.sosy_lab.cpachecker.cpa.explicit.refiner.ExplicitInterpolationBasedExplicitRefiner.ExplicitValueInterpolant;
import org.sosy_lab.cpachecker.cpa.explicit.refiner.utils.ExplictFeasibilityChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Precisions;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;

@Options(prefix="cpa.explicit.refiner")
public class ExplicitGlobalRefiner implements Refiner, StatisticsProvider {

  @Option(description="whether or not to do lazy-abstraction")
  private boolean doLazyAbstraction = true;

  @Option(description="checkForRepeatedRefinements")
  private boolean checkForRepeatedRefinements = true;

  @Option(description="whether or not to stop further interpolation once no new interpolants are identified "
      + "between two subsequent path interpolations")
  private boolean stopOnEmptyInterpolationIncrement = false;

  @Option(description="whether or not to stop further interpolation once no new refinement roots identified "
      + "between two subsequent path interpolations")
  private boolean stopOnIdenticalRefinementRoots = false;

  @Option(description="when to export the interpolation tree"
      + "\nNEVER:   never export the interpolation tree"
      + "\nFINAL:   export the interpolation tree once after each refinement"
      + "\nALWAYD:  export the interpolation tree once after each interpolation, i.e. multiple times per refinmenet",
      values={"NEVER", "FINAL", "ALWAYS"})
  private String exportInterpolationTree = "NEVER";

  ExplicitInterpolationBasedExplicitRefiner interpolatingRefiner;
  ExplictFeasibilityChecker checker;

  private final LogManager logger;

  // statistics
  private int totalRefinements        = 0;
  private int totalTargetsFound       = 0;

  private final Timer totalTime       = new Timer();

  public static ExplicitGlobalRefiner create(final ConfigurableProgramAnalysis pCpa) throws InvalidConfigurationException {
    final ExplicitCPA explicitCpa = CPAs.retrieveCPA(pCpa, ExplicitCPA.class);
    if (explicitCpa == null) {
      throw new InvalidConfigurationException(ExplicitGlobalRefiner.class.getSimpleName() + " needs a ExplicitCPA");
    }

    ExplicitGlobalRefiner refiner = new ExplicitGlobalRefiner(explicitCpa.getConfiguration(),
                                    explicitCpa.getLogger(),
                                    (ARGCPA)pCpa,
                                    explicitCpa.getShutdownNotifier(),
                                    explicitCpa.getCFA());

    explicitCpa.getStats().addRefiner(refiner);


    return refiner;
  }

  private ExplicitGlobalRefiner(final Configuration pConfig, final LogManager pLogger,
      final ARGCPA pArgCpa, final ShutdownNotifier pShutdownNotifier, final CFA pCfa)
          throws InvalidConfigurationException {

    pConfig.inject(this);

    logger                = pLogger;
    interpolatingRefiner  = new ExplicitInterpolationBasedExplicitRefiner(pConfig, pLogger, pShutdownNotifier, pCfa);
    checker               = new ExplictFeasibilityChecker(pLogger, pCfa);
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
      totalTime.stop();
      return false;
    }

    InterpolationTree itpTree = new InterpolationTree(logger, targets);

    Deque<ARGState> interpolationRoots = new ArrayDeque<>(Collections.singleton(((ARGState)pReached.getFirstState())));

    int i = 0;
    Set<ARGState> refinementRoots = new HashSet<>();
    while(!interpolationRoots.isEmpty()) {
      i++;
      ARGState currentRoot = interpolationRoots.pop();

      logger.log(Level.FINEST, "taking new root ", currentRoot.getStateId(), " from stack");

      if(!itpTree.isValidInterpolationRoot(currentRoot)) {
        logger.log(Level.FINEST, "itp of predecessor ", currentRoot.getStateId(), " is already false ... go ahead");
        continue;
      }

      ARGPath errorPath = itpTree.getNextErrorPath(currentRoot, interpolationRoots);

      ExplicitValueInterpolant initialItp = itpTree.getInitialItp(errorPath.getFirst().getFirst());
      if(initialItp == null) {
        initialItp = ExplicitValueInterpolant.createInitial();
        assert i == 1 : "initial interpolant was null after initial iteration!";
      }

      logger.log(Level.FINEST, "perform itp, starting at ", errorPath.getFirst().getFirst().getStateId(), ", using itp ", initialItp);

      if(initialItp.isFalse()) {
        logger.log(Level.FINEST, "itp is false, skipping");
        continue;
      }

      boolean newInterpolantAdded = itpTree.addInterpolants(interpolatingRefiner.performInterpolation(errorPath, initialItp));

      if(exportInterpolationTree.equals("ALWAYS")) {
        itpTree.exportToDot(totalRefinements, i);
      }

      if(stopOnEmptyInterpolationIncrement && !newInterpolantAdded) {
        break;
      }

      boolean newRootFound = false;
      for(ARGState root : itpTree.obtainRefinementRoots(doLazyAbstraction)) {
        newRootFound = newRootFound || refinementRoots.add(root);
      }
      if(stopOnIdenticalRefinementRoots && !newRootFound) {
        break;
      }

      logger.log(Level.FINEST, "itp done");
    }

    if(exportInterpolationTree.equals("FINAL") && !exportInterpolationTree.equals("ALWAYS")) {
      itpTree.exportToDot(totalRefinements, i);
    }

    ARGReachedSet reached = new ARGReachedSet(pReached);

    for(ARGState root : itpTree.obtainRefinementRoots(doLazyAbstraction)) {
      Collection<ARGState> targetsReachableFromRoot = itpTree.getTargetsInSubtree(root);

      // get precision of first target, and make it initial one
      final ExplicitPrecision inital = Precisions.extractPrecisionByType(pReached.getPrecision(targetsReachableFromRoot.iterator().next()), ExplicitPrecision.class);
      for(ARGState target : targetsReachableFromRoot) {
        ExplicitPrecision precisionOfTarget = Precisions.extractPrecisionByType(pReached.getPrecision(target), ExplicitPrecision.class);

        // join precision of target state it with the original one
        inital.getRefinablePrecision().join(precisionOfTarget.getRefinablePrecision());
      }

      final ExplicitPrecision refinedPrecision = new ExplicitPrecision(inital, itpTree.extractPrecisionIncrement(root));

      // replace the refinement root with its first child, if the refinement root equals the root of the ARG
      if(root == pReached.getFirstState()) {
        root = root.getChildren().iterator().next();
      }
      reached.removeSubtree(root, refinedPrecision, ExplicitPrecision.class);
    }

    totalTime.stop();
    return true;
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

  private int previousRefinementId = -1;

  private boolean isRepeatedRefinementRoot(final ARGState root) {
    final int currentRefinementId = AbstractStates.extractLocation(root).getLineNumber();
    final boolean result          = (previousRefinementId == currentRefinementId);
    previousRefinementId          = currentRefinementId;

    return result && checkForRepeatedRefinements;
  }

  @Override
  public void collectStatistics(final Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new Statistics() {

      @Override
      public String getName() {
        return "ExplicitGlobalRefiner";
      }

      @Override
      public void printStatistics(final PrintStream pOut, final Result pResult, final ReachedSet pReached) {
        ExplicitGlobalRefiner.this.printStatistics(pOut, pResult, pReached);
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
    private final Map<ARGState, ExplicitValueInterpolant> interpolants = new HashMap<>();

    /**
     * the root of the tree
     */
    private final ARGState root;

    /**
     * This method acts as constructor of the interpolation tree.
     *
     * @param targetStates the set of target states from which to build the interpolation tree
     */
    private InterpolationTree(final LogManager pLogger, final Collection<ARGState> targetStates) {
      logger  = pLogger;
      root    = buildTree(targetStates);
    }

    /**
     * This method creates the successor and predecessor relations, which make up the interpolation tree,
     * from the target states given as input.
     *
     * @param targetStates the target states to build the tree from
     * @return the root of the tree
     */
    private ARGState buildTree(Collection<ARGState> targetStates) {
      Deque<ARGState> todo = new ArrayDeque<>(targetStates);
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
     * This method checks if the given state is a valid interpolation root, i.e., if the interpolant
     * associated with the given state, if any, is not false.
     *
     * @param state the state for which to perofrm the check
     * @return true, if the interpolant associated with the state, if any, is valid, i.e, is not false
     */
    private boolean isValidInterpolationRoot(ARGState state) {
      ARGState predecessor = predecessorRelation.get(state);

      if(!interpolants.containsKey(predecessor)) {
        return true;
      }

      if(!interpolants.get(predecessor).isFalse()) {
        return true;
      }

      return false;
    }

    /**
     * This method returns the next error path for interpolation.
     *
     * @param current the current root of the error path to retrieve for a subsequent interpolation
     * @param interpolationRoots the mutable stack of interpolation roots, which might be added to within this method
     * @return the next error path for a subsequent interpolation
     */
    private ARGPath getNextErrorPath(ARGState current, Deque<ARGState> interpolationRoots) {
      ARGPath errorPath = new ARGPath();

      while(successorRelation.get(current).iterator().hasNext()) {
        Iterator<ARGState> children = successorRelation.get(current).iterator();
        ARGState child = children.next();
        errorPath.add(Pair.of(current, current.getEdgeToChild(child)));

        if(children.hasNext()) {
          ARGState sibling = children.next();
          logger.log(Level.FINEST, "\tpush new root ", sibling.getStateId(), " onto stack for parent ", predecessorRelation.get(sibling).getStateId());
          interpolationRoots.push(sibling);
        }

        current = child;

        // add out-going edges of final state, too (just for compatiblity reasons to compare to DelegatingRefiner)
        if(!successorRelation.get(current).iterator().hasNext()) {
          errorPath.add(Pair.of(current, CFAUtils.leavingEdges(AbstractStates.extractLocation(current)).first().orNull()));
        }
      }

      return errorPath;
    }

    /**
     * This method returns a CFA edge from the current state to its next successor.
     * TODO: always picks first, no good!
     *
     * @param state the state for which to obtain an edge to its successor.
     * @return the edge to the successor
     */
    private CFAEdge getEdgeToSuccessor(ARGState state) {
      return state.getEdgeToChild(successorRelation.get(state).iterator().next());
    }

    /**
     * This method returns the interpolant to be used for interpolation starting at the given state.
     *
     * @param root the state for which to obtain the initial interpolant
     * @return the initial interpolant for the given state
     */
    private ExplicitValueInterpolant getInitialItp(ARGState root) {
      return interpolants.get(predecessorRelation.get(root));
    }

    /**
     * This method updates the mapping from states to interpolants.
     *
     * @param newItps the new mapping to add
     */
    private boolean addInterpolants(Map<ARGState, ExplicitValueInterpolant> newItps) {
      boolean result = interpolants.values().containsAll(newItps.values());

      interpolants.putAll(newItps);

      return result;
    }

    /**
     * This method extracts the precision increment for the given refinement root.
     * It does so by collection all non-trivial interpolants in the subtree of the given refinement root.
     *
     * @return the precision increment for the given refinement root
     */
    private Multimap<CFANode, MemoryLocation> extractPrecisionIncrement(ARGState refinmentRoot) {
      Multimap<CFANode, MemoryLocation> increment = HashMultimap.create();

      Deque<ARGState> todo = new ArrayDeque<>(Collections.singleton(refinmentRoot));
      while (!todo.isEmpty()) {
        final ARGState currentState = todo.removeFirst();

        if (isNonTrivialInterpolantAvailable(currentState) && !currentState.isTarget()) {
          ExplicitValueInterpolant itp = interpolants.get(currentState);
          for (MemoryLocation memoryLocation : itp.getMemoryLocations()) {
            increment.put(getEdgeToSuccessor(currentState).getSuccessor(), memoryLocation);
          }
        }

        Set<ARGState> successors = successorRelation.get(currentState);
        todo.addAll(successors);
      }

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
    private Collection<ARGState> obtainRefinementRoots(boolean doLazyAbstraction) {
      if (!doLazyAbstraction) {
        return new HashSet<>(Collections.singleton(root));
      }

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
  }
}
