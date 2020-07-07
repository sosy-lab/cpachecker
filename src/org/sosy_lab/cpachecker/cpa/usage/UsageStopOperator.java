// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage;

import java.util.Collection;
import java.util.Collections;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class UsageStopOperator implements StopOperator {

  private final StopOperator wrappedStop;

  UsageStopOperator(StopOperator pWrappedStop) {
    wrappedStop = pWrappedStop;
  }

  @Override
  public boolean stop(
      AbstractState pState, Collection<AbstractState> pReached, Precision pPrecision)
      throws CPAException, InterruptedException {

    UsageState usageState = (UsageState) pState;
    UsagePrecision usagePrecision = (UsagePrecision) pPrecision;

    for (AbstractState reached : pReached) {
      UsageState reachedUsageState = (UsageState) reached;
      boolean result = usageState.isLessOrEqual(reachedUsageState);
      if (!result) {
        continue;
      }
      result =
          wrappedStop.stop(
              usageState.getWrappedState(),
              Collections.singleton(reachedUsageState.getWrappedState()),
              usagePrecision.getWrappedPrecision());
      if (result) {
        return true;
      }
    }
    return false;
  }
}
