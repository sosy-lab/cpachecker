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
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.singleloop.ProgramCounterValueAssumeEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;

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

    final CallstackState e = (CallstackState) pElement;
    final CFANode nextAnalysisLoc = pEdge.getPredecessor();
    final CFANode prevAnalysisLoc = pEdge.getSuccessor();
    final String prevAnalysisFunction = prevAnalysisLoc.getFunctionName();
    final String nextAnalysisFunction = nextAnalysisLoc.getFunctionName();

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
      if (pEdge instanceof ProgramCounterValueAssumeEdge) {
        throw new UnsupportedCCodeException("ProgramCounterValueAssumeEdge not yet supported for the backwards analysis!", pEdge);
      }
      break;
    }

    case FunctionReturnEdge: {
        if (hasRecursion(e, nextAnalysisFunction)) {
          if (skipRecursiveFunctionCall(e, (FunctionCallEdge)pEdge)) {
            // skip recursion, don't enter function
            logger.logOnce(
                Level.WARNING,
                "Skipping recursive function call from",
                prevAnalysisFunction,
                "to", nextAnalysisFunction);

            return Collections.emptySet();

          } else {
            // recursion is unsupported
            logger.log(Level.INFO, "Recursion detected, aborting. To ignore recursion, add -skipRecursion to the command line.");
            throw new UnsupportedCCodeException("recursion", pEdge);
          }

        } else {
          // regular function call
          return Collections.singleton(new CallstackState(e, nextAnalysisFunction, prevAnalysisLoc));
        }
      }

    case FunctionCallEdge: {
        final CFANode callNode = nextAnalysisLoc;
        final CallstackState returnElement;

        if (!isWildcardState(e)) {
          if (!callNode.equals(e.getCallNode())) {
            // this is not the right return edge
            return Collections.emptySet();
          }
          returnElement = e.getPreviousState();

          assert nextAnalysisFunction.equals(returnElement.getCurrentFunction());

        } else {
          throw new UnsupportedCCodeException("ARTIFICIAL_PROGRAM_COUNTER not yet supported for the backwards analysis!", pEdge);
        }

        return Collections.singleton(returnElement);
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
