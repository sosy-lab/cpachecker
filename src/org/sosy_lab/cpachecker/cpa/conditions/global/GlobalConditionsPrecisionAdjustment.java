// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.conditions.global;

import com.google.common.base.Function;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

class GlobalConditionsPrecisionAdjustment implements PrecisionAdjustment {

  private final LogManager logger;

  private final GlobalConditionsThresholds thresholds;

  private final GlobalConditionsSimplePrecisionAdjustment delegate;

  GlobalConditionsPrecisionAdjustment(
      LogManager pLogger,
      GlobalConditionsThresholds pThresholds,
      GlobalConditionsSimplePrecisionAdjustment pDelegate) {
    logger = pLogger;
    thresholds = pThresholds;
    delegate = pDelegate;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pElement,
      Precision pPrecision,
      UnmodifiableReachedSet pElements,
      Function<AbstractState, AbstractState> projection,
      AbstractState fullState)
      throws CPAException {

    if (checkReachedSetSize(pElements)) {
      logger.log(Level.WARNING, "Reached set size threshold reached, terminating.");
      return Optional.of(PrecisionAdjustmentResult.create(pElement, pPrecision, Action.BREAK));
    }

    return Optional.of(
        PrecisionAdjustmentResult.create(
            pElement, pPrecision, delegate.prec(pElement, pPrecision)));
  }

  private boolean checkReachedSetSize(UnmodifiableReachedSet elements) {

    long threshold = thresholds.getReachedSetSizeThreshold();
    if (threshold >= 0) {
      return (elements.size() > threshold);
    }

    return false;
  }
}
