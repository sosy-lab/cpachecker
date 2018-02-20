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

import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Collections;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMCache;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;

/**
 * This {@link TransferRelation} behaves similar to {@link BAMTransferRelation}. It does not create
 * a new sub-reached-set when reaching a block-entry location, but returns a {@link
 * MissingBlockAbstractionState}.
 */
public class BAMTransferRelationWithBreakOnMissingBlock
    extends AbstractBAMTransferRelation<CPATransferException> {

  public BAMTransferRelationWithBreakOnMissingBlock(
      AbstractBAMCPA bamCPA, ShutdownNotifier pShutdownNotifier) {
    super(bamCPA, pShutdownNotifier);
  }

  /**
   * Enters a new block and returns a cached result from {@link BAMCache} if possible.
   *
   * @param initialState Initial state of the analyzed block.
   * @param pPrecision Initial precision associated with the block start.
   * @param node Node corresponding to the block start.
   * @return Set of states associated with the block exit.
   */
  @Override
  protected Collection<AbstractState> doRecursiveAnalysis(
      final AbstractState initialState, final Precision pPrecision, final CFANode node)
      throws InterruptedException {
    final Block outerSubtree = getBlockForState((ARGState) initialState);
    final Block innerSubtree = partitioning.getBlockForCallNode(node);
    assert innerSubtree.getCallNodes().contains(node);

    final AbstractState reducedInitialState =
        wrappedReducer.getVariableReducedState(initialState, innerSubtree, node);
    final Precision reducedInitialPrecision =
        wrappedReducer.getVariableReducedPrecision(pPrecision, innerSubtree);

    final Pair<Collection<AbstractState>, ReachedSet> reducedResult =
        getReducedResult(initialState, reducedInitialState, reducedInitialPrecision, innerSubtree);
    Collection<AbstractState> cachedReturnStates = reducedResult.getFirst();

    if (cachedReturnStates.size() == 1
        && Iterables.getOnlyElement(cachedReturnStates) instanceof MissingBlockAbstractionState) {
      // special case: we abort the current analysis.
      return cachedReturnStates;
    }

    return expandResultStates(
        cachedReturnStates,
        reducedResult.getSecond(),
        innerSubtree,
        outerSubtree,
        initialState,
        pPrecision);
  }

  /**
   * Analyze the block starting at the node with {@code initialState}. If there is a result in the
   * cache ({@code data.bamCache}), it is used, otherwise a {@link MissingBlockAbstractionState} is
   * thrown.
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
      final Block innerSubtree) {

    // Try to get an element from cache. A previously computed element consists of
    // a reached set associated with the recursive call.
    final Pair<ReachedSet, Collection<AbstractState>> pair =
        data.getCache().get(reducedInitialState, reducedInitialPrecision, innerSubtree);
    final ReachedSet cachedReached = pair.getFirst();
    final Collection<AbstractState> cachedReturnStates = pair.getSecond();

    assert cachedReturnStates == null || cachedReached != null
        : "there cannot be result-states without reached-states";

    if (!isCacheHit(cachedReached, cachedReturnStates)) {
      // no cache hit, block summary missing, -> compute states from scratch or from waitlist
      MissingBlockAbstractionState mbas =
          new MissingBlockAbstractionState(
              initialState,
              reducedInitialState,
              reducedInitialPrecision,
              innerSubtree,
              cachedReached);
      return Pair.of(Collections.singleton(mbas), cachedReached);
    }

    assert cachedReached != null;
    registerInitalAndExitStates(initialState, cachedReturnStates, cachedReached);

    // use 'reducedResult' for cache and 'statesForFurtherAnalysis' as return value,
    // both are always equal, except analysis of recursive procedures (@fixpoint-algorithm)
    data.getCache().put(
        reducedInitialState, reducedInitialPrecision, innerSubtree, cachedReturnStates, null);

    return Pair.of(cachedReturnStates, cachedReached);
  }
}
