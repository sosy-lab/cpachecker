/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.lock;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@Options(prefix = "cpa.lock")
public class LockReducer implements Reducer, StatisticsProvider {

  public class LockReducerStatistics implements Statistics {

    private StatTimer lockReducing = new StatTimer("Time for reducing locks");
    private StatTimer reduceUselessLocksTimer = new StatTimer("Time for reducing useless locks");
    private StatTimer reduceLockCountersTimer = new StatTimer("Time for reducing lock counters");
    private StatTimer lockExpanding = new StatTimer("Time for expanding locks");
    private StatTimer expandUselessLocksTimer = new StatTimer("Time for expanding useless locks");
    private StatTimer expandLockCountersTimer = new StatTimer("Time for expanding lock counters");


    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      StatisticsWriter.writingStatisticsTo(pOut)
          .put(lockReducing)
          .beginLevel()
          .put(reduceUselessLocksTimer)
          .put(reduceLockCountersTimer)
          .endLevel()
          .put(lockExpanding)
          .beginLevel()
          .put(expandUselessLocksTimer)
          .put(expandLockCountersTimer)
          .endLevel()
          .put("Size of unreducable map", LockReducer.this.notReducedLocks.size());
    }

    @Override
    public @Nullable String getName() {
      return "Lock Reducer";
    }

  }

  public enum ReduceStrategy {
    NONE,
    BLOCK,
    ALL
  }

  @Option(description = "reduce recursive locks to a single access", secure = true)
  private ReduceStrategy reduceLockCounters = ReduceStrategy.BLOCK;

  // Attention! Error trace may be restored incorrectly.
  // If two states with different locks are reduced to the one state,
  // the path will be always restored through the first one
  @Option(description = "reduce unused locks", secure = true)
  private boolean reduceUselessLocks = false;

  private final Multimap<CFANode, LockIdentifier> notReducedLocks;

  private final LockReducerStatistics stats;

  public LockReducer(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
    if (reduceUselessLocks && reduceLockCounters == ReduceStrategy.BLOCK) {
      // reducing counters in this case is useless
      reduceLockCounters = ReduceStrategy.NONE;
    }
    notReducedLocks = ArrayListMultimap.create();
    stats = new LockReducerStatistics();
  }

  @Override
  public AbstractLockState
      getVariableReducedState(AbstractState pExpandedElement, Block pContext, CFANode pCallNode) {
    AbstractLockState expandedElement = (AbstractLockState) pExpandedElement;
    stats.lockReducing.start();
    AbstractLockStateBuilder builder = expandedElement.builder();
    Set<LockIdentifier> locksToProcess = expandedElement.getLocks();

    builder.reduce();
    stats.reduceUselessLocksTimer.start();
    if (reduceUselessLocks) {
      Set<LockIdentifier> notReduce = new HashSet<>(notReducedLocks.get(pCallNode));
      builder.removeLocksExcept(Sets.union(pContext.getCapturedLocks(), notReduce));
      // All other locks are successfully removed
      locksToProcess = Sets.intersection(locksToProcess, pContext.getCapturedLocks());
    }
    stats.reduceUselessLocksTimer.stop();

    stats.reduceLockCountersTimer.start();
    switch (reduceLockCounters) {
      case BLOCK:
        locksToProcess = Sets.difference(locksToProcess, pContext.getCapturedLocks());
        //$FALL-THROUGH$
      case ALL:
        builder.reduceLockCounters(locksToProcess);
        break;
      case NONE:
        break;
    }
    stats.reduceLockCountersTimer.stop();
    AbstractLockState reducedState = builder.build();
    assert getVariableExpandedState(pExpandedElement, pContext, reducedState)
        .equals(pExpandedElement);
    stats.lockReducing.stop();
    return reducedState;
  }

  @Override
  public AbstractState getVariableExpandedState(
      AbstractState pRootElement,
      Block pReducedContext,
      AbstractState pReducedElement) {

    stats.lockExpanding.start();
    AbstractLockState rootElement = (AbstractLockState) pRootElement;
    AbstractLockState reducedElement = (AbstractLockState) pReducedElement;
    AbstractLockStateBuilder builder = reducedElement.builder();
    // Restore only what we reduced
    Set<LockIdentifier> locksToProcess = rootElement.getLocks();

    stats.expandUselessLocksTimer.start();
    builder.expand(rootElement);
    if (reduceUselessLocks) {
      Set<LockIdentifier> notReduce =
          new HashSet<>(notReducedLocks.get(pReducedContext.getCallNode()));
      builder.returnLocksExcept(
          (LockState) pRootElement,
          Sets.union(pReducedContext.getCapturedLocks(), notReduce));
      locksToProcess = Sets.intersection(locksToProcess, pReducedContext.getCapturedLocks());
    }
    stats.expandUselessLocksTimer.stop();

    stats.expandLockCountersTimer.start();
    switch (reduceLockCounters) {
      case BLOCK:
        locksToProcess = Sets.difference(locksToProcess, pReducedContext.getCapturedLocks());
        //$FALL-THROUGH$
      case ALL:
        builder.expandLockCounters(rootElement, locksToProcess);
        break;
      case NONE:
        break;
    }
    stats.expandLockCountersTimer.stop();
    stats.lockExpanding.stop();
    return builder.build();
  }

  @Override
  public Precision getVariableReducedPrecision(
      Precision pPrecision,
      Block pContext) {
    return pPrecision;
  }

  @Override
  public Precision getVariableExpandedPrecision(
      Precision pRootPrecision,
      Block pRootContext,
      Precision pReducedPrecision) {
    return pReducedPrecision;
  }

  @Override
  public Object getHashCodeForState(
      AbstractState pElementKey,
      Precision pPrecisionKey) {
    return ((AbstractLockState) pElementKey).getHashCodeForState();
  }

  @Override
  public AbstractState rebuildStateAfterFunctionCall(
      AbstractState pRootState,
      AbstractState pEntryState,
      AbstractState pExpandedState,
      FunctionExitNode pExitLocation) {
    return pExpandedState;
  }

  public void consider(List<FunctionEntryNode> pStack, LockIdentifier pFId) {
    pStack.forEach(n -> notReducedLocks.put(n, pFId));
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }
}
