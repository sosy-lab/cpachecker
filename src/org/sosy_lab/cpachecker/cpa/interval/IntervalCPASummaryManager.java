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
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.Summary;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.SummaryManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Summary manager for the interval CPA.
 */
public class IntervalCPASummaryManager implements SummaryManager {


  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForSummary(
      AbstractState state,
      Precision precision,
      Summary pSummary,
      Block pBlock)
      throws CPAException, InterruptedException {

    // Propagate the intervals for those variables invariant
    // under the function call, use summary for others.
    IntervalAnalysisState iState = (IntervalAnalysisState) state;
    IntervalSummary iSummary = (IntervalSummary) pSummary;

    IntervalAnalysisState copy = IntervalAnalysisState.copyOf(iState);
    List<String> toRemove = new ArrayList<>();
    for (String key : copy.getIntervalMap().keySet()) {
      if (pBlock.getReferencedVariables().stream().anyMatch(
          v -> v.equals(key)
      )) {
        toRemove.add(key);
      }
    }
    toRemove.forEach(key -> copy.removeInterval(key));
    iSummary.getStateAtExit().getIntervalMap().forEach(
        (var, interval) -> copy.addInterval(var, interval, -1)
    );
    return Collections.singleton(copy);
  }

  @Override
  public AbstractState projectToPrecondition(Summary pSummary) {
    IntervalSummary iSummary = (IntervalSummary) pSummary;
    return iSummary.getStateAtEntry();
  }

  @Override
  public AbstractState projectToPostcondition(Summary pSummary) {
    IntervalSummary iSummary = (IntervalSummary) pSummary;
    return iSummary.getStateAtExit();
  }

  @Override
  public Summary generateSummary(
      AbstractState pEntryState,
      Precision pEntryPrecision,
      AbstractState pReturnState,
      Precision pReturnPrecision,
      CFANode pEntryNode,
      Block pBlock
  ) {
    IntervalAnalysisState iEntryState = (IntervalAnalysisState) pEntryState;
    IntervalAnalysisState iReturnState = (IntervalAnalysisState) pReturnState;

    return new IntervalSummary(
        weaken(iEntryState, pBlock), weaken(iReturnState, pBlock)
    );
  }

  /**
   * Weaken the {@code pState} by removing all constraints associated
   * with variables not read inside the {@code pBlock}.
   *
   * @return weakened interval state
   */
  private IntervalAnalysisState weaken(IntervalAnalysisState pState, Block pBlock) {
    IntervalAnalysisState clone = IntervalAnalysisState.copyOf(pState);

    for (String var : pState.getIntervalMap().keySet()) {
      if (!pBlock.getReferencedVariables().stream().anyMatch(
          r -> r.getName().equals(var)
      )) {
        clone.removeInterval(var);
      }
    }
    return clone;
  }

  @Override
  public Summary merge(
      Summary pSummary1,
      Summary pSummary2) throws CPAException, InterruptedException {

    IntervalSummary iSummary1 = (IntervalSummary) pSummary1;
    IntervalSummary iSummary2 = (IntervalSummary) pSummary2;
    return new IntervalSummary(
        iSummary1.getStateAtEntry().join(iSummary2.getStateAtEntry()),
        iSummary2.getStateAtExit().join(iSummary2.getStateAtExit())
    );
  }

  private static class IntervalSummary implements Summary {

    /**
     * Intervals over parameters, read global variables.
     */
    private final IntervalAnalysisState stateAtEntry;

    /**
     * Intervals over returned variable, changed global variables.
     */
    private final IntervalAnalysisState stateAtExit;

    private IntervalSummary(
        IntervalAnalysisState pStateAtEntry,
        IntervalAnalysisState pStateAtExit) {
      stateAtEntry = pStateAtEntry;
      stateAtExit = pStateAtExit;
    }

    IntervalAnalysisState getStateAtEntry() {
      return stateAtEntry;
    }

    IntervalAnalysisState getStateAtExit() {
      return stateAtExit;
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
      return Objects.equals(stateAtEntry, that.stateAtEntry) &&
          Objects.equals(stateAtExit, that.stateAtExit);
    }

    @Override
    public int hashCode() {
      return Objects.hash(stateAtEntry, stateAtExit);
    }

    @Override
    public String toString() {
      return "IntervalSummary{" +
          "stateAtEntry=" + stateAtEntry +
          ", stateAtExit=" + stateAtExit + '}';
    }
  }
}
