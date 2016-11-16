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

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

public class BAMCEXSubgraphComputer {

  private final BlockPartitioning partitioning;
  private final Reducer reducer;
  private final BAMDataManager data;
  private final LogManager logger;

  BAMCEXSubgraphComputer(BAMCPA bamCpa) {
    this.partitioning = bamCpa.getBlockPartitioning();
    this.reducer = bamCpa.getReducer();
    this.data = bamCpa.getData();
    this.logger = bamCpa.getLogger();
  }

  /**
   * Returns the root of a subtree, leading from the root element of the
   * given reachedSet to the target state.
   * The subtree is represented using children and parents of ARGElements,
   * where newTreeTarget is the ARGState in the constructed subtree that represents target.
   *
   * <p>If the target is reachable via a missing block (aka "hole"),
   * the MissingBlockException is thrown.
   * Then we expect, that the next actions are removing cache-entries from bam-cache,
   * updating some waitlists and restarting the CPA-algorithm, so that the missing block is analyzed again.
   *
   * <p>If the CEX contains a state, where several blocks overlap (happens at
   * block-start and block-end),
   * the new CEX-graph contains the states of the most-outer block/reached-set.
   *
   * @param target a state from the reachedSet, is used as the last state of the returned subgraph.
   * @param pMainReachedSet most outer reached set, contains the target-state.
   *
   * @return root of a subgraph, that contains all states on all paths to newTreeTarget.
   *         The subgraph contains only copies of the real ARG states,
   *         because one real state can be used multiple times in one path.
   * @throws MissingBlockException for re-computing some blocks
   */
  BackwardARGState computeCounterexampleSubgraph(
      final ARGState target, final ARGReachedSet pMainReachedSet)
      throws MissingBlockException, InterruptedException {
    assert pMainReachedSet.asReachedSet().contains(target);
    BackwardARGState root = computeCounterexampleSubgraph(pMainReachedSet, Collections.singleton(new BackwardARGState(target)));
    assert pMainReachedSet.asReachedSet().getFirstState() == root.getARGState();
    return root;
  }

