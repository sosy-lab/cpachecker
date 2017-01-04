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

  IntervalsSummaryManager(LogManager pLogger) {
    logger = pLogger;
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
          (IntervalAnalysisState) pCallState, (IntervalSummary) s, pBlock, pCallEdge
      ));
    }
    return out;
  }

  private IntervalAnalysisState getAbstractSuccessorForSummary(
      IntervalAnalysisState pCallState,
      IntervalSummary iSummary,
      Block pBlock,
      CFAEdge pCallEdge) throws CPATransferException, InterruptedException {
    IntervalAnalysisState copy = IntervalAnalysisState.copyOf(pCallState);

    // todo: this does not seem to be working.
    Set<String> modifiedVarNames = pBlock.getModifiedVariablesForCallEdge(pCallEdge).stream()
        .map(w -> w.get().getQualifiedName()).collect(Collectors.toSet());

    logger.log(Level.INFO, "Variables modified inside the block", pBlock, "are", modifiedVarNames);

    // Remove all intervals associated with variables potentially modified inside the called
    // function.
    pCallState.getIntervalMap().keySet().stream()
        .filter(v -> modifiedVarNames.contains(v))
        .forEach(v -> copy.removeInterval(v));

    IntervalAnalysisState joinedState = iSummary.getStateAtJoin();

    joinedState.getIntervalMap().forEach(
        (var, interval) -> copy.addInterval(var, interval, -1)
    );

    logger.log(Level.INFO, "Postcondition after application of the summary\n",
        iSummary, "to state\n", pCallState, "is\n", copy);
    return copy;
  }

  @Override
  public AbstractState getWeakenedCallState(
      AbstractState pCallState, Precision pPrecision, CFAEdge pCallEdge, Block pBlock) {
    IntervalAnalysisState iState = (IntervalAnalysisState) pCallState;
    IntervalAnalysisState clone = IntervalAnalysisState.copyOf(iState);

    Set<String> readVarNames = pBlock.getReadVariablesForCallEdge(pCallEdge).stream()
        .map(w -> w.get().getQualifiedName()).collect(Collectors.toSet());

    logger.log(Level.INFO, "Vars read inside the block", pBlock, "are: ", readVarNames);

    iState.getIntervalMap().keySet().stream()
        .filter(v -> !readVarNames.contains(v))
        .forEach(v -> clone.removeInterval(v));
    logger.log(Level.INFO, "Weakened ", iState, " to ", clone);
    return clone;
  }

  @Override
  public List<? extends Summary> generateSummaries(
      AbstractState pCallState,
      Precision pCallPrecision,
      List<? extends AbstractState> pJoinStates,
      List<Precision> pJoinPrecisions,
      CFANode pEntryNode,
      Block pBlock
  ) {
    IntervalAnalysisState iCallstackState = (IntervalAnalysisState) pCallState;

    assert !pJoinStates.isEmpty();
    Stream<IntervalAnalysisState> stream =
        pJoinStates.stream().map(s -> (IntervalAnalysisState) s);

    Optional<IntervalAnalysisState> out = stream.reduce((a, b) -> a.join(b));
    return Collections.singletonList(new IntervalSummary(iCallstackState, out.get()));
  }

  @Override
  public IntervalSummary merge(
      Summary pSummary1,
      Summary pSummary2) throws CPAException, InterruptedException {

    IntervalSummary iSummary1 = (IntervalSummary) pSummary1;
    IntervalSummary iSummary2 = (IntervalSummary) pSummary2;
    return new IntervalSummary(
        iSummary1.getStateAtCallsite().join(iSummary2.getStateAtCallsite()),
        iSummary1.getStateAtJoin().join(iSummary2.getStateAtJoin())
    );
  }

  @Override
  public boolean isDescribedBy(Summary pSummary1, Summary pSummary2) {
    IntervalSummary iSummary1 = (IntervalSummary) pSummary1;
    IntervalSummary iSummary2 = (IntervalSummary) pSummary2;

    return iSummary1.getStateAtCallsite().isLessOrEqual(
        iSummary2.getStateAtCallsite()
    ) && iSummary1.getStateAtJoin().isLessOrEqual(
        iSummary2.getStateAtJoin()
    );
  }

  @Override
  public boolean isSummaryApplicableAtCallsite(Summary pSummary, AbstractState pCallsite) {
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
    private final IntervalAnalysisState stateAtJoin;

    private IntervalSummary(
        IntervalAnalysisState pStateAtCallsite,
        IntervalAnalysisState pStateAtJoin) {
      stateAtCallsite = pStateAtCallsite;
      stateAtJoin = pStateAtJoin;
    }

    IntervalAnalysisState getStateAtCallsite() {
      return stateAtCallsite;
    }

    IntervalAnalysisState getStateAtJoin() {
      return stateAtJoin;
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
          Objects.equals(stateAtJoin, that.stateAtJoin);
    }

    @Override
    public int hashCode() {
      return Objects.hash(stateAtCallsite, stateAtJoin);
    }

    @Override
    public String toString() {
      return "IntervalSummary{stateAtCall=" + stateAtCallsite
          + ", stateAtJoin=" + stateAtJoin + '}';
    }
  }
}
