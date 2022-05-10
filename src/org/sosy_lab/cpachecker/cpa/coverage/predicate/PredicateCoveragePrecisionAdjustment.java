// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.coverage.predicate;

import com.google.common.base.Function;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class PredicateCoveragePrecisionAdjustment implements PrecisionAdjustment {

  private final PrecisionAdjustment precisionAdjustment;

  public PredicateCoveragePrecisionAdjustment(PrecisionAdjustment pPrecisionAdjustment) {
    precisionAdjustment = pPrecisionAdjustment;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState state,
      Precision precision,
      UnmodifiableReachedSet states,
      Function<AbstractState, AbstractState> stateProjection,
      AbstractState fullState)
      throws CPAException, InterruptedException {
    return precisionAdjustment.prec(state, precision, states, stateProjection, fullState);
  }
}
