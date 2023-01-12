// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.slicing;

import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Stop operator of {@link SlicingCPA}. Uses the stop operator of the CPA wrapped by the SlicingCPA,
 * with the precision of the CPA wrapped by the SlicingCPA.
 */
public class PrecisionDelegatingStop implements StopOperator {

  private final StopOperator delegateStop;

  public PrecisionDelegatingStop(final StopOperator pDelegateStop) {
    delegateStop = pDelegateStop;
  }

  @Override
  public boolean stop(
      final AbstractState pState,
      final Collection<AbstractState> pReached,
      final Precision pPrecision)
      throws CPAException, InterruptedException {
    checkState(
        pPrecision instanceof SlicingPrecision,
        "Precision not of type %s, but %s",
        SlicingPrecision.class.getSimpleName(),
        pPrecision.getClass().getSimpleName());

    Precision wrappedPrecision = ((SlicingPrecision) pPrecision).getWrappedPrec();
    return delegateStop.stop(pState, pReached, wrappedPrecision);
  }
}
