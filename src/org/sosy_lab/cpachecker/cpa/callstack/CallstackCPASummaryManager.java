/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.summary.blocks.Block;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.Summary;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.SummaryManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * Summary manager for the CallstackCPA.
 */
public class CallstackCPASummaryManager implements SummaryManager {

  @Override
  public AbstractState getAbstractSuccessorForSummary(
      AbstractState state,
      Precision precision,
      List<Summary> pSummary,
      Block pBlock)
      throws CPATransferException, InterruptedException {

    // Summary application leaves the callstack invariant
    // (we have entered the function and we have left the function)
    return state;
  }

  @Override
  public AbstractState getWeakenedCallState(
      AbstractState pState, Precision pPrecision, Block pBlock) {
    CallstackState cState = (CallstackState) pState;
    if (cState.getDepth() == 1) {
      return cState;
    } else {

      return new CallstackState(
      // Remove all the frames except for the very last one.
          null, cState.getCurrentFunction(), cState.getCallNode()
      );
    }
  }

  @Override
  public AbstractState projectToCallsite(Summary pSummary) {
    CallstackSummary cSummary = (CallstackSummary) pSummary;
    return new CallstackState(
        null, cSummary.getFunctionName(), cSummary.getEntryNode());
  }

  @Override
  public AbstractState projectToPostcondition(Summary pSummary) {
    CallstackSummary cSummary = (CallstackSummary) pSummary;
    return new CallstackState(
        null, cSummary.getFunctionName(), cSummary.getEntryNode());
  }

  @Override
  public List<? extends Summary> generateSummaries(
      AbstractState pCallState,
      Precision pEntryPrecision,
      List<? extends AbstractState> pReturnState,
      List<Precision> pReturnPrecision,
      CFANode pEntryNode,
      Block pBlock) {
    return Collections.singletonList(new CallstackSummary(pEntryNode));
  }

  @Override
  public Summary merge(
      Summary pSummary1, Summary pSummary2) {

    // Do not merge states.
    return pSummary2;
  }

  @Override
  public boolean isDescribedBy(Summary pSummary1, Summary pSummary2)
      throws CPAException, InterruptedException {
    return pSummary1.equals(pSummary2);
  }

  @Override
  public boolean isSummaryCoveringCallsite(
      Summary pSummary,
      AbstractState pCallsite,
      AbstractDomain pAbstractDomain
  ) throws CPAException, InterruptedException {

    // todo: any cases where it's not the case?
    return true;
  }

  /**
   * Callstack is the same at the function entry and exit:
   * hence having a single state is enough.
   */
  private static class CallstackSummary implements Summary {

    private final CFANode entryNode;

    private CallstackSummary(CFANode pEntryNode) {
      entryNode = pEntryNode;
    }

    public CFANode getEntryNode() {
      return entryNode;
    }

    public String getFunctionName() {
      return entryNode.getFunctionName();
    }

    @Override
    public boolean equals(@Nullable Object pO) {
      if (this == pO) {
        return true;
      }
      if (pO == null || getClass() != pO.getClass()) {
        return false;
      }
      CallstackSummary that = (CallstackSummary) pO;
      return Objects.equals(entryNode, that.entryNode);
    }

    @Override
    public int hashCode() {
      return Objects.hash(entryNode);
    }

    @Override
    public String toString() {
      return "CallstackSummary{" +
          "entryNode=" + entryNode +
          '}';
    }
  }
}
