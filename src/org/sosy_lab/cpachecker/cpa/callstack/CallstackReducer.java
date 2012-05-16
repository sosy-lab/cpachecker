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
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;


public class CallstackReducer implements Reducer {

  @Override
  public AbstractElement getVariableReducedElement(
      AbstractElement pExpandedElement, Block pContext, CFANode callNode) {

    CallstackElement element = (CallstackElement)pExpandedElement;

    return copyCallstackUpToCallNode(element, callNode);
//    return new CallstackElement(null, element.getCurrentFunction(), location);
  }

  private CallstackElement copyCallstackUpToCallNode(CallstackElement element, CFANode callNode) {
    if (element.getCurrentFunction().equals(callNode.getFunctionName())) {
      return new CallstackElement(null, element.getCurrentFunction(), callNode);
    } else {
      assert element.getPreviousElement() != null;
      CallstackElement recursiveResult = copyCallstackUpToCallNode(element.getPreviousElement(), callNode);
      return new CallstackElement(recursiveResult, element.getCurrentFunction(), element.getCallNode());
    }
  }

  @Override
  public AbstractElement getVariableExpandedElement(
      AbstractElement pRootElement, Block pReducedContext,
      AbstractElement pReducedElement) {

    CallstackElement rootElement = (CallstackElement)pRootElement;
    CallstackElement reducedElement = (CallstackElement)pReducedElement;

    // the stackframe on top of rootElement and the stackframe on bottom of reducedElement are the same function
    // now glue both stacks together at this element

    return copyCallstackExceptLast(rootElement, reducedElement);
  }

  private CallstackElement copyCallstackExceptLast(CallstackElement target, CallstackElement source) {
    if (source.getDepth() == 1) {
      assert source.getPreviousElement() == null;
      assert source.getCurrentFunction().equals(target.getCurrentFunction());
      return target;
    } else {
      CallstackElement recursiveResult = copyCallstackExceptLast(target, source.getPreviousElement());
      return new CallstackElement(recursiveResult, source.getCurrentFunction(), source.getCallNode());
    }
  }

 private static boolean isEqual(CallstackElement reducedTargetElement,
      CallstackElement candidateElement) {
    if (reducedTargetElement.getDepth() != candidateElement.getDepth()) {
      return false;
    }

    while (reducedTargetElement != null) {
      if ( !reducedTargetElement.getCallNode().equals(candidateElement.getCallNode())
        || !reducedTargetElement.getCurrentFunction().equals(candidateElement.getCurrentFunction())) {
          return false;
      }
      reducedTargetElement = reducedTargetElement.getPreviousElement();
      candidateElement = candidateElement.getPreviousElement();
    }

    return true;
  }

  @Override
  public Object getHashCodeForElement(AbstractElement pElementKey, Precision pPrecisionKey) {
    return new CallstackElementWithEquals((CallstackElement)pElementKey);
  }

  private static class CallstackElementWithEquals {
    private final CallstackElement element;

    public CallstackElementWithEquals(CallstackElement pElement) {
      element = pElement;
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof CallstackElementWithEquals)) {
        return false;
      }

      return isEqual(element, ((CallstackElementWithEquals)other).element);
    }

    @Override
    public int hashCode() {
      return (element.getDepth() * 17 + element.getCurrentFunction().hashCode()) * 31 + element.getCallNode().hashCode();
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
