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

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;

public class BAM2TransferRelation implements TransferRelation {

  private final BlockPartitioning partitioning;
  private final ShutdownNotifier shutdownNotifier;
  private final TransferRelation wrappedTransfer;
  private final Reducer wrappedReducer;
  private final BAMDataManager data;
  private final LogManager logger;

  public BAM2TransferRelation(
      BlockPartitioning pPartitioning,
      ShutdownNotifier pShutdownNotifier,
      TransferRelation pTransfer,
      Reducer pReducer,
      BAMDataManager pData,
      LogManager pLogger) {
    partitioning = pPartitioning;
    shutdownNotifier = pShutdownNotifier;
    wrappedTransfer = pTransfer;
    wrappedReducer = pReducer;
    data = pData;
    logger = pLogger;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pState, Precision pPrecision)
      throws InterruptedException, CPATransferException {
    shutdownNotifier.shutdownIfNecessary();

    final CFANode node = extractLocation(pState);
    final ARGState argState = (ARGState) pState;

    if (exitBlockAnalysis(argState, node)) {
      return Collections.emptySet();
    }

    if (startNewBlockAnalysis(argState, node)) {
      return doRecursiveAnalysis(argState, pPrecision, node);
    }

    return wrappedTransfer.getAbstractSuccessors(pState, pPrecision);
  }

  /**
   * When a block-start-location is reached, we start a new sub-analysis for the entered block.
   *
   * @param pState the abstract state at the location
   * @param node the node of the location
   */
  private boolean startNewBlockAnalysis(final ARGState pState, final CFANode node) {
    return partitioning.isCallNode(node)
        // at begin of a block, we do not want to enter it again immediately
        && !pState.getParents().isEmpty()
        && !partitioning.getBlockForCallNode(node).equals(getBlockForState(pState));
  }

  /**
   * When finding a block-exit-location, we do not return any further states. This stops the current
   * running CPA-algorithm, when its waitlist is emtpy.
   *
   * @param argState the abstract state at the location
   * @param node the node of the location
   */
  private boolean exitBlockAnalysis(final ARGState argState, final CFANode node) {
    // exit- and start-locations can overlap -> order of statements in calling method.

    return partitioning.isReturnNode(node)
        // multiple exits at same location possible.
        && partitioning.getBlocksForReturnNode(node).contains(getBlockForState(argState));
  }

  /**
   * We assume that the root of a reached-set is a initial state at block-entry-location. Searching
   * backwards from an ARGstate should end in the root-state.
   */
  protected Block getBlockForState(ARGState state) {
    while (!state.getParents().isEmpty()) {
      state = state.getParents().iterator().next();
    }
    CFANode location = extractLocation(state);
    assert partitioning.isCallNode(location)
        : "root of reached-set must be located at block entry.";
    return partitioning.getBlockForCallNode(location);
  }

  /**
   * Enters a new block and performs a new analysis by recursively initiating {@link CPAAlgorithm},
   * or returns a cached result from {@link BAMCache}.
   *
   * <p>Postcondition: sets the {@code currentBlock} variable to the currently processed block.
   *
   * <p>Postcondition: pushes the current recursive level on the {@code stack}.
   *
   * @param initialState Initial state of the analyzed block.
   * @param pPrecision Initial precision associated with the block start.
   * @param node Node corresponding to the block start.
   * @return Set of states associated with the block exit.
   */
  private Collection<AbstractState> doRecursiveAnalysis(
      final ARGState initialState, final Precision pPrecision, final CFANode node)
      throws CPATransferException, InterruptedException {

    //Create ReachSet with node as initial element (+ add corresponding Location+CallStackElement)
    //do an CPA analysis to get the complete reachset
    //if lastElement is error State
    // -> return lastElement and break at precision adjustment
    //else
    // -> compute which states refer to return nodes
    // -> return these states as successor
    // -> cache the result

    final Block outerSubtree = getBlockForState(initialState);
    final Block innerSubtree = partitioning.getBlockForCallNode(node);
    assert innerSubtree.getCallNodes().contains(node);

    final AbstractState reducedInitialState =
        wrappedReducer.getVariableReducedState(initialState, innerSubtree, node);
    final Precision reducedInitialPrecision =
        wrappedReducer.getVariableReducedPrecision(pPrecision, innerSubtree);

    final Pair<Collection<AbstractState>, ReachedSet> reducedResult =
        getReducedResult(initialState, reducedInitialState, reducedInitialPrecision, innerSubtree);

    return expandResultStates(
        reducedResult.getFirst(),
        reducedResult.getSecond(),
        innerSubtree,
        outerSubtree,
        initialState,
        pPrecision);
  }

