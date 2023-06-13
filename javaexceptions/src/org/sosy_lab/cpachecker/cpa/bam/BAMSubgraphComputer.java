// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMCache.BAMCacheEntry;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMDataManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;

public class BAMSubgraphComputer {

  private final BlockPartitioning partitioning;
  private final Reducer reducer;
  protected final BAMDataManager data;
  private final LogManager logger;
  private final boolean useCopyOnWriteRefinement;
  private final boolean cleanupOnMissingBlock;

  BAMSubgraphComputer(AbstractBAMCPA bamCpa, boolean pCleanupOnMissingBlock) {
    partitioning = bamCpa.getBlockPartitioning();
    reducer = bamCpa.getReducer();
    data = bamCpa.getData();
    logger = bamCpa.getLogger();
    useCopyOnWriteRefinement = bamCpa.useCopyOnWriteRefinement();
    cleanupOnMissingBlock = pCleanupOnMissingBlock;
  }

  /**
   * Returns the root of a subtree, leading from the root element of the given reachedSet to the
   * target state. The subtree is represented using children and parents of ARGElements, where
   * newTreeTarget is the ARGState in the constructed subtree that represents target.
   *
   * <p>If the target is reachable via a missing block (aka "hole"), the MissingBlockException is
   * thrown. Then we expect, that the next actions are removing cache-entries from bam-cache,
   * updating some waitlists and restarting the CPA-algorithm, so that the missing block is analyzed
   * again.
   *
   * <p>If the CEX contains a state, where several blocks overlap (happens at block-start and
   * block-end), the new CEX-graph contains the states of the most-outer block/reached-set.
   *
   * @param target a state from the reachedSet, is used as the last state of the returned subgraph.
   * @param pMainReachedSet most outer reached set corresponding to the current refinement. The
   *     given reached-set can be nested inside another reached-set, which must not be used in the
   *     current refinement. The given reached-set contains the target-state.
   * @return root and target of a subgraph, that contains all states on all paths to newTreeTarget.
   *     The subgraph contains only copies of the real ARG states, because one real state can be
   *     used multiple times in one path.
   * @throws MissingBlockException for re-computing some blocks
   */
  Pair<BackwardARGState, BackwardARGState> computeCounterexampleSubgraph(
      final ARGState target, final ARGReachedSet pMainReachedSet)
      throws MissingBlockException, InterruptedException {
    Pair<BackwardARGState, Collection<BackwardARGState>> p =
        computeCounterexampleSubgraph(Collections.singleton(target), pMainReachedSet);
    return Pair.of(p.getFirst(), Iterables.getOnlyElement(p.getSecond()));
  }

  Pair<BackwardARGState, Collection<BackwardARGState>> computeCounterexampleSubgraph(
      final Collection<ARGState> targets, final ARGReachedSet pMainReachedSet)
      throws MissingBlockException, InterruptedException {
    UnmodifiableReachedSet mainRs = pMainReachedSet.asReachedSet();
    assert mainRs.asCollection().containsAll(targets)
        : "target states should be contained in reached-set. The following states are not"
            + " contained: "
            + Iterables.filter(targets, s -> !mainRs.contains(s));
    if (targets.isEmpty()) {
      // cannot compute subgraph without target states
      return Pair.of(new BackwardARGState((ARGState) mainRs.getFirstState()), ImmutableList.of());
    }
    Collection<BackwardARGState> newTargets =
        transformedImmutableListCopy(targets, BackwardARGState::new);
    BackwardARGState root = computeCounterexampleSubgraph(pMainReachedSet, newTargets);
    assert mainRs.getFirstState() == root.getARGState();
    return Pair.of(root, newTargets);
  }

