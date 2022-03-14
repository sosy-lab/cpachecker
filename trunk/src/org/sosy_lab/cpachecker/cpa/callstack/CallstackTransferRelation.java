// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.callstack;

import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CThreadOperationStatement.CThreadCreateStatement;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class CallstackTransferRelation extends SingleEdgeTransferRelation {

  /**
   * This flag might be set by external CPAs (e.g. BAM) to indicate a recursive context that might
   * not be recognized by the CallstackCPA. (In case of BAM the operator Reduce splits an indirect
   * recursive call f-g-f into two calls f-g and g-f, which are both non-recursive.) A function-call
   * in a recursive context will be skipped, if the Option 'skipRecursion' is enabled.
   */
  private boolean isRecursiveContext = false;

  protected final CallstackOptions options;
  protected final LogManagerWithoutDuplicates logger;

  public CallstackTransferRelation(CallstackOptions pOptions, LogManager pLogger) {
    options = pOptions;
    logger = new LogManagerWithoutDuplicates(pLogger);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pElement, Precision pPrecision, CFAEdge pEdge) throws CPATransferException {

    final CallstackState e = (CallstackState) pElement;
    final CFANode pred = pEdge.getPredecessor();
    final CFANode succ = pEdge.getSuccessor();
    final String predFunction = pred.getFunctionName();
    final String succFunction = succ.getFunctionName();

    switch (pEdge.getEdgeType()) {
      case StatementEdge:
        {
          AStatementEdge edge = (AStatementEdge) pEdge;
          if (edge.getStatement() instanceof AFunctionCall) {
            AExpression functionNameExp =
                ((AFunctionCall) edge.getStatement())
                    .getFunctionCallExpression()
                    .getFunctionNameExpression();
            if (functionNameExp instanceof AIdExpression) {
              String functionName = ((AIdExpression) functionNameExp).getName();
              if (options.getUnsupportedFunctions().contains(functionName)) {
                throw new UnsupportedCodeException(functionName, edge, edge.getStatement());
              }
            }
          }

          if (pEdge instanceof CFunctionSummaryStatementEdge) {
            if (!shouldGoByFunctionSummaryStatement(e, (CFunctionSummaryStatementEdge) pEdge)) {
              // should go by function call and skip the current edge
              return ImmutableSet.of();
            }
            // otherwise use this edge just like a normal edge
          }
          break;
        }

      case FunctionCallEdge:
        {
          final String calledFunction = succ.getFunctionName();
          final CFANode callerNode = pred;

          if (hasRecursion(e, calledFunction)) {
            if (skipRecursiveFunctionCall(e, (FunctionCallEdge) pEdge)) {
              // skip recursion, don't enter function
              logger.logOnce(
                  Level.WARNING,
                  "Skipping recursive function call from",
                  pred.getFunctionName(),
                  "to",
                  calledFunction);
              return ImmutableSet.of();
            } else {
              // recursion is unsupported
              logger.log(
                  Level.INFO,
                  "Recursion detected, aborting. To ignore recursion, add -skipRecursion to the"
                      + " command line.");
              throw new UnsupportedCodeException("recursion", pEdge);
            }
          } else {
            // regular function call:
            //    add the called function to the current stack

            return ImmutableSet.of(new CallstackState(e, calledFunction, callerNode));
          }
        }

      case FunctionReturnEdge:
        {
          final String calledFunction = predFunction;
          final String callerFunction = succFunction;
          final CFANode callNode = succ.getEnteringSummaryEdge().getPredecessor();
          final CallstackState returnElement;

          assert calledFunction.equals(e.getCurrentFunction())
                  || isWildcardState(e, AnalysisDirection.FORWARD)
              : String.format(
                  "not in scope of called function \"%s\" when leaving function \"%s\" in state"
                      + " \"%s\"",
                  calledFunction, e.getCurrentFunction(), e);

          if (isWildcardState(e, AnalysisDirection.FORWARD)) {
            returnElement = new CallstackState(null, callerFunction, e.getCallNode());

          } else {
            if (!callNode.equals(e.getCallNode())) {
              // this is not the right return edge
              return ImmutableSet.of();
            }

            // we are in a function return:
            //    remove the current function from the stack;
            //    the new abstract state is the predecessor state in the stack
            returnElement = e.getPreviousState();

            assert callerFunction.equals(returnElement.getCurrentFunction())
                    || isWildcardState(returnElement, AnalysisDirection.FORWARD)
                : String.format(
                    "calling function \"%s\" not available after function return into function"
                        + " scope \"%s\" in state \"%s\"",
                    callerFunction, returnElement.getCurrentFunction(), returnElement);
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
   * @param direction direction of the analysis
   * @return {@code true} if the given state should be treated as a wildcard, {@code false}
   *     otherwise.
   */
  protected boolean isWildcardState(final CallstackState pState, AnalysisDirection direction) {
    // TODO: Maybe it would be better to have designated wildcard states (without a call node)
    // instead of this heuristic.
    String function = pState.getCurrentFunction();
    CFANode callNode = pState.getCallNode();

    // main function "call" case
    if (callNode instanceof FunctionEntryNode && callNode.getFunctionName().equals(function)) {
      return false;
    }

    // Normal function call case
    for (FunctionEntryNode node :
        CFAUtils.successorsOf(pState.getCallNode()).filter(FunctionEntryNode.class)) {
      if (node.getFunctionName().equals(pState.getCurrentFunction())) {
        return false;
      }
    }

    // Not a function call node -> wildcard state
    // Info: a backward-analysis causes an callstack-state with a non-function-call-node,
    // build from the target state on getInitialState.
    return direction == AnalysisDirection.FORWARD;
  }

  protected boolean skipRecursiveFunctionCall(
      final CallstackState element, final FunctionCallEdge callEdge) {
    // Cannot skip if there is no edge for skipping
    // (this would just terminate the path here -> unsound).
    if (leavingEdges(callEdge.getPredecessor())
        .filter(CFunctionSummaryStatementEdge.class)
        .isEmpty()) {
      return false;
    }

    if (options.skipRecursion()) {
      return true;
    }
    if (options.skipFunctionPointerRecursion() && hasFunctionPointerRecursion(element, callEdge)) {
      return true;
    }
    if (options.skipVoidRecursion() && hasVoidRecursion(element, callEdge)) {
      return true;
    }
    return false;
  }

  /** check, if the current function-call has already appeared in the call-stack. */
  protected boolean hasRecursion(final CallstackState pCurrentState, final String pCalledFunction) {
    if (isRecursiveContext) { // external CPA has seen recursion
      return true;
    }
    // iterate through the current stack and search for an equal name
    CallstackState e = pCurrentState;
    int counter = 0;
    while (e != null) {
      if (e.getCurrentFunction().equals(pCalledFunction)) {
        counter++;
        if (counter > options.getRecursionBoundDepth()) {
          return true;
        }
      }
      e = e.getPreviousState();
    }
    return false;
  }

  protected boolean hasFunctionPointerRecursion(
      final CallstackState element, final FunctionCallEdge pCallEdge) {
    if (pCallEdge
        .getRawStatement()
        .startsWith("pointer call(")) { // Hack, see CFunctionPointerResolver
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

      if (e.getPreviousState() == null) {
        // reached beginning of program or current BAM-block, abort
        return false;
      }

      FunctionCallEdge callEdge = findOutgoingCallEdge(e.getCallNode());
      if (callEdge.getRawStatement().startsWith("pointer call(")) {
        return true;
      }

      e = e.getPreviousState();
    }
    throw new AssertionError();
  }

  protected boolean hasVoidRecursion(
      final CallstackState element, final FunctionCallEdge pCallEdge) {
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

      if (e.getPreviousState() == null) {
        // reached beginning of program or current BAM-block, abort
        return false;
      }

      FunctionSummaryEdge summaryEdge = e.getCallNode().getLeavingSummaryEdge();
      if (summaryEdge.getExpression() instanceof AFunctionCallStatement) {
        return true;
      }

      e = e.getPreviousState();
    }
    throw new AssertionError();
  }

  protected boolean shouldGoByFunctionSummaryStatement(
      CallstackState element, CFunctionSummaryStatementEdge sumEdge) {
    String functionName = sumEdge.getFunctionName();
    FunctionCallEdge callEdge = findOutgoingCallEdge(sumEdge.getPredecessor());
    if (sumEdge.getFunctionCall() instanceof CThreadCreateStatement) {
      // Thread operations should be handled twice, so, go by the summary edge
      return true;
    }
    assert functionName.equals(callEdge.getSuccessor().getFunctionName());
    return hasRecursion(element, functionName) && skipRecursiveFunctionCall(element, callEdge);
  }

  protected FunctionCallEdge findOutgoingCallEdge(CFANode predNode) {
    for (CFAEdge edge : leavingEdges(predNode)) {
      if (edge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
        return (FunctionCallEdge) edge;
      }
    }
    throw new AssertionError(
        "Missing function call edge for function call summary edge after node " + predNode);
  }

  public void enableRecursiveContext() {
    isRecursiveContext = true;
  }

  public void disableRecursiveContext() {
    isRecursiveContext = false;
  }
}
