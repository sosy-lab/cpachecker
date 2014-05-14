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
package org.sosy_lab.cpachecker.cpa.bam;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Level;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

public class BAMCEXSubgraphComputer {

  private final BlockPartitioning partitioning;
  private final Reducer reducer;
  private final BAMCache bamCache;
  private final Map<ARGState, ARGState> pathStateToReachedState;
  private final Map<AbstractState, ReachedSet> abstractStateToReachedSet;
  private final Map<AbstractState, AbstractState> expandedToReducedCache;
  private final LogManager logger;

  BAMCEXSubgraphComputer(BlockPartitioning partitioning, Reducer reducer, BAMCache bamCache,
                         Map<ARGState, ARGState> pathStateToReachedState,
                         Map<AbstractState, ReachedSet> abstractStateToReachedSet,
                         Map<AbstractState, AbstractState> expandedToReducedCache,
                         LogManager logger) {
    this.partitioning = partitioning;
    this.reducer = reducer;
    this.bamCache = bamCache;
    this.pathStateToReachedState = pathStateToReachedState;
    this.abstractStateToReachedSet = abstractStateToReachedSet;
    this.expandedToReducedCache = expandedToReducedCache;
    this.logger = logger;
  }

  //returns root of a subtree leading from the root element of the given reachedSet to the target state
  //subtree is represented using children and parents of ARGElements, where newTreeTarget is the ARGState
  //in the constructed subtree that represents target
  BackwardARGState computeCounterexampleSubgraph(ARGState target, ARGReachedSet reachedSet, BackwardARGState newTreeTarget) throws InterruptedException, RecursiveAnalysisFailedException {
    assert reachedSet.asReachedSet().contains(target);

    //start by creating ARGElements for each node needed in the tree
    Map<ARGState, BackwardARGState> elementsMap = new HashMap<>();
    Stack<ARGState> openElements = new Stack<>();
    BackwardARGState root = null;

    pathStateToReachedState.put(newTreeTarget, target);
    elementsMap.put(target, newTreeTarget);
    openElements.push(target);
    while (!openElements.empty()) {
      ARGState currentElement = openElements.pop();

      assert reachedSet.asReachedSet().contains(currentElement);

      for (ARGState parent : currentElement.getParents()) {
        if (!elementsMap.containsKey(parent)) {
          //create node for parent in the new subtree
          elementsMap.put(parent, new BackwardARGState(parent));
          pathStateToReachedState.put(elementsMap.get(parent), parent);
          //and remember to explore the parent later
          openElements.push(parent);
        }
        CFAEdge edge = BAMARGUtils.getEdgeToChild(parent, currentElement);
        if (edge == null) {
          //this is a summarized call and thus an direct edge could not be found
          //we have the transfer function to handle this case, as our reachSet is wrong
          //(we have to use the cached ones)
          BackwardARGState innerTree = computeCounterexampleSubgraph(
                  parent, reachedSet.asReachedSet().getPrecision(parent), elementsMap.get(currentElement));
          if (innerTree == null) {
            ARGSubtreeRemover.removeSubtree(reachedSet, parent);
            return null;
          }
          for (ARGState child : innerTree.getChildren()) {
            child.addParent(elementsMap.get(parent));
          }
          innerTree.removeFromARG();
          elementsMap.get(parent).updateDecreaseId();
        } else {
          //normal edge
          //create an edge from parent to current
          elementsMap.get(currentElement).addParent(elementsMap.get(parent));
        }
      }
      if (currentElement.getParents().isEmpty()) {
        root = elementsMap.get(currentElement);
      }
    }
    assert root != null;
    return root;
  }

  /**
   * This method looks for the reached set that belongs to (root, rootPrecision),
   * then looks for target in this reached set and constructs a tree from root to target
   * (recursively, if needed).
   * @throws RecursiveAnalysisFailedException
   */
  private BackwardARGState computeCounterexampleSubgraph(ARGState root, Precision rootPrecision, BackwardARGState newTreeTarget)
          throws InterruptedException, RecursiveAnalysisFailedException {
    CFANode rootNode = extractLocation(root);
    Block rootSubtree = partitioning.getBlockForCallNode(rootNode);

    AbstractState reducedRootState = reducer.getVariableReducedState(root, rootSubtree, rootNode);
    ReachedSet reachSet = abstractStateToReachedSet.get(root);

    //we found the to the root and precision corresponding reach set
    //now try to find the target in the reach set
    ARGState targetARGState = (ARGState) expandedToReducedCache.get(pathStateToReachedState.get(newTreeTarget));
    if (targetARGState.isDestroyed()) {
      logger.log(Level.FINE,
              "Target state refers to a destroyed ARGState, i.e., the cached subtree is outdated. Updating it.");
      return null;
    }
    assert reachSet.contains(targetARGState);
    //we found the target; now construct a subtree in the ARG starting with targetARTElement
    BackwardARGState result = computeCounterexampleSubgraph(
            targetARGState, new ARGReachedSet(reachSet), newTreeTarget);
    if (result == null) {
      //enforce recomputation to update cached subtree
      bamCache.removeReturnEntry(reducedRootState, reachSet.getPrecision(reachSet.getFirstState()), rootSubtree);
    }
    return result;
  }


  /**
   * This is a ARGState, that counts backwards, used to build the Pseudo-ARG for CEX-retrieval.
   * As the Pseudo-ARG is build backwards starting at its end-state, we count the ID backwards.
   */
  static class BackwardARGState extends ARGState {

    private static final long serialVersionUID = -3279533907385516993L;
    private int decreasingStateID;
    private static int nextDecreaseID = Integer.MAX_VALUE;

    public BackwardARGState(ARGState originalState) {
      super(originalState.getWrappedState(), null);
      decreasingStateID = nextDecreaseID--;
    }

    @Override
    public boolean isOlderThan(ARGState other) {
      if (other instanceof BackwardARGState) { return decreasingStateID < ((BackwardARGState) other).decreasingStateID; }
      return super.isOlderThan(other);
    }

    void updateDecreaseId() {
      decreasingStateID = nextDecreaseID--;
    }
  }
}
