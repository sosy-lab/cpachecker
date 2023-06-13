// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Base implementation for precision adjustment implementations wrap other precision adjustment
 * operators.
 */
public abstract class WrappingPrecisionAdjustment implements PrecisionAdjustment {

  private final PrecisionAdjustment wrappedPrecOp;

  protected WrappingPrecisionAdjustment(PrecisionAdjustment pWrappedPrecOp) {
    wrappedPrecOp = Preconditions.checkNotNull(pWrappedPrecOp);
  }

  protected abstract Optional<PrecisionAdjustmentResult> wrappingPrec(
      AbstractState pState,
      Precision pPrecision,
      UnmodifiableReachedSet pStates,
      Function<AbstractState, AbstractState> pProjection,
      AbstractState pFullState)
      throws CPAException;

  @Override
  public final Optional<PrecisionAdjustmentResult> prec(
      AbstractState pState,
      Precision pPrecision,
      UnmodifiableReachedSet pStates,
      Function<AbstractState, AbstractState> pProjection,
      AbstractState pFullState)
      throws CPAException, InterruptedException {

    Optional<PrecisionAdjustmentResult> result =
        wrappedPrecOp.prec(pState, pPrecision, pStates, pProjection, pFullState);

    if (result.isPresent()) {
      if (result.orElseThrow().action() == Action.BREAK) {
        return result;
      } else {
        return wrappingPrec(
            result.orElseThrow().abstractState(), pPrecision, pStates, pProjection, pFullState);
      }
    } else {
      return wrappingPrec(pState, pPrecision, pStates, pProjection, pFullState);
    }
  }

  public abstract Action prec(AbstractState pState, Precision pPrecision) throws CPAException;
}
