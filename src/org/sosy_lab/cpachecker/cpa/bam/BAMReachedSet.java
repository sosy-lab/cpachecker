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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.bam.BAMSubgraphComputer.BackwardARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;

public class BAMReachedSet extends ARGReachedSet.ForwardingARGReachedSet {

  private final BlockPartitioning partitioning;
  private final BAMDataManager data;
  private final ARGPath path;
  private final StatTimer removeCachedSubtreeTimer;
  private final LogManager logger;

  public BAMReachedSet(BAMCPA cpa, ARGReachedSet pMainReachedSet, ARGPath pPath,
      StatTimer pRemoveCachedSubtreeTimer) {
    super(pMainReachedSet);
    this.partitioning = cpa.getBlockPartitioning();
    this.data = cpa.getData();
    this.logger = cpa.getLogger();
    this.path = pPath;
    this.removeCachedSubtreeTimer = pRemoveCachedSubtreeTimer;

    assert path.getFirstState().getSubgraph().containsAll(path.asStatesList()) : "path should traverse reachable states";
  }

  @Override
  public UnmodifiableReachedSet asReachedSet() {
    return new BAMReachedSetView(path.getFirstState(), path.getLastState(),
        s -> super.asReachedSet().getPrecision(super.asReachedSet().getLastState()));
    // TODO do we really need the target-precision for refinements and not the actual one?
  }

  @Override
  public void removeSubtree(
      ARGState element, Precision newPrecision, Predicate<? super Precision> pPrecisionType)
      throws InterruptedException {
    removeSubtree(element, ImmutableList.of(newPrecision), ImmutableList.of(pPrecisionType));
  }

  @Override
  public void removeSubtree(
      ARGState cutState,
      final List<Precision> pPrecisions,
      final List<Predicate<? super Precision>> pPrecisionTypes)
      throws InterruptedException {
    Preconditions.checkArgument(pPrecisionTypes.size() == pPrecisionTypes.size());
    final List<Pair<Precision, Predicate<? super Precision>>> newPrecisionsLst =
        Pair.zipList(pPrecisions, pPrecisionTypes);

    assert path.getFirstState().getSubgraph().contains(cutState);

    // get blocks that need to be touched
    Map<BackwardARGState, ARGState> blockInitAndExitStates =
        getBlockInitAndExitStates(path.asStatesList());
    Deque<ARGState> relevantCallStates = getRelevantCallStates(path.asStatesList(), cutState);
    assert relevantCallStates.peekLast() == path.getFirstState()
        : "root should be relevant: " + relevantCallStates.peekLast() + " + " + path.getFirstState();
    assert relevantCallStates.size() >= 1
        : "at least the main-function should be open at the target-state";
    // TODO add element's block, if necessary?

    ARGState tmp = cutState;
    for (ARGState callState : relevantCallStates) {

      logger.logf(Level.FINEST, "removing %s from reachedset with root %s", tmp, callState);

      removeCachedSubtreeTimer.start();

      handleSubtree(
          ((BackwardARGState) callState).getARGState(),
          checkNotNull(blockInitAndExitStates.get(callState)),
          data.getInnermostState(((BackwardARGState) tmp).getARGState()),
          newPrecisionsLst);
      tmp = callState;

      removeCachedSubtreeTimer.stop();
    }

    // post-processing, cleanup data-structures: We remove all states reachable from 'element'.
    // The only important step is to remove the last state of the reached-set,
    // because without this step there is an assertion in Predicate-RefinementStrategy.
    // We can ignore waitlist-updates and coverage here, because there is no coverage in a BAM-CEX.
    for (ARGState state : cutState.getSubgraph()) {
      state.removeFromARG();
    }

    super.delegate.removeSubtree((ARGState) super.delegate.asReachedSet().getLastState());
  }

