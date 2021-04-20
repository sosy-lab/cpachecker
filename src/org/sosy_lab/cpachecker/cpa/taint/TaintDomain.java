// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.taint;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public enum TaintDomain implements AbstractDomain {
  INSTANCE;

  @Override
  public AbstractState join(AbstractState pState1, AbstractState pState2) throws CPAException {
    // TaintAnalysisState state1 = (TaintAnalysisState) pState1;
    TaintAnalysisState state2 = (TaintAnalysisState) pState2;
    TaintAnalysisState result = state2;
    // for (Entry<MemoryLocation, LocationSet> pointsToEntry : state1.getPointsToMap().entrySet()) {
    // result = result.addPointsToInformation(pointsToEntry.getKey(), pointsToEntry.getValue());
    // }
    // if (result.equals(state2)) {
    // return state2;
    // }
    // if (result.equals(state1)) {
    // return state1;
    // }
    return result;
  }

  @Override
  public boolean isLessOrEqual(AbstractState pState1, AbstractState pState2)
      throws CPAException, InterruptedException {
    if (pState1 == pState2) {
      return true;
    } else return false;
  }
}
