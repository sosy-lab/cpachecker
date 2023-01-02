// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.base.Function;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ControlAutomatonPrecisionAdjustment implements PrecisionAdjustment {

  private final @Nullable PrecisionAdjustment wrappedPrec;
  private final AutomatonState topState;
  private final boolean topOnFinalSelfLoopingState;

  public ControlAutomatonPrecisionAdjustment(
      AutomatonState pTopState,
      PrecisionAdjustment pWrappedPrecisionAdjustment,
      boolean pTopOnFinalSelfLoopingState) {
    topState = pTopState;
    wrappedPrec = pWrappedPrecisionAdjustment;
    topOnFinalSelfLoopingState = pTopOnFinalSelfLoopingState;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pState,
      Precision pPrecision,
      UnmodifiableReachedSet pStates,
      Function<AbstractState, AbstractState> pStateProjection,
      AbstractState pFullState)
      throws CPAException, InterruptedException {

    Optional<PrecisionAdjustmentResult> wrappedPrecResult =
        wrappedPrec.prec(pState, pPrecision, pStates, pStateProjection, pFullState);

    if (!wrappedPrecResult.isPresent()) {
      return wrappedPrecResult;
    }

    AutomatonInternalState internalState = ((AutomatonState) pState).getInternalState();

    // Handle the BREAK state
    if (internalState.getName().equals(AutomatonInternalState.BREAK.getName())) {
      return Optional.of(wrappedPrecResult.orElseThrow().withAction(Action.BREAK));
    }

    // Handle SINK state
    if (topOnFinalSelfLoopingState && internalState.isFinalSelfLoopingState()) {

      AbstractState adjustedSate = topState;
      Precision adjustedPrecision = pPrecision;
      return Optional.of(
          PrecisionAdjustmentResult.create(adjustedSate, adjustedPrecision, Action.CONTINUE));
    }

    return wrappedPrecResult;
  }
}
