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
import com.google.common.collect.Lists;
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
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.bam.BAMSubgraphComputer.BackwardARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Precisions;

public class BAMReachedSet extends ARGReachedSet.ForwardingARGReachedSet {

  private final BAMCPA bamCpa;
  private final BAMDataManager data;
  private final ARGPath path;
  private final ARGState rootOfSubgraph;
  private final Timer removeCachedSubtreeTimer;

  public BAMReachedSet(BAMCPA cpa, ARGReachedSet pMainReachedSet, ARGPath pPath,
      ARGState pRootOfSubgraph,
      Timer pRemoveCachedSubtreeTimer) {
    super(pMainReachedSet);
    this.bamCpa = cpa;
    this.data = bamCpa.getData();
    this.path = pPath;
    this.rootOfSubgraph = pRootOfSubgraph;
    this.removeCachedSubtreeTimer = pRemoveCachedSubtreeTimer;

    assert rootOfSubgraph.getSubgraph().containsAll(path.asStatesList()) : "path should traverse reachable states";
    assert pRootOfSubgraph == path.getFirstState() : "path should start with root-state";
  }

  @Override
  public UnmodifiableReachedSet asReachedSet() {
    return new BAMReachedSetView(rootOfSubgraph, path.getLastState(),
        s -> super.asReachedSet().getPrecision(super.asReachedSet().getLastState()));
    // TODO do we really need the target-precision for refinements and not the actual one?
  }

  @SuppressWarnings("unchecked")
  @Override
  public void removeSubtree(
      ARGState element, Precision newPrecision, Predicate<? super Precision> pPrecisionType)
      throws InterruptedException {
    removeSubtree(element, ImmutableList.of(newPrecision), ImmutableList.of(pPrecisionType));
  }

  @Override
  public void removeSubtree(
      ARGState element,
      List<Precision> pPrecisions,
      List<Predicate<? super Precision>> pPrecisionTypes)
      throws InterruptedException {
    Preconditions.checkArgument(pPrecisionTypes.size() == pPrecisionTypes.size());
    assert rootOfSubgraph.getSubgraph().contains(element);

    // get blocks that need to be touched
    Map<BackwardARGState, ARGState> blockInitAndExitStates =
        getBlockInitAndExitStates(path.asStatesList());
    List<ARGState> relevantCallStates = getRelevantCallStates(path.asStatesList(), element);
    assert relevantCallStates.get(0) == rootOfSubgraph
        : "root should be relevant: " + relevantCallStates.get(0) + " + " + rootOfSubgraph;
    assert relevantCallStates.size() >= 1
        : "at least the main-function should be open at the target-state";
    // TODO add element's block, if necessary?

    ARGState tmp = element;
    for (ARGState callState : Lists.reverse(relevantCallStates)) {

      bamCpa
          .getLogger()
          .logf(Level.FINEST, "removing %s from reachedset with root %s", tmp, callState);

      removeCachedSubtreeTimer.start();

      handleSubtree(
          ((BackwardARGState) callState).getARGState(),
          checkNotNull(blockInitAndExitStates.get(callState)),
          data.getInnermostState(((BackwardARGState) tmp).getARGState()),
          pPrecisions,
          pPrecisionTypes);
      tmp = callState;

      removeCachedSubtreeTimer.stop();
    }

    // post-processing, cleanup data-structures: We remove all states reachable from 'element'.
    // The only important step is to remove the last state of the reached-set,
    // because without this step there is an assertion in Predicate-RefinementStrategy.
    // We can ignore waitlist-updates and coverage here, because there is no coverage in a BAM-CEX.
    for (ARGState state : element.getSubgraph()) {
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
        BackwardARGState initialState = openCallStates.removeLast();
        AbstractState reducedExitState = data.getReducedStateForExpandedState(exitState);
        assert null
            != data.getReachedSetForInitialState(initialState.getWrappedState(), reducedExitState);
        blockInitAndExitStates.put(initialState, (ARGState) reducedExitState);
      }

      if (data.hasInitialState(state)) {
        assert bamCpa.getBlockPartitioning().isCallNode(extractLocation(state))
            : "the mapping of initial state to reached-set should only exist for block-start-locations";
        // we start a new sub-reached-set, add state as start-state of a (possibly) open block.
        // if we are at lastState, we do not want to enter the block
        openCallStates.addLast((BackwardARGState) bamState);
      }
    }

