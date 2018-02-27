/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.slicing;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Function;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Precision adjustment operator of {@link SlicingCPA}.
 * <p>
 * Delegates precision adjustment to the CPA wrapped
 * by the slicing CPA, using its original precision
 * that is wrapped by the {@link SlicingPrecision}.
 * </p>
 */
public class PrecisionDelegatingPrecisionAdjustment
    implements PrecisionAdjustment {

  private final PrecisionAdjustment delegate;

  public PrecisionDelegatingPrecisionAdjustment(final PrecisionAdjustment pDelegateAdjustment) {
    delegate = pDelegateAdjustment;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      final AbstractState pState,
      final Precision pPrecision,
      final UnmodifiableReachedSet pStates,
      final Function<AbstractState, AbstractState> pStateProjection,
      final AbstractState pFullState
  ) throws CPAException, InterruptedException {
    checkState(pPrecision instanceof SlicingPrecision, "Precision not of type " +
        SlicingPrecision.class.getSimpleName() + ", but " + pPrecision.getClass().getSimpleName());

    AbstractState wrappedState = pState;
    Precision wrappedPrecision = ((SlicingPrecision) pPrecision).getWrappedPrec();
    Optional<PrecisionAdjustmentResult> delegateResult =
        delegate.prec(wrappedState, wrappedPrecision, pStates, pStateProjection, pFullState);

    if (delegateResult.isPresent()) {
      PrecisionAdjustmentResult adjustmentResult = delegateResult.get();
      AbstractState state = adjustmentResult.abstractState();
      Precision precision = adjustmentResult.precision();

      PrecisionAdjustmentResult finalResult;
      if (state != wrappedState || precision != wrappedPrecision) {
        // something changed
        finalResult =
            PrecisionAdjustmentResult.create(
                state,
                ((SlicingPrecision) pPrecision).getNew(precision),
                adjustmentResult.action());

      } else { // nothing changed
        finalResult =
            PrecisionAdjustmentResult.create(pState, pPrecision, adjustmentResult.action());
      }

      return Optional.of(finalResult);

    } else {
      return delegateResult;
    }
  }
}
