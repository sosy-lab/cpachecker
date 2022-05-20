// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;

@SuppressWarnings("unchecked")
public abstract class GenericReducer<S extends AbstractState, P extends Precision>
    implements Reducer {

  @Override
  public final AbstractState getVariableReducedState(
      AbstractState pExpandedState, Block pContext, CFANode pCallNode) throws InterruptedException {
    return getVariableReducedState0((S) pExpandedState, pContext, pCallNode);
  }

  protected abstract S getVariableReducedState0(S pExpandedState, Block pContext, CFANode pCallNode)
      throws InterruptedException;

  @Override
  public final AbstractState getVariableExpandedState(
      AbstractState pRootState, Block pReducedContext, AbstractState pReducedState)
      throws InterruptedException {
    return getVariableExpandedState0((S) pRootState, pReducedContext, (S) pReducedState);
  }

  protected abstract S getVariableExpandedState0(
      S pRootState, Block pReducedContext, S pReducedState) throws InterruptedException;

  @Override
  public final Object getHashCodeForState(AbstractState pStateKey, Precision pPrecisionKey) {
    return getHashCodeForState0((S) pStateKey, (P) pPrecisionKey);
  }

  protected abstract Object getHashCodeForState0(S pStateKey, P pPrecisionKey);

  @Override
  public final Precision getVariableReducedPrecision(Precision pPrecision, Block pContext) {
    return getVariableReducedPrecision0((P) pPrecision, pContext);
  }

  protected abstract Precision getVariableReducedPrecision0(P pPrecision, Block pContext);

  @Override
  public final P getVariableExpandedPrecision(
      Precision rootPrecision, Block rootContext, Precision reducedPrecision) {
    return getVariableExpandedPrecision0((P) rootPrecision, rootContext, (P) reducedPrecision);
  }

  protected abstract P getVariableExpandedPrecision0(
      P pRootPrecision, Block pRootContext, P pReducedPrecision);

  @Override
  public final AbstractState rebuildStateAfterFunctionCall(
      AbstractState rootState,
      AbstractState entryState,
      AbstractState expandedState,
      FunctionExitNode exitLocation) {
    return rebuildStateAfterFunctionCall0(
        (S) rootState, (S) entryState, (S) expandedState, exitLocation);
  }

  protected abstract S rebuildStateAfterFunctionCall0(
      S pRootState, S pEntryState, S pExpandedState, FunctionExitNode pExitLocation);

  @Override
  public final int measurePrecisionDifference(Precision pPrecision, Precision pOtherPrecision) {
    return measurePrecisionDifference0((P) pPrecision, (P) pOtherPrecision);
  }

  @SuppressWarnings("unused")
  protected int measurePrecisionDifference0(P pPrecision, P pOtherPrecision) {
    // default
    return Reducer.super.measurePrecisionDifference(pPrecision, pOtherPrecision);
  }

  @Override
  public AbstractState getVariableReducedStateForProofChecking(
      AbstractState expandedState, Block context, CFANode callNode) throws InterruptedException {
    return getVariableReducedStateForProofChecking0((S) expandedState, context, callNode);
  }

  protected S getVariableReducedStateForProofChecking0(
      S pExpandedState, Block pContext, CFANode pCallNode) throws InterruptedException {
    // default
    return (S)
        Reducer.super.getVariableReducedStateForProofChecking(pExpandedState, pContext, pCallNode);
  }

  @Override
  public AbstractState getVariableExpandedStateForProofChecking(
      AbstractState rootState, Block reducedContext, AbstractState reducedState)
      throws InterruptedException {
    return getVariableExpandedStateForProofChecking0(
        (S) rootState, reducedContext, (S) reducedState);
  }

  protected S getVariableExpandedStateForProofChecking0(
      S pRootState, Block pReducedContext, S pReducedState) throws InterruptedException {
    // default
    return (S)
        Reducer.super.getVariableExpandedStateForProofChecking(
            pRootState, pReducedContext, pReducedState);
  }

  protected boolean canBeUsedInCache0(S pState) {
    //default
    return Reducer.super.canBeUsedInCache(pState);
  }

  @Override
  public boolean canBeUsedInCache(AbstractState pState) {
    return canBeUsedInCache0((S) pState);
  }
}