  /**
   * Compute a subgraph within the given reached set,
   * backwards from target (wrapped by newTreeTarget) towards the root of the reached set.
   * */
  private BackwardARGState computeCounterexampleSubgraph(
      final ARGReachedSet reachedSet, final Collection<BackwardARGState> newTreeTargets)
      throws MissingBlockException, InterruptedException {

    // start by creating ARGElements for each node needed in the tree
    final Map<ARGState, BackwardARGState> finishedStates = new HashMap<>();
    final NavigableSet<ARGState> waitlist = new TreeSet<>(); // for sorted IDs in ARGstates
    BackwardARGState root = null; // to be assigned later

    for (BackwardARGState newTreeTarget : newTreeTargets) {
      ARGState target = newTreeTarget.getARGState();
      assert reachedSet.asReachedSet().contains(target);
      finishedStates.put(target, newTreeTarget);
      waitlist.addAll(target.getParents()); // add parent for further processing
    }

    while (!waitlist.isEmpty()) {
      final ARGState currentState = waitlist.pollLast(); // get state with biggest ID
      assert reachedSet.asReachedSet().contains(currentState);

      if (finishedStates.containsKey(currentState)) {
        continue; // state already done
      }

      final BackwardARGState newCurrentState = new BackwardARGState(currentState);
      finishedStates.put(currentState, newCurrentState);

      // add parent for further processing
      waitlist.addAll(currentState.getParents());

      final Set<BackwardARGState> childrenInSubgraph = new TreeSet<>();
      for (final ARGState child : currentState.getChildren()) {
        // if a child is not in the subgraph, it does not lead to the target, so ignore it.
        // Because of the ordering, all important children should be finished already.
        if (finishedStates.containsKey(child)) {
          childrenInSubgraph.add(finishedStates.get(child));
        }
      }

      if (data.initialStateToReachedSet.containsKey(currentState)) {

        // If child-state is an expanded state, the child is at the exit-location of a block.
        // In this case, we enter the block (backwards).
        // We must use a cached reachedSet to process further, because the block has its own reachedSet.
        // The returned 'innerTreeRoot' is the rootNode of the subtree, created from the cached reachedSet.
        // The current subtree (successors of child) is appended beyond the innerTree, to get a complete subgraph.
        try {
          computeCounterexampleSubgraphForBlock(newCurrentState, childrenInSubgraph);
        } catch (MissingBlockException e) {
          ARGSubtreeRemover.removeSubtree(reachedSet, currentState);
          throw new MissingBlockException();
        }

      } else {
        // children are a normal successors -> create an connection from parent to children
        for (final BackwardARGState newChild : childrenInSubgraph) {
          assert !currentState.getEdgesToChild(newChild.getARGState()).isEmpty()
              : "unexpected ARG state: parent has no edge to child.";
          newChild.addParent(newCurrentState);
        }
      }

      if (currentState.getParents().isEmpty()) {
        assert root == null : "root should not be set before";
        assert waitlist.isEmpty() : "root should have the smallest ID";
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
   * we throw a MissingBlockException.
   * Then we expect, that the next actions are removing cache-entries from bam-cache,
   * updating some waitlists and restarting the CPA-algorithm, so that the missing block is analyzed again.
   *
   * @param newExpandedRoot the (wrapped) expanded initial state of the reachedSet of current block
   * @param newExpandedTargets copy of the exit-state of the reachedSet of current block.
   *                     newExpandedTarget has only children, that are all part of the Pseudo-ARG
   *                     (these children are copies of states from reachedSets of other blocks)
   */
  private void computeCounterexampleSubgraphForBlock(
          final BackwardARGState newExpandedRoot,
          final Set<BackwardARGState> newExpandedTargets)
      throws MissingBlockException, InterruptedException {

    ARGState expandedRoot = (ARGState) newExpandedRoot.getWrappedState();
    final ReachedSet reachedSet = data.initialStateToReachedSet.get(expandedRoot);
    final Map<BackwardARGState, BackwardARGState> newExpandedToNewInnerTargets = new HashMap<>();

    for (BackwardARGState newExpandedTarget : newExpandedTargets) {
      final ARGState reducedTarget = (ARGState) data.expandedStateToReducedState.get(newExpandedTarget.getARGState());

      // first check, if the cached state is valid.
      if (reducedTarget.isDestroyed()) {
        logger.log(Level.FINE,
            "Target state refers to a destroyed ARGState, i.e., the cached subtree is outdated. Updating it.");
        throw new MissingBlockException();
      }

      assert reachedSet.contains(reducedTarget) : "reduced state '" + reducedTarget
      + "' is not part of reachedset with root '" + reachedSet.getFirstState() + "'";

      // we found the reached-set, corresponding to the root and precision.
      // now try to find a path from the target towards the root of the reached-set.
      newExpandedToNewInnerTargets.put(newExpandedTarget, new BackwardARGState(reducedTarget));
    }

    final BackwardARGState newInnerRoot;
    try {
      newInnerRoot = computeCounterexampleSubgraph(new ARGReachedSet(reachedSet), newExpandedToNewInnerTargets.values());
    } catch (MissingBlockException e) {
      //enforce recomputation to update cached subtree
      logger.log(Level.FINE,
              "Target state refers to a destroyed ARGState, i.e., the cached subtree will be removed.");

      // TODO why do we use precision of reachedSet from 'abstractStateToReachedSet' here and not the reduced precision?
      final CFANode rootNode = extractLocation(expandedRoot);
      final Block rootBlock = partitioning.getBlockForCallNode(rootNode);
      final AbstractState reducedRootState = reducer.getVariableReducedState(expandedRoot, rootBlock, rootNode);
      data.bamCache.removeReturnEntry(reducedRootState, reachedSet.getPrecision(reachedSet.getFirstState()), rootBlock);
      throw new MissingBlockException();
    }

    // reconnect ARG: replace the root of the inner block
    // with the existing state from the outer block with the current state,
    // then delete this node.
    for (ARGState innerChild : newInnerRoot.getChildren()) {
      innerChild.addParent(newExpandedRoot);
    }
    newInnerRoot.removeFromARG();

    // reconnect ARG: replace the target of the inner block
    // with the existing state from the outer block with the current state,
    // then delete this node.
    for (ARGState newExpandedTarget : newExpandedTargets) {
      BackwardARGState newInnerTarget = newExpandedToNewInnerTargets.get(newExpandedTarget);
      for (ARGState innerParent : newInnerTarget.getParents()) {
        newExpandedTarget.addParent(innerParent);
      }
      newInnerTarget.removeFromARG();
    }
    // now the complete inner tree (including all successors of the state innerTree on paths to reducedTarget)
    // is inserted between newCurrentState and child.
  }


  /** This ARGState is used to build the Pseudo-ARG for CEX-retrieval.
   *
   * TODO we could replace the BackwardARGState completely by a normal ARGState,
   * we just keep it for debugging. */
  static class BackwardARGState extends ARGState {

    private static final long serialVersionUID = -3279533907385516993L;

    public BackwardARGState(ARGState originalState) {
      super(originalState, null);
    }

    public ARGState getARGState() {
      return (ARGState) getWrappedState();
    }

    @Override
    public String toString() {
      return "BackwardARGState {{" + super.toString() + "}}";
    }
  }

  /** A class to signal a deleted block for re-computation. */
  static class MissingBlockException extends CPAException {

    private static final long serialVersionUID = 123L;

    public MissingBlockException() {
      super("missing block");
    }
  }
}
