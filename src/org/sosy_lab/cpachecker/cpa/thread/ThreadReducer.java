/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.thread;

import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.thread.ThreadState.ThreadStateBuilder;


public class ThreadReducer implements Reducer {

  private final Reducer lReducer;
  private final Reducer cReducer;

  public ThreadReducer(Reducer l, Reducer c) {
    lReducer = l;
    cReducer = c;
  }

  @Override
  public AbstractState getVariableReducedState(AbstractState pExpandedState, Block pContext,
      CFANode pCallNode) throws InterruptedException {
    ThreadState expState = (ThreadState)pExpandedState;
    LocationState expLocation = expState.getLocationState();
    CallstackState expCallstackState = expState.getCallstackState();
    ThreadStateBuilder builder = expState.getBuilder();
    LocationState redLocation = (LocationState)lReducer.getVariableReducedState(expLocation, pContext, pCallNode);
    CallstackState redCallstack = (CallstackState)cReducer.getVariableReducedState(expCallstackState, pContext, pCallNode);
    builder.setWrappedStates(redLocation, redCallstack);
    return builder.build();
  }

  @Override
  public AbstractState getVariableExpandedState(AbstractState pRootState, Block pReducedContext,
      AbstractState pReducedState) throws InterruptedException {
    ThreadState rootState = (ThreadState)pRootState;
    LocationState rootLocation = rootState.getLocationState();
    CallstackState rootCallstack = rootState.getCallstackState();
    ThreadState redState = (ThreadState)pReducedState;
    LocationState redLocation = redState.getLocationState();
    CallstackState redCallstack = redState.getCallstackState();
    ThreadStateBuilder builder = redState.getBuilder();
    LocationState expLocation = (LocationState)lReducer.getVariableExpandedState(rootLocation, pReducedContext, redLocation);
    CallstackState expCallstack = (CallstackState)cReducer.getVariableExpandedState(rootCallstack, pReducedContext, redCallstack);
    builder.setWrappedStates(expLocation, expCallstack);
    return builder.build();
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
    List<Object> result = new ArrayList<>(4);

    ThreadState tState = (ThreadState) pStateKey;

    result.add(lReducer.getHashCodeForState(tState.getLocationState(), pPrecisionKey));
    result.add(cReducer.getHashCodeForState(tState.getCallstackState(), pPrecisionKey));
    result.add(tState.getThreadSet());
    result.add(tState.getRemovedSet());
    return result;
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
