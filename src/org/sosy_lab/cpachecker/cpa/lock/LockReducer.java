// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.lock;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.io.PrintStream;
import java.util.Collection;
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
import org.sosy_lab.cpachecker.util.Pair;
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
          .put("Size of unreducable map", notReducedLocks.size());
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
  public AbstractLockState getVariableReducedState(
      AbstractState pExpandedElement, Block pContext, CFANode pCallNode) {
    assert pContext.getCallNode().equals(pCallNode);
    AbstractLockState expandedElement = (AbstractLockState) pExpandedElement;
    stats.lockReducing.start();
    AbstractLockStateBuilder builder = expandedElement.builder();
    Pair<Set<LockIdentifier>, Set<LockIdentifier>> lockSets =
        getLockSetsFor(expandedElement, pContext);
    Set<LockIdentifier> locksToProcess = lockSets.getFirst();
    Set<LockIdentifier> uselessLocks = lockSets.getSecond();

    builder.reduce(locksToProcess, uselessLocks);
    AbstractLockState reducedState = builder.build();
    assert getVariableExpandedState(pExpandedElement, pContext, reducedState)
        .equals(pExpandedElement);
    stats.lockReducing.stop();
    return reducedState;
  }

  @Override
  public AbstractState getVariableExpandedState(
      AbstractState pRootElement, Block pReducedContext, AbstractState pReducedElement) {

    stats.lockExpanding.start();
    AbstractLockState rootElement = (AbstractLockState) pRootElement;
    AbstractLockState reducedElement = (AbstractLockState) pReducedElement;
    AbstractLockStateBuilder builder = reducedElement.builder();
    // Restore only what we reduced
    Pair<Set<LockIdentifier>, Set<LockIdentifier>> lockSets =
        getLockSetsFor(rootElement, pReducedContext);
    Set<LockIdentifier> locksToProcess = lockSets.getFirst();
    Set<LockIdentifier> uselessLocks = lockSets.getSecond();

    builder.expand(rootElement, locksToProcess, uselessLocks);

    stats.lockExpanding.stop();
    return builder.build();
  }

  @Override
  public Precision getVariableReducedPrecision(Precision pPrecision, Block pContext) {
    return pPrecision;
  }

  @Override
  public Precision getVariableExpandedPrecision(
      Precision pRootPrecision, Block pRootContext, Precision pReducedPrecision) {
    return pReducedPrecision;
  }

  @Override
  public Object getHashCodeForState(AbstractState pElementKey, Precision pPrecisionKey) {
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

  private Pair<Set<LockIdentifier>, Set<LockIdentifier>> getLockSetsFor(
      AbstractLockState rootState, @SuppressWarnings("unused") Block pContext) {
    Set<LockIdentifier> locksToProcess = ImmutableSet.of();
    Set<LockIdentifier> uselessLocks = ImmutableSet.of();

    /*
     * if (reduceUselessLocks) { uselessLocks = Sets.filter( rootState.getLocks(), l ->
     * !pContext.getCapturedLocks().contains(l) &&
     * !notReducedLocks.get(pContext.getCallNode()).contains(l)); }
     */
    switch (reduceLockCounters) {
      case BLOCK:
        locksToProcess = Sets.difference(rootState.getLocks(), uselessLocks);
        // locksToProcess = Sets.difference(locksToProcess, pContext.getCapturedLocks());
        break;
      case ALL:
        locksToProcess = Sets.difference(rootState.getLocks(), uselessLocks);
        break;
      case NONE:
        break;
    }
    return Pair.of(locksToProcess, uselessLocks);
  }
}
