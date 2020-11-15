// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.lock;

import java.util.Collection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class LockStopOperator implements StopOperator {

  @Override
  public boolean stop(
      AbstractState pState, Collection<AbstractState> pReached, Precision pPrecision)
      throws CPAException, InterruptedException {

    AbstractLockState lState = (AbstractLockState) pState;

    for (AbstractState reached : pReached) {
      AbstractLockState lReached = (LockState) reached;

      if (lReached.getSize() > 0 && lState.isLessOrEqual(lReached)) {
        return true;
      } else if (lReached.getSize() == 0 && lState.getSize() == 0) {
        return true;
      }
    }
    return false;
  }
}
