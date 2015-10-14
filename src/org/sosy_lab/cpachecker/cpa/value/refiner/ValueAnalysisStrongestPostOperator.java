/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.value.refiner;
import static com.google.common.collect.FluentIterable.from;

import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.conditions.path.AssignmentsInPathCondition.UniqueAssignmentsInPathConditionState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

/**
 * Strongest post-operator using {@link ValueAnalysisTransferRelation}.
 */
public class ValueAnalysisStrongestPostOperator implements StrongestPostOperator<ValueAnalysisState> {

  private final ValueAnalysisTransferRelation transfer;

  public ValueAnalysisStrongestPostOperator(
      final LogManager pLogger,
      final Configuration pConfig,
      final CFA pCfa
  ) throws InvalidConfigurationException {

    transfer = new ValueAnalysisTransferRelation(pConfig, pLogger, pCfa);
  }

  @Override
  public Optional<ValueAnalysisState> getStrongestPost(
      final ValueAnalysisState pOrigin,
      final Precision pPrecision,
      final CFAEdge pOperation
  ) throws CPAException {

    final Collection<ValueAnalysisState> successors =
        transfer.getAbstractSuccessorsForEdge(pOrigin, pPrecision, pOperation);

    if (successors.isEmpty()) {
      return Optional.absent();

    } else {
      return Optional.of(Iterables.getOnlyElement(successors));
    }
  }

  @Override
  public ValueAnalysisState handleFunctionCall(ValueAnalysisState state, CFAEdge edge,
      Deque<ValueAnalysisState> callstack) {
    callstack.addLast(state);
    return state;
  }

  @Override
  public ValueAnalysisState handleFunctionReturn(ValueAnalysisState next, CFAEdge edge,
      Deque<ValueAnalysisState> callstack) {

    final ValueAnalysisState callState = callstack.removeLast();
    return next.rebuildStateAfterFunctionCall(callState, (FunctionExitNode)edge.getPredecessor());
  }

  @Override
  public ValueAnalysisState performAbstraction(
      final ValueAnalysisState pNext,
      final CFANode pCurrNode,
      final ARGPath pErrorPath,
      final Precision pPrecision
  ) {

    assert pPrecision instanceof VariableTrackingPrecision;

    VariableTrackingPrecision precision = (VariableTrackingPrecision)pPrecision;

    final boolean performAbstraction = precision.allowsAbstraction();
    final Collection<MemoryLocation> exceedingMemoryLocations =
        obtainExceedingMemoryLocations(pErrorPath);

    if (performAbstraction) {
      for (MemoryLocation memoryLocation : pNext.getTrackedMemoryLocations()) {
        if (!precision.isTracking(memoryLocation,
            pNext.getTypeForMemoryLocation(memoryLocation),
            pCurrNode)) {
          pNext.forget(memoryLocation);
        }
      }
    }

    for (MemoryLocation exceedingMemoryLocation : exceedingMemoryLocations) {
      pNext.forget(exceedingMemoryLocation);
    }

    return pNext;
  }

  protected Set<MemoryLocation> obtainExceedingMemoryLocations(final ARGPath pPath) {
    Set<MemoryLocation> result = new HashSet<>();

    /* Due to current implementation of reducer of UniqueAssignmentsInPathConditionState
     * we may loose information about unique assignments at entry state.
     * May be, reducer should be refactored.
     */
    Set<ARGState> functionCalls = from(pPath.asStatesList()).filter(new Predicate<ARGState>() {
      @Override
      public boolean apply(@Nullable ARGState pArg0) {
        return AbstractStates.extractLocation(pArg0) instanceof CFunctionEntryNode;
      }

    }).toSet();

    for (ARGState state : functionCalls) {
      UniqueAssignmentsInPathConditionState assignments =
          AbstractStates.extractStateByType(state,
              UniqueAssignmentsInPathConditionState.class);

      if (assignments != null) {
        result.addAll(assignments.getMemoryLocationsExceedingThreshold());
      }

    }
    return result;
  }
}
