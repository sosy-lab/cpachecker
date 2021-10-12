// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.thread;

import java.util.TreeMap;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;


public class AgressiveThreadReducer implements Reducer {

  public AgressiveThreadReducer() {}

  @Override
  public AbstractState getVariableReducedState(AbstractState pExpandedState, Block pContext,
      CFANode pCallNode) throws InterruptedException {
    ThreadState tState = (ThreadState) pExpandedState;
    return reduce(tState);
  }

  private ThreadState reduce(ThreadState state) {
    if (state.threadSet.isEmpty()) {
      return state;
    } else {
      return state.copyWith(new TreeMap<>());
    }
  }

  @Override
  public AbstractState getVariableExpandedState(AbstractState pRootState, Block pReducedContext,
      AbstractState pReducedState) throws InterruptedException {

    ThreadState root = (ThreadState) pRootState;
    ThreadState reduced = (ThreadState) pReducedState;
    ThreadState reducedRoot = reduce(root);

    if (reducedRoot == root) {
      return reduced;
    }
    ThreadDelta delta = (ThreadDelta) reducedRoot.getDeltaBetween(root);
    return delta.apply(reduced);
  }

  @Override
  public Precision getVariableReducedPrecision(Precision pPrecision, Block pContext) {
    return pPrecision;
  }

  @Override
  public Precision getVariableExpandedPrecision(Precision pRootPrecision, Block pRootContext,
      Precision pReducedPrecision) {
    return pReducedPrecision;
  }

  @Override
  public Object getHashCodeForState(AbstractState pStateKey, Precision pPrecisionKey) {
    return pStateKey;
  }

  @Override
  public int measurePrecisionDifference(Precision pPrecision, Precision pOtherPrecision) {
    return 0;
  }

  @Override
  public AbstractState rebuildStateAfterFunctionCall(AbstractState pRootState,
      AbstractState pEntryState, AbstractState pExpandedState, FunctionExitNode pExitLocation) {
    return pExpandedState;
  }
}
