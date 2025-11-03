// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interval;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class IntervalFunArrayDomain implements AbstractDomain {

  private static final AbstractDomain INSTANCE = new IntervalFunArrayDomain();

  public static AbstractDomain getInstance() {
    return INSTANCE;
  }

  @Override
  public AbstractState join(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {

    if (state1 instanceof IntervalAnalysisState intervalState1
        && state2 instanceof IntervalAnalysisState intervalState2) {
      return intervalState1.join(intervalState2);
    }

    throw new CPAException("Cannot join non interval states");
  }

  @Override
  public boolean isLessOrEqual(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {

    if (state1 instanceof IntervalAnalysisState intervalState1
        && state2 instanceof IntervalAnalysisState intervalState2) {

      var intervalsSubset =
          intervalState1.intervals().keySet().stream()
              .allMatch(e -> intervalState2.intervals().containsKey(e));

      var arraysSubset =
          intervalState1.arrays().keySet().stream()
              .allMatch(e -> intervalState2.arrays().containsKey(e));

      if (!intervalsSubset || !arraysSubset) {
        return false;
      }

      var allIntervalsLessEqual =
          intervalState1.intervals().entrySet().stream()
              .allMatch(
                  e -> intervalState2.intervals().get(e.getKey()).isGreaterThan(e.getValue()));

      var allArraysLessEqual =
          intervalState1.arrays().entrySet().stream()
              .allMatch(e -> e.getValue().isLessOrEqual(intervalState2.arrays().get(e.getKey())));

      return allIntervalsLessEqual && allArraysLessEqual;
    }
    return false;
  }
}
