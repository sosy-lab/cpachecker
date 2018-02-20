/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.bam;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Function;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * The subgraph computer is used to restore paths not to target states, but to any other.
 * The difficulty is to determine the outer block.
 *
 * One more feature of the computer is skipping such paths, which contains special (repeated) states.
 * The feature is extremely important for refinement optimization: we do not refine and even not compute the similar paths
 */

public class BAMMultipleCEXSubgraphComputer extends BAMSubgraphComputer{

  private final Multimap<AbstractState, AbstractState> reducedToExpanded;
  private Set<LinkedList<Integer>> remainingStates = new HashSet<>();
  private final Function<ARGState, Integer> getStateId;

  private Map<ARGState, BackwardARGState> previousForkForState = new IdentityHashMap<>();
  private Map<AbstractState, Iterator<ARGState>> toCallerStatesIterator = new HashMap<>();

  BAMMultipleCEXSubgraphComputer(BAMCPA bamCPA,
      Multimap<AbstractState, AbstractState> pReduced,
      Function<ARGState, Integer> idExtractor) {
    super(bamCPA);
    this.reducedToExpanded = pReduced;
    getStateId = idExtractor;
  }


  public ARGState findPath(BackwardARGState newTreeTarget, Set<List<Integer>> pProcessedStates) throws InterruptedException, MissingBlockException {

    Map<ARGState, BackwardARGState> elementsMap = new HashMap<>();
    final NavigableSet<ARGState> openElements = new TreeSet<>(); // for sorted IDs in ARGstates
    ARGState root = null;
    boolean inCallstackFunction = false;

    //Deep clone to be patient about modification
    remainingStates.clear();
    for (List<Integer> newList : pProcessedStates) {
      remainingStates.add(new LinkedList<>(newList));
    }

    ARGState target = newTreeTarget.getARGState();
    elementsMap.put(target, newTreeTarget);
    ARGState currentState = target;

    //Find path to nearest abstraction state
    PredicateAbstractState pState = AbstractStates.extractStateByType(currentState, PredicateAbstractState.class);
    if (pState != null) {
      assert (pState.isAbstractionState());
    }

    openElements.addAll(target.getParents());
    while (!openElements.isEmpty()) {
      currentState = openElements.pollLast();
      BackwardARGState newCurrentElement = new BackwardARGState(currentState);
      elementsMap.put(currentState, newCurrentElement);

      final Set<BackwardARGState> childrenInSubgraph = new TreeSet<>();
      for (final ARGState child : currentState.getChildren()) {
        // if a child is not in the subgraph, it does not lead to the target, so ignore it.
        // Because of the ordering, all important children should be finished already.
        if (elementsMap.containsKey(child)) {
          childrenInSubgraph.add(elementsMap.get(child));
        }
      }

      if (childrenInSubgraph.isEmpty()) {
        continue;
      }

      inCallstackFunction = false;
      if (currentState.getParents().isEmpty()) {
        //Find correct expanded state
        Collection<AbstractState> expandedStates = reducedToExpanded.get(currentState);

        if (expandedStates == null) {
          // children are a normal successors -> create an connection from parent to children
          for (final BackwardARGState newChild : childrenInSubgraph) {
            newChild.addParent(newCurrentElement);
            if (checkRepeatitionOfState(newChild)) {
              return DUMMY_STATE_FOR_REPEATED_STATE;
            }
          }

          //The first state
          root = newCurrentElement;
          break;
        }

        //Try to find path.
        //Exchange the reduced state by the expanded one
        currentState = (ARGState) expandedStates.iterator().next();
        newCurrentElement = new BackwardARGState(currentState);
        elementsMap.put(currentState, newCurrentElement);
        inCallstackFunction = true;
      }

      // add parent for further processing
      openElements.addAll(currentState.getParents());

      if (data.hasInitialState(currentState) && !inCallstackFunction) {

        // If child-state is an expanded state, the child is at the exit-location of a block.
        // In this case, we enter the block (backwards).
        // We must use a cached reachedSet to process further, because the block has its own reachedSet.
        // The returned 'innerTreeRoot' is the rootNode of the subtree, created from the cached reachedSet.
        // The current subtree (successors of child) is appended beyond the innerTree, to get a complete subgraph.
        computeCounterexampleSubgraphForBlock(newCurrentElement, childrenInSubgraph);
        assert childrenInSubgraph.size() == 1;
        BackwardARGState tmpState = childrenInSubgraph.iterator().next();
        //Check repetition of constructed states
        while (tmpState != newCurrentElement) {
          if (checkRepeatitionOfState(tmpState.getARGState())) {
            return DUMMY_STATE_FOR_REPEATED_STATE;
          }
          Collection<ARGState> parents = tmpState.getParents();
          assert parents.size() == 1;
          tmpState = (BackwardARGState) parents.iterator().next();
        }

      } else {
        // children are normal successors -> create an connection from parent to children
        for (final BackwardARGState newChild : childrenInSubgraph) {
          newChild.addParent(newCurrentElement);
          if (checkRepeatitionOfState(newChild)) {
            return DUMMY_STATE_FOR_REPEATED_STATE;
          }
        }

        if (currentState.getParents().isEmpty()) {
          //The first state
          root = newCurrentElement;
          break;
        }
      }
      if (currentState.isDestroyed()) {
        return null;
      }
    }
    assert root != null;
    return root;
  }

