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
package org.sosy_lab.cpachecker.cpa.arg;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.summary.blocks.Block;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.Summary;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.SummaryManager;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.UseSummaryCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * Summary manager for {@link ARGCPA}.
 * Operates over wrapped summaries directly.
 *
 * <p>For now, does not add any value, and simply propagates all calls.
 */
public class ARGSummaryManager implements SummaryManager {

  private final SummaryManager wrapped;

  ARGSummaryManager(
      ConfigurableProgramAnalysis pCpa) {
    Preconditions.checkArgument(pCpa instanceof UseSummaryCPA,
        "For summary generation all nested CPAs have to "
            + "implement UseSummaryCPA interface.");
    wrapped = ((UseSummaryCPA) pCpa).getSummaryManager();
  }

  @Override
  public List<? extends AbstractState> getAbstractSuccessorsForSummary(
      AbstractState pCallState,
      Precision pCallPrecision,
      List<Summary> pSummaries,
      Block pBlock,
      CFAEdge pCallEdge)
      throws CPAException, InterruptedException {
    ARGState aState = (ARGState) pCallState;

    return wrapped.getAbstractSuccessorsForSummary(
          aState.getWrappedState(), pCallPrecision, pSummaries, pBlock, pCallEdge
      ).stream().map(s -> new ARGState(s, null)).collect(Collectors.toList());
  }

  @Override
  public AbstractState getWeakenedCallState(
      AbstractState pCallState, Precision pPrecision, CFAEdge pCFAEdge, Block pBlock) {
    ARGState aState = (ARGState) pCallState;
    return new ARGState(
        wrapped.getWeakenedCallState(aState.getWrappedState(), pPrecision, pCFAEdge, pBlock),
        null
    );
  }

  @Override
  public boolean isDescribedBy(Summary pSummary1, Summary pSummary2) {
    return wrapped.isDescribedBy(pSummary1, pSummary2);
  }

  @Override
  public List<? extends Summary> generateSummaries(
      AbstractState pCallState,
      Precision pCallPrecision,
      List<? extends AbstractState> pReturnStates,
      List<Precision> pJoinPrecisions,
      CFANode pCallNode,
      Block pBlock) throws CPATransferException {

    ARGState aEntryState = (ARGState) pCallState;
    return wrapped.generateSummaries(
        aEntryState.getWrappedState(),
        pCallPrecision,
        pReturnStates.stream().map(a -> ((ARGState) a).getWrappedState()).collect(Collectors.toList()),
        pJoinPrecisions,
        pCallNode,
        pBlock
    );
  }

  @Override
  public Summary merge(
      Summary pSummary1, Summary pSummary2) throws CPAException, InterruptedException {
    return wrapped.merge(pSummary1, pSummary2);
  }

  @Override
  public boolean isCallsiteLessThanSummary(
      AbstractState pCallsite, Summary pSummary) {

    ARGState aState = (ARGState) pCallsite;
    return wrapped.isCallsiteLessThanSummary(
        aState.getWrappedState(), pSummary
    );
  }

  @Override
  public String getSummaryPartition(Summary pSummary) {
    return wrapped.getSummaryPartition(pSummary);
  }

  @Override
  public String getCallstatePartition(AbstractState pCallstate) {
    ARGState aState = (ARGState) pCallstate;
    return wrapped.getCallstatePartition(aState.getWrappedState());
  }
}
