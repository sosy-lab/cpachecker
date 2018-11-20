/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.interval.Refiner;

import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Deque;
import java.util.Map.Entry;
import java.util.Optional;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.interval.Interval;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisPrecision;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisPrecision.IntervalAnalysisFullPrecision;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisState;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class IntervalAnalysisStrongestPostOperator
    implements StrongestPostOperator<IntervalAnalysisState> {

  private final IntervalAnalysisTransferRelation transfer;

  public IntervalAnalysisStrongestPostOperator(
      final LogManager pLogger, boolean pSplitIntervals, int pThreshold) {
    transfer = new IntervalAnalysisTransferRelation(pSplitIntervals, pThreshold, pLogger);
  }

  @Override
  public Optional<IntervalAnalysisState> getStrongestPost(
      final IntervalAnalysisState pOrigin, final Precision pPrecision, final CFAEdge pOperation)
      throws CPAException {
    final Collection<IntervalAnalysisState> successors =
        transfer.getAbstractSuccessorsForEdge(pOrigin, pPrecision, pOperation);
    if (successors.isEmpty()) return Optional.empty();

    return Optional.of(Iterables.getOnlyElement(successors));
  }

  @Override
  public IntervalAnalysisState handleFunctionCall(
      IntervalAnalysisState state, CFAEdge edge, Deque<IntervalAnalysisState> callstack) {
    callstack.push(state);
    return state;
  }

  @Override
  public IntervalAnalysisState handleFunctionReturn(
      IntervalAnalysisState next, CFAEdge edge, Deque<IntervalAnalysisState> callstack) {

    final IntervalAnalysisState callState = callstack.pop();
    return next.rebuildStateAfterFunctionCall(callState, (FunctionExitNode) edge.getPredecessor());
  }

  @Override
  public IntervalAnalysisState performAbstraction(
      final IntervalAnalysisState pNext,
      final CFANode pCurrNode,
      final ARGPath pErrorPath,
      final Precision pPrecision) {

    assert pPrecision instanceof IntervalAnalysisPrecision;

    IntervalAnalysisPrecision precision;
    if (pPrecision instanceof IntervalAnalysisFullPrecision)
      precision = (IntervalAnalysisFullPrecision) pPrecision;
    else precision = (IntervalAnalysisPrecision) pPrecision;

    for (Entry<String, Interval> e : pNext.getConstants()) {
      MemoryLocation memoryLocation = MemoryLocation.valueOf(e.getKey());
      if (!precision.isTracking(e.getKey())) pNext.forget(memoryLocation);
    }

    return pNext;
  }
}
