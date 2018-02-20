/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.bam.BAMSubgraphComputer.BackwardARGState;


public class BAMSubgraphIterator {

  private final ARGState targetState;
  private final BAMMultipleCEXSubgraphComputer subgraphComputer;
  private final Multimap<AbstractState, AbstractState> reducedToExpanded;

  private BackwardARGState firstState;
  private Map<AbstractState, Iterator<ARGState>> toCallerStatesIterator = new HashMap<>();

  BAMSubgraphIterator(ARGState pTargetState, BAMMultipleCEXSubgraphComputer sComputer,
      Multimap<AbstractState, AbstractState> pReducedToExpand) {
    targetState = pTargetState;
    subgraphComputer = sComputer;
    reducedToExpanded = pReducedToExpand;
    firstState = null;
  }


  //lastAffectedState is Backward!
  //Actually it is possible to implement an optimization,
  //which allows to search forks not from the first state, but from a some middle state
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
      newPath = subgraphComputer.restorePathFrom(clonedNextParent, pRefinedStates);

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

  public ARGPath getNextPath(Set<List<Integer>> pRefinedStates) {
    ARGPath path;
    if (firstState == null) {
      //The first time, we have no path to iterate
      path = subgraphComputer.restorePathFrom(new BackwardARGState(targetState), pRefinedStates);
    } else {
      path = computeNextPath(firstState, pRefinedStates);
    }
    if (path != null) {
      //currentPath may become null if it goes through repeated (refined) states
      firstState = (BackwardARGState) path.getFirstState();
    }

    return path;
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
