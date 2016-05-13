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

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.ARGPathBuilder;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * This class represents an interpolation tree, i.e. a set of states connected through a successor-predecessor-relation.
 * The tree is built from traversing backwards from error states. It can be used to retrieve paths from the root of the
 * tree to error states, in a way, that only path not yet excluded by previous path interpolation need to be interpolated.
 */
public class InterpolationTree<S extends AbstractState, I extends Interpolant<S>> {
  /**
   * the logger in use
   */
  private final LogManager logger;

  /**
   * the counter to count interpolation queries
   */
  private int interpolationCounter = 0;

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
   * this has to be a linked hash map, because the ImpactRefiner has to iterate over this in insertion-order
   */
  private final Map<ARGState, I> interpolants = new LinkedHashMap<>();

  /**
   * the root of the tree
   */
  private final ARGState root;

  /**
   * the strategy on how to select paths for interpolation
   */
  private final InterpolationStrategy<I> strategy;

  private final InterpolantManager<S, I> interpolantManager;

  /**
   * the path denoting the empty path
   */
  public static final ARGPath EMPTY_PATH = null;

  /**
   * This method acts as constructor of the interpolation tree.
   *
   * @param pInterpolantManager the interpolant manager for creating interpolants
   * @param pLogger the logger to use
   * @param pTargetPaths the set of target paths from which to build the interpolation tree
   * @param useTopDownInterpolationStrategy the flag to choose the strategy to apply
   */
  public InterpolationTree(
      final InterpolantManager<S, I> pInterpolantManager,
      final LogManager pLogger, final List<ARGPath> pTargetPaths,
      final boolean useTopDownInterpolationStrategy) {
    logger    = pLogger;
    interpolantManager = pInterpolantManager;

    root      = build(pTargetPaths);

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
   *
   * Note that, while this is just a special case of {@link #buildTreeFromMultiplePaths},
   * this is the preferred way, because the given path could come from any analysis,
   * e.g., a predicate analysis, and the exact given path should be used for interpolation.
   * This is not guaranteed by the more general approach given in {@link #buildTreeFromMultiplePaths},
   * because there the interpolation tree is build from a (non-unambiguous) set of states.
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
    return FluentIterable.from(targetsPaths).transform(ARGPath::getLastState).toSet();
  }

  public ARGState getRoot() {
    return root;
  }

  /**
   * This method decides whether or not there are more paths left for interpolation.
   *
   * @return true if there are more paths left for interpolation, else false
   */
  public boolean hasNextPathForInterpolation() {
    interpolationCounter++;
    return strategy.hasNextPathForInterpolation();
  }

  /**
   * This method exports the current representation in dot format to the given file.
   *
   * @param file file the file to write to
   */
  public void exportToDot(PathTemplate file, int refinementCounter) {
    StringBuilder result = new StringBuilder().append("digraph tree {" + "\n");
    for (Map.Entry<ARGState, ARGState> current : successorRelation.entries()) {
      if (interpolants.containsKey(current.getKey())) {
        StringBuilder sb = new StringBuilder();

        sb.append("itp is " + interpolants.get(current.getKey()));

        result.append(current.getKey().getStateId() + " [label=\"" + (current.getKey().getStateId() + " / " + AbstractStates.extractLocation(current.getKey())) + " has itp " + (sb.toString()) + "\"]" + "\n");
        result.append(current.getKey().getStateId() + " -> " + current.getValue().getStateId() + "\n");// + " [label=\"" + current.getKey().getEdgeToChild(current.getValue()).getRawStatement().replace("\n", "") + "\"]\n");

      } else {
        result.append(current.getKey().getStateId() + " [label=\"" + current.getKey().getStateId() + " has itp NA\"]" + "\n");
        result.append(current.getKey().getStateId() + " -> " + current.getValue().getStateId() + "\n");// + " [label=\"" + current.getKey().getEdgeToChild(current.getValue()).getRawStatement().replace("\n", "") + "\"]\n");
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
  public I getInitialInterpolantForPath(ARGPath errorPath) {
    return strategy.getInitialInterpolantForRoot(errorPath.getFirstState());
  }

  /**
   * This method updates the mapping from states to interpolants.
   *
   * @param interpolantsToAdd the new mapping to add
   */
  public void addInterpolants(Map<ARGState, I> interpolantsToAdd) {
    for (Map.Entry<ARGState, I> entry : interpolantsToAdd.entrySet()) {
      ARGState state = entry.getKey();
      I itp = entry.getValue();

      if (interpolants.containsKey(state)) {
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
  public Multimap<CFANode, MemoryLocation> extractPrecisionIncrement(ARGState pRefinementRoot) {
    Multimap<CFANode, MemoryLocation> increment = HashMultimap.create();

    Deque<ARGState> todo =
        new ArrayDeque<>(Collections.singleton(predecessorRelation.get(pRefinementRoot)));

    while (!todo.isEmpty()) {
      final ARGState currentState = todo.removeFirst();

      if (stateHasNonTrivialInterpolant(currentState) && !currentState.isTarget()) {
        I itp = interpolants.get(currentState);
        for (MemoryLocation memoryLocation : itp.getMemoryLocations()) {
          increment.put(AbstractStates.extractLocation(currentState), memoryLocation);
        }
      }

      if (!stateHasFalseInterpolant(currentState)) {
        todo.addAll(successorRelation.get(currentState));
      }
    }

    return increment;
  }

  /**
   * This method obtains the refinement roots, i.e., for each disjunct path from target states
   * to the root, it collects the highest state that has a non-trivial interpolant associated.
   * With non-lazy abstraction, the root of the interpolation tree is used as refinement root.
   *
   * @param strategy whether to perform lazy abstraction or not
   * @return the set of refinement roots
   */
  public Collection<ARGState> obtainRefinementRoots(GenericRefiner.RestartStrategy strategy) {
    if (strategy == GenericRefiner.RestartStrategy.ROOT) {
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

      if (stateHasNonTrivialInterpolant(currentState)) {
        refinementRoots.add(currentState);

        if (strategy == GenericRefiner.RestartStrategy.COMMON && refinementRoots.size() > 2) {
          assert commonRoot != null: "common root not yet set";
          return new HashSet<>(Collections.singleton(commonRoot));
        }
        continue;
      }

      Collection<ARGState> successors = successorRelation.get(currentState);
      todo.addAll(successors);
    }

    return refinementRoots;
  }

  /**
   * This method obtains, for the IMPACT-like approach, the cut-off roots,
   * i.e., for each disjunct path from target states to the root, it collects
   * the highest state that has a false interpolant associated.
   *
   * @return the set of cut-off roots
   */
  public Collection<ARGState> obtainCutOffRoots() {
    Collection<ARGState> refinementRoots = new HashSet<>();

    Deque<ARGState> todo = new ArrayDeque<>(Collections.singleton(root));
    while (!todo.isEmpty()) {
      final ARGState currentState = todo.removeFirst();

      if (stateHasFalseInterpolant(currentState)) {
        refinementRoots.add(currentState);
        continue;

      }

      Collection<ARGState> successors = successorRelation.get(currentState);
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
  public Collection<ARGState> getTargetsInSubtree(ARGState state) {
    Collection<ARGState> targetStates = new HashSet<>();

    Deque<ARGState> todo = new ArrayDeque<>(Collections.singleton(state));
    while (!todo.isEmpty()) {
      final ARGState currentState = todo.removeFirst();

      if (currentState.isTarget()) {
        targetStates.add(currentState);
        continue;
      }

      Collection<ARGState> successors = successorRelation.get(currentState);
      todo.addAll(successors);
    }

    return targetStates;
  }

  /**
   * This method returns, from the subtree of the given state, the target states
   * that have an interpolant, i.e., that were interpolated.
   *
   * @param state the state for which to collect the interpolated target states in its subtree.
   * @return the target states that were interpolated
   */
  public Collection<ARGState> getInterpolatedTargetsInSubtree(ARGState state) {
    return from(getTargetsInSubtree(state)).filter(interpolants::containsKey).toSet();
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
   * This method checks if an interpolant is associated with the given state.
   *
   * @param currentState the state for which to check for associated interpolants
   * @return true, if an interpolant is associated with the given state
   */
  public boolean hasInterpolantForState(final ARGState currentState) {
    return interpolants.containsKey(currentState);
  }

  public Set<Map.Entry<ARGState, I>> getInterpolantMapping() {
    return interpolants.entrySet();
  }

  /**
   * This method returns, if any, the interpolant associated with the given state.
   *
   * @param currentState the state for which to return the associated interpolant
   * @return the interpolant associated with the given state
   */
  public I getInterpolantForState(final ARGState currentState) {
    return interpolants.get(currentState);
  }

  public Collection<ARGState> getSuccessors(final ARGState pState) {
    return successorRelation.get(pState);
  }

  public ARGState getPredecessor(final ARGState pState) {
    return predecessorRelation.get(pState);
  }

  private interface InterpolationStrategy<I extends Interpolant<?>> {

    ARGPath getNextPathForInterpolation();

    boolean hasNextPathForInterpolation();

    I getInitialInterpolantForRoot(ARGState root);
  }

  private class TopDownInterpolationStrategy implements InterpolationStrategy<I> {

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
    public I getInitialInterpolantForRoot(ARGState root) {
      I initialInterpolant = interpolants.get(root);

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

  private class BottomUpInterpolationStrategy implements InterpolationStrategy<I> {

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
    public I getInitialInterpolantForRoot(ARGState root) {
      return interpolantManager.createInitialInterpolant();
    }

    @Override
    public boolean hasNextPathForInterpolation() {
      return !sources.isEmpty();
    }
  }
}
