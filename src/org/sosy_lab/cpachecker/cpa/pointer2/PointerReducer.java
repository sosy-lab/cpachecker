/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.pointer2;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.defaults.GenericReducer;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class PointerReducer extends GenericReducer<PointerState, PointerPrecision> {

  @Override
  protected PointerState getVariableReducedState0(
      PointerState pExpandedState, Block pContext, CFANode pCallNode) throws InterruptedException {
    PointerState clonedState = PointerState.copyOf(pExpandedState);
    for (MemoryLocation ptr : clonedState.getTrackedMemoryLocations()) {
      if (!pContext.getVariables().contains(ptr.getAsSimpleString())) {
        clonedState.forget(ptr);
      }
    }
    return clonedState;
  }

  @Override
  protected PointerState getVariableExpandedState0(
      PointerState pRootState, Block pReducedContext, PointerState pReducedState)
      throws InterruptedException {
    PointerState clonedState = PointerState.copyOf(pReducedState);
    for (MemoryLocation ptr : pRootState.getTrackedMemoryLocations()) {
      if (!pReducedContext.getVariables().contains(ptr.getAsSimpleString())) {
        clonedState = clonedState.addPointsToInformation(ptr, pRootState.getPointsToMap().get(ptr));
      }
    }
    return clonedState;
  }

  @Override
  protected Object getHashCodeForState0(
      PointerState pStateKey, PointerPrecision pPrecisionKey) {
    return Pair.of(pStateKey, pPrecisionKey);
  }

  @Override
  protected Precision getVariableReducedPrecision0(
      PointerPrecision pPrecision, Block pContext) {
    return pPrecision;
  }

  @Override
  protected PointerPrecision getVariableExpandedPrecision0(
      PointerPrecision pRootPrecision,
      Block pRootContext,
      PointerPrecision pReducedPrecision) {
    return pRootPrecision;
  }

  @Override
  protected PointerState rebuildStateAfterFunctionCall0(
      PointerState pRootState,
      PointerState pEntryState,
      PointerState pExpandedState,
      FunctionExitNode pExitLocation) {
    // TODO: no testing on files with recursion was conducted
    return pRootState;
  }
}
