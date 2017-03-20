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

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
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
import org.sosy_lab.cpachecker.cfa.postprocessing.global.singleloop.ProgramCounterValueAssumeEdge;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;

@Options(prefix="cpa.callstack")
public class CallstackTransferRelationBackwards extends CallstackTransferRelation {

  public CallstackTransferRelationBackwards(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pLogger);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pElement, Precision pPrecision, CFAEdge pEdge)
      throws CPATransferException {

    // Transfer relation for a BACKWARDS analysis!!!

    // Goal of this CPA: Different states for different function-calls
    // caller node

    final CallstackState e = (CallstackState) pElement;
    final CFANode nextAnalysisLoc = pEdge.getPredecessor();
    final CFANode prevAnalysisLoc = pEdge.getSuccessor();
    final String prevAnalysisFunction = prevAnalysisLoc.getFunctionName();
    final String nextAnalysisFunction = nextAnalysisLoc.getFunctionName();

    switch (pEdge.getEdgeType()) {
    case StatementEdge: {
      AStatementEdge edge = (AStatementEdge)pEdge;
      if (edge.getStatement() instanceof AFunctionCall) {
        AExpression functionNameExp = ((AFunctionCall)edge.getStatement()).getFunctionCallExpression().getFunctionNameExpression();
        if (functionNameExp instanceof AIdExpression) {
          String functionName = ((AIdExpression)functionNameExp).getName();
          if (UNSUPPORTED_FUNCTIONS.containsKey(functionName)) {
            throw new UnrecognizedCodeException(
                "Unsupported feature: " + functionName,
                edge, edge.getStatement());
          }
        }
      }

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
      if (pEdge instanceof ProgramCounterValueAssumeEdge) {
        throw new UnsupportedCCodeException("ProgramCounterValueAssumeEdge not yet supported for the backwards analysis!", pEdge);
      }
      break;
    }

    case FunctionReturnEdge: {
      FunctionReturnEdge edge = (FunctionReturnEdge) pEdge;
      CFANode correspondingCallNode = edge.getSummaryEdge().getPredecessor();
        if (hasRecursion(e, nextAnalysisFunction)) {
          if (skipRecursion) {
            logger.logOnce(
                Level.WARNING, "Skipping recursive function call from",
                prevAnalysisFunction, "to", nextAnalysisFunction);

            return Collections.emptySet();
          } else {
            logger.log(Level.INFO, "Recursion detected, aborting. To ignore recursion, add -skipRecursion to the command line.");
            throw new UnsupportedCCodeException("recursion", pEdge);
          }

        } else {
          // BACKWARDS: Build the stack on the function-return edge (add element to the stack)
          return Collections.singleton(new CallstackState(e,
              nextAnalysisFunction,
              correspondingCallNode));
        }
      }

    case FunctionCallEdge: {
        // FIXME: Actually, during backwards analysis you always have wildcard
        // states, because you never know where you "came from",
        // and obviously, there is some handling of that situation below,
        // see "if (nextStackState == null) { ...".
        if (isWildcardState(e, AnalysisDirection.BACKWARD)) {
          throw new UnsupportedCCodeException("ARTIFICIAL_PROGRAM_COUNTER not yet supported for the backwards analysis!", pEdge);
        }
        Collection<CallstackState> result;

        CallstackState nextStackState = e.getPreviousState();
        if (nextStackState == null) {
          // BACKWARDS: The analysis might start somewhere in the call tree (and we might have not predecessor state)
          result = Collections.singleton(
              new CallstackState(null, nextAnalysisFunction, nextAnalysisLoc)
          );

          // This if clause is needed to check if the correct FunctionCallEdge is taken.
          // Consider a method which is called from different other methods, then
          // there is more than one FunctionCallEdge at this CFANode. To chose the
          // correct one, we compare the callNode that is saved in the current
          // CallStackState with the next location of the analysis.
        } else if (e.getCallNode().equals(nextAnalysisLoc)) {
          result = Collections.singleton(nextStackState);
        } else {
          result = Collections.emptySet();
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
        return (FunctionCallEdge)edge;
      }
    }

    throw new AssertionError("Missing function call edge for function call summary edge");
  }
}
