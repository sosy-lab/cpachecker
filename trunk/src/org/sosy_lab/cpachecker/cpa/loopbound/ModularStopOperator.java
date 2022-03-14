// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopbound;

import java.util.Collection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ModularStopOperator implements StopOperator {
  private final int modulus;

  public ModularStopOperator(int pModulus) {
    modulus = pModulus;
  }

  @Override
  public boolean stop(
      AbstractState pState, Collection<AbstractState> pReached, Precision pPrecision)
      throws CPAException, InterruptedException {

    int k = ((LoopBoundState) pState).getDeepestIteration();
    for (AbstractState s : pReached) {
      int k2 = ((LoopBoundState) s).getDeepestIteration();
      if (k % modulus == k2 % modulus) {
        return true;
      }
    }
    return false;
  }
}
