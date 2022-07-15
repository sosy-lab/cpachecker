// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.powerset;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class PowerSetDomain implements AbstractDomain {

  private final StopOperator stop;
  private Precision prec;

  public PowerSetDomain(final StopOperator pStop) {
    stop = pStop;
  }

  @Override
  public AbstractState join(final AbstractState pState1, final AbstractState pState2)
      throws CPAException, InterruptedException {
    Preconditions.checkState(prec != null);

    PowerSetState state1 = (PowerSetState) pState1;
    PowerSetState state2 = (PowerSetState) pState2;

    Collection<AbstractState> coverSet = state2.getWrappedStates();

    Set<AbstractState> stateSet =
        Sets.newHashSetWithExpectedSize(coverSet.size() + state1.getWrappedStates().size());

    for (AbstractState state : state1.getWrappedStates()) {

      if (!coverSet.contains(state) && !stop.stop(state, coverSet, prec)) {
        stateSet.add(state);
      }
    }

    if (stateSet.isEmpty()) {
      return pState2;
    }

    stateSet.addAll(coverSet);

    return new PowerSetState(stateSet, state1, state2);
  }

  @Override
  public boolean isLessOrEqual(final AbstractState pState1, final AbstractState pState2)
      throws CPAException, InterruptedException {

    PowerSetState state1 = (PowerSetState) pState1;
    PowerSetState state2 = (PowerSetState) pState2;

    return state1.isMergedInto(state2) || isCoverage(state1, state2);
  }

  private boolean isCoverage(final PowerSetState pCovered, final PowerSetState pCovering)
      throws CPAException, InterruptedException {
    if (prec == null) {
      return false;
    }
    Collection<AbstractState> coverSet = pCovering.getWrappedStates();
    for (AbstractState state : pCovered.getWrappedStates()) {

      if (!coverSet.contains(state) && !stop.stop(state, coverSet, prec)) {
        return false;
      }
    }
    return true;
  }

  public void setPrecision(final Precision pPrec) {
    prec = pPrec;
  }
}