  private Map<BackwardARGState, ARGState> getBlockInitAndExitStates(ImmutableList<ARGState> path) {
    final Map<BackwardARGState, ARGState> blockInitAndExitStates = new LinkedHashMap<>();
    final Deque<BackwardARGState> openCallStates = new ArrayDeque<>();
    for (final ARGState bamState : path) {
      final ARGState state = ((BackwardARGState) bamState).getARGState();

      // ASSUMPTION: there can be several block-exits at once per location, but only one block-entry per location.

      // we use a loop here, because a return-node can be the exit of several blocks at once.
      for (AbstractState exitState : data.getExpandedStateList(state)) {
        // we are leaving a block, remove the start-state from the stack.
        BackwardARGState initialState = openCallStates.pop();
        AbstractState reducedExitState = data.getReducedStateForExpandedState(exitState);
        assert null
            != data.getReachedSetForInitialState(initialState.getWrappedState(), reducedExitState);
        blockInitAndExitStates.put(initialState, (ARGState) reducedExitState);
      }

      if (data.hasInitialState(state)) {
        assert partitioning.isCallNode(extractLocation(state))
            : "the mapping of initial state to reached-set should only exist for block-start-locations";
        // we start a new sub-reached-set, add state as start-state of a (possibly) open block.
        // if we are at lastState, we do not want to enter the block
        openCallStates.push((BackwardARGState) bamState);
      }
    }

    assert openCallStates.isEmpty()
        : "empty callstack expected after traversing the path: " + openCallStates;
    return blockInitAndExitStates;
  }

  /**
   * This method clones a reached-set partially and inserts it into the caches. The cloned
   * reached-set contains all abstract states except the cutState and the subtree below the
   * cutState. The predecessor of the cutState (and other states covered by subtree-states) will be
   * updated with a new precision.
   *
   * @param rootState the non-reduced initial state of the reached-set. This state is not part of
   *     the reached-set
   * @param cutState where to abort cloning, no child will be cloned, and states covered by children
   *     will be added to waitlist. This state is part of the reached-set.
   * @param pPrecisionsLst new precision for the cutState
   */
  private void handleSubtree(
      final ARGState rootState,
      final ARGState exitState,
      final ARGState cutState,
      final List<Pair<Precision, Predicate<? super Precision>>> pPrecisionsLst) {
    ReachedSet reached = data.getReachedSetForInitialState(rootState, exitState);
    assert reached.contains(cutState);
    assert reached.contains(exitState);

    ReachedSet clone = cloneReachedSetPartially(reached, cutState, pPrecisionsLst);
    Block block = partitioning.getBlockForCallNode(AbstractStates.extractLocation(rootState));

    data.bamCache.remove(clone.getFirstState(), clone.getPrecision(clone.getFirstState()), block);
    data.bamCache.put(
        clone.getFirstState(), clone.getPrecision(clone.getFirstState()), block, clone);
  }

  /**
   * returns only those states, where a block starts that is 'open' at the cutState. We use the most
   * inner block as reference point for the cut-state.
   * Order of result is from most inner block to most outer block (=mainBlock).
   */
  private Deque<ARGState> getRelevantCallStates(
      final List<ARGState> path, final ARGState bamCutState) {
    final Deque<ARGState> openCallStates = new ArrayDeque<>();
    for (final ARGState bamState : path) {

      final ARGState state = ((BackwardARGState) bamState).getARGState();

      if (bamCutState == bamState) {
        // do not enter or leave a block, when we found the cutState.
        break;
      }

      // ASSUMPTION: there can be several block-exits at once per location, but only one block-entry per location.

      // we use a loop here, because a return-node can be the exit of several blocks at once.
      ARGState tmp = state;
      while (data.hasExpandedState(tmp) && bamCutState != bamState) {
        assert partitioning.isReturnNode(extractLocation(tmp))
            : "the mapping of expanded to reduced state should only exist for block-return-locations";
        // we are leaving a block, remove the start-state from the stack.
        tmp = (ARGState) data.getReducedStateForExpandedState(tmp);
        openCallStates.pop();
        // INFO:
        // if we leave several blocks at once, we leave the blocks in reverse order,
        // because the call-state of the most outer block is checked first.
        // We ignore this here, because we just need the 'number' of block-exits.
      }

      if (data.hasInitialState(state)) {
        assert partitioning.isCallNode(extractLocation(state))
            : "the mapping of initial state to reached-set should only exist for block-start-locations";
        // we start a new sub-reached-set, add state as start-state of a (possibly) open block.
        // if we are at lastState, we do not want to enter the block
        openCallStates.push(bamState);
      }
    }

    return openCallStates;
  }