  /**
   * Compute a subgraph within the given reached set, backwards from target (wrapped by
   * newTreeTarget) towards the root of the reached set.
   */
  private BackwardARGState computeCounterexampleSubgraph(
      final ARGReachedSet reachedSet, final Collection<BackwardARGState> newTreeTargets)
      throws MissingBlockException, InterruptedException {
    final UnmodifiableReachedSet rs = reachedSet.asReachedSet();

    // start by creating ARGElements for each node needed in the tree
    final Map<ARGState, BackwardARGState> finishedStates = new HashMap<>();
    final NavigableSet<ARGState> waitlist = new TreeSet<>(); // for sorted IDs in ARGstates
    BackwardARGState root = null; // to be assigned later

    for (BackwardARGState newTreeTarget : newTreeTargets) {
      ARGState target = newTreeTarget.getARGState();
      assert rs.contains(target);
      finishedStates.put(target, newTreeTarget);
      waitlist.addAll(target.getParents()); // add parent for further processing
    }

    while (!waitlist.isEmpty()) {
      final ARGState currentState = waitlist.pollLast(); // get state with biggest ID
      assert rs.contains(currentState);

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

      if (data.hasInitialState(currentState)) {

        // If child-state is an expanded state, the child is at the exit-location of a block.
        // In this case, we enter the block (backwards).
        // We must use a cached reachedSet to process further, because the block has its own
        // reachedSet.
        // The returned 'innerTreeRoot' is the rootNode of the subtree, created from the cached
        // reachedSet.
        // The current subtree (successors of child) is appended beyond the innerTree, to get a
        // complete subgraph.
        try {
          computeCounterexampleSubgraphForBlock(newCurrentState, childrenInSubgraph);
        } catch (MissingBlockException e) {
          assert !useCopyOnWriteRefinement
              : "CopyOnWrite-refinement should never cause missing blocks: " + e;
          if (cleanupOnMissingBlock) {
            if (!currentState.isDestroyed()) {
              // TODO Why is the state (and subtree) already removed before?
              ARGInPlaceSubtreeRemover.removeSubtree(reachedSet, currentState);
            }
          }
          throw e;
        }

      } else {
        // children are a normal successors -> create an connection from parent to children
        for (final BackwardARGState newChild : childrenInSubgraph) {
          // assert !currentState.getEdgesToChild(newChild.getARGState()).isEmpty()
          // : String.format(
          // "unexpected ARG state: parent has no edge to child: %s -/-> %s",
          // currentState, newChild.getARGState());
          newChild.addParent(newCurrentState);
        }
      }

      if (currentState.getParents().isEmpty()) {
        assert root == null : "root should not be set before";
        assert waitlist.isEmpty() : "root should have the smallest ID";
        root = newCurrentState;
      }
    }
    assert root != null
        : "no root state found in reachedset with initial state "
            + reachedSet.asReachedSet().getFirstState();
    return root;
  }

