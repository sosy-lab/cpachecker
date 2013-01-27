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
package org.sosy_lab.cpachecker.cpa.arg;

import static com.google.common.base.Preconditions.*;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import java.util.AbstractCollection;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.AbstractStates;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.UnmodifiableIterator;

/**
 * Helper class with collection of ARG related utility methods.
 */
public class ARGUtils {

  private ARGUtils() { }

  /**
   * Get all elements on all paths from the ARG root to a given element.
   *
   * @param pLastElement The last element in the paths.
   * @return A set of elements, all of which have pLastElement as their (transitive) child.
   */
  public static Set<ARGState> getAllStatesOnPathsTo(ARGState pLastElement) {

    Set<ARGState> result = new HashSet<>();
    Deque<ARGState> waitList = new ArrayDeque<>();

    result.add(pLastElement);
    waitList.add(pLastElement);

    while (!waitList.isEmpty()) {
      ARGState currentElement = waitList.poll();
      for (ARGState parent : currentElement.getParents()) {
        if (result.add(parent)) {
          waitList.push(parent);
        }
      }
    }

    return result;
  }

  /**
   * Create a path in the ARG from root to the given element.
   * If there are several such paths, one is chosen randomly.
   *
   * @param pLastElement The last element in the path.
   * @return A path from root to lastElement.
   */
  public static ARGPath getOnePathTo(ARGState pLastElement) {
    ARGPath path = new ARGPath();
    Set<ARGState> seenElements = new HashSet<>();

    // each element of the path consists of the abstract state and the outgoing
    // edge to its successor

    ARGState currentARGState = pLastElement;
    // add the error node and its -first- outgoing edge
    // that edge is not important so we pick the first even
    // if there are more outgoing edges
    CFANode loc = extractLocation(currentARGState);
    CFAEdge lastEdge = null;
    if (loc.getNumLeavingEdges() > 0) {
      lastEdge = loc.getLeavingEdge(0);
    }
    path.addFirst(Pair.of(currentARGState, lastEdge));
    seenElements.add(currentARGState);

    while (!currentARGState.getParents().isEmpty()) {
      Iterator<ARGState> parents = currentARGState.getParents().iterator();

      ARGState parentElement = parents.next();
      while (!seenElements.add(parentElement) && parents.hasNext()) {
        // while seenElements already contained parentElement, try next parent
        parentElement = parents.next();
      }

      CFAEdge edge = parentElement.getEdgeToChild(currentARGState);
      path.addFirst(Pair.of(parentElement, edge));

      currentARGState = parentElement;
    }
    return path;
  }

  /**
   * Get the set of all elements covered by any of the given elements,
   * i.e., the union of calling {@link ARGState#getCoveredByThis()} on all
   * elements.
   *
   * However, elements in the given set are never in the returned set.
   * If you pass in a subtree, this will return exactly the set of covering
   * edges which enter the subtree.
   */
  public static Set<ARGState> getCoveredBy(Set<ARGState> elements) {
    Set<ARGState> result = new HashSet<>();
    for (ARGState element : elements) {
      result.addAll(element.getCoveredByThis());
    }

    result.removeAll(elements);
    return result;
  }


  static final Function<ARGState, Set<ARGState>> CHILDREN_OF_STATE = new Function<ARGState, Set<ARGState>>() {
        @Override
        public Set<ARGState> apply(ARGState pInput) {
          return pInput.getChildren();
        }
      };

  static final Function<ARGState, Set<ARGState>> PARENTS_OF_STATE = new Function<ARGState, Set<ARGState>>() {
        @Override
        public Set<ARGState> apply(ARGState pInput) {
          return pInput.getParents();
        }
      };

  private static final Predicate<AbstractState> AT_RELEVANT_LOCATION = Predicates.compose(
      new Predicate<CFANode>() {
        @Override
        public boolean apply(CFANode pInput) {
          return pInput.isLoopStart()
              || pInput instanceof FunctionEntryNode
              || pInput instanceof FunctionExitNode;
        }
      },
      AbstractStates.EXTRACT_LOCATION);

  static final Predicate<ARGState> RELEVANT_STATE = Predicates.or(
      AbstractStates.IS_TARGET_STATE,
      AT_RELEVANT_LOCATION
      );

