// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.powerset;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Sets;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class PowerSetPrecisionAdjustment implements PrecisionAdjustment {

  private final PrecisionAdjustment wrappedPrec;

  public PowerSetPrecisionAdjustment(PrecisionAdjustment pPrecisionAdjustment) {
    wrappedPrec = pPrecisionAdjustment;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pState,
      Precision pPrecision,
      UnmodifiableReachedSet pStates,
      Function<AbstractState, AbstractState> pStateProjection,
      AbstractState pFullState)
      throws CPAException, InterruptedException {

    PowerSetState states = (PowerSetState) pState;
    Action action = Action.CONTINUE;

    Set<AbstractState> newStates =
        Sets.newHashSetWithExpectedSize(states.getWrappedStates().size());

    boolean present = false, changed = false;
    Optional<PrecisionAdjustmentResult> wrappedRes;

    for (AbstractState state : states.getWrappedStates()) {
      wrappedRes =
          wrappedPrec.prec(
              state,
              pPrecision,
              pStates,
              Functions.compose(abstractState -> state, pStateProjection),
              pFullState);
      if (wrappedRes.isPresent()) {
        present = true;

        if (wrappedRes.orElseThrow().abstractState() != pState) {
          changed = true;
          newStates.add(wrappedRes.orElseThrow().abstractState());
        } else {
          newStates.add(state);
        }

        if (wrappedRes.orElseThrow().action() == Action.BREAK) {
          action = Action.BREAK;
        }
      }
    }

    if (!present) {
      return Optional.empty();
    }

    PowerSetState newState = changed ? new PowerSetState(newStates) : states;
    return Optional.of(PrecisionAdjustmentResult.create(newState, pPrecision, action));
  }
}
