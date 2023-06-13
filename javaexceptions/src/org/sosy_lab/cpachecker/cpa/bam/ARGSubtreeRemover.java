// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.bam.BAMSubgraphComputer.BackwardARGState;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMCache;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMDataManager;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;

public abstract class ARGSubtreeRemover {

  protected final BlockPartitioning partitioning;
  protected final AbstractBAMCPA bamCpa;
  protected final BAMDataManager data;
  protected final Reducer wrappedReducer;
  protected final BAMCache bamCache;
  protected final LogManager logger;
  protected final TimerWrapper removeCachedSubtreeTimer;
  protected final boolean doPrecisionRefinementForAllStates;

  protected ARGSubtreeRemover(AbstractBAMCPA bamCpa, TimerWrapper pRemoveCachedSubtreeTimer) {
    partitioning = bamCpa.getBlockPartitioning();
    this.bamCpa = bamCpa;
    data = bamCpa.getData();
    wrappedReducer = bamCpa.getReducer();
    bamCache = bamCpa.getData().getCache();
    logger = bamCpa.getLogger();
    removeCachedSubtreeTimer = pRemoveCachedSubtreeTimer;
    doPrecisionRefinementForAllStates = bamCpa.doPrecisionRefinementForAllStates();
  }

  /**
   * Update the reached-sets such that the subtree below the given state is removed and the state
   * itself is updated with the new precision. The sub-class can decide how many other states are
   * updated and whether the cache is touched.
   */
  abstract void removeSubtree(
      ARGReachedSet pMainReachedSet,
      ARGPath pPath,
      ARGState pState,
      List<Precision> pNewPrecisions,
      List<Predicate<? super Precision>> pNewPrecisionTypes)
      throws InterruptedException;

  protected ARGState getReachedState(ARGState state) {
    return (ARGState) data.getInnermostState(((BackwardARGState) state).getARGState());
  }

  /**
   * Returns a mapping of wrapped non-reduced init-state towards non-wrapped reduced exit-state
   * along the path.
   */
  protected Map<BackwardARGState, ARGState> getBlockInitAndExitStates(
      ImmutableList<ARGState> path) {
    final Map<BackwardARGState, ARGState> blockInitAndExitStates = new LinkedHashMap<>();
    final Deque<BackwardARGState> openCallStates = new ArrayDeque<>();
    for (final ARGState bamState : path) {
      final ARGState state = ((BackwardARGState) bamState).getARGState();

      // ASSUMPTION: there can be several block-exits at once per location,
      // but only one block-entry per location.

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
        assert partitioning.isCallNode(extractLocation(state))
            : "the mapping of initial state to reached-set should only exist for"
                + " block-start-locations";
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
   * returns only those states, where a block starts that is 'open' at the cutState. main-RS-root is
   * only included, if it was reduced in TransferRelation.
   */
  protected List<BackwardARGState> getRelevantCallStates(
      List<ARGState> path, ARGState bamCutState) {
    final Deque<BackwardARGState> openCallStates = new ArrayDeque<>();
    for (final ARGState pathState : path) {

      final BackwardARGState bamState = (BackwardARGState) pathState;
      final ARGState state = bamState.getARGState();

      // ASSUMPTION: there can be several block-exits at once per location, but only one block-entry
      // per location.

      // we use a loop here, because a return-node can be the exit of several blocks at once.
      ARGState tmp = state;
      while (data.hasExpandedState(tmp) && !bamCutState.equals(bamState)) {
        assert partitioning.isReturnNode(extractLocation(tmp))
            : "the mapping of expanded to reduced state should only exist for"
                + " block-return-locations";
        // we are leaving a block, remove the start-state from the stack.
        tmp = (ARGState) data.getReducedStateForExpandedState(tmp);
        openCallStates.removeLast();
        // INFO:
        // if we leave several blocks at once, we leave the blocks in reverse order,
        // because the call-state of the most outer block is checked first.
        // We ignore this here, because we just need the 'number' of block-exits.
      }

      if (bamCutState.equals(bamState)) {
        // do not enter or leave a block, when we found the cutState.
        break;
      }

      if (data.hasInitialState(state)) {
        assert partitioning.isCallNode(extractLocation(state))
            : "the mapping of initial state to reached-set should only exist for"
                + " block-start-locations";
        // we start a new sub-reached-set, add state as start-state of a (possibly) open block.
        // if we are at lastState, we do not want to enter the block
        openCallStates.addLast(bamState);
      }
    }

    return new ArrayList<>(openCallStates);
  }
}
