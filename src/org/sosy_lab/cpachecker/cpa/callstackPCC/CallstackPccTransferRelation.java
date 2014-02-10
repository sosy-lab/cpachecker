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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;

@Options(prefix="cpa.callstack")
public class CallstackPccTransferRelation implements TransferRelation {

  @Option(name="depth", description = "depth of recursion bound")
  private int recursionBoundDepth = 0;

  @Option(name="skipRecursion", description = "Skip recursion." +
      " Treat function call as a statement (the same as for functions without bodies)")
  private boolean skipRecursion = false;

  public CallstackPccTransferRelation(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {

    switch (pCfaEdge.getEdgeType()) {
    case StatementEdge: {
      if (pCfaEdge instanceof CFunctionSummaryStatementEdge) {
        //has function call edge
        CFunctionSummaryStatementEdge summary = (CFunctionSummaryStatementEdge)pCfaEdge;
        CallstackPccState element = (CallstackPccState)pElement;
        if (shouldGoByStatement(element, summary)) {//skip call, return the same element
          return Collections.singleton(pElement);
        } else {//should go by function call (skip current edge)
            return Collections.emptySet();
        }
      } else {
        return Collections.singleton(pElement);
      }
    }
    case FunctionCallEdge: {
        FunctionCallEdge cfaEdge = (FunctionCallEdge)pCfaEdge;
        CallstackPccState element = (CallstackPccState)pElement;
        if (shouldGoByFunctionCall(element, cfaEdge)) {
          String functionName = cfaEdge.getSuccessor().getFunctionName();
          CFANode callNode = cfaEdge.getPredecessor();
          if (hasRecursion(element, cfaEdge, recursionBoundDepth)) {
            throw new UnsupportedCCodeException("recursion", pCfaEdge);
          } else {
            return Collections.singleton(new CallstackPccState(element, functionName, callNode));
          }
        } else {
          return Collections.emptySet();
        }
      }
    case FunctionReturnEdge: {
        FunctionReturnEdge cfaEdge = (FunctionReturnEdge)pCfaEdge;

        CallstackPccState element = (CallstackPccState)pElement;

        String calledFunction = cfaEdge.getPredecessor().getFunctionName();
        String callerFunction = cfaEdge.getSuccessor().getFunctionName();

        CFANode returnNode = cfaEdge.getSuccessor();
        CFANode callNode = returnNode.getEnteringSummaryEdge().getPredecessor();

        assert calledFunction.equals(element.getCurrentFunction());

        if (!callNode.equals(element.getCallNode())) {
          // this is not the right return edge
          return Collections.emptySet();
        }

        CallstackPccState returnElement = element.getPreviousState();

        assert callerFunction.equals(returnElement.getCurrentFunction());

        return Collections.singleton(returnElement);
      }
    default:
      break;
    }

    return Collections.singleton(pElement);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pElement, List<AbstractState> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) {

    return null;
  }

  private boolean hasRecursion(CallstackPccState element, FunctionCallEdge callEdge, int depth) {
    String functionName = callEdge.getSuccessor().getFunctionName();
    CallstackPccState e = element;
    int counter = 0;
    while (e != null) {
      if (e.getCurrentFunction().equals(functionName)) {
        counter++;
        if (counter > depth) {
          return true;
        }
      }
      e = e.getPreviousState();
    }
    return false;
  }

  //call edge
  private boolean shouldGoByFunctionCall(CallstackPccState element, FunctionCallEdge callEdge) {
    if (!skipRecursion) {
      return true;
    } else {
      if (hasRecursion(element, callEdge, recursionBoundDepth)) {
        return false;
      } else {
        return true;
      }
    }
  }

  private boolean shouldGoByStatement(CallstackPccState element, CFunctionSummaryStatementEdge sumEdge) {
    String functionName = sumEdge.getFunctionName();
    if (functionName==null) {
      //TODO: decide what todo with it
      return true;
    }
    FunctionCallEdge callEdge = findCallEdge(sumEdge, functionName);
    if (callEdge==null) {
      return true;
    }
    return !shouldGoByFunctionCall(element, callEdge);
  }

  private FunctionCallEdge findCallEdge(CFAEdge pCfaEdge, String functionName) {
    CFANode predNode = pCfaEdge.getPredecessor();
    for (int i=0; i<predNode.getNumLeavingEdges(); i++) {
      CFAEdge edge = predNode.getLeavingEdge(i);
      if (edge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
        String fcallname = edge.getSuccessor().getFunctionName();
        if (functionName.equals(fcallname)) {
          return (FunctionCallEdge)edge;
        }
      }
    }
    return null;
  }

}
