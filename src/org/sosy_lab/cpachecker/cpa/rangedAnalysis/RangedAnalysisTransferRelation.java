// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.rangedAnalysis;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LoopStructure;

public class RangedAnalysisTransferRelation extends SingleEdgeTransferRelation {

  private final LogManager logger;

  private final Optional<LoopStructure> loopStruct;

  int abortCount = 0;
  private ValueAnalysisTransferRelation leftTR;
  private ValueAnalysisTransferRelation rightTR;

  public RangedAnalysisTransferRelation(LogManager pLogger, Configuration pConfig, CFA pCfa)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;
    loopStruct = pCfa.getLoopStructure();
  }

  /**
   * This is the main method that delegates the control-flow to the corresponding edge-type-specific
   * methods.
   */
  @Override
  public Collection<RangedAnalysisState> getAbstractSuccessorsForEdge(
      final AbstractState abstractState, final Precision abstractPrecision, final CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    RangedAnalysisState state =
        AbstractStates.extractStateByType(abstractState, RangedAnalysisState.class);

    if (Objects.isNull(state)) {
      throw new CPATransferException("state has the wrong format");
    }

    ValueAnalysisState oldLeft = state.getLeftState();
    ValueAnalysisState oldRight = state.getRightState();

    if (isMiddleState(oldLeft, oldRight) || reachesEndlessLoopHead(cfaEdge)) {
      // Just for optimization
      return ImmutableSet.of(RangedAnalysisState.getMiddleState());
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
      if (oldLeft != null) {
        if (truthAssumption && newLeft == null) {
          // The left bounds goes right (false branch), hence the left (true) branch is not in range
          // anymore, hence stop the computation at this point
          return ImmutableSet.of();
        }
        // We are either still on the left path (if newLeft != null or in the middle (if
        // !truthAssumption && newLeft == null. Both cases are handled with the same return value.
        return Collections.singleton(new RangedAnalysisState(newLeft, newRight));
      }
      // Check for right states:
      if (oldRight != null) {
        if (!truthAssumption && newRight == null) {
          // The right bound goes left (true branch), hence the right(false) branch is not in range
          // anymore, hence stop the computation at this point
          return ImmutableSet.of();
        }
        // We are either still on the right bound path (if newRight != null) or in the middle (if
        // truthAssumption && newRight == null). BOth cases are handled with the same return value.
        return Collections.singleton(new RangedAnalysisState(newLeft, newRight));
      }
    } else {

      // Two special cases needs to  be checked further:
      // If we are at the left bound path and the computation stops (at a non-assume edge), because
      // the testcase is under-specified, follow this path and treat it as "middel-path"
      if (oldLeft != null && newLeft == null) {
        return Collections.singleton(new RangedAnalysisState(newLeft, newRight));
      }
      // If we are at the right bound path and the computation stops (at a non-assume edge), because
      // the testcase is under-specified,stop the computation
      if (oldRight != null && newRight == null) {
        return ImmutableSet.of();
      }
    }
    return Collections.singleton(new RangedAnalysisState(newLeft, newRight));
  }

  private boolean reachesEndlessLoopHead(CFAEdge pCfaEdge) {
    if (loopStruct.isPresent()) {
      return loopStruct.orElseThrow().getAllLoops().stream()
          .filter(l -> l.getLoopHeads().contains(pCfaEdge.getSuccessor()))
          .anyMatch(l -> l.getOutgoingEdges().isEmpty());
    }
    return false;
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
    logger.logf(
        Level.FINE,
        "Current abstract state at location %s is  '%s'",
        AbstractStates.extractLocations(pOtherStates).first().get(),
        AbstractStates.extractStateByType(pState, RangedAnalysisState.class));
    return super.strengthen(pState, pOtherStates, pCfaEdge, pPrecision);
  }

  public void setCPTRs(
      ValueAnalysisTransferRelation pLeftTR, ValueAnalysisTransferRelation pRightTR) {
    this.leftTR = pLeftTR;
    this.rightTR = pRightTR;
  }
}
