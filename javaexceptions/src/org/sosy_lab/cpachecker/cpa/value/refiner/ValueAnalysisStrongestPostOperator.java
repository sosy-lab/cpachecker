// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.refiner;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Deque;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.conditions.path.AssignmentsInPathCondition.UniqueAssignmentsInPathConditionState;
import org.sosy_lab.cpachecker.cpa.value.UnknownValueAssigner;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation;
import org.sosy_lab.cpachecker.cpa.value.symbolic.ConstraintsStrengthenOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** Strongest post-operator using {@link ValueAnalysisTransferRelation}. */
public class ValueAnalysisStrongestPostOperator
    implements StrongestPostOperator<ValueAnalysisState> {

  private final ValueAnalysisTransferRelation transfer;

  public ValueAnalysisStrongestPostOperator(
      final LogManager pLogger, final Configuration pConfig, final CFA pCfa)
      throws InvalidConfigurationException {

    transfer =
        new ValueAnalysisTransferRelation(
            pLogger,
            pCfa,
            new ValueAnalysisTransferRelation.ValueTransferOptions(pConfig),
            new UnknownValueAssigner(),
            new ConstraintsStrengthenOperator(pConfig, pLogger),
            null);
  }

  @Override
  public Optional<ValueAnalysisState> getStrongestPost(
      final ValueAnalysisState pOrigin, final Precision pPrecision, final CFAEdge pOperation)
      throws CPAException, InterruptedException {

    final Collection<ValueAnalysisState> successors =
        transfer.getAbstractSuccessorsForEdge(pOrigin, pPrecision, pOperation);

    if (successors.isEmpty()) {
      return Optional.empty();

    } else {
      return Optional.of(Iterables.getOnlyElement(successors));
    }
  }

  @Override
  public ValueAnalysisState handleFunctionCall(
      ValueAnalysisState state, CFAEdge edge, Deque<ValueAnalysisState> callstack) {
    callstack.push(state);
    return state;
  }

  @Override
  public ValueAnalysisState handleFunctionReturn(
      ValueAnalysisState next, CFAEdge edge, Deque<ValueAnalysisState> callstack) {

    final ValueAnalysisState callState = callstack.pop();
    return next.rebuildStateAfterFunctionCall(callState, (FunctionExitNode) edge.getPredecessor());
  }

  @Override
  public ValueAnalysisState performAbstraction(
      final ValueAnalysisState pNext,
      final CFANode pCurrNode,
      final ARGPath pErrorPath,
      final Precision pPrecision) {

    assert pPrecision instanceof VariableTrackingPrecision;

    VariableTrackingPrecision precision = (VariableTrackingPrecision) pPrecision;

    final boolean performAbstraction = precision.allowsAbstraction();
    final Collection<MemoryLocation> exceedingMemoryLocations =
        obtainExceedingMemoryLocations(pErrorPath);

    if (performAbstraction) {
      for (Entry<MemoryLocation, ValueAndType> e : pNext.getConstants()) {
        MemoryLocation memoryLocation = e.getKey();
        if (!precision.isTracking(memoryLocation, e.getValue().getType(), pCurrNode)) {
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
    UniqueAssignmentsInPathConditionState assignments =
        AbstractStates.extractStateByType(
            pPath.getLastState(), UniqueAssignmentsInPathConditionState.class);

    if (assignments == null) {
      return ImmutableSet.of();
    }

    return assignments.getMemoryLocationsExceedingThreshold();
  }
}
