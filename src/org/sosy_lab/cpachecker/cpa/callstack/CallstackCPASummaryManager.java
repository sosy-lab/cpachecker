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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.summary.blocks.Block;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.Summary;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.SummaryManager;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * Summary manager for the CallstackCPA: uses dumb do-nothing summary.
 */
public class CallstackCPASummaryManager implements SummaryManager {

  @Override
  public List<AbstractState> getAbstractSuccessorsForSummary(
      AbstractState pCallState,
      Precision pCallPrecision,
      List<Summary> pSummary,
      Block pBlock,
      CFAEdge pCallEdge)
      throws CPATransferException, InterruptedException {

    // Summary application leaves the callstack invariant
    // (we have entered the function and we have left the function)
    return Collections.singletonList(pCallState);
  }

  @Override
  public AbstractState getWeakenedCallState(
      AbstractState pCallState, Precision pPrecision, CFAEdge pCFAEdge, Block pBlock) {
    CallstackState cState = (CallstackState) pCallState;
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
  public List<? extends Summary> generateSummaries(
      AbstractState pCallState,
      Precision pCallPrecision,
      List<? extends AbstractState> pReturnStates,
      List<Precision> pJoinPrecisions,
      CFANode pCallNode,
      Block pBlock) {
    return Collections.singletonList(dumbSummary);
  }

  @Override
  public Summary merge(
      Summary pSummary1, Summary pSummary2) {
    return pSummary2;
  }

  @Override
  public boolean isDescribedBy(Summary pSummary1, Summary pSummary2) {
    return true;
  }

  @Override
  public boolean isCallsiteLessThanSummary(
      AbstractState pCallsite, Summary pSummary) {
    return true;
  }

  private final Summary dumbSummary = new Summary() {
    @Override
    public String toString() {
      return "";
    }
  };

}
