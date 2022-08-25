// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.refiner;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Deque;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.conditions.path.AssignmentsInPathCondition.UniqueAssignmentsInPathConditionState;
import org.sosy_lab.cpachecker.cpa.smg2.SMGCPAExportOptions;
import org.sosy_lab.cpachecker.cpa.smg2.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.smg2.SMGTransferRelation;
import org.sosy_lab.cpachecker.cpa.value.symbolic.ConstraintsStrengthenOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** Strongest post-operator using {@link SMGTransferRelation}. */
public class SMGStrongestPostOperator implements StrongestPostOperator<SMGState> {

  private final SMGTransferRelation transfer;

  public SMGStrongestPostOperator(
      final LogManager pLogger, final Configuration pConfig, final CFA pCfa)
      throws InvalidConfigurationException {

    SMGOptions options = new SMGOptions(pConfig);
    SMGCPAExportOptions exportOptions =
        new SMGCPAExportOptions(options.getExportSMGFilePattern(), options.getExportSMGLevel());
    transfer =
        new SMGTransferRelation(
            pLogger,
            options,
            exportOptions,
            pCfa,
            new ConstraintsStrengthenOperator(pConfig, pLogger),
            null);
  }

  @Override
  public Optional<SMGState> getStrongestPost(
      final SMGState pOrigin, final Precision pPrecision, final CFAEdge pOperation)
      throws CPAException, InterruptedException {

    final Collection<SMGState> successors =
        transfer.getAbstractSuccessorsForEdge(pOrigin, pPrecision, pOperation);

    if (successors.isEmpty()) {
      return Optional.empty();

    } else {
      return Optional.of(Iterables.getOnlyElement(successors));
    }
  }

  @Override
  public SMGState handleFunctionCall(SMGState state, CFAEdge edge, Deque<SMGState> callstack) {
    callstack.push(state);
    return state;
  }

  @Override
  public SMGState handleFunctionReturn(SMGState next, CFAEdge edge, Deque<SMGState> callstack) {
    callstack.pop();
    return next;
  }

  @Override
  public SMGState performAbstraction(
      final SMGState pNext,
      final CFANode pCurrNode,
      final ARGPath pErrorPath,
      final Precision pPrecision) {

    assert pPrecision instanceof VariableTrackingPrecision;

    SMGState nextState = pNext;
    VariableTrackingPrecision precision = (VariableTrackingPrecision) pPrecision;

    final boolean performAbstraction = precision.allowsAbstraction();
    final Collection<MemoryLocation> exceedingMemoryLocations =
        obtainExceedingMemoryLocations(pErrorPath);

    if (performAbstraction) {
      for (MemoryLocation memoryLocation :
          nextState.getMemoryModel().getMemoryLocationsAndValuesForSPCWithoutHeap().keySet()) {
        CType trackedType = nextState.getMemoryModel().getTypeOfVariable(memoryLocation);
        if (!precision.isTracking(memoryLocation, trackedType, pCurrNode)) {
          nextState = nextState.copyAndForget(memoryLocation).getState();
        }
      }
    }

    for (MemoryLocation exceedingMemoryLocation : exceedingMemoryLocations) {
      nextState = nextState.copyAndForget(exceedingMemoryLocation).getState();
    }

    return nextState;
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
