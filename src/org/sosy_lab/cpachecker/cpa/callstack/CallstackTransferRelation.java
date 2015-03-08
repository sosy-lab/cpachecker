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
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.singleloop.CFASingleLoopTransformation;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.singleloop.ProgramCounterValueAssumeEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;

@Options(prefix="cpa.callstack")
public class CallstackTransferRelation extends SingleEdgeTransferRelation {

  @Option(secure=true, name="depth",
      description = "depth of recursion bound")
  protected int recursionBoundDepth = 0;

  @Option(secure=true, name="skipRecursion", description = "Skip recursion (this is unsound)." +
      " Treat function call as a statement (the same as for functions without bodies)")
  protected boolean skipRecursion = false;

  @Option(secure=true, description = "Skip recursion if it happens only by going via a function pointer (this is unsound)." +
      " Imprecise function pointer tracking often lead to false recursions.")
  protected boolean skipFunctionPointerRecursion = false;

  @Option(secure=true, description = "Skip recursion if it happens only by going via a void function (this is unsound).")
  protected boolean skipVoidRecursion = false;

  private final CallstackStateFactory callstackStateFactory;

  protected final LogManagerWithoutDuplicates logger;

  public CallstackTransferRelation(Configuration config, LogManager pLogger,
      CallstackStateFactory pCallstackStateFactory) throws InvalidConfigurationException {
    config.inject(this);
    logger = new LogManagerWithoutDuplicates(pLogger);
    callstackStateFactory = pCallstackStateFactory;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pElement, Precision pPrecision, CFAEdge pEdge)
      throws CPATransferException {

    final CallstackState e = (CallstackState) pElement;
    final CFANode pred = pEdge.getPredecessor();
    final CFANode succ = pEdge.getSuccessor();
    final String predFunction = pred.getFunctionName();
    final String succFunction = succ.getFunctionName();

    switch (pEdge.getEdgeType()) {
    case StatementEdge: {
      if (pEdge instanceof CFunctionSummaryStatementEdge) {
        if (!shouldGoByFunctionSummaryStatement(e, (CFunctionSummaryStatementEdge) pEdge)) {
          // should go by function call and skip the current edge
          return Collections.emptySet();
        }
        // otherwise use this edge just like a normal edge
      }
      break;
    }

    case AssumeEdge: {
      boolean successorIsInCallstackContext = succFunction.equals(e.getCurrentFunction());
      boolean isArtificialPCVEdge = pEdge instanceof ProgramCounterValueAssumeEdge;
      boolean isSuccessorAritificialPCNode = succFunction.equals(CFASingleLoopTransformation.ARTIFICIAL_PROGRAM_COUNTER_FUNCTION_NAME);
      boolean isPredecessorAritificialPCNode = predFunction.equals(CFASingleLoopTransformation.ARTIFICIAL_PROGRAM_COUNTER_FUNCTION_NAME);
      boolean isFunctionTransition = !succFunction.equals(predFunction);
      if (!successorIsInCallstackContext
          && !e.getCurrentFunction().equals(CFASingleLoopTransformation.ARTIFICIAL_PROGRAM_COUNTER_FUNCTION_NAME)
          && ((!isSuccessorAritificialPCNode && isArtificialPCVEdge)
              || isPredecessorAritificialPCNode && isFunctionTransition)) {
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
        final String calledFunction = succ.getFunctionName();
        final CFANode callerNode = pred;

        if (hasRecursion(e, calledFunction)) {
          if (skipRecursiveFunctionCall(e, (FunctionCallEdge)pEdge)) {
            // skip recursion, don't enter function
            logger.logOnce(Level.WARNING, "Skipping recursive function call from",
                pred.getFunctionName(), "to", calledFunction);
            return Collections.emptySet();
          } else {
            // recursion is unsupported
            logger.log(Level.INFO, "Recursion detected, aborting. To ignore recursion, add -skipRecursion to the command line.");
            throw new UnsupportedCCodeException("recursion", pEdge);
          }
        } else {
          // regular function call:
          //    add the called function to the current stack

          return Collections.singleton(
              callstackStateFactory.create(e, calledFunction, callerNode));
        }
      }

    case FunctionReturnEdge: {
        final String calledFunction = predFunction;
        final String callerFunction = succFunction;
        final CFANode callNode = succ.getEnteringSummaryEdge().getPredecessor();
        final CallstackState returnElement;

        assert calledFunction.equals(e.getCurrentFunction()) || e.getCurrentFunction().equals(CFASingleLoopTransformation.ARTIFICIAL_PROGRAM_COUNTER_FUNCTION_NAME);

        if (isWildcardState(e)) {
          returnElement = e;

        } else {
          if (!callNode.equals(e.getCallNode())) {
            // this is not the right return edge
            return Collections.emptySet();
          }

          // we are in a function return:
          //    remove the current function from the stack;
          //    the new abstract state is the predecessor state in the stack
          returnElement = e.getPreviousState();

          assert callerFunction.equals(returnElement.getCurrentFunction()) || isWildcardState(returnElement);
        }

        return Collections.singleton(returnElement);
      }

    default:
      break;
    }

    return Collections.singleton(pElement);
  }

  /**
   * Checks if the given callstack state should be treated as a wildcard state.
   *
   * @param pState the state to check.
   *
   * @return {@code true} if the given state should be treated as a wildcard,
   * {@code false} otherwise.
   */
  protected boolean isWildcardState(CallstackState pState) {
    return pState.getCurrentFunction().equals(CFASingleLoopTransformation.ARTIFICIAL_PROGRAM_COUNTER_FUNCTION_NAME);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pElement, List<AbstractState> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) {

    return null;
  }

  protected boolean skipRecursiveFunctionCall(final CallstackState element,
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

  protected boolean hasRecursion(final CallstackState pCurrentState, final String pCalledFunction) {
    CallstackState e = pCurrentState;
    int counter = 0;
    while (e != null) {
      if (e.getCurrentFunction().equals(pCalledFunction)) {
        counter++;
        if (counter > recursionBoundDepth) {
          return true;
        }
      }
      e = e.getPreviousState();
    }
    return false;
  }

  protected boolean hasFunctionPointerRecursion(final CallstackState element,
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

  protected boolean hasVoidRecursion(final CallstackState element,
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

  protected boolean shouldGoByFunctionSummaryStatement(CallstackState element, CFunctionSummaryStatementEdge sumEdge) {
    String functionName = sumEdge.getFunctionName();
    FunctionCallEdge callEdge = findOutgoingCallEdge(sumEdge.getPredecessor());
    assert functionName.equals(callEdge.getSuccessor().getFunctionName());
    return hasRecursion(element, functionName) && skipRecursiveFunctionCall(element, callEdge);
  }

  protected FunctionCallEdge findOutgoingCallEdge(CFANode predNode) {
    for (CFAEdge edge : leavingEdges(predNode)) {
      if (edge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
        return (FunctionCallEdge)edge;
      }
    }
    throw new AssertionError("Missing function call edge for function call summary edge");
  }
}
