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
package org.sosy_lab.cpachecker.cpa.callstackPCC;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;


public class CallstackPccReducer implements Reducer {

  @Override
  public AbstractState getVariableReducedState(
      AbstractState pExpandedState, Block pContext, CFANode callNode) {

    CallstackPccState element = (CallstackPccState) pExpandedState;

    return copyCallstackUpToCallNode(element, callNode);
    //    return new CallstackPccState(null, state.getCurrentFunction(), location);
  }

  private CallstackPccState copyCallstackUpToCallNode(CallstackPccState element, CFANode callNode) {
    if (element.getCurrentFunction().equals(callNode.getFunctionName())) {
      return new CallstackPccState(null, element.getCurrentFunction(), callNode);
    } else {
      assert element.getPreviousState() != null;
      CallstackPccState recursiveResult = copyCallstackUpToCallNode(element.getPreviousState(), callNode);
      return new CallstackPccState(recursiveResult, element.getCurrentFunction(), element.getCallNode());
    }
  }

  @Override
  public AbstractState getVariableExpandedState(
      AbstractState pRootState, Block pReducedContext,
      AbstractState pReducedState) {

    CallstackPccState rootState = (CallstackPccState) pRootState;
    CallstackPccState reducedState = (CallstackPccState) pReducedState;

    // the stackframe on top of rootState and the stackframe on bottom of reducedState are the same function
    // now glue both stacks together at this state

    return copyCallstackExceptLast(rootState, reducedState);
  }

  private CallstackPccState copyCallstackExceptLast(CallstackPccState target, CallstackPccState source) {
    if (source.getDepth() == 1) {
      assert source.getPreviousState() == null;
      assert source.getCurrentFunction().equals(target.getCurrentFunction());
      return target;
    } else {
      CallstackPccState recursiveResult = copyCallstackExceptLast(target, source.getPreviousState());
      return new CallstackPccState(recursiveResult, source.getCurrentFunction(), source.getCallNode());
    }
  }

  @Override
  public Precision getVariableReducedPrecision(Precision pPrecision,
      Block pContext) {
    return pPrecision;
  }

  @Override
  public Precision getVariableExpandedPrecision(Precision rootPrecision, Block rootContext, Precision reducedPrecision) {
    return reducedPrecision;
  }

  @Override
  public int measurePrecisionDifference(Precision pPrecision, Precision pOtherPrecision) {
    return 0;
  }

  @Override
  public AbstractState getVariableReducedStateForProofChecking(AbstractState pExpandedState, Block pContext,
      CFANode pCallNode) {
    return getVariableReducedState(pExpandedState, pContext, pCallNode);
  }

  @Override
  public AbstractState getVariableExpandedStateForProofChecking(AbstractState pRootState, Block pReducedContext,
      AbstractState pReducedState) {
    return getVariableExpandedState(pRootState, pReducedContext, pReducedState);
  }

  @Override
  public Object getHashCodeForState(AbstractState pStateKey, Precision pPrecisionKey) {
    return pStateKey.hashCode();
  }

}
