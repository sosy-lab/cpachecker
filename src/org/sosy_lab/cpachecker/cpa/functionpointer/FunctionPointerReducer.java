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
package org.sosy_lab.cpachecker.cpa.functionpointer;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;

// TODO implements a no-op for the function pointer map and passes the wrapped abstract state to the wrappedReducer; could be improved
// TODO functions called through function pointers are yet not considered for the computation of relevant predicates of a block. Using ABM with FunctionPointerCPA might thus yield unsound verification results.
public class FunctionPointerReducer implements Reducer {

  private final Reducer wrappedReducer;

  public FunctionPointerReducer(Reducer pWrappedReducer) {
    wrappedReducer = pWrappedReducer;
  }

  @Override
  public AbstractState getVariableReducedState(
      AbstractState pExpandedState, Block pContext,
      CFANode pLocation) {

    FunctionPointerState funElement = (FunctionPointerState)pExpandedState;
    return funElement.createDuplicateWithNewWrappedState(wrappedReducer.getVariableReducedState(funElement.getWrappedState(), pContext, pLocation));
  }

  @Override
  public AbstractState getVariableExpandedState(
      AbstractState pRootState, Block pReducedContext,
      AbstractState pReducedState) {

    FunctionPointerState funRootState = (FunctionPointerState)pRootState;
    FunctionPointerState funReducedState = (FunctionPointerState)pReducedState;

    return funReducedState.createDuplicateWithNewWrappedState(wrappedReducer.getVariableExpandedState(funRootState.getWrappedState(), pReducedContext, funReducedState.getWrappedState()));
  }

  @Override
  public Object getHashCodeForState(AbstractState pElementKey, Precision pPrecisionKey) {
    FunctionPointerState funElement = (FunctionPointerState)pElementKey;
    return Pair.of(funElement.getTargetMap(), wrappedReducer.getHashCodeForState(funElement.getWrappedState(), pPrecisionKey));
  }

  @Override
  public Precision getVariableReducedPrecision(Precision pPrecision,
      Block pContext) {
    return wrappedReducer.getVariableReducedPrecision(pPrecision, pContext);
  }

  @Override
  public Precision getVariableExpandedPrecision(Precision rootPrecision, Block rootContext, Precision reducedPrecision) {
    return wrappedReducer.getVariableExpandedPrecision(rootPrecision, rootContext, reducedPrecision);
  }

  @Override
  public int measurePrecisionDifference(Precision pPrecision, Precision pOtherPrecision) {
    return wrappedReducer.measurePrecisionDifference(pPrecision, pOtherPrecision);
  }

}
