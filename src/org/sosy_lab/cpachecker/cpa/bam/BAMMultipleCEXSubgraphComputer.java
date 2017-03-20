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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;


public class BAMMultipleCEXSubgraphComputer extends BAMSubgraphComputer{

  private final Map<AbstractState, AbstractState> reducedToExpanded;
  private Set<LinkedList<Integer>> remainingStates = new HashSet<>();

  BAMMultipleCEXSubgraphComputer(BAMCPA bamCPA,
      Map<AbstractState, AbstractState> pReduced) {
    super(bamCPA);
    this.reducedToExpanded = pReduced;
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

    openElements.add(target);
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

      inCallstackFunction = false;
      if (currentState.getParents().isEmpty()) {
        //Find correct expanded state
        ARGState expandedState = (ARGState) reducedToExpanded.get(currentState);

        assert expandedState != null;


        //Try to find path.
        //Exchange the reduced state by the expanded one
        currentState = expandedState;
        newCurrentElement = new BackwardARGState(currentState);
        elementsMap.put(currentState, newCurrentElement);
        inCallstackFunction = true;
      }

      // add parent for further processing
      openElements.addAll(currentState.getParents());

      if (data.hasInitialState(currentState) && !inCallstackFunction && !childrenInSubgraph.isEmpty()) {

        // If child-state is an expanded state, the child is at the exit-location of a block.
        // In this case, we enter the block (backwards).
        // We must use a cached reachedSet to process further, because the block has its own reachedSet.
        // The returned 'innerTreeRoot' is the rootNode of the subtree, created from the cached reachedSet.
        // The current subtree (successors of child) is appended beyond the innerTree, to get a complete subgraph.
        computeCounterexampleSubgraphForBlock(newCurrentElement, childrenInSubgraph);
        assert childrenInSubgraph.size() == 1;
        BackwardARGState tmpState = childrenInSubgraph.iterator().next(), nextState;
        while (tmpState != newCurrentElement) {
          Collection<ARGState> parents = tmpState.getParents();
          assert parents.size() == 1;
          nextState = (BackwardARGState) parents.iterator().next();
          if (checkRepeatitionOfState(tmpState.getARGState())) {
            return DUMMY_STATE_FOR_REPEATED_STATE;
          }
          tmpState = nextState;
        }

      } else {
        // children are a normal successors -> create an connection from parent to children
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
    int currentId = currentElement.getStateId();
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
}
