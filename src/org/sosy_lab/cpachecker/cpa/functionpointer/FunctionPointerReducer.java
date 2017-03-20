/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.functionpointer;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.util.Pair;


public class FunctionPointerReducer implements Reducer {

  @Override
  public AbstractState getVariableReducedState(AbstractState pExpandedState, Block pContext, CFANode pCallNode) {
    FunctionPointerState state = (FunctionPointerState) pExpandedState;
    FunctionPointerState.Builder builder = state.createBuilder();
    builder.clearVariablesExceptPrefix(pCallNode.getFunctionName());
    return builder.build();
  }

  @Override
  public AbstractState getVariableExpandedState(AbstractState pRootState, Block pReducedContext, AbstractState pReducedState) {
    FunctionPointerState state = (FunctionPointerState) pRootState;
    FunctionPointerState.Builder builder = state.createBuilder();
    builder.addGlobalVariables((FunctionPointerState)pReducedState);
    builder.clearVariablesForFunction(pReducedContext.getCallNode().getFunctionName());
    return builder.build();
  }

  @Override
  public Precision getVariableReducedPrecision(Precision pPrecision, Block pContext) {
    return pPrecision;
  }

  @Override
  public Precision getVariableExpandedPrecision(Precision pRootPrecision, Block pRootContext,
      Precision pReducedPrecision) {
    return pRootPrecision;
  }

  @Override
  public Object getHashCodeForState(AbstractState pStateKey, Precision pPrecisionKey) {
    return Pair.of(pStateKey, pPrecisionKey);
  }

  @Override
  public int measurePrecisionDifference(Precision pPrecision, Precision pOtherPrecision) {
    return 0;
  }

  @Override
  public AbstractState getVariableReducedStateForProofChecking(AbstractState pExpandedState, Block pContext,
      CFANode pCallNode) {
    return pExpandedState;
  }

  @Override
  public AbstractState getVariableExpandedStateForProofChecking(AbstractState pRootState, Block pReducedContext,
      AbstractState pReducedState) {
    return pRootState;
  }

  @Override
  public AbstractState rebuildStateAfterFunctionCall(AbstractState pRootState, AbstractState pEntryState,
      AbstractState pExpandedState, FunctionExitNode pExitLocation) {
    return pExpandedState;
  }

}
