// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.callstack;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.defaults.GenericReducer;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

class CallstackReducer extends GenericReducer<CallstackState, Precision> {

  @Override
  protected CallstackState getVariableReducedState0(
      CallstackState pExpandedState, Block pContext, CFANode callNode) {
    return copyCallstackUpToCallNode(pExpandedState, callNode);
    //    return new CallstackState(null, state.getCurrentFunction(), location);
  }

  private CallstackState copyCallstackUpToCallNode(CallstackState element, CFANode callNode) {
    if (element.getCurrentFunction().equals(callNode.getFunctionName())) {
      return new CallstackState(null, element.getCurrentFunction(), callNode);
    } else {
      assert element.getPreviousState() != null;
      CallstackState recursiveResult =
          copyCallstackUpToCallNode(element.getPreviousState(), callNode);
      return new CallstackState(
          recursiveResult, element.getCurrentFunction(), element.getCallNode());
    }
  }

  @Override
  protected CallstackState getVariableExpandedState0(
      CallstackState pRootState, Block pReducedContext, CallstackState pReducedState) {
    // the stackframe on top of rootState and the stackframe on bottom of reducedState
    // are the same function, now glue both stacks together at this state

    return copyCallstackExceptLast(pRootState, pReducedState);
  }

  private CallstackState copyCallstackExceptLast(CallstackState target, CallstackState source) {
    if (source.getDepth() == 1) {
      assert source.getPreviousState() == null;
      assert source.getCurrentFunction().equals(target.getCurrentFunction())
          : "names of functions do not match: '"
              + source.getCurrentFunction()
              + "' != '"
              + target.getCurrentFunction()
              + "'";
      return target;
    } else {
      CallstackState recursiveResult = copyCallstackExceptLast(target, source.getPreviousState());

      return new CallstackState(recursiveResult, source.getCurrentFunction(), source.getCallNode());
    }
  }

  @Override
  protected Object getHashCodeForState0(CallstackState pElementKey, Precision pPrecisionKey) {
    return new CallstackStateEqualsWrapper(pElementKey);
  }

  @Override
  protected Precision getVariableReducedPrecision0(Precision pPrecision, Block pContext) {
    return pPrecision;
  }

  @Override
  protected Precision getVariableExpandedPrecision0(
      Precision rootPrecision, Block rootContext, Precision reducedPrecision) {
    return reducedPrecision;
  }

  @Override
  protected CallstackState rebuildStateAfterFunctionCall0(
      CallstackState rootState,
      CallstackState entryState,
      CallstackState expandedState,
      FunctionExitNode exitLocation) {
    return expandedState;
  }
}
