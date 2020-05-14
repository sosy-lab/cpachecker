/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.bam.BAMSubgraphComputer.BackwardARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;

public class ARGCopyOnWriteSubtreeRemover extends ARGSubtreeRemover {

  private final boolean doPrecisionRefinementForMostInnerBlock;

  public ARGCopyOnWriteSubtreeRemover(
      AbstractBAMCPA bamCpa, TimerWrapper pRemoveCachedSubtreeTimer) {
    super(bamCpa, pRemoveCachedSubtreeTimer);
    doPrecisionRefinementForMostInnerBlock = bamCpa.doPrecisionRefinementForMostInnerBlock();
  }

  @Override
  void removeSubtree(
      ARGReachedSet pMainReachedSet,
      ARGPath pPath,
      ARGState pState,
      List<Precision> pNewPrecisions,
      List<Predicate<? super Precision>> pNewPrecisionTypes)
      throws InterruptedException {

    final BackwardARGState cutState = (BackwardARGState) pState;
    final ARGState cutPointAsArgState = getReachedState(cutState);
    Preconditions.checkArgument(pNewPrecisions.size() == pNewPrecisionTypes.size());

    assert pPath.getFirstState().getSubgraph().contains(cutState);

    // get blocks that need to be touched
    Map<BackwardARGState, ARGState> blockInitAndExitStates =
        getBlockInitAndExitStates(pPath.asStatesList());
    List<BackwardARGState> relevantCallStates =
        getRelevantCallStates(pPath.asStatesList(), cutState);
    //  assert relevantCallStates.peekLast() == path.getFirstState()
    //      : "root should be relevant: " + relevantCallStates.peekLast() + " + " +
    // path.getFirstState();
    //  assert relevantCallStates.size() >= 1
    //      : "at least the main-function should be open at the target-state";
    // TODO add element's block, if necessary?

    if (pMainReachedSet.asReachedSet().contains(cutPointAsArgState)) {
      assert relevantCallStates.isEmpty();
      pMainReachedSet.removeSubtree(cutPointAsArgState, pNewPrecisions, pNewPrecisionTypes);
      // nothing else needed, because cutPoint is not at entry- or exit-location of a block.

    } else {
      assert !relevantCallStates.isEmpty();
      final BackwardARGState lastRelevantNode = Iterables.getLast(relevantCallStates);
      final List<Pair<Precision, Predicate<? super Precision>>> newPrecisionsLst =
          Pair.zipList(pNewPrecisions, pNewPrecisionTypes);

      // handle reached-sets from most inner reached-set to most-outer reached-set
      BackwardARGState currentCutState = cutState;
      for (BackwardARGState callState : Lists.reverse(relevantCallStates)) {

        logger.logf(
            Level.FINEST, "removing %s from reachedset with root %s", currentCutState, callState);

        removeCachedSubtreeTimer.start();

        handleSubtree(
            callState.getARGState(),
            checkNotNull(blockInitAndExitStates.get(callState)),
            getReachedState(currentCutState),
            mustUpdatePrecision(lastRelevantNode, cutState, currentCutState)
                ? newPrecisionsLst
                : ImmutableList.of());

        currentCutState = callState;

        removeCachedSubtreeTimer.stop();
      }

      final UnmodifiableReachedSet mainReachedSet = pMainReachedSet.asReachedSet();
      if (mainReachedSet.getFirstState() == relevantCallStates.get(0).getARGState()
          && ((ARGState) mainReachedSet.getLastState())
              .getParents()
              .contains(mainReachedSet.getFirstState())) {
        // This code is needed for analysis of recursive programs. The main-loop in
        // {@link BAMTransferRelationWithFixPointForRecursion} uses the complete program as block.
        // The main-reachedset contains only the root, exit-states and targets.
        // Instead of removing the root-state, we only remove the target state.
        pMainReachedSet.removeSubtree((ARGState) mainReachedSet.getLastState());
      } else {
        if (mustUpdatePrecision(lastRelevantNode, cutState, currentCutState)) {
          pMainReachedSet.removeSubtree(
              relevantCallStates.get(0).getARGState(), pNewPrecisions, pNewPrecisionTypes);
        } else {
          pMainReachedSet.removeSubtree(relevantCallStates.get(0).getARGState());
        }
      }
    }
  }

  private boolean mustUpdatePrecision(
      final BackwardARGState lastRelevantNode,
      final BackwardARGState cutState,
      final BackwardARGState currentCutState) {

    // special option (mostly for testing)
    if (doPrecisionRefinementForAllStates) {
      return true;
    }

    // last iteration, most inner block for refinement
    // This heuristics works best on test_locks-examples, otherwise they have exponential blowup.
    if (doPrecisionRefinementForMostInnerBlock
        && Objects.equals(currentCutState, lastRelevantNode)) {
      return true;
    }

    // this is the important case: lazy refinement expects a new precision at this place.
    if (Objects.equals(currentCutState, cutState)) {
      return true;
    }

    // otherwise we do not need to update the precision
    return false;
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
    assert reached.contains(cutState)
        : String.format(
            "cutState %s is not in reached-set with root %s.", cutState, reached.getFirstState());
    assert reached.contains(exitState)
        : String.format(
            "exitState %s is not in reached-set with root %s.", exitState, reached.getFirstState());

    ReachedSet clone = cloneReachedSetPartially(reached, cutState, pPrecisionsLst);
    Block block = partitioning.getBlockForCallNode(AbstractStates.extractLocation(rootState));

    // override existing cache-entry
    data.getCache()
        .put(clone.getFirstState(), clone.getPrecision(clone.getFirstState()), block, clone);
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

    // first get a swallow copy of the ARG
    Map<ARGState, ARGState> cloneMapping = cloneARG(reachedStates, keepStates);

    // then update BAM-data for entry- and exit-nodes,
    // including intermediate nodes that lead to sub-reached-sets
    updateBAMData(cloneMapping);

    ReachedSet clonedReached =
        buildClonedReachedSet(pReached, pPrecisionsLst, keepStates, cloneMapping);

    logger.log(
        Level.ALL,
        Collections2.transform(
            cloneMapping.entrySet(),
            e -> e.getKey().getStateId() + "-" + e.getValue().getStateId()));

    assert clonedReached.size() < pReached.size();
    return clonedReached;
  }

  /**
   * returns the set of all states that need to be removed with the given cutpoint, all transitive
   * successors and all states that are covered by them.
   */
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

  /** Build cloned ARG as a flat copy of existing states, but limited to only some states. */
  private static Map<ARGState, ARGState> cloneARG(
      final Set<ARGState> reachedStates, final Predicate<AbstractState> keepStates) {

    final Map<ARGState, ARGState> cloneMapping = new LinkedHashMap<>();

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
    for (Entry<ARGState, ARGState> e : cloneMapping.entrySet()) {
      for (ARGState covered : Iterables.filter(e.getKey().getCoveredByThis(), keepStates)) {
        cloneMapping.get(covered).setCovered(cloneMapping.get(e.getKey()));
      }
    }

    return cloneMapping;
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
            clonedState, updatePrecision(pReached.getPrecision(state), pPrecisionsLst));
      }
    }
    return clonedReached;
  }

  /** Update any sub-precision with matching type. */
  private static Precision updatePrecision(
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
}
