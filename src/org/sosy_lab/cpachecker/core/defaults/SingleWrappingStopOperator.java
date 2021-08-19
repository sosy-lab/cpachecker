// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class SingleWrappingStopOperator implements StopOperator {

  private final StopOperator delegate;

  public SingleWrappingStopOperator(StopOperator pDelegate) {
    delegate = checkNotNull(pDelegate);
  }

  @Override
  public boolean stop(
      final AbstractState pState,
      final Collection<AbstractState> pOtherStates,
      final Precision pPrecision)
      throws CPAException, InterruptedException {
    AbstractState wrappedState = ((AbstractSingleWrapperState) pState).getWrappedState();
    ImmutableSet<AbstractState> otherStates =
        Collections3.transformedImmutableSetCopy(
            pOtherStates, x -> ((AbstractSingleWrapperState) x).getWrappedState());
    Precision wrappedPrecision =
        ((SingleWrapperPrecision) pPrecision).getWrappedPrecisions().iterator().next();
    return delegate.stop(wrappedState, otherStates, wrappedPrecision);
  }
}
