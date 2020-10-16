// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.termination;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class TerminationPrecisionAdjustment implements PrecisionAdjustment {

  private final PrecisionAdjustment precisionAdjustment;

  public TerminationPrecisionAdjustment(PrecisionAdjustment pPrecisionAdjustment) {
    precisionAdjustment = Preconditions.checkNotNull(pPrecisionAdjustment);
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pState,
      Precision pPrecision,
      UnmodifiableReachedSet pStates,
      Function<AbstractState, AbstractState> pStateProjection,
      AbstractState pFullState)
      throws CPAException, InterruptedException {
    TerminationState state = (TerminationState) pState;
    AbstractState wrappedState = state.getWrappedState();
    Optional<PrecisionAdjustmentResult> result =
        precisionAdjustment.prec(wrappedState, pPrecision, pStates, pStateProjection, pFullState);

    return result.map(r -> r.withAbstractState(state.withWrappedState(r.abstractState())));
  }
}
