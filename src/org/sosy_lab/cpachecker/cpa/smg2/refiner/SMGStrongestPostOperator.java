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
    transfer = new SMGTransferRelation(pLogger, options, exportOptions, pCfa);
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

    // Are old variables already pruned?
    // SMGs restore the old variables on their own, but we keep changes in heap (and stack through
    // pointers)!
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

    VariableTrackingPrecision precision = (VariableTrackingPrecision) pPrecision;

    final boolean performAbstraction = precision.allowsAbstraction();
    final Collection<MemoryLocation> exceedingMemoryLocations =
        obtainExceedingMemoryLocations(pErrorPath);

    if (performAbstraction) {
      for (MemoryLocation memoryLocation :
          pNext.getMemoryModel().getMemoryLocationsAndValuesForSPCWithoutHeap().keySet()) {
        CType trackedType = pNext.getMemoryModel().getTypeOfVariable(memoryLocation);
        if (!precision.isTracking(memoryLocation, trackedType, pCurrNode)) {
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
