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
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
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
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;

public class BAMReachedSet extends ARGReachedSet.ForwardingARGReachedSet {

  private final AbstractBAMCPA bamcpa;
  private final BAMDataManager data;
  private final ARGPath path;
  private final StatTimer removeCachedSubtreeTimer;

  public BAMReachedSet(AbstractBAMCPA cpa, ARGReachedSet pMainReachedSet, ARGPath pPath,
      StatTimer pRemoveCachedSubtreeTimer) {
    super(pMainReachedSet);
    this.bamcpa = cpa;
    this.data = cpa.getData();
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
      ARGState pCutState,
      final List<Precision> pPrecisions,
      final List<Predicate<? super Precision>> pPrecisionTypes)
      throws InterruptedException {
    final BackwardARGState cutState = (BackwardARGState) pCutState;
    final ARGState cutPointAsArgState = getReachedState(cutState);
    Preconditions.checkArgument(pPrecisions.size() == pPrecisionTypes.size());
    final List<Pair<Precision, Predicate<? super Precision>>> newPrecisionsLst =
        Pair.zipList(pPrecisions, pPrecisionTypes);

    assert path.getFirstState().getSubgraph().contains(cutState);

    // get blocks that need to be touched
    Map<BackwardARGState, ARGState> blockInitAndExitStates =
        getBlockInitAndExitStates(path.asStatesList());
    List<BackwardARGState> relevantCallStates = getRelevantCallStates(path.asStatesList(), cutState);
//    assert relevantCallStates.peekLast() == path.getFirstState()
//        : "root should be relevant: " + relevantCallStates.peekLast() + " + " + path.getFirstState();
//    assert relevantCallStates.size() >= 1
//        : "at least the main-function should be open at the target-state";
    // TODO add element's block, if necessary?

    bamcpa.getLogger().log(Level.FINEST, "Path across blocks:\n" +
        dumpPath(path.asStatesList(), blockInitAndExitStates, false));

    if (delegate.asReachedSet().contains(cutPointAsArgState)) {
      assert relevantCallStates.isEmpty();
      delegate.removeSubtree(cutPointAsArgState, pPrecisions, pPrecisionTypes);
      // nothing else needed, because cutPoint is not at entry- or exit-location of a block.

    } else {
      assert !relevantCallStates.isEmpty();
      final BackwardARGState lastRelevantNode = Iterables.getLast(relevantCallStates);

      // handle reached-sets from most inner reached-set to most-outer reached-set
      BackwardARGState currentCutState = cutState;
      for (BackwardARGState callState : Lists.reverse(relevantCallStates)) {

        bamcpa.getLogger().logf(Level.FINEST, "removing %s from reachedset with root %s", currentCutState, callState);

        removeCachedSubtreeTimer.start();

        handleSubtree(
            callState.getARGState(),
            checkNotNull(blockInitAndExitStates.get(callState)),
            getReachedState(currentCutState),
            mustUpdatePrecision(lastRelevantNode, cutState, currentCutState) ? newPrecisionsLst : Collections.emptyList());

        currentCutState = callState;

        removeCachedSubtreeTimer.stop();
      }

      final UnmodifiableReachedSet mainReachedSet = delegate.asReachedSet();
      if (mainReachedSet.getFirstState() == relevantCallStates.get(0).getARGState()
          && ((ARGState)mainReachedSet.getLastState()).getParents().contains(mainReachedSet.getFirstState())) {
        // This code is needed for analysis of recursive programs. The main-loop in
        // {@link BAMTransferRelationWithFixPointForRecursion} uses the complete program as block.
        // The main-reachedset contains only the root, exit-states and targets.
        // Instead of removing the root-state, we only remove the target state.
        delegate.removeSubtree((ARGState)mainReachedSet.getLastState());
      } else {
        if (mustUpdatePrecision(lastRelevantNode, cutState, currentCutState)) {
          delegate.removeSubtree(relevantCallStates.get(0).getARGState(), pPrecisions, pPrecisionTypes);
        } else {
          delegate.removeSubtree(relevantCallStates.get(0).getARGState());
        }
      }
    }

    // post-processing, cleanup data-structures: We remove all states reachable from 'element'.
    // The only important step is to remove the last state of the reached-set,
    // because without this step there is an assertion in Predicate-RefinementStrategy.
    // We can ignore waitlist-updates and coverage here, because there is no coverage in a BAM-CEX.
    for (ARGState state : cutState.getSubgraph()) {
      state.removeFromARG();
    }
  }

  private boolean mustUpdatePrecision(
      final BackwardARGState lastRelevantNode, final BackwardARGState cutState,
      final BackwardARGState currentCutState) {

    // special option (mostly for testing)
    if (bamcpa.getRefinementHeuristics().doPrecisionRefinementForAllStates()) {
      return true;
    }

    // last iteration, most inner block for refinement
    // This heuristics works best on test_locks-examples, otherwise they have exponential blowup.
    if (bamcpa.getRefinementHeuristics().doPrecisionRefinementForMostInnerBlock()
        && currentCutState == lastRelevantNode) {
      return true;
    }

    // this is the important case: lazy refinement expects a new precision at this place.
    if (currentCutState == cutState) {
      return true;
    }

    // otherwise we do not need to update the precision
    return false;
  }

  private ARGState getReachedState(ARGState state) {
    return (ARGState) data.getInnermostState(((BackwardARGState) state).getARGState());
  }

