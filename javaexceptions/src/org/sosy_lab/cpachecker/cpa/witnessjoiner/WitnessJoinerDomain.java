// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.witnessjoiner;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class WitnessJoinerDomain implements AbstractDomain {

  private final AbstractDomain wrappedDomain;

  public WitnessJoinerDomain(final AbstractDomain pWrappedDomain) {
    wrappedDomain = Preconditions.checkNotNull(pWrappedDomain);
  }

  @Override
  public AbstractState join(AbstractState pState1, AbstractState pState2)
      throws CPAException, InterruptedException {
    AbstractState wrappedState1 = ((WitnessJoinerState) pState1).getWrappedState();
    AbstractState wrappedState2 = ((WitnessJoinerState) pState2).getWrappedState();

    AbstractState wrappedJoin = wrappedDomain.join(wrappedState1, wrappedState2);

    if (wrappedJoin == wrappedState2) {
      return pState2;
    }
    if (wrappedJoin == wrappedState1) {
      return pState1;
    }

    return new WitnessJoinerState(wrappedJoin);
  }

  @Override
  public boolean isLessOrEqual(AbstractState pState1, AbstractState pState2)
      throws CPAException, InterruptedException {
    return pState1 instanceof WitnessJoinerState
        && pState2 instanceof WitnessJoinerState
        && wrappedDomain.isLessOrEqual(
            ((WitnessJoinerState) pState1).getWrappedState(),
            ((WitnessJoinerState) pState2).getWrappedState());
  }
}
