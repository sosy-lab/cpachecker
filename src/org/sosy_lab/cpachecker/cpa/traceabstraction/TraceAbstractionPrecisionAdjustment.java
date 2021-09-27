// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.traceabstraction;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Function;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecisionAdjustment;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * PrecisionAdjustment of the {@link TraceAbstractionCPA}. It mainly delegates the precision and the
 * wrapped predicate-state to the {@link PredicatePrecisionAdjustment}.
 */
class TraceAbstractionPrecisionAdjustment implements PrecisionAdjustment {

  private PrecisionAdjustment wrappedPrecAdjustment;

  TraceAbstractionPrecisionAdjustment(PrecisionAdjustment pWrappedPrecAdjustment) {
    wrappedPrecAdjustment = pWrappedPrecAdjustment;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pState,
      Precision pPrecision,
      UnmodifiableReachedSet pStates,
      Function<AbstractState, AbstractState> pStateProjection,
      AbstractState pFullState)
      throws CPAException, InterruptedException {

    checkArgument(pState instanceof TraceAbstractionState);
    checkArgument(pPrecision instanceof PredicatePrecision);

    TraceAbstractionState taState = (TraceAbstractionState) pState;
    AbstractState wrappedPredState = taState.getWrappedState();

    Optional<PrecisionAdjustmentResult> wrappedPrecResult =
        wrappedPrecAdjustment.prec(
            wrappedPredState, pPrecision, pStates, pStateProjection, pFullState);

    if (wrappedPrecResult.isEmpty()) {
      return Optional.empty();
    }

    PrecisionAdjustmentResult precisionAdjustmentResult = wrappedPrecResult.orElseThrow();
    AbstractState newPredState = precisionAdjustmentResult.abstractState();
    Precision newPredPrecision = precisionAdjustmentResult.precision();

    PrecisionAdjustmentResult newResult =
        PrecisionAdjustmentResult.create(pState, pPrecision, precisionAdjustmentResult.action());

    if (newPredState != wrappedPredState) {
      newResult =
          newResult.withAbstractState(
              new TraceAbstractionState(newPredState, taState.getActivePredicates()));
    }
    if (newPredPrecision != pPrecision) {
      newResult = newResult.withPrecision(newPredPrecision);
    }

    return Optional.of(newResult);
  }
}