  private boolean checkRepeatitionOfState(ARGState currentElement) {
    int currentId = getStateId.apply(currentElement);
    for (LinkedList<Integer> rest : remainingStates) {
      if (rest.getLast() == currentId) {
        rest.removeLast();
        if (rest.isEmpty()) {
          return true;
        }
      }
    }
    return false;
  }

  /** This states is used for UsageStatisticsRefinement:
   *  If after some refinement iterations the path goes through already processed states,
   *  this marked state is returned.
   */
  public final static BackwardARGState DUMMY_STATE_FOR_REPEATED_STATE = new BackwardARGState(new ARGState(null, null));
  /**
   * This is a ARGState, that counts backwards, used to build the Pseudo-ARG for CEX-retrieval.
   * As the Pseudo-ARG is build backwards starting at its end-state, we count the ID backwards.
   */

  //lastAffectedState is Backward!
  private ARGPath computeNextPath(BackwardARGState lastAffectedState, Set<List<Integer>> pRefinedStates) {
    assert lastAffectedState != null;

    ARGState nextParent = null;
    BackwardARGState childOfForkState = null;
    ARGPath newPath = null;

    List<BackwardARGState> potentialForkStates = findNextForksInTail(lastAffectedState);
    if (potentialForkStates.isEmpty()) {
      return null;
    }
    Iterator<BackwardARGState> forkIterator = potentialForkStates.iterator();

    do {
      //Determine next branching point
      nextParent = null;
      while (nextParent == null && forkIterator.hasNext()) {
        //This is a backward state, which displays the following state after real reduced state, which we want to found
        childOfForkState = forkIterator.next();

        nextParent = findNextBranchingParent(childOfForkState);
      }

      if (nextParent == null) {
        return null;
      }

      BackwardARGState clonedNextParent = new BackwardARGState(nextParent);
      //Because of cached paths, we cannot change the part of it
      ARGState rootOfTheClonedPart = cloneTheRestOfPath(childOfForkState);
      rootOfTheClonedPart.addParent(clonedNextParent);
      //Restore the new path from branching point
      newPath = restorePathFrom(clonedNextParent, pRefinedStates);

    } while (newPath != null);

    return newPath;
  }

  private ARGState cloneTheRestOfPath(BackwardARGState pChildOfForkState) {
    BackwardARGState currentState = pChildOfForkState;
    ARGState originState = currentState.getARGState();
    BackwardARGState previousState = new BackwardARGState(originState), clonedState;
    ARGState root = previousState;

    while (!currentState.getChildren().isEmpty()) {
      assert currentState.getChildren().size() == 1;
      currentState = getNextStateOnPath(currentState);
      originState = currentState.getARGState();
      clonedState = new BackwardARGState(originState);
      clonedState.addParent(previousState);
      previousState = clonedState;
    }
    return root;
  }

  /** Finds the parentState (in ARG), which corresponds to the child (in the path)
   *
   * @param forkChildInPath child, which has more than one parents, which are not yet explored
   * @return found parent state
   */

  private ARGState findNextBranchingParent(BackwardARGState forkChildInPath) {

    ARGState forkChildInARG = forkChildInPath.getARGState();
    assert forkChildInARG.getParents().size() == 1;

    Iterator<ARGState> iterator;
    //It is important to put a backward state in map, because we can find the same real state during exploration
    //but for it a new backward state will be created
    //Disagree, try to put a real state
    if (toCallerStatesIterator.containsKey(forkChildInARG)) {
      //Means we have already handled this state, just get the next one
      iterator = toCallerStatesIterator.get(forkChildInARG);
    } else {
      ARGState forkState = forkChildInARG.getParents().iterator().next();
      //Clone is necessary, make tree set for determinism
      Set<ARGState> callerStates = Sets.newTreeSet();

      from(reducedToExpanded.get(forkState))
        .transform(s -> (ARGState) s)
        .forEach(callerStates::add);
      //We get this fork the second time (the first one was from path computer)
      //Found the caller, we have explored the first time
      ARGState previousCallerInARG = getPreviousStateOnPath(forkChildInPath).getARGState();
      assert callerStates.remove(previousCallerInARG);
      iterator = callerStates.iterator();
      toCallerStatesIterator.put(forkChildInARG, iterator);
    }

    if (iterator.hasNext()) {
      return iterator.next();
    } else {
      //We need to find the next fork
      //Do not change the fork state and start exploration from this one
      return null;
    }
  }

