/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;

import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.ARGPathBuilder;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGInterpolant.SMGPrecisionIncrement;
import org.sosy_lab.cpachecker.util.AbstractStates;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class SMGInterpolationTree {

  /**
   * the logger in use
   */
  private final LogManager logger;

  /**
   * the counter to count interpolation queries
   */
  private int interpolationCounter = 0;

  /**
   * the strategy on how to select paths for interpolation
   */
  private final SMGInterpolationStrategy strategy;

  /**
   * the root of the tree
   */
  private final ARGState root;

  /**
   * the predecessor relation of the states contained in this tree
   */
  private final Map<ARGState, ARGState> predecessorRelation = Maps.newLinkedHashMap();

  /**
   * the successor relation of the states contained in this tree
   */
  private final ListMultimap<ARGState, ARGState> successorRelation = ArrayListMultimap.create();

  /**
   * the mapping from state to the identified interpolants
   *
   */
  private final Map<ARGState, SMGInterpolant> interpolants = new HashMap<>();

  private final SMGInterpolantManager interpolantManager;

  /**
   * the path denoting the empty path
   */
  public static final ARGPath EMPTY_PATH = null;

  public SMGInterpolationTree(SMGInterpolantManager pInterpolantManager,
      final LogManager pLogger, final List<ARGPath> pTargetPaths,
      final boolean useTopDownInterpolationStrategy) {
    logger = pLogger;
    interpolantManager = pInterpolantManager;
    root = build(pTargetPaths);

    if (useTopDownInterpolationStrategy) {
      strategy = new TopDownInterpolationStrategy();
    } else {
      strategy = new BottomUpInterpolationStrategy(extractTargets(pTargetPaths));
    }
  }

  /**
   * This method creates the successor and predecessor relations using the target paths.
   *
   * @return the root of the tree
   */
  private ARGState build(final Collection<ARGPath> targetPaths) {
    return (targetPaths.size() == 1)
        ? buildTreeFromSinglePath(Iterables.getOnlyElement(targetPaths))
        : buildTreeFromMultiplePaths(targetPaths);
  }

  /**
   * This method builds a (linear) tree from a single path.
   */
  private ARGState buildTreeFromSinglePath(final ARGPath targetPath) {
    ImmutableList<ARGState> states = targetPath.asStatesList();

    for (int i = 0; i < states.size() - 1; i++) {
      ARGState predecessorState = states.get(i);

      ARGState successorState = states.get(i + 1);
      predecessorRelation.put(successorState, predecessorState);
      successorRelation.put(predecessorState, successorState);
    }

    return states.get(0);
  }

  /**
   * This method builds an actual tree from multiple path.
   */
  private ARGState buildTreeFromMultiplePaths(final Collection<ARGPath> targetPaths) {
    ARGState itpTreeRoot = null;
    Deque<ARGState> todo = new ArrayDeque<>(extractTargets(targetPaths));

    // build the tree, bottom-up, starting from the target states
    while (!todo.isEmpty()) {
      final ARGState currentState = todo.removeFirst();

      if (currentState.getParents().iterator().hasNext()) {

        if(!predecessorRelation.containsKey(currentState)) {
          ARGState parentState = currentState.getParents().iterator().next();

          predecessorRelation.put(currentState, parentState);
          successorRelation.put(parentState, currentState);

          todo.addFirst(parentState);
        }

      } else if (itpTreeRoot == null) {
        itpTreeRoot = currentState;
      }
    }

    return itpTreeRoot;
  }

  /**
   * This method extracts all targets states from the target paths.
   */
  private Set<ARGState> extractTargets(final Collection<ARGPath> targetsPaths) {
    return FluentIterable.from(targetsPaths).transform(new Function<ARGPath, ARGState>() {
      @Override
      public ARGState apply(ARGPath targetsPath) {
        return targetsPath.getLastState();
      }}).toSet();
  }

  public ARGState getRoot() {
    return root;
  }

  /**
   * This method obtains the refinement roots, i.e., for each disjunct path from target states
   * to the root, it collects the highest state that has a non-trivial interpolant associated.
   *
   * @return the set of refinement roots
   */
  public Collection<ARGState> obtainRefinementRoots() {

    Collection<ARGState> refinementRoots = new HashSet<>();

    Deque<ARGState> todo = new ArrayDeque<>(Collections.singleton(root));
    while (!todo.isEmpty()) {
      final ARGState currentState = todo.removeFirst();

      if (stateHasNonTrivialInterpolant(currentState)) {
        refinementRoots.add(currentState);
        continue;
      }

      Collection<ARGState> successors = successorRelation.get(currentState);
      todo.addAll(successors);
    }

    return refinementRoots;
  }

  /**
   * This method checks if for the given state a non-trivial interpolant is present.
   *
   * @param currentState the state for which to check
   * @return true, if a non-trivial interpolant is present, else false
   */
  private boolean stateHasNonTrivialInterpolant(final ARGState currentState) {
    return interpolants.containsKey(currentState)
        && !interpolants.get(currentState).isTrivial();
  }

  /**
   * This method updates the mapping from states to interpolants.
   *
   * @param interpolantsToAdd the new mapping to add
   */
  public void addInterpolants(Map<ARGState, SMGInterpolant> interpolantsToAdd) {
    for (Map.Entry<ARGState, SMGInterpolant> entry : interpolantsToAdd.entrySet()) {
      ARGState state = entry.getKey();
      SMGInterpolant itp = entry.getValue();

      if (interpolants.containsKey(state)) {
        interpolants.put(state, interpolants.get(state).join(itp));
      } else {
        interpolants.put(state, itp);
      }
    }
  }

  /**
   * This method checks if for the given state a false interpolant is present.
   *
   * @param currentState the state for which to check
   * @return true, if a false interpolant is present, else false
   */
  private boolean stateHasFalseInterpolant(final ARGState currentState) {
    return interpolants.containsKey(currentState)
        && interpolants.get(currentState).isFalse();
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
   * This method returns the next error path for interpolation.
   *
   * @return the next error path for a subsequent interpolation
   */
  public ARGPath getNextPathForInterpolation() {
    return strategy.getNextPathForInterpolation();
  }

  /**
   * This method returns the interpolant to be used for interpolation of the given path.
   *
   * @param errorPath the path for which to obtain the initial interpolant
   * @return the initial interpolant for the given path
   */
  public SMGInterpolant getInitialInterpolantForPath(ARGPath errorPath) {
    return strategy.getInitialInterpolantForRoot(errorPath.getFirstState());
  }

  /**
   * This method extracts the precision increment for the given refinement root.
   * It does so by collection all non-trivial interpolants in the subtree of the given refinement root.
   *
   * @return the precision increment for the given refinement root
   */
  public Map<CFANode, SMGPrecisionIncrement> extractPrecisionIncrement(ARGState pRefinementRoot) {
    Map<CFANode, SMGPrecisionIncrement> increment = new HashMap<>();

    Deque<ARGState> todo =
        new ArrayDeque<>(Collections.singleton(predecessorRelation.get(pRefinementRoot)));

    while (!todo.isEmpty()) {
      final ARGState currentState = todo.removeFirst();

      if (stateHasNonTrivialInterpolant(currentState) && !currentState.isTarget()) {
        SMGInterpolant itp = interpolants.get(currentState);
        SMGPrecisionIncrement inc = itp.getPrecisionIncrement();
        CFANode loc = AbstractStates.extractLocation(currentState);

        if (increment.containsKey(loc)) {
          SMGPrecisionIncrement inc2 = increment.get(loc);
          SMGPrecisionIncrement joinInc = inc.join(inc2);
          increment.put(loc, joinInc);
        } else {
          increment.put(loc, inc);
        }
      }

      if (!stateHasFalseInterpolant(currentState)) {
        todo.addAll(successorRelation.get(currentState));
      }
    }

    return increment;
  }

  private interface SMGInterpolationStrategy {

    ARGPath getNextPathForInterpolation();

    boolean hasNextPathForInterpolation();

    SMGInterpolant getInitialInterpolantForRoot(ARGState root);
  }

  private class TopDownInterpolationStrategy implements SMGInterpolationStrategy {

    /**
     * the states that are the sources for obtaining (partial) error paths
     */
    private Deque<ARGState> sources = new ArrayDeque<>(Collections.singleton(root));

    @Override
    public ARGPath getNextPathForInterpolation() {
      ARGPathBuilder errorPathBuilder = ARGPath.builder();

      ARGState current = sources.pop();

      if (!isValidInterpolationRoot(predecessorRelation.get(current))) {
        logger.log(Level.FINEST, "interpolant of predecessor of ", current.getStateId(), " is already false, so return empty path");
        return EMPTY_PATH;
      }

      // if the current state is not the root, it is a child of a branch , however, the path should not start with the
      // child, but with the branching node (children are stored on the stack because this needs less book-keeping)
      if (current != root) {
        errorPathBuilder.add(predecessorRelation.get(current), predecessorRelation.get(current).getEdgeToChild(current));
      }

      while (successorRelation.get(current).iterator().hasNext()) {
        Iterator<ARGState> children = successorRelation.get(current).iterator();
        ARGState child = children.next();
        errorPathBuilder.add(current, current.getEdgeToChild(child));

        // push all other children of the current state, if any, onto the stack for later interpolations
        int size = 1;
        while (children.hasNext()) {
          size++;
          ARGState sibling = children.next();
          logger.log(Level.FINEST, "\tpush new root ", sibling.getStateId(), " onto stack for parent ", predecessorRelation.get(sibling).getStateId());
          sources.push(sibling);
        }
        assert(size <= 2);

        current = child;
      }

      return errorPathBuilder.build(current);
    }

    /**
     * The given state is not a valid interpolation root if it is associated with a interpolant representing "false"
     */
    private boolean isValidInterpolationRoot(ARGState root) {
      return !stateHasFalseInterpolant(root);
    }

    @Override
    public SMGInterpolant getInitialInterpolantForRoot(ARGState root) {
      SMGInterpolant initialInterpolant = interpolants.get(root);

      if (initialInterpolant == null) {
        initialInterpolant = interpolantManager.createInitialInterpolant();

        assert (interpolants.size() == 0) : "initial interpolant was null after initial interpolation!";
      }

      return initialInterpolant;
    }

    @Override
    public boolean hasNextPathForInterpolation() {
      return !sources.isEmpty();
    }
  }

  private class BottomUpInterpolationStrategy implements SMGInterpolationStrategy {

    /**
     * the states that are the sources for obtaining error paths
     */
    private List<ARGState> sources;

    public BottomUpInterpolationStrategy(Set<ARGState> pTargets) {
      sources = new ArrayList<>(pTargets);
    }

    @Override
    public ARGPath getNextPathForInterpolation() {
      ARGState current = sources.remove(0);

      assert current.isTarget() : "current element is not a target";

      ARGPathBuilder errorPathBuilder = ARGPath.reverseBuilder();

      errorPathBuilder.add(current, FluentIterable.from(AbstractStates.getOutgoingEdges(current)).first().orNull());

      while (predecessorRelation.get(current) != null) {

        ARGState parent = predecessorRelation.get(current);

        if(stateHasFalseInterpolant(parent)) {
          logger.log(Level.FINEST, "interpolant on path, namely for state ", parent.getStateId(), " is already false, so return empty path");
          return EMPTY_PATH;
        }

        if (predecessorRelation.get(parent) != null) {
          errorPathBuilder.add(parent, parent.getEdgeToChild(current));
        }

        current = parent;
      }

      return errorPathBuilder.build(current);
    }

    @Override
    public SMGInterpolant getInitialInterpolantForRoot(ARGState root) {
      return interpolantManager.createInitialInterpolant();
    }

    @Override
    public boolean hasNextPathForInterpolation() {
      return !sources.isEmpty();
    }
  }

  /**
   * This method exports the current representation in dot format to the given file.
   *
   * @param file file the file to write to
   */
  public void exportToDot(PathTemplate file, long refinementCounter) {
    StringBuilder result = new StringBuilder().append("digraph tree {" + "\n");
    for (Map.Entry<ARGState, ARGState> current : successorRelation.entries()) {
      if (interpolants.containsKey(current.getKey())) {
        StringBuilder sb = new StringBuilder();

        sb.append("itp is " + interpolants.get(current.getKey()));

        result.append(current.getKey().getStateId() + " [label=\"" + (current.getKey().getStateId() + " / " + AbstractStates.extractLocation(current.getKey())) + " has itp " + (sb.toString()) + "\"]" + "\n");
        result.append(current.getKey().getStateId() + " -> " + current.getValue().getStateId() + "\n");

      } else {
        result.append(current.getKey().getStateId() + " [label=\"" + current.getKey().getStateId() + " has itp NA\"]" + "\n");
        result.append(current.getKey().getStateId() + " -> " + current.getValue().getStateId() + "\n");
      }

      if (current.getValue().isTarget()) {
        result.append(current.getValue().getStateId() + " [style=filled, fillcolor=\"red\"]" + "\n");
      }

      assert (!current.getKey().isTarget());
    }
    result.append("}");

    try {
      MoreFiles.writeFile(
          file.getPath(refinementCounter, interpolationCounter), Charset.defaultCharset(), result);
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write interpolation tree to file");
    }
  }
}