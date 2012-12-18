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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;

public class CallstackTransferRelation implements TransferRelation {

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {

    switch (pCfaEdge.getEdgeType()) {
    case FunctionCallEdge:
      {
        FunctionCallEdge cfaEdge = (FunctionCallEdge)pCfaEdge;
        CallstackState element = (CallstackState)pElement;
        String functionName = cfaEdge.getSuccessor().getFunctionName();
        CFANode callNode = cfaEdge.getPredecessor();

        CallstackState e = element;
        while (e != null) {
          if (e.getCurrentFunction().equals(functionName)) {
            throw new UnsupportedCCodeException("recursion", pCfaEdge);
          }
          e = e.getPreviousState();
        }

        return Collections.singleton(new CallstackState(element, functionName, callNode));
      }
    case FunctionReturnEdge:
      {
        FunctionReturnEdge cfaEdge = (FunctionReturnEdge)pCfaEdge;

        CallstackState element = (CallstackState)pElement;

        String calledFunction = cfaEdge.getPredecessor().getFunctionName();
        String callerFunction = cfaEdge.getSuccessor().getFunctionName();

        CFANode returnNode = cfaEdge.getSuccessor();
        CFANode callNode = returnNode.getEnteringSummaryEdge().getPredecessor();

        assert calledFunction.equals(element.getCurrentFunction());

        if (!callNode.equals(element.getCallNode())) {
          // this is not the right return edge
          return Collections.emptySet();
        }

        CallstackState returnElement = element.getPreviousState();

        assert callerFunction.equals(returnElement.getCurrentFunction());

        return Collections.singleton(returnElement);
      }
    }

    return Collections.singleton(pElement);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pElement, List<AbstractState> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) {

    return null;
  }
}