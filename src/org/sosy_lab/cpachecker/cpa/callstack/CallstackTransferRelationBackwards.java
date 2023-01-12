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
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

public class CallstackTransferRelationBackwards extends CallstackTransferRelation {

  public CallstackTransferRelationBackwards(CallstackOptions pOptions, LogManager pLogger) {
    super(pOptions, pLogger);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pElement, Precision pPrecision, CFAEdge pEdge) throws CPATransferException {

    // Transfer relation for a BACKWARDS analysis!!!

    // Goal of this CPA: Different states for different function-calls
    // caller node

    final CallstackState e = (CallstackState) pElement;
    final CFANode nextAnalysisLoc = pEdge.getPredecessor();
    final CFANode prevAnalysisLoc = pEdge.getSuccessor();
    final String prevAnalysisFunction = prevAnalysisLoc.getFunctionName();
    final String nextAnalysisFunction = nextAnalysisLoc.getFunctionName();

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
                throw new UnrecognizedCodeException(
                    "Unsupported feature: " + options.getUnsupportedFunctions(),
                    edge,
                    edge.getStatement());
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

      case FunctionReturnEdge:
        {
          FunctionReturnEdge edge = (FunctionReturnEdge) pEdge;
          CFANode correspondingCallNode = edge.getSummaryEdge().getPredecessor();
          if (hasRecursion(e, nextAnalysisFunction)) {
            if (options.skipRecursion()) {
              logger.logOnce(
                  Level.WARNING,
                  "Skipping recursive function call from",
                  prevAnalysisFunction,
                  "to",
                  nextAnalysisFunction);

              return ImmutableSet.of();
            } else {
              logger.log(
                  Level.INFO,
                  "Recursion detected, aborting. To ignore recursion, add -skipRecursion to the"
                      + " command line.");
              throw new UnsupportedCodeException("recursion", pEdge);
            }

          } else {
            // BACKWARDS: Build the stack on the function-return edge (add element to the stack)
            return ImmutableSet.of(
                new CallstackState(e, nextAnalysisFunction, correspondingCallNode));
          }
        }

      case FunctionCallEdge:
        {
          // FIXME: Actually, during backwards analysis you always have wildcard
          // states, because you never know where you "came from",
          // and obviously, there is some handling of that situation below,
          // see "if (nextStackState == null) { ...".
          // FIXME: ARTIFICIAL_PROGRAM_COUNTER does not even exist anymore
          if (isWildcardState(e, AnalysisDirection.BACKWARD)) {
            throw new UnsupportedCodeException(
                "ARTIFICIAL_PROGRAM_COUNTER not yet supported for the backwards analysis!", pEdge);
          }
          Collection<CallstackState> result;

          CallstackState nextStackState = e.getPreviousState();
          if (nextStackState == null) {
            // BACKWARDS: The analysis might start somewhere in the call tree (and we might have not
            // predecessor state)
            result =
                ImmutableSet.of(new CallstackState(null, nextAnalysisFunction, nextAnalysisLoc));

            // This if clause is needed to check if the correct FunctionCallEdge is taken.
            // Consider a method which is called from different other methods, then
            // there is more than one FunctionCallEdge at this CFANode. To chose the
            // correct one, we compare the callNode that is saved in the current
            // CallStackState with the next location of the analysis.
          } else if (e.getCallNode().equals(nextAnalysisLoc)) {
            result = Collections.singleton(nextStackState);
          } else {
            result = ImmutableSet.of();
          }

          return result;
        }

      default:
        break;
    }

    return Collections.singleton(pElement);
  }

  @Override
  protected FunctionCallEdge findOutgoingCallEdge(CFANode predNode) {
    for (CFAEdge edge : leavingEdges(predNode)) {
      if (edge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
        return (FunctionCallEdge) edge;
      }
    }

    throw new AssertionError("Missing function call edge for function call summary edge");
  }
}
