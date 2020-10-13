// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class UsageMergeOperator implements MergeOperator {

  private final MergeOperator wrappedMerge;
  private final UsageCPAStatistics stats;

  public UsageMergeOperator(MergeOperator wrapped, UsageCPAStatistics pStats) {
    wrappedMerge = wrapped;
    stats = pStats;
  }

  @Override
  public AbstractState merge(AbstractState pState1, AbstractState pState2, Precision pPrecision)
      throws CPAException, InterruptedException {

    stats.mergeTimer.start();
    UsageState uState1 = (UsageState) pState1;
    UsageState uState2 = (UsageState) pState2;

    AbstractState wrappedState1 = uState1.getWrappedState();
    AbstractState wrappedState2 = uState2.getWrappedState();

    AbstractState mergedState = wrappedMerge.merge(wrappedState1, wrappedState2, pPrecision);

    UsageState result;

    if (uState1.isLessOrEqual(uState2)) {
      result = uState2.copy(mergedState);
    } else if (uState2.isLessOrEqual(uState1)) {
      result = uState1.copy(mergedState);
    } else {
      result = uState1.copy(mergedState);
      result.join(uState2);
    }

    if (mergedState.equals(wrappedState2) && result.equals(uState2)) {
      stats.mergeTimer.stop();
      return pState2;
    } else {
      stats.mergeTimer.stop();
      return result;
    }
  }
}
