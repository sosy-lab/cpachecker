/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.conditions.global;

import com.google.common.base.Function;
import com.google.common.base.Optional;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.logging.Level;


class GlobalConditionsPrecisionAdjustment implements PrecisionAdjustment {

  private final LogManager logger;

  private final GlobalConditionsThresholds thresholds;

  private final GlobalConditionsSimplePrecisionAdjustment delegate;

  GlobalConditionsPrecisionAdjustment(LogManager pLogger, GlobalConditionsThresholds pThresholds,
      GlobalConditionsSimplePrecisionAdjustment pDelegate) {
    logger = pLogger;
    thresholds = pThresholds;
    delegate = pDelegate;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(AbstractState pElement, Precision pPrecision,
      UnmodifiableReachedSet pElements,
      Function<AbstractState, AbstractState> projection,
      AbstractState fullState) throws CPAException {

    if (checkReachedSetSize(pElements)) {
      logger.log(Level.WARNING, "Reached set size threshold reached, terminating.");
      return Optional.of(PrecisionAdjustmentResult.create(pElement, pPrecision, Action.BREAK));
    }

    return Optional.of(PrecisionAdjustmentResult.create(pElement, pPrecision, delegate.prec(pElement, pPrecision)));
  }

  @Override
  public Optional<PrecisionAdjustmentResult> postAdjustmentStrengthen(
      AbstractState result,
      Precision precision,
      Iterable<AbstractState> otherStates,
      Iterable<Precision> otherPrecisions,
      UnmodifiableReachedSet states,
      Function<AbstractState, AbstractState> stateProjection,
      AbstractState resultFullState) throws CPAException, InterruptedException {
    return Optional.of(PrecisionAdjustmentResult.create(result, precision, Action.CONTINUE));
  }


  private boolean checkReachedSetSize(UnmodifiableReachedSet elements) {

    long threshold = thresholds.getReachedSetSizeThreshold();
    if (threshold >= 0) {
      return (elements.size() > threshold);
    }

    return false;
  }
}
