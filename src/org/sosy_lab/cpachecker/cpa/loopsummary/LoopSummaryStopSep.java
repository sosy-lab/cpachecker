// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary;

import java.util.Collection;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.cpa.arg.ARGStopSep;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class LoopSummaryStopSep extends ARGStopSep {

  public LoopSummaryStopSep(
      StopOperator pWrappedStop,
      LogManager pLogger,
      boolean pInCPAEnabledAnalysis,
      boolean pKeepCoveredStatesInReached,
      boolean pCoverTargetStates) {
    super(
        pWrappedStop,
        pLogger,
        pInCPAEnabledAnalysis,
        pKeepCoveredStatesInReached,
        pCoverTargetStates);
    // TODO Auto-generated constructor stub
  }

  @Override
  public boolean stop(
      AbstractState pElement, Collection<AbstractState> pReached, Precision pPrecision)
      throws CPAException, InterruptedException {
    return super.stop(pElement, pReached, ((LoopSummaryPrecision) pPrecision).getPrecision());
  }
}
