// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.witnessjoiner;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class WitnessJoinerPrecisionAdjustment implements PrecisionAdjustment {

  private final PrecisionAdjustment wrappedPrecAdj;

  public WitnessJoinerPrecisionAdjustment(PrecisionAdjustment pPrecisionAdjustment) {
    wrappedPrecAdj = Preconditions.checkNotNull(pPrecisionAdjustment);
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pState,
      Precision pPrecision,
      UnmodifiableReachedSet pStates,
      Function<AbstractState, AbstractState> pStateProjection,
      AbstractState pFullState)
      throws CPAException, InterruptedException {

    Preconditions.checkArgument(pState instanceof WitnessJoinerState);
    AbstractState oldWrapped = ((WitnessJoinerState) pState).getWrappedState();

    Optional<PrecisionAdjustmentResult> optionalUnwrappedResult =
        wrappedPrecAdj.prec(
            oldWrapped,
            pPrecision,
            pStates,
            Functions.compose(
                (state) -> ((WitnessJoinerState) state).getWrappedState(), pStateProjection),
            pFullState);

    if (!optionalUnwrappedResult.isPresent()) {
      return Optional.empty();
    }

    PrecisionAdjustmentResult unwrappedResult = optionalUnwrappedResult.orElseThrow();
    AbstractState newWrapped = unwrappedResult.abstractState();
    Precision newPrecision = unwrappedResult.precision();
    Action action = unwrappedResult.action();

    if ((oldWrapped == newWrapped) && (pPrecision == newPrecision)) {
      // nothing has changed
      return Optional.of(new PrecisionAdjustmentResult(pState, pPrecision, action));
    }

    return Optional.of(
        new PrecisionAdjustmentResult(new WitnessJoinerState(newWrapped), newPrecision, action));
  }
}
