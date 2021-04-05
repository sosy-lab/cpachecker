// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary;

import com.google.common.base.Function;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPrecisionAdjustment;
import org.sosy_lab.cpachecker.cpa.arg.ARGStatistics;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class LoopSummaryPrecisionAdjustment extends ARGPrecisionAdjustment {

  public LoopSummaryPrecisionAdjustment(
      PrecisionAdjustment pWrappedPrecAdjustment,
      boolean pInCPAEnabledAnalysis,
      ARGStatistics pStats) {
    super(pWrappedPrecAdjustment, pInCPAEnabledAnalysis, pStats);
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(AbstractState pElement,
      Precision oldPrecision,
      UnmodifiableReachedSet pElements,
      final Function<AbstractState, AbstractState> projection,
      AbstractState fullState)
      throws CPAException, InterruptedException {
    Optional<PrecisionAdjustmentResult> result = super.prec(
        pElement,
        ((LoopSummaryPrecision) oldPrecision).getPrecision(),
        pElements,
        projection,
        fullState);
    if (result.isEmpty()) {
      return result;
    } else {
      return Optional.of(
          PrecisionAdjustmentResult.create(
              result.get().abstractState(),
              new LoopSummaryPrecision(
                  result.get().precision(),
                  ((LoopSummaryPrecision) oldPrecision).getStrategyCounter()),
              result.get().action()));
    }
  }
}
