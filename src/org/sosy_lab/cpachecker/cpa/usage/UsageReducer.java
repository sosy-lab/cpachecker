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
package org.sosy_lab.cpachecker.cpa.usage;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;

public class UsageReducer implements Reducer {
  private final Reducer wrappedReducer;

  public UsageReducer(Reducer pWrappedReducer) {
    wrappedReducer = pWrappedReducer;
  }

  @Override
  public AbstractState getVariableReducedState(
      AbstractState pExpandedElement, Block pContext, CFANode pLocation)
      throws InterruptedException {

    UsageState funElement = (UsageState) pExpandedElement;
    AbstractState red =
        wrappedReducer.getVariableReducedState(funElement.getWrappedState(), pContext, pLocation);
    return funElement.copy(red);
  }

  @Override
  public AbstractState getVariableExpandedState(
      AbstractState pRootElement, Block pReducedContext, AbstractState pReducedElement)
      throws InterruptedException {
    UsageState funRootState = (UsageState) pRootElement;
    UsageState funReducedState = (UsageState) pReducedElement;
    AbstractState exp =
          wrappedReducer.getVariableExpandedState(
              funRootState.getWrappedState(), pReducedContext, funReducedState.getWrappedState());

    return funRootState.copy(exp);
  }

  @Override
  public Object getHashCodeForState(AbstractState pElementKey, Precision pPrecisionKey) {
    UsageState funElement = (UsageState) pElementKey;
    return wrappedReducer.getHashCodeForState(funElement.getWrappedState(), pPrecisionKey);
  }

  @Override
  public Precision getVariableReducedPrecision(Precision pPrecision, Block pContext) {
    return wrappedReducer.getVariableReducedPrecision(pPrecision, pContext);
  }

  @Override
  public Precision getVariableExpandedPrecision(
      Precision rootPrecision, Block rootContext, Precision reducedPrecision) {
    return wrappedReducer.getVariableExpandedPrecision(
        rootPrecision, rootContext, reducedPrecision);
  }

  @Override
  public int measurePrecisionDifference(Precision pPrecision, Precision pOtherPrecision) {
    return wrappedReducer.measurePrecisionDifference(pPrecision, pOtherPrecision);
  }

  @Override
  public AbstractState rebuildStateAfterFunctionCall(
      AbstractState pRootState,
      AbstractState pEntryState,
      AbstractState pExpandedState,
      FunctionExitNode pExitLocation) {
    return pExpandedState;
  }
}
