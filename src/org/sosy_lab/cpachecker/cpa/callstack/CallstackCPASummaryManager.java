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
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.summary.blocks.Block;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.Summary;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.SummaryManager;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * Summary manager for the CallstackCPA.
 */
public class CallstackCPASummaryManager implements SummaryManager {

  @Override
  public List<AbstractState> getAbstractSuccessorsForSummary(
      AbstractState state,
      Precision precision,
      List<Summary> pSummary,
      Block pBlock,
      CFANode pCallsite)
      throws CPATransferException, InterruptedException {

    // Summary application leaves the callstack invariant
    // (we have entered the function and we have left the function)
    return Collections.singletonList(state);
  }

  @Override
  public AbstractState getWeakenedCallState(
      AbstractState pCallState, Precision pPrecision, CFANode pCallNode, Block pBlock) {
    CallstackState cState = (CallstackState) pCallState;
    if (cState.getDepth() == 1) {
      return pCallState;
    } else {

      return new CallstackState(
          // Remove all the frames except for the very last one.
          null, cState.getCurrentFunction(), cState.getCallNode()
      );
    }
  }

  @Override
  public List<? extends Summary> generateSummaries(
      AbstractState pCallState,
      Precision pCallPrecision,
      List<? extends AbstractState> pJoinStates,
      List<Precision> pJoinPrecisions,
      CFANode pEntryNode,
      Block pBlock) {

    CallstackState cCallstackState = (CallstackState) pCallState;
    return Collections.singletonList(new CallstackSummary(cCallstackState));
  }

  @Override
  public Summary merge(
      Summary pSummary1, Summary pSummary2) {

    // Do not merge states.
    return pSummary2;
  }

  @Override
  public boolean isDescribedBy(Summary pSummary1, Summary pSummary2) {
    return pSummary1.equals(pSummary2);
  }

  @Override
  public boolean isSummaryApplicableAtCallsite(
      Summary pSummary,
      AbstractState pCallsite) {
    CallstackSummary cSummary = (CallstackSummary) pSummary;
    CallstackState cCallsite = (CallstackState) pCallsite;
    return cCallsite.equalsByValue(cSummary.getCallsiteCallstack());
  }

  /**
   * Callstack is the same at the function entry and exit:
   * hence having a single state is enough.
   */
  private static class CallstackSummary implements Summary {

    private final CallstackState callsiteCallstack;

    private CallstackSummary(CallstackState pCallsiteCallstack) {
      callsiteCallstack = pCallsiteCallstack;
    }

    CallstackState getCallsiteCallstack() {
      return callsiteCallstack;
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
      return callsiteCallstack.equalsByValue(that.callsiteCallstack);
    }

    @Override
    public int hashCode() {
      return Objects.hash(callsiteCallstack);
    }

    @Override
    public String toString() {
      return "CallstackSummary{callsiteStack=" + callsiteCallstack.getStack() + '}';
    }
  }
}
