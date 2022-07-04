// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.rangedExecution;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class RangedExecutionTransferRelation extends SingleEdgeTransferRelation {

  private final LogManager logger;

  private ValueAnalysisTransferRelation leftTR;
  private ValueAnalysisTransferRelation rightTR;

  public RangedExecutionTransferRelation(LogManager pLogger) {
    logger = pLogger;
  }

  /**
   * This is the main method that delegates the control-flow to the corresponding edge-type-specific
   * methods.
   */
  @Override
  public Collection<RangedExecutionState> getAbstractSuccessorsForEdge(
      final AbstractState abstractState, final Precision abstractPrecision, final CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {

    RangedExecutionState state =
        AbstractStates.extractStateByType(abstractState, RangedExecutionState.class);

    if (Objects.isNull(state)) {
      throw new CPATransferException("state has the wrong format");
    }

    ValueAnalysisState oldLeft = state.getLeftState();
    ValueAnalysisState oldRight = state.getRightState();

    if (isMiddleState(oldLeft, oldRight)) {
      // Just for optimization
      return ImmutableSet.of(RangedExecutionState.getMiddleState());
    }

    ValueAnalysisState newLeft =
        oldLeft == null
            ? null
            : leftTR.getAbstractSuccessorsForEdge(oldLeft, abstractPrecision, cfaEdge).stream()
                .findAny()
                .orElse(null);
    ValueAnalysisState newRight =
        oldRight == null
            ? null
            : rightTR.getAbstractSuccessorsForEdge(oldRight, abstractPrecision, cfaEdge).stream()
                .findAny()
                .orElse(null);

    if (cfaEdge instanceof CAssumeEdge) {
      boolean truthAssumption = ((CAssumeEdge) cfaEdge).getTruthAssumption();

      // Middle states are already handled above
      // Check for beeing on left path and right path is empty:
      if (oldLeft != null && oldRight == null) {
        if (truthAssumption && newLeft == null) {
          // The left bounds goes right (false branch), hence the left (true) branch is not in range
          // anymore, hence stop the computation at this point
          return ImmutableSet.of();
        }
        // We are either still on the left path (if newLeft != null or in the middle (if
        // !truthAssumption && newLeft == null. Both cases are handled with the same return value.
        return Collections.singleton(new RangedExecutionState(newLeft, newRight));
      }
      // Check for right states:
      if (oldRight != null && oldLeft == null) {
        if (!truthAssumption && newRight == null) {
          // The right bound goes left (true branch), hence the right(false) branch is not in range
          // anymore, hence stop the computation at this point
          return ImmutableSet.of();
        }
        // We are either still on the right bound path (if newRight != null) or in the middle (if
        // truthAssumption && newRight == null). BOth cases are handled with the same return value.
        return Collections.singleton(new RangedExecutionState(newLeft, newRight));
      }
    } else {

      // Two special cases needs to  be checked further:
      // If we are at the left bound path and the computation stops (at a non-assume edge), because
      // the testcase is under-specified, follow this path and treat it as "middel-path"
      if (oldLeft != null && newLeft == null) {
        return Collections.singleton(new RangedExecutionState(newLeft, newRight));
      }
      // If we are at the right bound path and the computation stops (at a non-assume edge), because
      // the testcase is under-specified,stop the computation
      if (oldRight != null && newRight == null) {
        return ImmutableSet.of();
      }
    }
    return Collections.singleton(new RangedExecutionState(newLeft, newRight));
  }

  private boolean isMiddleState(ValueAnalysisState pOldLeft, ValueAnalysisState pOldRight) {
    return pOldLeft == null && pOldRight == null;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      Iterable<AbstractState> pOtherStates,
      @Nullable CFAEdge pCfaEdge,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {
    logger.log(
        Level.FINE,
        String.format(
            "Current abstract state at location %s is  '%s'",
            AbstractStates.extractLocations(pOtherStates).first().get(),
            AbstractStates.extractStateByType(pState, RangedExecutionState.class)));
    return super.strengthen(pState, pOtherStates, pCfaEdge, pPrecision);
  }

  public void setCPTRs(
      ValueAnalysisTransferRelation pLeftTR, ValueAnalysisTransferRelation pRightTR) {
    this.leftTR = pLeftTR;
    this.rightTR = pRightTR;
  }
}
