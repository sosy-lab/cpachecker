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
package org.sosy_lab.cpachecker.cpa.callstack;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;


public class CallstackReducer implements Reducer {

  @Override
  public AbstractState getVariableReducedState(
      AbstractState pExpandedElement, Block pContext, CFANode callNode) {

    CallstackState element = (CallstackState)pExpandedElement;

    return copyCallstackUpToCallNode(element, callNode);
//    return new CallstackState(null, state.getCurrentFunction(), location);
  }

  private CallstackState copyCallstackUpToCallNode(CallstackState element, CFANode callNode) {
    if (element.getCurrentFunction().equals(callNode.getFunctionName())) {
      return new CallstackState(null, element.getCurrentFunction(), callNode);
    } else {
      assert element.getPreviousState() != null;
      CallstackState recursiveResult = copyCallstackUpToCallNode(element.getPreviousState(), callNode);
      return new CallstackState(recursiveResult, element.getCurrentFunction(), element.getCallNode());
    }
  }

  @Override
  public AbstractState getVariableExpandedState(
      AbstractState pRootElement, Block pReducedContext,
      AbstractState pReducedElement) {

    CallstackState rootElement = (CallstackState)pRootElement;
    CallstackState reducedElement = (CallstackState)pReducedElement;

    // the stackframe on top of rootElement and the stackframe on bottom of reducedElement are the same function
    // now glue both stacks together at this state

    return copyCallstackExceptLast(rootElement, reducedElement);
  }

  private CallstackState copyCallstackExceptLast(CallstackState target, CallstackState source) {
    if (source.getDepth() == 1) {
      assert source.getPreviousState() == null;
      assert source.getCurrentFunction().equals(target.getCurrentFunction());
      return target;
    } else {
      CallstackState recursiveResult = copyCallstackExceptLast(target, source.getPreviousState());
      return new CallstackState(recursiveResult, source.getCurrentFunction(), source.getCallNode());
    }
  }

 private static boolean isEqual(CallstackState reducedTargetElement,
      CallstackState candidateElement) {
    if (reducedTargetElement.getDepth() != candidateElement.getDepth()) {
      return false;
    }

    while (reducedTargetElement != null) {
      if ( !reducedTargetElement.getCallNode().equals(candidateElement.getCallNode())
        || !reducedTargetElement.getCurrentFunction().equals(candidateElement.getCurrentFunction())) {
          return false;
      }
      reducedTargetElement = reducedTargetElement.getPreviousState();
      candidateElement = candidateElement.getPreviousState();
    }

    return true;
  }

  @Override
  public Object getHashCodeForState(AbstractState pElementKey, Precision pPrecisionKey) {
    return new CallstackStateWithEquals((CallstackState)pElementKey);
  }

  private static class CallstackStateWithEquals {
    private final CallstackState state;

    public CallstackStateWithEquals(CallstackState pElement) {
      state = pElement;
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof CallstackStateWithEquals)) {
        return false;
      }

      return isEqual(state, ((CallstackStateWithEquals)other).state);
    }

    @Override
    public int hashCode() {
      return (state.getDepth() * 17 + state.getCurrentFunction().hashCode()) * 31 + state.getCallNode().hashCode();
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
}
