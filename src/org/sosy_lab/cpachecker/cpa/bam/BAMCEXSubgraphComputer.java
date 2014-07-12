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
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.logging.Level;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;
import static org.sosy_lab.cpachecker.cpa.bam.AbstractBAMBasedRefiner.DUMMY_STATE_FOR_MISSING_BLOCK;

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

  /** returns the root of a subtree, leading from the root element of the given reachedSet to the target state.
   * The subtree is represented using children and parents of ARGElements,
   * where newTreeTarget is the ARGState in the constructed subtree that represents target.
   *
   * If the target is reachable via a missing block (aka "hole"),
   * the DUMMY_STATE_FOR_MISSING_BLOCK is returned.
   * Then we expect, that the next actions are removing cache-entries from bam-cache,
   * updating some waitlists and restarting the CPA-algorithm, so that the missing block is analyzed again.
   *
   * @param target a state from the reachedSet, is used as the last state of the returned subgraph.
   * @param reachedSet contains the target-state.
   * @param newTreeTarget a copy of the target, should contain the same information as target.
   *
   * @return root of a subgraph, that contains all states on all paths to newTreeTarget.
   *         The subgraph contains only copies of the real ARG states,
   *         because one real state can be used multiple times in one path.
   *         The map "pathStateToReachedState" should be used to search the correct real state.
   */
  BackwardARGState computeCounterexampleSubgraph(final ARGState target, final ARGReachedSet reachedSet, final BackwardARGState newTreeTarget) {
    assert reachedSet.asReachedSet().contains(target);

    //start by creating ARGElements for each node needed in the tree
    final Map<ARGState, BackwardARGState> finishedStates = new HashMap<>();
    final NavigableSet<ARGState> waitlist = new TreeSet<>(); // for sorted IDs in ARGstates
    BackwardARGState root = null;

    pathStateToReachedState.put(newTreeTarget, target);
    finishedStates.put(target, newTreeTarget);
    waitlist.addAll(target.getParents()); // add parent for further processing
    while (!waitlist.isEmpty()) {
      final ARGState currentState = waitlist.pollLast(); // get state with biggest ID
      assert reachedSet.asReachedSet().contains(currentState);

      if (finishedStates.containsKey(currentState)) {
        continue; // state already done
      }

      final BackwardARGState newCurrentState = new BackwardARGState(currentState);
      finishedStates.put(currentState, newCurrentState);
      pathStateToReachedState.put(newCurrentState, currentState);

      // add parent for further processing
      waitlist.addAll(currentState.getParents());

      for (final ARGState child : currentState.getChildren()) {
        if (!finishedStates.containsKey(child)) {
          // child is not in the subgraph, that leads to the target,so ignore it.
          // because of the ordering, all important children should be finished already.
          continue;
        }

        final BackwardARGState newChild = finishedStates.get(child);

        if (expandedToReducedCache.containsKey(child)) {
          // If child-state is an expanded state, we are at the exit-location of a block.
          // In this case, we enter the block (backwards).
          // We must use a cached reachedSet to process further, because the block has its own reachedSet.
          // The returned 'innerTree' is the rootNode of the subtree, created from the cached reachedSet.
          // The current subtree (successors of child) is appended beyond the innerTree, to get a complete subgraph.
          final ARGState reducedTarget = (ARGState) expandedToReducedCache.get(child);
          BackwardARGState innerTree = computeCounterexampleSubgraphForBlock(currentState, reducedTarget, newChild);
          if (innerTree == DUMMY_STATE_FOR_MISSING_BLOCK) {
            ARGSubtreeRemover.removeSubtree(reachedSet, currentState);
            return DUMMY_STATE_FOR_MISSING_BLOCK;
          }

          // reconnect ARG: replace the state 'innerTree' with the current state.
          for (ARGState innerChild : innerTree.getChildren()) {
            innerChild.addParent(newCurrentState);
          }
          innerTree.removeFromARG();

          // now the complete inner tree (including all successors of the state innerTree on paths to reducedTarget)
          // is inserted between newCurrentState and child.

        } else {
          // child is a normal successor
          // -> create an simple connection from parent to current
          assert currentState.getEdgeToChild(child) != null: "unexpected ARG state: parent has no edge to child.";
          newChild.addParent(newCurrentState);
        }
      }

      if (currentState.getParents().isEmpty()) {
        assert root == null : "root should not be set before";
        root = newCurrentState;
      }
    }
    assert root != null;
    return root;
  }

  /**
   * This method looks for the reached set that belongs to (root, rootPrecision),
   * then looks for target in this reached set and constructs a tree from root to target
   * (recursively, if needed).
   *
   * If the target is reachable via a missing block (aka "hole"),
   * the DUMMY_STATE_FOR_MISSING_BLOCK is returned.
   * Then we expect, that the next actions are removing cache-entries from bam-cache,
   * updating some waitlists and restarting the CPA-algorithm, so that the missing block is analyzed again.
   *
   * @param expandedRoot the expanded initial state of the reachedSet of current block
   * @param reducedTarget exit-state of the reachedSet of current block
   * @param newTreeTarget copy of the exit-state of the reachedSet of current block.
   *                     newTreeTarget has only children, that are all part of the Pseudo-ARG
   *                     (these children are copies of states from reachedSets of other blocks)
   *
   * @return the default return value is the rootState of the Pseudo-ARG.
   *         We return NULL, if reachedSet is invalid, i.e. there is a 'hole' in it,
   *         maybe because of a refinement of the block at another CEX-refinement.
   *         In that case we also perform some cleanup-operations.
   */
  private BackwardARGState computeCounterexampleSubgraphForBlock(
          final ARGState expandedRoot, final ARGState reducedTarget, final BackwardARGState newTreeTarget) {

    // first check, if the cached state is valid.
    if (reducedTarget.isDestroyed()) {
      logger.log(Level.FINE,
              "Target state refers to a destroyed ARGState, i.e., the cached subtree is outdated. Updating it.");
      return DUMMY_STATE_FOR_MISSING_BLOCK;
    }

    // TODO why do we use 'abstractStateToReachedSet' to get the reachedSet and not 'bamCache'?
    final ReachedSet reachedSet = abstractStateToReachedSet.get(expandedRoot);

    // we found the reachedSet, corresponding to the root and precision.
    // now try to find the target in the reach set.

    assert reachedSet.contains(reducedTarget);

    // we found the target; now construct a subtree in the ARG starting with targetARGElement
    final BackwardARGState result = computeCounterexampleSubgraph(reducedTarget, new ARGReachedSet(reachedSet), newTreeTarget);
    if (result == DUMMY_STATE_FOR_MISSING_BLOCK) {
      //enforce recomputation to update cached subtree
      logger.log(Level.FINE,
              "Target state refers to a destroyed ARGState, i.e., the cached subtree will be removed.");

      // TODO why do we use precision of reachedSet from 'abstractStateToReachedSet' here and not the reduced precision?
      final CFANode rootNode = extractLocation(expandedRoot);
      final Block rootBlock = partitioning.getBlockForCallNode(rootNode);
      final AbstractState reducedRootState = reducer.getVariableReducedState(expandedRoot, rootBlock, rootNode);
      bamCache.removeReturnEntry(reducedRootState, reachedSet.getPrecision(reachedSet.getFirstState()), rootBlock);
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
