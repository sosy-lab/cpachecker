// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.execution;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Applies the precision adjustment of the wrapped CPA to the wrapped state and keeps the additional
 * information of the {@link ExecutionState}.
 *
 * <p>Note that this is applied only to the states that the transfer relation actually returns, not
 * to the intermediate states of an execution (cf. {@code cpa.execution.stepsPerTransfer}). An
 * execution must not abstract from any information anyway, so the precision adjustment of the
 * wrapped CPA should not change the state.
 */
class ExecutionPrecisionAdjustment implements PrecisionAdjustment {

  private final PrecisionAdjustment wrappedPrecisionAdjustment;

  ExecutionPrecisionAdjustment(PrecisionAdjustment pWrappedPrecisionAdjustment) {
    wrappedPrecisionAdjustment = Preconditions.checkNotNull(pWrappedPrecisionAdjustment);
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pState,
      Precision pPrecision,
      UnmodifiableReachedSet pStates,
      Function<AbstractState, AbstractState> pProjection,
      AbstractState pFullState)
      throws CPAException, InterruptedException {

    ExecutionState state = (ExecutionState) pState;
    Optional<PrecisionAdjustmentResult> result =
        wrappedPrecisionAdjustment.prec(
            state.getWrappedState(),
            pPrecision,
            pStates,
            Functions.compose(s -> ((ExecutionState) s).getWrappedState(), pProjection),
            pFullState);

    if (result.isEmpty()) {
      return result;
    }
    PrecisionAdjustmentResult unwrapped = result.orElseThrow();
    if (unwrapped.abstractState() == state.getWrappedState()) {
      return Optional.of(
          new PrecisionAdjustmentResult(state, unwrapped.precision(), unwrapped.action()));
    }
    return Optional.of(
        new PrecisionAdjustmentResult(
            new ExecutionState(unwrapped.abstractState(), state.getCallStack()),
            unwrapped.precision(),
            unwrapped.action()));
  }
}
