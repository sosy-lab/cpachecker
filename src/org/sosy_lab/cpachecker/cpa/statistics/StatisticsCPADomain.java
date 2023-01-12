// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.statistics;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * The Domain of the StatisticsCPA; delegates its work to the states, which in turn delegate to
 * StatisticsStateFactory.
 */
public class StatisticsCPADomain implements AbstractDomain {

  @Override
  public AbstractState join(AbstractState pState1, AbstractState pState2) throws CPAException {
    StatisticsState state1 = (StatisticsState) pState1;
    StatisticsState state2 = (StatisticsState) pState2;
    assert state1.getLocationNode().equals(state2.getLocationNode())
        : "can only merge on the same location";
    return state1.mergeState(state2);
  }

  @Override
  public boolean isLessOrEqual(AbstractState pState1, AbstractState pState2)
      throws CPAException, InterruptedException {
    StatisticsState state1 = (StatisticsState) pState1;
    StatisticsState state2 = (StatisticsState) pState2;

    return state2.containsPrevious(state1);
  }
}
