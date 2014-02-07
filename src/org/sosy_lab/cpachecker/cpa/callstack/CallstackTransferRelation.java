/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.CFASingleLoopTransformation;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;

@Options(prefix="cpa.callstack")
public class CallstackTransferRelation implements TransferRelation {

  @Option(name="depth", description = "depth of recursion bound")
  private int recursionBoundDepth = 0;

  @Option(name="skipRecursion", description = "Skip recursion (this is unsound)." +
      " Treat function call as a statement (the same as for functions without bodies)")
  private boolean skipRecursion = false;

  @Option(description = "Skip recursion if it happens only by going via a function pointer (this is unsound)." +
      " Imprecise function pointer tracking often lead to false recursions.")
  private boolean skipFunctionPointerRecursion = false;

  @Option(description = "Skip recursion if it happens only by going via a void function (this is unsound).")
  private boolean skipVoidRecursion = false;

  private final LogManagerWithoutDuplicates logger;

  public CallstackTransferRelation(Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    config.inject(this);
    logger = new LogManagerWithoutDuplicates(pLogger);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {

    switch (pCfaEdge.getEdgeType()) {
    case StatementEdge: {
      if (pCfaEdge instanceof CFunctionSummaryStatementEdge) {
        CFunctionSummaryStatementEdge summary = (CFunctionSummaryStatementEdge)pCfaEdge;
        CallstackState element = (CallstackState)pElement;
        if (!shouldGoByFunctionSummaryStatement(element, summary)) {
          // should go by function call and skip the current edge
          return Collections.emptySet();
        }
        // otherwise use this edge just like a normal edge
      }
      break;
    }
    case AssumeEdge: {
      CallstackState element = (CallstackState) pElement;
      String successorFunctionName = pCfaEdge.getSuccessor().getFunctionName();
      if (!successorFunctionName.equals(CFASingleLoopTransformation.ARTIFICIAL_PROGRAM_COUNTER_FUNCTION_NAME)
          && !successorFunctionName.equals(element.getCurrentFunction())
          && pCfaEdge instanceof CFASingleLoopTransformation.ProgramCounterValueAssumeEdge) {
        /*
         * This edge is syntactically reachable, but makes no sense from this
         * state, as it would change function without passing a function entry
         * or exit node.
         *
         * Edges like this are introduced by the single loop transformation.
         */
        return Collections.emptySet();
      }
      break;
    }
    case FunctionCallEdge: {
        CallstackState element = (CallstackState)pElement;
        String functionName = pCfaEdge.getSuccessor().getFunctionName();

        if (hasRecursion(element, functionName)) {
          if (skipRecursiveFunctionCall(element, (FunctionCallEdge)pCfaEdge)) {
            // skip recursion, don't enter function
            logger.logOnce(Level.WARNING, "Skipping recursive function call from",
                pCfaEdge.getPredecessor().getFunctionName(), "to", functionName);
            return Collections.emptySet();
          } else {
            // recursion is unsupported
            logger.log(Level.INFO, "Recursion detected, aborting. To ignore recursion, add -skipRecursion to the command line.");
            throw new UnsupportedCCodeException("recursion", pCfaEdge);
          }
        } else {
          // regular function call
          CFANode callNode = pCfaEdge.getPredecessor();
          return Collections.singleton(new CallstackState(element, functionName, callNode));
        }
      }
    case FunctionReturnEdge: {
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

  private boolean skipRecursiveFunctionCall(final CallstackState element,
      final FunctionCallEdge callEdge) {
    if (skipRecursion) {
      return true;
    }
    if (skipFunctionPointerRecursion && hasFunctionPointerRecursion(element, callEdge)) {
      return true;
    }
    if (skipVoidRecursion && hasVoidRecursion(element, callEdge)) {
      return true;
    }
    return false;
  }

  private boolean hasRecursion(final CallstackState element, final String functionName) {
    CallstackState e = element;
    int counter = 0;
    while (e != null) {
      if (e.getCurrentFunction().equals(functionName)) {
        counter++;
        if (counter > recursionBoundDepth) {
          return true;
        }
      }
      e = e.getPreviousState();
    }
    return false;
  }

  private boolean hasFunctionPointerRecursion(final CallstackState element,
      final FunctionCallEdge pCallEdge) {
    if (pCallEdge.getRawStatement().startsWith("pointer call(")) { // Hack, see CFunctionPointerResolver
      return true;
    }

    final String functionName = pCallEdge.getSuccessor().getFunctionName();
    CallstackState e = element;
    while (e != null) {
      if (e.getCurrentFunction().equals(functionName)) {
        // reached the previous stack frame of the same function,
        // and no function pointer so far
        return false;
      }

      FunctionCallEdge callEdge = findOutgoingCallEdge(element.getCallNode());
      if (callEdge.getRawStatement().startsWith("pointer call(")) {
        return true;
      }

      e = e.getPreviousState();
    }
    throw new AssertionError();
  }

  private boolean hasVoidRecursion(final CallstackState element,
      final FunctionCallEdge pCallEdge) {
    if (pCallEdge.getSummaryEdge().getExpression() instanceof AFunctionCallStatement) {
      return true;
    }

    final String functionName = pCallEdge.getSuccessor().getFunctionName();
    CallstackState e = element;
    while (e != null) {
      if (e.getCurrentFunction().equals(functionName)) {
        // reached the previous stack frame of the same function,
        // and no function pointer so far
        return false;
      }

      FunctionSummaryEdge summaryEdge = element.getCallNode().getLeavingSummaryEdge();
      if (summaryEdge.getExpression() instanceof AFunctionCallStatement) {
        return true;
      }

      e = e.getPreviousState();
    }
    throw new AssertionError();
  }

  private boolean shouldGoByFunctionSummaryStatement(CallstackState element, CFunctionSummaryStatementEdge sumEdge) {
    String functionName = sumEdge.getFunctionName();
    FunctionCallEdge callEdge = findOutgoingCallEdge(sumEdge.getPredecessor());
    assert functionName.equals(callEdge.getSuccessor().getFunctionName());
    return hasRecursion(element, functionName) && skipRecursiveFunctionCall(element, callEdge);
  }

  private FunctionCallEdge findOutgoingCallEdge(CFANode predNode) {
    for (CFAEdge edge : leavingEdges(predNode)) {
      if (edge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
        return (FunctionCallEdge)edge;
      }
    }
    throw new AssertionError("Missing function call edge for function call summary edge");
  }
}
