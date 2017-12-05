/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

    if (stateSet.size() == 0) { return pState2; }

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

  private boolean isCoverage(final PowerSetState pCovered, final PowerSetState pCovering) {
    if (prec == null) { return false; }
    Collection<AbstractState> coverSet = pCovering.getWrappedStates();
    try {
      for (AbstractState state : pCovered.getWrappedStates()) {

        if (!coverSet.contains(state) && !stop.stop(state, coverSet, prec)) {
          return false;
        }
      }
    } catch (CPAException | InterruptedException e) {
      return false;
    }
    return true;
  }

  public void setPrecision(final Precision pPrec) {
    prec = pPrec;
  }

}
