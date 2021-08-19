// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.WrapperPrecision;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Precision adjustment operator for a wrapped precision.
 *
 * <p>Forwards the precision adjustment to another CPA. Based on the outcome, the objects within the
 * resulting {@link PrecisionAdjustmentResult} can be wrapped again by the original CPA, for example
 * with either an {@link AbstractWrapperState} or a {@link WrapperPrecision}.
 */
public abstract class SingleWrappingPrecisionAdjustment implements PrecisionAdjustment {

  private final PrecisionAdjustment wrappedPrecOp;

  public SingleWrappingPrecisionAdjustment(PrecisionAdjustment pWrappedPrecOp) {
    this.wrappedPrecOp = checkNotNull(pWrappedPrecOp);
  }

  @Override
  public final Optional<PrecisionAdjustmentResult> prec(
      AbstractState pState,
      Precision pPrecision,
      UnmodifiableReachedSet pReachedSet,
      Function<AbstractState, AbstractState> pProjection,
      AbstractState pFullState)
      throws CPAException, InterruptedException {

    AbstractState wrappedState = ((AbstractSingleWrapperState) pState).getWrappedState();
    Precision wrappedPrecision = ((SingleWrapperPrecision) pPrecision).getWrappedPrecision();
    Optional<PrecisionAdjustmentResult> wrappedPrecResult =
        wrappedPrecOp.prec(wrappedState, wrappedPrecision, pReachedSet, pProjection, pFullState);

    if (wrappedPrecResult.isEmpty()) {
      return wrappedPrecResult;
    }

    PrecisionAdjustmentResult precisionAdjustmentResult = wrappedPrecResult.orElseThrow();
    AbstractState newState = precisionAdjustmentResult.abstractState();
    Precision newPrecision = precisionAdjustmentResult.precision();

    if (newState == wrappedState && newPrecision == wrappedPrecision) {
      // nothing changed
      return Optional.of(
          PrecisionAdjustmentResult.create(pState, pPrecision, precisionAdjustmentResult.action()));
    }

    PrecisionAdjustmentResult newResult =
        PrecisionAdjustmentResult.create(pState, pPrecision, precisionAdjustmentResult.action());

    if (newState != wrappedState) {
      newResult = newResult.withAbstractState(createWrapperState(pState, newState));
    }
    if (newPrecision != wrappedPrecision) {
      newResult = newResult.withPrecision(createWrapperPrecision(pPrecision, newPrecision));
    }

    return Optional.of(newResult);
  }

  protected abstract Precision createWrapperPrecision(
      Precision pPrecision, Precision pNewDelegatePrecision);

  protected abstract AbstractState createWrapperState(
      AbstractState pState, AbstractState pNewDelegateState);
}