  /**
   * This method creates a copy of the given {@link ReachedSet}, without a subtree of states
   * collected as children of cutState (inclusive). Nested Block-ReachedSets are only referenced,
   * not cloned.
   *
   * <p>We assume that all abstract states (except first level {@link ARGState}s) are immutable,
   * because we only build new {@link ARGState}s and re-use their content directly.
   */
  private ReachedSet cloneReachedSetPartially(
      final ReachedSet pReached,
      final ARGState cutState,
      final List<Pair<Precision, Predicate<? super Precision>>> pPrecisionsLst) {

    assert pReached.contains(cutState);

    // get subgraph, iteration order (natural state numbering -> Treeset) is important,
    // because we have to keep it and create cloned states in the same order
    Set<ARGState> reachedStates =
        new TreeSet<>(((ARGState) pReached.getFirstState()).getSubgraph());

    // get all states that should not be part of the cloned reached-set,
    // because the states are below the cutState (including transitive coverage)
    Set<ARGState> toRemove = getStatesToRemove(cutState);
    Predicate<AbstractState> keepStates = s -> !toRemove.contains(s); // negated filter

    assert reachedStates.size() > toRemove.size() : "removing all states is not possible";
    assert reachedStates.containsAll(pReached.asCollection())
        : "reachedset should be subset of all states";
    assert reachedStates.containsAll(toRemove)
        : "states to be removed should be subset of all states";

    Map<ARGState, ARGState> cloneMapping = new LinkedHashMap<>();

    // first get a swallow copy of the ARG
    cloneARG(reachedStates, keepStates, cloneMapping);

    // then update BAM-data for entry- and exit-nodes,
    // including intermediate nodes that lead to sub-reached-sets
    updateBAMData(cloneMapping);

    ReachedSet clonedReached =
        buildClonedReachedSet(pReached, pPrecisionsLst, keepStates, cloneMapping);

    assert clonedReached.size() < pReached.size();
    return clonedReached;
  }

  private Set<ARGState> getStatesToRemove(final ARGState cutState) {
    Set<ARGState> toRemove = cutState.getSubgraph();

    // collect all elements covered by the subtree,
    // we assume there is no transitive coverage a->b->c, then we need a fixed-point algorithm
    List<ARGState> newToUnreach = new ArrayList<>();
    for (ARGState ae : toRemove) {
      newToUnreach.addAll(ae.getCoveredByThis());
    }
    toRemove.addAll(newToUnreach);

    return toRemove;
  }

  /** Build cloned ARG as a flat copy of existing states. */
  private void cloneARG(
      final Set<ARGState> reachedStates,
      final Predicate<AbstractState> keepStates,
      final Map<ARGState, ARGState> cloneMapping) {
    for (AbstractState abstractState : Iterables.filter(reachedStates, keepStates)) {
      ARGState state = (ARGState) abstractState;
      ARGState clonedState = new ARGState(state.getWrappedState(), null /* add parents later */);
      cloneMapping.put(state, clonedState);
    }

    // add parents, to get a graph like the old reached-set
    for (Entry<ARGState, ARGState> e : cloneMapping.entrySet()) {
      for (ARGState parent : Iterables.filter(e.getKey().getParents(), keepStates)) {
        e.getValue().addParent(cloneMapping.get(parent));
      }
    }

    // add coverage-edges, to get a graph like the old reached-set
    Set<ARGState> coveredStates = new HashSet<>();
    for (Entry<ARGState, ARGState> e : cloneMapping.entrySet()) {
      for (ARGState covered : Iterables.filter(e.getKey().getCoveredByThis(), keepStates)) {
        cloneMapping.get(covered).setCovered(cloneMapping.get(e.getKey()));
        coveredStates.add(covered);
      }
    }
  }