  /**
   * Due to special structure of ARGPath,
   * the real fork state (reduced entry) is not included into it.
   * We need to get it.
   *
   * @param parent a state after that we need to found a fork
   * @return a state of the nearest fork
   */
  private List<BackwardARGState> findNextForksInTail(BackwardARGState parent) {

    List<BackwardARGState> potentialForkStates = new ArrayList<>();
    Map<ARGState, ARGState> exitStateToEntryState = new TreeMap<>();
    BackwardARGState currentStateOnPath = parent;
    ARGState currentStateInARG, realParent;

    while (currentStateOnPath.getChildren().size() > 0) {

      assert currentStateOnPath.getChildren().size() == 1;
      currentStateOnPath = getNextStateOnPath(currentStateOnPath);
      currentStateInARG = currentStateOnPath.getARGState();

      //No matter which parent to take - interesting one is single anyway
      realParent = currentStateInARG.getParents().iterator().next();

      //Check if it is an exit state, we are waiting
      //Attention! Recursion is not supported here!
      if (exitStateToEntryState.containsKey(realParent)) {
        //Due to complicated structure of path we saved an expanded exit state and it isn't contained in the path,
        //so, we look for its parent
        ARGState expandedEntryState = exitStateToEntryState.get(realParent);
        //remove all child in cache
        for (ARGState expandedExitState : expandedEntryState.getChildren()) {
          exitStateToEntryState.remove(expandedExitState);
        }
        potentialForkStates.remove(expandedEntryState);
      }

      if (reducedToExpanded.containsKey(realParent) &&
          reducedToExpanded.get(realParent).size() > 1) {

        assert realParent.getParents().size() == 0;
        assert reducedToExpanded.get(realParent).size() > 1;

        //Now we should check, that there is no corresponding exit state in the path
        //only in this case this is a real fork

        //This is expanded state on the path at function call
        ARGState expandedEntryState = getPreviousStateOnPath(currentStateOnPath).getARGState();
        //We may have several children, so add all of them
        for (ARGState expandedExitState : expandedEntryState.getChildren()) {
          exitStateToEntryState.put(expandedExitState, currentStateOnPath);
        }
        //Save child and if we meet it, we remove the parent as not a fork

        potentialForkStates.add(currentStateOnPath);
      }
    }

    return potentialForkStates;
  }

  private ARGPath restorePathFrom(BackwardARGState pLastElement, Set<List<Integer>> pRefinedStates) {
    assert (pLastElement != null && !pLastElement.isDestroyed());

    try {
      ARGState rootOfSubgraph = findPath(pLastElement, pRefinedStates);
      assert (rootOfSubgraph != null);
      if (rootOfSubgraph == BAMMultipleCEXSubgraphComputer.DUMMY_STATE_FOR_REPEATED_STATE) {
        return null;
      }
      return ARGUtils.getRandomPath(rootOfSubgraph);
    } catch (MissingBlockException e) {
      return null;
    } catch (InterruptedException e) {
      return null;
    }
  }

  public ARGPath getNextPathFrom(ARGState pLastElement, Set<List<Integer>> pRefinedStates) {
    ARGPath path;
    if (!previousForkForState.containsKey(pLastElement)) {
      //The first time, we have no path to iterate
      BackwardARGState newTarget = new BackwardARGState(pLastElement);
      path = restorePathFrom(newTarget, pRefinedStates);
      if (path != null) {
        //currentPath may become null if it goes through repeated (refined) states
        previousForkForState.put(pLastElement, (BackwardARGState) path.getFirstState());
      }
    } else {
      path = computeNextPath(previousForkForState.get(pLastElement), pRefinedStates);
    }

    if (path != null && checkThePathHasRepeatedStates(path, pRefinedStates)) {
      return null;
    }
    return path;
  }

  private boolean checkThePathHasRepeatedStates(ARGPath path, Set<List<Integer>> pRefinedStates) {
    List<Integer> ids =
        from(path.asStatesList())
        .transform(getStateId)
        .toList();

    return from(pRefinedStates)
        .anyMatch(ids::containsAll);
  }

  /* Functions only to simplify the understanding:
   */

  private BackwardARGState getNextStateOnPath(BackwardARGState state) {
    return (BackwardARGState) state.getChildren().iterator().next();
  }

  private BackwardARGState getPreviousStateOnPath(BackwardARGState state) {
    return (BackwardARGState) state.getParents().iterator().next();
  }
}
