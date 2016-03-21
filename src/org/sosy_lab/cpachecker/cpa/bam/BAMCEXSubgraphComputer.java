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

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.logging.Level;

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

public class BAMCEXSubgraphComputer {

  private final BlockPartitioning partitioning;
  private final Reducer reducer;
  private final BAMDataManager data;
  private final Map<ARGState, ARGState> pathStateToReachedState;
  private final LogManager logger;

  BAMCEXSubgraphComputer(BAMCPA bamCpa, Map<ARGState, ARGState> pPathStateToReachedState) {
    this.partitioning = bamCpa.getBlockPartitioning();
    this.reducer = bamCpa.getReducer();
    this.data = bamCpa.getData();
    this.pathStateToReachedState = pPathStateToReachedState;
    this.logger = bamCpa.getLogger();
  }

  /** returns the root of a subtree, leading from the root element of the given reachedSet to the target state.
   * The subtree is represented using children and parents of ARGElements,
   * where newTreeTarget is the ARGState in the constructed subtree that represents target.
   *
   * If the target is reachable via a missing block (aka "hole"),
   * the MissingBlockException is thrown.
   * Then we expect, that the next actions are removing cache-entries from bam-cache,
   * updating some waitlists and restarting the CPA-algorithm, so that the missing block is analyzed again.
   *
   * @param target a state from the reachedSet, is used as the last state of the returned subgraph.
   * @param pMainReachedSet most outer reached set, contains the target-state.
   *
   * @return root of a subgraph, that contains all states on all paths to newTreeTarget.
   *         The subgraph contains only copies of the real ARG states,
   *         because one real state can be used multiple times in one path.
   *         The map "pathStateToReachedState" should be used to search the correct real state.
   * @throws MissingBlockException for re-computing some blocks
   */
  BackwardARGState computeCounterexampleSubgraph(final ARGState target, final ARGReachedSet reachedSet)
      throws MissingBlockException {
    assert reachedSet.asReachedSet().contains(target);
    return computeCounterexampleSubgraph(target, reachedSet, new BAMCEXSubgraphComputer.BackwardARGState(target));
  }

  private BackwardARGState computeCounterexampleSubgraph(final ARGState target,
        final ARGReachedSet reachedSet, final BackwardARGState newTreeTarget)
            throws MissingBlockException {
      assert reachedSet.asReachedSet().contains(target);
    // start by creating ARGElements for each node needed in the tree
    final Map<ARGState, BackwardARGState> finishedStates = new HashMap<>();
    final NavigableSet<ARGState> waitlist = new TreeSet<>(); // for sorted IDs in ARGstates
    BackwardARGState root = null; // to be assigned later

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
          // We skip children that are not finished,
          // because they belong to branches that do not lead to the target.
          continue;
        }

        final BackwardARGState newChild = finishedStates.get(child);

        if (data.expandedStateToReducedState.containsKey(child)) {
          assert data.initialStateToReachedSet.containsKey(currentState) : "parent should be initial state of reached-set";
          // If child-state is an expanded state, the child is at the exit-location of a block.
          // In this case, we enter the block (backwards).
          // We must use a cached reachedSet to process further, because the block has its own reachedSet.
          // The returned 'innerTreeRoot' is the rootNode of the subtree, created from the cached reachedSet.
          // The current subtree (successors of child) is appended beyond the innerTree, to get a complete subgraph.
          final ARGState reducedTarget = (ARGState) data.expandedStateToReducedState.get(child);
          BackwardARGState innerTreeRoot;
          try {
            innerTreeRoot = computeCounterexampleSubgraphForBlock(currentState, reducedTarget, newChild);
          } catch (MissingBlockException e) {
            ARGSubtreeRemover.removeSubtree(reachedSet, currentState);
            throw new MissingBlockException();
          }

          // reconnect ARG: replace the state 'innerTree' with the current state.
          for (ARGState innerChild : innerTreeRoot.getChildren()) {
            innerChild.addParent(newCurrentState);
          }
          innerTreeRoot.removeFromARG();

          assert pathStateToReachedState.containsKey(innerTreeRoot) : "root of subgraph was not finished";
          pathStateToReachedState.remove(innerTreeRoot); // not needed any more

          // now the complete inner tree (including all successors of the state innerTree on paths to reducedTarget)
          // is inserted between newCurrentState and child.

          assert pathStateToReachedState.containsKey(newChild) : "end of subgraph was not handled";
          assert pathStateToReachedState.get(newCurrentState) == currentState : "input-state must be from outer reachedset";

          // check that at block output locations the first reached state is used for the CEXsubgraph,
          // i.e. the reduced abstract state from the (next) inner block's reached set.
          assert pathStateToReachedState.get(newChild) == data.expandedStateToReducedState.get(child) : "output-state must be from (next) inner reachedset";

          pathStateToReachedState.put(newChild, child); // override previous entry for newChild with child-state of most outer block-exit.

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
      final ARGState expandedRoot, final ARGState reducedTarget, final BackwardARGState newTreeTarget)
          throws MissingBlockException {

    // first check, if the cached state is valid.
    if (reducedTarget.isDestroyed()) {
      logger.log(Level.FINE,
              "Target state refers to a destroyed ARGState, i.e., the cached subtree is outdated. Updating it.");
      throw new MissingBlockException();
    }

    // TODO why do we use 'abstractStateToReachedSet' to get the reachedSet and not 'bamCache'?
    final ReachedSet reachedSet = data.initialStateToReachedSet.get(expandedRoot);

    // we found the reachedSet, corresponding to the root and precision.
    // now try to find the target in the reach set.

    assert reachedSet.contains(reducedTarget) :
      "reduced state '" + reducedTarget + "' is not part of reachedset with root '" + reachedSet.getFirstState() + "'";

    // we found the target; now construct a subtree in the ARG starting with targetARGElement
    final BackwardARGState result;
    try {
      result = computeCounterexampleSubgraph(reducedTarget, new ARGReachedSet(reachedSet), newTreeTarget);
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
    return result;
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
