/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
  public AbstractState getVariableReducedState(
      AbstractState pExpandedState, Block pContext, Block outerContext,
      CFANode pLocation) throws InterruptedException {

    return new ARGState(wrappedReducer.getVariableReducedState(((ARGState) pExpandedState).getWrappedState(), pContext, outerContext,
        pLocation), null);
  }

  @Override
  public AbstractState getVariableExpandedState(
      AbstractState pRootState, Block pReducedContext, Block outerContext,
      AbstractState pReducedState) throws InterruptedException {

    return new ARGState(wrappedReducer.getVariableExpandedState(((ARGState) pRootState).getWrappedState(),
        pReducedContext, outerContext, ((ARGState) pReducedState).getWrappedState()), null);
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

    return new ARGState(
        wrappedReducer.getVariableExpandedState(
            pRootState.getWrappedState(), pReducedContext, pReducedState.getWrappedState()),
        null);
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
    return wrappedReducer.getVariableExpandedPrecision(rootPrecision, rootContext, reducedPrecision);
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
    return new ARGState(
        wrappedReducer.getVariableExpandedStateForProofChecking(
            pRootState.getWrappedState(), pReducedContext, pReducedState.getWrappedState()),
        null);
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
}
