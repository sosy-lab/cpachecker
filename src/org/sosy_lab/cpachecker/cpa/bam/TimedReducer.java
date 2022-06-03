// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;

class TimedReducer implements Reducer {

  @SuppressWarnings("deprecation")
  static class ReducerStatistics {
    final ThreadSafeTimerContainer reduceTime =
        new ThreadSafeTimerContainer("Time for reducing abstract states");
    final ThreadSafeTimerContainer expandTime =
        new ThreadSafeTimerContainer("Time for expanding abstract states");
    final ThreadSafeTimerContainer reducePrecisionTime =
        new ThreadSafeTimerContainer("Time for reducing precisions");
    final ThreadSafeTimerContainer expandPrecisionTime =
        new ThreadSafeTimerContainer("Time for expanding precisions");
  }

  private final Reducer wrappedReducer;

  private final TimerWrapper reduceTimer;
  private final TimerWrapper expandTimer;
  private final TimerWrapper reducePrecisionTimer;
  private final TimerWrapper expandPrecisionTimer;

  public TimedReducer(ReducerStatistics pReducerStatistics, Reducer pWrappedReducer) {
    wrappedReducer = pWrappedReducer;
    reduceTimer = pReducerStatistics.reduceTime.getNewTimer();
    expandTimer = pReducerStatistics.expandTime.getNewTimer();
    reducePrecisionTimer = pReducerStatistics.reducePrecisionTime.getNewTimer();
    expandPrecisionTimer = pReducerStatistics.expandPrecisionTime.getNewTimer();
  }

  @Override
  public AbstractState getVariableReducedState(
      AbstractState pExpandedState, Block pContext, CFANode pCallNode) throws InterruptedException {
    reduceTimer.start();
    try {
      return wrappedReducer.getVariableReducedState(pExpandedState, pContext, pCallNode);
    } finally {
      reduceTimer.stop();
    }
  }

  @Override
  public AbstractState getVariableExpandedState(
      AbstractState pRootState, Block pReducedContext, AbstractState pReducedState)
      throws InterruptedException {
    expandTimer.start();
    try {
      return wrappedReducer.getVariableExpandedState(pRootState, pReducedContext, pReducedState);
    } finally {
      expandTimer.stop();
    }
  }

  @Override
  public Object getHashCodeForState(AbstractState pElementKey, Precision pPrecisionKey) {
    return wrappedReducer.getHashCodeForState(pElementKey, pPrecisionKey);
  }

  @Override
  public Precision getVariableReducedPrecision(Precision pPrecision, Block pContext) {
    reducePrecisionTimer.start();
    try {
      return wrappedReducer.getVariableReducedPrecision(pPrecision, pContext);
    } finally {
      reducePrecisionTimer.stop();
    }
  }

  @Override
  public Precision getVariableExpandedPrecision(
      Precision rootPrecision, Block rootContext, Precision reducedPrecision) {
    expandPrecisionTimer.start();
    try {
      return wrappedReducer.getVariableExpandedPrecision(
          rootPrecision, rootContext, reducedPrecision);
    } finally {
      expandPrecisionTimer.stop();
    }
  }

  @Override
  public int measurePrecisionDifference(Precision pPrecision, Precision pOtherPrecision) {
    return wrappedReducer.measurePrecisionDifference(pPrecision, pOtherPrecision);
  }

  @Override
  public AbstractState getVariableReducedStateForProofChecking(
      AbstractState pExpandedState, Block pContext, CFANode pCallNode) throws InterruptedException {
    return wrappedReducer.getVariableReducedStateForProofChecking(
        pExpandedState, pContext, pCallNode);
  }

  @Override
  public AbstractState getVariableExpandedStateForProofChecking(
      AbstractState pRootState, Block pReducedContext, AbstractState pReducedState)
      throws InterruptedException {
    return wrappedReducer.getVariableExpandedStateForProofChecking(
        pRootState, pReducedContext, pReducedState);
  }

  @Override
  public AbstractState rebuildStateAfterFunctionCall(
      AbstractState rootState,
      AbstractState entryState,
      AbstractState expandedState,
      FunctionExitNode exitLocation) {
    return wrappedReducer.rebuildStateAfterFunctionCall(
        rootState, entryState, expandedState, exitLocation);
  }

  @Override
  public boolean canBeUsedInCache(AbstractState pState) {
    return wrappedReducer.canBeUsedInCache(pState);
  }
}
