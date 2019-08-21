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

import java.util.Collection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Stop operator of {@link SlicingCPA}.
 * Uses the stop operator of the CPA wrapped by the SlicingCPA,
 * with the precision of the CPA wrapped by the SlicingCPA.
 */
public class PrecisionDelegatingStop
    implements StopOperator {

  private final StopOperator delegateStop;

  public PrecisionDelegatingStop(final StopOperator pDelegateStop) {
    delegateStop = pDelegateStop;
  }

  @Override
  public boolean stop(
      final AbstractState pState,
      final Collection<AbstractState> pReached,
      final Precision pPrecision
  ) throws CPAException, InterruptedException {
    checkState(pPrecision instanceof SlicingPrecision, "Precision not of type " +
        SlicingPrecision.class.getSimpleName() + ", but " + pPrecision.getClass().getSimpleName());

    Precision wrappedPrecision = ((SlicingPrecision) pPrecision).getWrappedPrec();
    return delegateStop.stop(pState, pReached, wrappedPrecision);
  }
}