    assert openCallStates.isEmpty()
        : "empty callstack expected after traversing the path: " + openCallStates;
    return blockInitAndExitStates;
  }

  /**
   * inserts a partially cloned reached-set into the caches.
   *
   * @param rootState the non-reduced initial state of the reached-set. This state is not part of
   *     the reached-set
   * @param cutState where to abort cloning, no child will be cloned, and states covered by children
   *     will be added to waitlist. This state is part of the reached-set.
   * @param pPrecisions new precision for the cutState
   * @param pPrecisionTypes new precision for the cutState
   */
  private void handleSubtree(
      final ARGState rootState,
      final ARGState exitState,
      final ARGState cutState,
      final List<Precision> pPrecisions,
      final List<Predicate<? super Precision>> pPrecisionTypes) {
    ReachedSet reached = data.getReachedSetForInitialState(rootState, exitState);
    assert reached.contains(cutState);
    assert reached.contains(exitState);

    ReachedSet clone = cloneReachedSetPartially(reached, cutState, pPrecisions, pPrecisionTypes);
    Block block =
        bamCpa
            .getBlockPartitioning()
            .getBlockForCallNode(AbstractStates.extractLocation(rootState));

    data.bamCache.remove(clone.getFirstState(), clone.getPrecision(clone.getFirstState()), block);
    data.bamCache.put(
        clone.getFirstState(), clone.getPrecision(clone.getFirstState()), block, clone);
  }

  /**
   * returns only those states, where a block starts that is 'open' at the cutState. We use the most
   * inner block as reference point for the cut-state.
   */
  private List<ARGState> getRelevantCallStates(
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
        assert bamCpa.getBlockPartitioning().isReturnNode(extractLocation(tmp))
            : "the mapping of expanded to reduced state should only exist for block-return-locations";
        // we are leaving a block, remove the start-state from the stack.
        tmp = (ARGState) data.getReducedStateForExpandedState(tmp);
        openCallStates.removeLast();
        // INFO:
        // if we leave several blocks at once, we leave the blocks in reverse order,
        // because the call-state of the most outer block is checked first.
        // We ignore this here, because we just need the 'number' of block-exits.
      }

      if (data.hasInitialState(state)) {
        assert bamCpa.getBlockPartitioning().isCallNode(extractLocation(state))
            : "the mapping of initial state to reached-set should only exist for block-start-locations";
        // we start a new sub-reached-set, add state as start-state of a (possibly) open block.
        // if we are at lastState, we do not want to enter the block
        openCallStates.addLast(bamState);
      }
    }

    return new ArrayList<>(openCallStates);
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
      final List<Precision> pPrecisions,
      final List<Predicate<? super Precision>> pPrecisionTypes) {

    assert pReached.contains(cutState);

    Set<ARGState> reachedStates = ((ARGState) pReached.getFirstState()).getSubgraph();
    Set<ARGState> toRemove = cutState.getSubgraph();

    // collect all elements covered by the subtree
    List<ARGState> newToUnreach = new ArrayList<>();
    for (ARGState ae : toRemove) {
      newToUnreach.addAll(ae.getCoveredByThis());
    }
    toRemove.addAll(newToUnreach);

    Predicate<AbstractState> keepStates = s -> !toRemove.contains(s); // negated filter

    assert reachedStates.size() > toRemove.size() : "removing all states is not possible";
    assert reachedStates.containsAll(pReached.asCollection())
        : "reachedset should be subset of all states";
    assert reachedStates.containsAll(toRemove)
        : "states to be removed should be subset of all states";

    Map<ARGState, ARGState> cloneMapping = new LinkedHashMap<>();

    // build cloned states
    reachedStates = new TreeSet<>(reachedStates);
    for (AbstractState abstractState : Iterables.filter(reachedStates, keepStates)) {
      ARGState state = (ARGState) abstractState;
      ARGState clonedState = new ARGState(state.getWrappedState(), null /* add parents later */);
      cloneMapping.put(state, clonedState);
    }

    // add parents
    for (Entry<ARGState, ARGState> e : cloneMapping.entrySet()) {
      for (ARGState parent : Iterables.filter(e.getKey().getParents(), keepStates)) {
        e.getValue().addParent(cloneMapping.get(parent));
      }
    }

    // add coverage
    Set<ARGState> coveredStates = new HashSet<>();
    for (Entry<ARGState, ARGState> e : cloneMapping.entrySet()) {
      for (ARGState covered : Iterables.filter(e.getKey().getCoveredByThis(), keepStates)) {
        cloneMapping.get(covered).setCovered(cloneMapping.get(e.getKey()));
        coveredStates.add(covered);
      }
    }

    // update all BAM-related mappings
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

    // build reachedset, iteration order is very important here,
    // because pReached and clonedReached should behave similar, e.g. first state is equal
    ReachedSet clonedReached = bamCpa.getData().reachedSetFactory.create();
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
            clonedState,
            updatePrecision(pReached.getPrecision(state), pPrecisions, pPrecisionTypes));
      }
    }

    assert clonedReached.size() < pReached.size();
    return clonedReached;
  }

  private Precision updatePrecision(
      Precision statePrec,
      final List<Precision> pPrecisions,
      final List<Predicate<? super Precision>> pPrecisionTypes) {
    Preconditions.checkNotNull(statePrec);
    for (int i = 0; i < pPrecisions.size(); i++) {
      Precision adaptedPrec =
          Precisions.replaceByType(statePrec, pPrecisions.get(i), pPrecisionTypes.get(i));
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