  /**
   * This method looks for the reached set that belongs to (root, rootPrecision), then looks for
   * target in this reached set and constructs a tree from root to target (recursively, if needed).
   *
   * <p>If the target is reachable via a missing block (aka "hole"), we throw a
   * MissingBlockException. Then we expect, that the next actions are removing cache-entries from
   * bam-cache, updating some waitlists and restarting the CPA-algorithm, so that the missing block
   * is analyzed again.
   *
   * @param newExpandedRoot the (wrapped) expanded initial state of the reachedSet of current block
   * @param newExpandedTargets copy of the exit-state of the reachedSet of current block.
   *     newExpandedTarget has only children, that are all part of the Pseudo-ARG (these children
   *     are copies of states from reachedSets of other blocks)
   */
  protected void computeCounterexampleSubgraphForBlock(
      final BackwardARGState newExpandedRoot, final Set<BackwardARGState> newExpandedTargets)
      throws MissingBlockException, InterruptedException {

    ARGState expandedRoot = (ARGState) newExpandedRoot.getWrappedState();
    final Multimap<ReachedSet, BackwardARGState> reachedSets = LinkedHashMultimap.create();

    final Map<BackwardARGState, BackwardARGState> newExpandedToNewInnerTargets = new HashMap<>();

    for (BackwardARGState newExpandedTarget : newExpandedTargets) {

      if (!data.hasExpandedState(newExpandedTarget.getARGState())) {
        logger.log(
            Level.FINE,
            "Target state refers to a missing ARGState, i.e., the cached subtree was deleted."
                + " Updating it.");
        throw new MissingBlockException(expandedRoot, newExpandedTarget.getWrappedState());
      }

      final ARGState reducedTarget =
          (ARGState) data.getReducedStateForExpandedState(newExpandedTarget.getARGState());

      // first check, if the cached state is valid.
      if (reducedTarget.isDestroyed()) {
        logger.log(
            Level.FINE,
            "Target state refers to a destroyed ARGState, i.e., the cached subtree is outdated."
                + " Updating it.");
        throw new MissingBlockException(expandedRoot, newExpandedTarget.getWrappedState());
      }

      final ReachedSet reachedSet = data.getReachedSetForInitialState(expandedRoot, reducedTarget);
      assert reachedSet.contains(reducedTarget)
          : String.format(
              "reduced state '%s' is not part of reachedset with root '%s' from expanded root '%s'",
              reducedTarget, reachedSet.getFirstState(), expandedRoot);

      // we found the reached-set, corresponding to the root and precision.
      // now try to find a path from the target towards the root of the reached-set.
      BackwardARGState newBackwardTarget = new BackwardARGState(reducedTarget);
      newExpandedToNewInnerTargets.put(newExpandedTarget, newBackwardTarget);
      reachedSets.put(reachedSet, newBackwardTarget);
    }

    for (final ReachedSet reachedSet : reachedSets.keySet()) {
      final BackwardARGState newInnerRoot;
      try {
        newInnerRoot =
            computeCounterexampleSubgraph(
                new ARGReachedSet(reachedSet), newExpandedToNewInnerTargets.values());
      } catch (MissingBlockException e) {
        // enforce recomputation to update cached subtree
        logger.log(
            Level.FINE,
            "Target state refers to a destroyed ARGState, i.e., the cached subtree will be"
                + " removed.");

        // TODO why do we use precision of reachedSet from 'abstractStateToReachedSet' here and not
        // the reduced precision?
        final CFANode rootNode = extractLocation(expandedRoot);
        final Block rootBlock = partitioning.getBlockForCallNode(rootNode);
        final AbstractState reducedRootState =
            reducer.getVariableReducedState(expandedRoot, rootBlock, rootNode);
        BAMCacheEntry cacheEntry =
            data.getCache()
                .get(
                    reducedRootState,
                    reachedSet.getPrecision(reachedSet.getFirstState()),
                    rootBlock);
        if (cacheEntry != null) {
          // TODO do we need this check? Maybe there is a bug, if the entry is not available?
          cacheEntry.deleteInfo();
        }
        throw e;
      }

      // reconnect ARG: replace the root of the inner block
      // with the existing state from the outer block with the current state,
      // then delete this node.
      for (ARGState innerChild : newInnerRoot.getChildren()) {
        innerChild.addParent(newExpandedRoot);
      }
      newInnerRoot.removeFromARG();
    }

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
    // now the complete inner tree (including all successors of the state innerTree on paths to
    // reducedTarget)
    // is inserted between newCurrentState and child.
  }

  /**
   * This ARGState is used to build the Pseudo-ARG for CEX-retrieval.
   *
   * <p>TODO we could replace the BackwardARGState completely by a normal ARGState, we just keep it
   * for debugging.
   */
  static class BackwardARGState extends ARGState {

    private static final long serialVersionUID = -3279533907385516993L;

    public BackwardARGState(ARGState originalState) {
      super(originalState, null);
    }

    public ARGState getARGState() {
      return (ARGState) getWrappedState();
    }

    public BackwardARGState copy() {
      return new BackwardARGState(getARGState());
    }

    @Override
    public String toString() {
      return "BackwardARGState {{" + super.toString() + "}}";
    }
  }

  /** A class to signal a deleted block for re-computation. */
  static class MissingBlockException extends CPAException {

    private static final long serialVersionUID = 123L;

    private final AbstractState initialState;
    private final AbstractState exitState;

    public MissingBlockException(AbstractState pInitialState, AbstractState pExitState) {
      super(
          String.format(
              "missing block for non-reduced initial state %n%s and expanded exit state %n%s",
              pInitialState, pExitState));
      initialState = Preconditions.checkNotNull(pInitialState);
      exitState = Preconditions.checkNotNull(pExitState);
    }

    AbstractState getInitialState() {
      return initialState;
    }

    AbstractState getExitState() {
      return exitState;
    }
  }
}