  /**
   * Update all BAM-related mappings, caches, data-structures, such that the new reached-set will be
   * used where needed.
   */
  private void updateBAMData(final Map<ARGState, ARGState> cloneMapping) {
    for (Entry<ARGState, ARGState> e : cloneMapping.entrySet()) {
      ARGState state = e.getKey();
      ARGState clonedState = e.getValue();
      if (data.hasInitialState(state)) {
        for (ARGState child : state.getChildren()) {
          assert data.hasExpandedState(child);
          ARGState reducedExitState = (ARGState) data.getReducedStateForExpandedState(child);
          ReachedSet reached = data.getReachedSetForInitialState(state, reducedExitState);
          data.registerInitialState(clonedState, reducedExitState, reached);
        }
      }
      if (data.hasExpandedState(state)) {
        data.registerExpandedState(
            clonedState,
            data.getExpandedPrecisionForExpandedState(state),
            data.getReducedStateForExpandedState(state),
            data.getInnerBlockForExpandedState(state));
      }
    }
  }

  /**
   * Build a new reached-set that contains the cloned states. The waitlist consists only of states
   * that need to be visited. The states in the waitlist get an updated precision if needed.
   */
  private ReachedSet buildClonedReachedSet(
      final ReachedSet pReached,
      final List<Pair<Precision, Predicate<? super Precision>>> pPrecisionsLst,
      final Predicate<AbstractState> keepStates,
      final Map<ARGState, ARGState> cloneMapping) {
    // build reachedset, iteration order is very important here,
    // because pReached and clonedReached should behave similar, e.g. first state is equal
    ReachedSet clonedReached = data.reachedSetFactory.create();
    for (AbstractState abstractState : Iterables.filter(pReached, keepStates)) {
      ARGState state = (ARGState) abstractState;
      ARGState clonedState = cloneMapping.get(state);

      // add cloned state to reached (and to waitlist)
      clonedReached.add(clonedState, pReached.getPrecision(state));

      // maybe we do not need to visit the state again, because its analysis is already finished
      boolean isStateFinished = !pReached.getWaitlist().contains(state);

      // all parent are cloned
      isStateFinished &= Iterables.all(state.getParents(), keepStates);

      // all children are cloned
      isStateFinished &= Iterables.all(state.getChildren(), keepStates);

      if (isStateFinished) {
        clonedReached.removeOnlyFromWaitlist(clonedState);
      } else {
        clonedReached.updatePrecision(
            clonedState, getUpdatedPrecision(pReached.getPrecision(state), pPrecisionsLst));
      }
    }
    return clonedReached;
  }

  /** Update any sub-precision with matching type. */
  private static Precision getUpdatedPrecision(
      Precision statePrec,
      final List<Pair<Precision, Predicate<? super Precision>>> pPrecisionsLst) {
    Preconditions.checkNotNull(statePrec);
    for (Pair<Precision, Predicate<? super Precision>> p : pPrecisionsLst) {
      Precision adaptedPrec = Precisions.replaceByType(statePrec, p.getFirst(), p.getSecond());
      // adaptedPrec == null, if the precision component was not changed
      if (adaptedPrec != null) {
        statePrec = adaptedPrec;
      }
      Preconditions.checkNotNull(statePrec);
    }
    return statePrec;
  }

  @Override
  public void removeSubtree(ARGState pE) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString(){
    return "BAMReachedSet {{" + asReachedSet().asCollection().toString() + "}}";
  }
}