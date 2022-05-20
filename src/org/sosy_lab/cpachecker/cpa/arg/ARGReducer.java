// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.defaults.GenericReducer;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;

class ARGReducer extends GenericReducer<ARGState, Precision> {

  private final Reducer wrappedReducer;

  protected ARGReducer(Reducer pWrappedReducer) {
    wrappedReducer = pWrappedReducer;
  }

  @Override
  protected ARGState getVariableReducedState0(
      ARGState pExpandedState, Block pContext, CFANode pLocation) throws InterruptedException {

    return new ARGState(
        wrappedReducer.getVariableReducedState(
            pExpandedState.getWrappedState(), pContext, pLocation),
        null);
  }

  @Override
  protected ARGState getVariableExpandedState0(
      ARGState pRootState, Block pReducedContext, ARGState pReducedState)
      throws InterruptedException {
    AbstractState nestedState =
        wrappedReducer.getVariableExpandedState(
            pRootState.getWrappedState(), pReducedContext, pReducedState.getWrappedState());
    return nestedState == null ? null : new ARGState(nestedState, null);
  }

  @Override
  protected Object getHashCodeForState0(ARGState pElementKey, Precision pPrecisionKey) {

    return wrappedReducer.getHashCodeForState(pElementKey.getWrappedState(), pPrecisionKey);
  }

  @Override
  protected Precision getVariableReducedPrecision0(Precision pPrecision, Block pContext) {
    return wrappedReducer.getVariableReducedPrecision(pPrecision, pContext);
  }

  @Override
  protected Precision getVariableExpandedPrecision0(
      Precision rootPrecision, Block rootContext, Precision reducedPrecision) {
    return wrappedReducer.getVariableExpandedPrecision(
        rootPrecision, rootContext, reducedPrecision);
  }

  @Override
  protected int measurePrecisionDifference0(Precision pPrecision, Precision pOtherPrecision) {
    return wrappedReducer.measurePrecisionDifference(pPrecision, pOtherPrecision);
  }

  @Override
  protected ARGState getVariableReducedStateForProofChecking0(
      ARGState pExpandedState, Block pContext, CFANode pCallNode) throws InterruptedException {
    return new ARGState(
        wrappedReducer.getVariableReducedStateForProofChecking(
            pExpandedState.getWrappedState(), pContext, pCallNode),
        null);
  }

  @Override
  protected ARGState getVariableExpandedStateForProofChecking0(
      ARGState pRootState, Block pReducedContext, ARGState pReducedState)
      throws InterruptedException {
    AbstractState nestedState =
        wrappedReducer.getVariableExpandedStateForProofChecking(
            pRootState.getWrappedState(), pReducedContext, pReducedState.getWrappedState());
    return nestedState == null ? null : new ARGState(nestedState, null);
  }

  @Override
  protected ARGState rebuildStateAfterFunctionCall0(
      ARGState rootState,
      ARGState entryState,
      ARGState expandedState,
      FunctionExitNode exitLocation) {
    return new ARGState(
        wrappedReducer.rebuildStateAfterFunctionCall(
            rootState.getWrappedState(),
            entryState.getWrappedState(),
            expandedState.getWrappedState(),
            exitLocation),
        null);
  }

  @Override
  protected boolean canBeUsedInCache0(ARGState pState) {
    return wrappedReducer.canBeUsedInCache(pState.getWrappedState());
  }
}