  /**
   * Analyse the block starting at the node with {@code initialState}. If there is a result in the
   * cache ({@code data.bamCache}), it is used, otherwise a recursive {@link CPAAlgorithm} is
   * started.
   *
   * @param initialState State associated with the block entry.
   * @param reducedInitialState Reduced {@code initialState}.
   * @param reducedInitialPrecision Reduced precision associated with the block entry.
   * @return Set of reduced pairs of abstract states associated with the exit of the block and the
   *     reached-set they belong to.
   */
  private Pair<Collection<AbstractState>, ReachedSet> getReducedResult(
      final AbstractState initialState,
      final AbstractState reducedInitialState,
      final Precision reducedInitialPrecision,
      final Block innerSubtree)
      throws CPATransferException {

    // Try to get an element from cache. A previously computed element consists of
    // a reached set associated with the recursive call.
    final Pair<ReachedSet, Collection<AbstractState>> pair =
        data.bamCache.get(reducedInitialState, reducedInitialPrecision, innerSubtree);
    ReachedSet cachedReached = pair.getFirst();
    final Collection<AbstractState> cachedReturnStates = pair.getSecond();

    assert cachedReturnStates == null || cachedReached != null
        : "there cannot be result-states without reached-states";

    if (cachedReturnStates == null) {
      // no cache hit, block summary missing, -> compute states from scratch or from waitlist

      throw new BlockSummaryMissingException(
          initialState, reducedInitialState, reducedInitialPrecision, innerSubtree, cachedReached);
    }

    assert cachedReached != null;
    data.registerInitialState(initialState, cachedReached);

    // use 'reducedResult' for cache and 'statesForFurtherAnalysis' as return value,
    // both are always equal, except analysis of recursive procedures (@fixpoint-algorithm)
    data.bamCache.put(
        reducedInitialState, reducedInitialPrecision, innerSubtree, cachedReturnStates, null);

    return Pair.of(cachedReturnStates, cachedReached);
  }

  /**
   * @param reducedResult pairs of reduced sets associated with the block exit.
   * @param innerSubtree block associated with {@code reducedResult}.
   * @param outerSubtree block above the one associated with {@code reducedResult}.
   * @param state state associated with the block entry.
   * @param precision precision associated with the block entry.
   * @return expanded states for all reduced states and updates the caches.
   */
  protected List<AbstractState> expandResultStates(
      final Collection<AbstractState> reducedResult,
      final ReachedSet reached,
      final Block innerSubtree,
      @Nullable final Block outerSubtree,
      final AbstractState state,
      final Precision precision)
      throws InterruptedException {

    final List<AbstractState> expandedResult = new ArrayList<>(reducedResult.size());
    for (AbstractState reducedState : reducedResult) {
      Precision reducedPrecision = reached.getPrecision(reducedState);
      AbstractState expandedState =
          wrappedReducer.getVariableExpandedState(state, innerSubtree, reducedState);
      Precision expandedPrecision =
          wrappedReducer.getVariableExpandedPrecision(precision, outerSubtree, reducedPrecision);
      ((ARGState) expandedState).addParent((ARGState) state);
      expandedResult.add(expandedState);
      data.registerExpandedState(expandedState, expandedPrecision, reducedState, innerSubtree);
    }

    return expandedResult;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    throw new UnsupportedOperationException(
        "BAMCPA needs to be used as the outermost CPA,"
            + " thus it does not support returning successors for a single edge.");
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {
    shutdownNotifier.shutdownIfNecessary();
    return wrappedTransfer.strengthen(pState, pOtherStates, pCfaEdge, pPrecision);
  }
}
