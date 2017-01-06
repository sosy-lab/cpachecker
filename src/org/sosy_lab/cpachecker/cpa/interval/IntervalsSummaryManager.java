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
package org.sosy_lab.cpachecker.cpa.interval;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.summary.blocks.Block;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.Summary;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.SummaryManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * Summary manager for the interval CPA.
 */
public class IntervalsSummaryManager implements SummaryManager {

  private final LogManager logger;
  private final IntervalAnalysisTransferRelation transferRelation;
  private final int threshold;

  IntervalsSummaryManager(
      LogManager pLogger,
      IntervalAnalysisTransferRelation pTransferRelation, int pThreshold) {
    logger = pLogger;
    transferRelation = pTransferRelation;
    threshold = pThreshold;
  }

  @Override
  public List<? extends Summary> generateSummaries(
      AbstractState pCallState,
      Precision pCallPrecision,
      List<? extends AbstractState> pReturnStates,
      List<Precision> pJoinPrecisions,
      CFANode pCallNode,
      Block pBlock
  ) throws CPATransferException {
    IntervalAnalysisState iCallState = (IntervalAnalysisState) pCallState;
    assert pCallNode.getNumLeavingEdges() == 1;
    CFAEdge callEdge = pCallNode.getLeavingEdge(0);

    IntervalAnalysisState weakenedCallstate =
        getWeakenedCallState(iCallState, pCallPrecision, callEdge, pBlock);

    assert !pReturnStates.isEmpty();
    Stream<IntervalAnalysisState> stream =
        pReturnStates.stream().map(s -> (IntervalAnalysisState) s);

    Optional<IntervalAnalysisState> out = stream.reduce((a, b) -> a.join(b, threshold));
    return Collections.singletonList(new IntervalSummary(
        weakenedCallstate,
        out.get()
    ));
  }

  @Override
  public List<AbstractState> getAbstractSuccessorsForSummary(
      AbstractState pCallState,
      Precision pCallPrecision,
      List<Summary> pSummaries,
      Block pBlock,
      CFAEdge pCallEdge)
      throws CPAException, InterruptedException {

    List<AbstractState> out = new ArrayList<>(pSummaries.size());
    for (Summary s : pSummaries) {
      out.add(getAbstractSuccessorForSummary(
          (IntervalAnalysisState) pCallState, (IntervalSummary) s, pCallEdge, pCallPrecision
      ));
    }
    return out;
  }

  private IntervalAnalysisState getAbstractSuccessorForSummary(
      IntervalAnalysisState pCallState,
      IntervalSummary iSummary,
      CFAEdge pCallEdge,
      Precision pCallPrecision) throws CPATransferException, InterruptedException {
    IntervalAnalysisState copy = IntervalAnalysisState.copyOf(pCallState);

    IntervalAnalysisState returnState = iSummary.getStateAtReturn();

    CFAEdge returnEdge = findReturnEdge(pCallEdge);

    Collection<IntervalAnalysisState> joinStates = transferRelation.getAbstractSuccessorsForEdge(
        returnState, pCallPrecision, returnEdge);
    assert joinStates.size() == 1;
    IntervalAnalysisState joinState = joinStates.iterator().next();

    joinState.getIntervalMap().forEach(
        (var, interval) -> copy.addInterval(var, interval, -1)
    );

    return copy;
  }

  private CFAEdge findReturnEdge(CFAEdge callEdge) {
    CFANode joinNode = callEdge.getPredecessor().getLeavingSummaryEdge().getSuccessor();
    assert joinNode.getNumEnteringEdges() == 1;
    return joinNode.getEnteringEdge(0);
  }

  @Override
  public IntervalAnalysisState getWeakenedCallState(
      AbstractState pCallState, Precision pPrecision, CFAEdge pCallEdge, Block pBlock) {
    IntervalAnalysisState iState = (IntervalAnalysisState) pCallState;
    IntervalAnalysisState clone = IntervalAnalysisState.copyOf(iState);

    Set<String> readVarNames = pBlock.getReadVariablesForCallEdge(pCallEdge).stream()
        .map(w -> w.get().getQualifiedName()).collect(Collectors.toSet());

    iState.getIntervalMap().keySet().stream()
        .filter(v -> !readVarNames.contains(v))
        .forEach(v -> clone.removeInterval(v));
    logger.log(Level.INFO, "Weakened", iState, "to", clone);
    return clone;
  }

  @Override
  public IntervalSummary merge(
      Summary pSummaryNew,
      Summary pSummaryExisting) throws CPAException, InterruptedException {

    IntervalSummary iSummaryNew = (IntervalSummary) pSummaryNew;
    IntervalSummary iSummaryExisting = (IntervalSummary) pSummaryExisting;
    if (isDescribedBy(iSummaryNew, iSummaryExisting)) {
      return iSummaryExisting;
    }

    return new IntervalSummary(
        iSummaryNew.getStateAtCallsite().join(iSummaryExisting.getStateAtCallsite(), threshold),
        iSummaryNew.getStateAtReturn().join(iSummaryExisting.getStateAtReturn(), threshold)
    );
  }

  @Override
  public boolean isDescribedBy(Summary pSummary1, Summary pSummary2) {
    IntervalSummary iSummary1 = (IntervalSummary) pSummary1;
    IntervalSummary iSummary2 = (IntervalSummary) pSummary2;

    return iSummary1.getStateAtCallsite().isLessOrEqual(
        iSummary2.getStateAtCallsite()
    ) && iSummary1.getStateAtReturn().isLessOrEqual(
        iSummary2.getStateAtReturn()
    );
  }

  @Override
  public boolean isCallsiteLessThanSummary(
      AbstractState pCallsite,
      Summary pSummary) {
    IntervalAnalysisState iState = (IntervalAnalysisState) pCallsite;
    IntervalSummary iSummary = (IntervalSummary) pSummary;
    return iState.isLessOrEqual(iSummary.getStateAtCallsite());
  }

  private static class IntervalSummary implements Summary {

    /**
     * Intervals over parameters, read global variables.
     */
    private final IntervalAnalysisState stateAtCallsite;

    /**
     * Intervals over returned variable, changed global variables.
     */
    private final IntervalAnalysisState stateAtReturn;

    private IntervalSummary(
        IntervalAnalysisState pStateAtCallsite,
        IntervalAnalysisState pStateAtReturn) {
      stateAtCallsite = pStateAtCallsite;
      stateAtReturn = pStateAtReturn;
    }

    IntervalAnalysisState getStateAtCallsite() {
      return stateAtCallsite;
    }

    IntervalAnalysisState getStateAtReturn() {
      return stateAtReturn;
    }

    @Override
    public boolean equals(@Nullable Object pO) {
      if (this == pO) {
        return true;
      }
      if (pO == null || getClass() != pO.getClass()) {
        return false;
      }
      IntervalSummary that = (IntervalSummary) pO;
      return Objects.equals(stateAtCallsite, that.stateAtCallsite) &&
          Objects.equals(stateAtReturn, that.stateAtReturn);
    }

    @Override
    public int hashCode() {
      return Objects.hash(stateAtCallsite, stateAtReturn);
    }

    @Override
    public String toString() {
      return "IntervalSummary{stateAtCallsite=(" + stateAtCallsite
          + "), stateAtReturn=(" + stateAtReturn + ")}";
    }
  }
}
