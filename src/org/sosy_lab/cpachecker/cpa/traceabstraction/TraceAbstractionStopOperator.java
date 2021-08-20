// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.traceabstraction;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ForcedCoveringStopOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

class TraceAbstractionStopOperator implements StopOperator, ForcedCoveringStopOperator {

  private final StopOperator delegateStopOperator;

  TraceAbstractionStopOperator(StopOperator pDelegate) {
    delegateStopOperator = checkNotNull(pDelegate);
  }

  @Override
  public boolean stop(
      final AbstractState pState,
      final Collection<AbstractState> pOtherStates,
      final Precision pPrecision)
      throws CPAException, InterruptedException {
    AbstractState wrappedState = ((TraceAbstractionState) pState).getWrappedState();
    ImmutableSet<AbstractState> otherStates =
        Collections3.transformedImmutableSetCopy(
            pOtherStates, x -> ((TraceAbstractionState) x).getWrappedState());
    return delegateStopOperator.stop(wrappedState, otherStates, pPrecision);
  }

  @Override
  public boolean isForcedCoveringPossible(
      AbstractState pState, AbstractState pReachedState, Precision pPrecision)
      throws CPAException, InterruptedException {
    checkArgument(delegateStopOperator instanceof ForcedCoveringStopOperator);
    return ((ForcedCoveringStopOperator) delegateStopOperator)
        .isForcedCoveringPossible(pState, pReachedState, pPrecision);
  }
}