  /**
   * Project the ARG to a subset of "relevant" states.
   * The result is a SetMultimap containing the successor relationships between all relevant states.
   * A pair of states (a, b) is in the SetMultimap,
   * if there is a path through the ARG from a to b which does not pass through
   * any other relevant state.
   *
   * To get the predecessor relationship, you can use {@link Multimaps#invertFrom()}.
   *
   * @param root The start of the subgraph of the ARG to project (always considered relevant).
   * @param isRelevant The predicate determining which states are in the resulting relationship.
   */
  static SetMultimap<ARGState, ARGState> projectARG(final ARGState root,
      final Function<? super ARGState, ? extends Iterable<ARGState>> successorFunction,
      Predicate<? super ARGState> isRelevant) {

    isRelevant = Predicates.or(Predicates.equalTo(root),
                               isRelevant);

    SetMultimap<ARGState, ARGState> successors = HashMultimap.create();

    // Our state is a stack of pairs of todo items.
    // The first item of each pair is a relevant state,
    // for which we are looking for relevant successor states.
    // The second item is a state,
    // whose children should be handled next.
    Deque<Pair<ARGState, ARGState>> todo = new ArrayDeque<>();
    Set<ARGState> visited = new HashSet<>();
    todo.push(Pair.of(root, root));

    while (!todo.isEmpty()) {
      final Pair<ARGState, ARGState> currentPair = todo.pop();
      final ARGState currentPredecessor = currentPair.getFirst();
      final ARGState currentState = currentPair.getSecond();

      if (!visited.add(currentState)) {
        continue;
      }

      for (ARGState child : successorFunction.apply(currentState)) {
        if (isRelevant.apply(child)) {
          successors.put(currentPredecessor, child);

          todo.push(Pair.of(child, child));

        } else {
          todo.push(Pair.of(currentPredecessor, child));
        }
      }
    }

    return successors;
  }

  /**
   * Find a path in the ARG. The necessary information to find the path is a
   * boolean value for each branching situation that indicates which of the two
   * AssumeEdges should be taken.
   *
   * @param root The root element of the ARG (where to start the path)
   * @param arg All elements in the ARG or a subset thereof (elements outside this set will be ignored).
   * @param branchingInformation A map from ARG state ids to boolean values indicating the outgoing direction.
   * @return A path through the ARG from root to target.
   * @throws IllegalArgumentException If the direction information doesn't match the ARG or the ARG is inconsistent.
   */
  public static ARGPath getPathFromBranchingInformation(
      ARGState root, Collection<? extends AbstractState> arg,
      Map<Integer, Boolean> branchingInformation) throws IllegalArgumentException {

    checkArgument(arg.contains(root));

    ARGPath result = new ARGPath();
    ARGState currentElement = root;
    while (!currentElement.isTarget()) {
      Set<ARGState> children = currentElement.getChildren();

      ARGState child;
      CFAEdge edge;
      switch (children.size()) {

      case 0:
        throw new IllegalArgumentException("ARG target path terminates without reaching target state!");

      case 1: // only one successor, easy
        child = Iterables.getOnlyElement(children);
        edge = currentElement.getEdgeToChild(child);
        break;

      case 2: // branch
        // first, find out the edges and the children
        CFAEdge trueEdge = null;
        CFAEdge falseEdge = null;
        ARGState trueChild = null;
        ARGState falseChild = null;

        for (ARGState currentChild : children) {
          CFAEdge currentEdge = currentElement.getEdgeToChild(currentChild);
          if (!(currentEdge instanceof CAssumeEdge)) {
            throw new IllegalArgumentException("ARG branches where there is no CAssumeEdge!");
          }

          if (((CAssumeEdge)currentEdge).getTruthAssumption()) {
            trueEdge = currentEdge;
            trueChild = currentChild;
          } else {
            falseEdge = currentEdge;
            falseChild = currentChild;
          }
        }
        if (trueEdge == null || falseEdge == null) {
          throw new IllegalArgumentException("ARG branches with non-complementary AssumeEdges!");
        }
        assert trueChild != null;
        assert falseChild != null;

        // search first idx where we have a predicate for the current branching
        Boolean predValue = branchingInformation.get(currentElement.getStateId());
        if (predValue == null) {
          throw new IllegalArgumentException("ARG branches without direction information!");
        }

        // now select the right edge
        if (predValue) {
          edge = trueEdge;
          child = trueChild;
        } else {
          edge = falseEdge;
          child = falseChild;
        }
        break;

      default:
        throw new IllegalArgumentException("ARG splits with more than two branches!");
      }

      if (!arg.contains(child)) {
        throw new IllegalArgumentException("ARG and direction information from solver disagree!");
      }

      result.add(Pair.of(currentElement, edge));
      currentElement = child;
    }


    // need to add another pair with target state and one (arbitrary) outgoing edge
    CFANode loc = extractLocation(currentElement);
    CFAEdge lastEdge = null;
    if (loc.getNumLeavingEdges() > 0) {
      lastEdge = loc.getLeavingEdge(0);
    }
    result.add(Pair.of(currentElement, lastEdge));

    return result;
  }

