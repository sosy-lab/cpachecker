// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.timedautomata;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class TAUnrollingState implements AbstractState {
  private final BooleanFormula formula;
  private final int stepCount;
  private boolean reachedBound;

  public TAUnrollingState(BooleanFormula pFormula, int pStepCount, boolean pReachedBound) {
    formula = pFormula;
    stepCount = pStepCount;
    reachedBound = pReachedBound;
  }

  public BooleanFormula getFormula() {
    return formula;
  }

  public int getStepCount() {
    return stepCount;
  }

  public boolean didReachBound() {
    return reachedBound;
  }

  public void setDidReachedBoundFalse() {
    reachedBound = false;
  }
}