  private String dumpPath(
      final ImmutableList<ARGState> path,
      final Map<BackwardARGState, ARGState> pBlockInitAndExitStates,
      final boolean onlyEntryOrExitStates) {
    final StringBuilder str = new StringBuilder();
    int indent = 0;
    for (final ARGState bamState : path) {
      final ARGState state = ((BackwardARGState) bamState).getARGState();
      StringBuilder line = new StringBuilder();
      boolean containsEntryOrExit = false;
      line.append(indent).append(" ").append(dump(bamState)).append(" :: ");
      StringBuilder expandedStr = new StringBuilder();
      for (AbstractState exitState : Lists.reverse(data.getExpandedStatesList(state))) {
        indent--;
        expandedStr.append(dump(exitState)).append(" <- ");
        containsEntryOrExit = true;
      }
      for (int i = 0; i < indent; i++) {
        line.append("     |       "); // length == 14
      }
      line.append(expandedStr);
      line.append(dump(data.hasExpandedState(state) ? data.getInnermostState(state) : state));
      if (data.hasInitialState(state)) {
        line.append(" -> ")
            .append(
                dump(
                    data.getReachedSetForInitialState(state, pBlockInitAndExitStates.get(bamState))
                        .getFirstState()));
        indent++;
        containsEntryOrExit = true;
      }
      line.append("\n");
      if (!onlyEntryOrExitStates || containsEntryOrExit) {
        str.append(line);
      }
    }
    assert indent == 0;
    return str.toString();
  }

  private String dump(AbstractState state) {
    return String.format("%9.9s",
        ((ARGState) state).getStateId() + "[" + extractLocation(state) + "]");
  }

  /** Returns a mapping of wrapped non-reduced init-state towards non-wrapped reduced exit-state. */
  private Map<BackwardARGState, ARGState> getBlockInitAndExitStates(ImmutableList<ARGState> path) {
    final Map<BackwardARGState, ARGState> blockInitAndExitStates = new LinkedHashMap<>();
    final Deque<BackwardARGState> openCallStates = new ArrayDeque<>();
    for (final ARGState bamState : path) {
      final ARGState state = ((BackwardARGState) bamState).getARGState();

      // ASSUMPTION: there can be several block-exits at once per location, but only one block-entry per location.

      // we use a loop here, because a return-node can be the exit of several blocks at once.
      for (AbstractState exitState : data.getExpandedStatesList(state)) {
        // we are leaving a block, remove the start-state from the stack.
        BackwardARGState initialState = openCallStates.pop();
        AbstractState reducedExitState = data.getReducedStateForExpandedState(exitState);
        assert null
            != data.getReachedSetForInitialState(initialState.getWrappedState(), reducedExitState);
        blockInitAndExitStates.put(initialState, (ARGState) reducedExitState);
      }

      if (data.hasInitialState(state)) {
        assert bamcpa.getBlockPartitioning().isCallNode(extractLocation(state))
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
   * @param exitState needed to identify the reached-set in the BAM-cache
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
    assert reached.contains(cutState) : String.format("cutState %s is not in reached-set with root %s.",
        cutState, reached.getFirstState());
    assert reached.contains(exitState) : String.format("exitState %s is not in reached-set with root %s.",
        exitState, reached.getFirstState());

    ReachedSet clone = cloneReachedSetPartially(reached, cutState, pPrecisionsLst);
    Block block = bamcpa.getBlockPartitioning().getBlockForCallNode(AbstractStates.extractLocation(rootState));

    data.getCache().remove(clone.getFirstState(), clone.getPrecision(clone.getFirstState()), block);
    data.getCache().put(
        clone.getFirstState(), clone.getPrecision(clone.getFirstState()), block, clone);
  }

  /** returns only those states, where a block starts that is 'open' at the cutState.
   * main-RS-root is only included, if it was reduced in TransferRelation. */
  private List<BackwardARGState> getRelevantCallStates(List<ARGState> path, ARGState bamCutState) {
    final Deque<BackwardARGState> openCallStates = new ArrayDeque<>();
    for (final ARGState pathState : path) {

      final BackwardARGState bamState = (BackwardARGState) pathState;
      final ARGState state = bamState.getARGState();

      // ASSUMPTION: there can be several block-exits at once per location, but only one block-entry per location.

      // we use a loop here, because a return-node can be the exit of several blocks at once.
      ARGState tmp = state;
      while (data.hasExpandedState(tmp) && bamCutState != bamState) {
        assert bamcpa.getBlockPartitioning().isReturnNode(extractLocation(tmp)) : "the mapping of expanded to reduced state should only exist for block-return-locations";
        // we are leaving a block, remove the start-state from the stack.
        tmp = (ARGState) data.getReducedStateForExpandedState(tmp);
        openCallStates.removeLast();
        // INFO:
        // if we leave several blocks at once, we leave the blocks in reverse order,
        // because the call-state of the most outer block is checked first.
        // We ignore this here, because we just need the 'number' of block-exits.
      }

      if (bamCutState == bamState) {
        // do not enter or leave a block, when we found the cutState.
        break;
      }

      if (data.hasInitialState(state)) {
        assert bamcpa.getBlockPartitioning().isCallNode(extractLocation(state)) : "the mapping of initial state to reached-set should only exist for block-start-locations";
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

    bamcpa.getLogger().log(Level.ALL, Collections2.transform(cloneMapping.entrySet(),
        e -> e.getKey().getStateId() + "-" + e.getValue().getStateId()));

    assert clonedReached.size() < pReached.size();
    return clonedReached;
  }

  private static Set<ARGState> getStatesToRemove(final ARGState cutState) {
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
  private static void cloneARG(
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
            data.getExpandedPrecisionForState(state),
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
    ReachedSet clonedReached = data.getReachedSetFactory().create();
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
        clonedState.markExpanded(); // for debugging and for nicer dot-graphs: set flag 'expanded'
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