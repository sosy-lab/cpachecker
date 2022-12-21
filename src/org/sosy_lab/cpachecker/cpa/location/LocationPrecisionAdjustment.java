// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.location;

import com.google.common.base.Function;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.SummaryInformation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class LocationPrecisionAdjustment implements PrecisionAdjustment {

  private Optional<SummaryInformation> summaryInformation = Optional.empty();

  LocationPrecisionAdjustment(Optional<SummaryInformation> pSummaryInformation) {
    summaryInformation = pSummaryInformation;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pState,
      Precision pPrecision,
      UnmodifiableReachedSet pStates,
      Function<AbstractState, AbstractState> pStateProjection,
      AbstractState pFullState)
      throws CPAException, InterruptedException {

    LocationPrecision newPrecision;
    if (summaryInformation.isEmpty()) {
      newPrecision = new LocationPrecision();
    } else {
      // Add the strategies to the precision itself, in order to have the information localized and
      // not need to constantly have a global summaryInformation object which needs to be passed
      // everywhere.

      CFANode node = ((LocationState) pState).getLocationNode();
      newPrecision =
          new LocationPrecision(summaryInformation.orElseThrow().getAvailableStrategies(node));

      newPrecision.setCurrentStrategy(
          summaryInformation.orElseThrow().getBestAllowedStrategy(node, newPrecision));
    }

    return Optional.of(PrecisionAdjustmentResult.create(pState, newPrecision, Action.CONTINUE));
  }
}
