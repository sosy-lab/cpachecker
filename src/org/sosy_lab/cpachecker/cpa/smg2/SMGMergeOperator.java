// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import java.util.Optional;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;

public class SMGMergeOperator implements MergeOperator {

  private final SMGCPAStatistics statistics;
  private final StatTimer totalMergeTimer;

  public SMGMergeOperator(SMGCPAStatistics pStatistics) {
    statistics = pStatistics;
    totalMergeTimer = statistics.getMergeTime();
  }

  @Override
  public AbstractState merge(
      AbstractState abstrState1, AbstractState abstrState2, Precision precision)
      throws CPAException, InterruptedException {

    SMGState state1 = (SMGState) abstrState1;
    SMGState state2 = (SMGState) abstrState2;

    totalMergeTimer.start();
    statistics.incrementMergeAttempts();
    Optional<SMGState> maybeMergedState = state1.merge(state2);
    totalMergeTimer.stop();
    if (maybeMergedState.isPresent()) {
      statistics.incrementNumberOfSuccessfulMerges();
      return maybeMergedState.orElseThrow();
    }
    return state2;
  }
}