  /**
   * Find a path in the ARG. The necessary information to find the path is a
   * boolean value for each branching situation that indicates which of the two
   * AssumeEdges should be taken.
   * This method checks that the path ends in a certain element.
   *
   * @param root The root element of the ARG (where to start the path)
   * @param target The target state (where to end the path, needs to be a target state)
   * @param arg All elements in the ARG or a subset thereof (elements outside this set will be ignored).
   * @param branchingInformation A map from ARG state ids to boolean values indicating the outgoing direction.
   * @return A path through the ARG from root to target.
   * @throws IllegalArgumentException If the direction information doesn't match the ARG or the ARG is inconsistent.
   */
  public static ARGPath getPathFromBranchingInformation(
      ARGState root, ARGState target, Collection<? extends AbstractState> arg,
      Map<Integer, Boolean> branchingInformation) throws IllegalArgumentException {

    checkArgument(arg.contains(target));
    checkArgument(target.isTarget());

    ARGPath result = getPathFromBranchingInformation(root, arg, branchingInformation);

    if (result.getLast().getFirst() != target) {
      throw new IllegalArgumentException("ARG target path reached the wrong target state!");
    }

    return result;
  }

  /**
   * This method gets all children from an ARGState,
   * but replaces all covered states by their respective covering state.
   * It can be seen as giving a view of the ARG where the covered states are
   * transparently replaced by their covering state.
   *
   * The returned collection is unmodifiable and a live view of the children of
   * the given state.
   *
   * @param s an ARGState
   * @return The children with covered states transparently replaced.
   */
  public static final Collection<ARGState> getUncoveredChildrenView(final ARGState s) {
    return new AbstractCollection<ARGState>() {

      @Override
      public Iterator<ARGState> iterator() {

        return new UnmodifiableIterator<ARGState>() {
          private final Iterator<ARGState> children = s.getChildren().iterator();

          @Override
          public boolean hasNext() {
            return children.hasNext();
          }

          @Override
          public ARGState next() {
            ARGState child = children.next();
            if (child.isCovered()) {
              return checkNotNull(child.getCoveringState());
            }
            return child;
          }
        };
      }

      @Override
      public int size() {
        return s.getChildren().size();
      }
    };
  }

  public static boolean checkART(ReachedSet pReached) {

      Deque<AbstractState> workList = new ArrayDeque<>();
      Set<ARGState> arg = new HashSet<>();

      workList.add(pReached.getFirstState());
      while (!workList.isEmpty()) {
        ARGState currentElement = (ARGState)workList.removeFirst();
        assert !currentElement.isDestroyed();

        for (ARGState parent : currentElement.getParents()) {
          assert parent.getChildren().contains(currentElement) : "Reference from parent to child is missing in ARG";
        }
        for (ARGState child : currentElement.getChildren()) {
          assert child.getParents().contains(currentElement) : "Reference from child to parent is missing in ARG";
        }

        // check if (e \in ARG) => (e \in Reached || e.isCovered())
        if (currentElement.isCovered()) {
          // Assertion removed because now covered states are allowed to be in the reached set.
          // But they don't need to be!
  //        assert !pReached.contains(currentElement) : "Reached set contains covered element";

        } else {
          // There is a special case here:
          // If the element is the sibling of the target state, it might have not
          // been added to the reached set if CPAAlgorithm stopped before.
          // But in this case its parent is in the waitlist.

          assert pReached.contains(currentElement)
              || pReached.getWaitlist().containsAll(currentElement.getParents())
              : "Element in ARG but not in reached set";
        }

        if (arg.add(currentElement)) {
          workList.addAll(currentElement.getChildren());
        }
      }

      // check if (e \in Reached) => (e \in ARG)
      assert arg.containsAll(pReached.asCollection()) : "Element in reached set but not in ARG";

      return true;
    }
}
