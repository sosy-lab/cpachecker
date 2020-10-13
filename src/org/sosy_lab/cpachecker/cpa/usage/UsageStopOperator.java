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
  private final UsageCPAStatistics stats;

  UsageStopOperator(StopOperator pWrappedStop, UsageCPAStatistics pStats) {
    wrappedStop = pWrappedStop;
    stats = pStats;
  }

  @Override
  public boolean stop(
      AbstractState pState, Collection<AbstractState> pReached, Precision pPrecision)
      throws CPAException, InterruptedException {

    UsageState usageState = (UsageState) pState;

    stats.stopTimer.start();
    for (AbstractState reached : pReached) {
      UsageState reachedUsageState = (UsageState) reached;
      stats.usageStopTimer.start();
      boolean result = usageState.isLessOrEqual(reachedUsageState);
      stats.usageStopTimer.stop();
      if (!result) {
        continue;
      }
      stats.innerStopTimer.start();
      result =
          wrappedStop.stop(
              usageState.getWrappedState(),
              Collections.singleton(reachedUsageState.getWrappedState()),
              pPrecision);
      stats.innerStopTimer.stop();
      if (result) {
        stats.stopTimer.stop();
        return true;
      }
    }
    stats.stopTimer.stop();
    return false;
  }
}
