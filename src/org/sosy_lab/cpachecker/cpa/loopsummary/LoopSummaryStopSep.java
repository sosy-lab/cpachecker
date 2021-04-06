// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary;

import java.util.Collection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ForcedCoveringStopOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class LoopSummaryStopSep implements StopOperator, ForcedCoveringStopOperator {

  private StopOperator stopOperator;

  public LoopSummaryStopSep(StopOperator pWrappedStop) {
    stopOperator = pWrappedStop;
  }

  @Override
  public boolean stop(
      AbstractState pElement, Collection<AbstractState> pReached, Precision pPrecision)
      throws CPAException, InterruptedException {
    return stopOperator.stop(pElement, pReached, ((LoopSummaryPrecision) pPrecision).getPrecision());
  }

  @Override
  public boolean isForcedCoveringPossible(
      AbstractState pState, AbstractState pReachedState, Precision pPrecision)
      throws CPAException, InterruptedException {
    return ((ForcedCoveringStopOperator) stopOperator)
        .isForcedCoveringPossible(
            pState, pReachedState, ((LoopSummaryPrecision) pPrecision).getPrecision());
  }
}
