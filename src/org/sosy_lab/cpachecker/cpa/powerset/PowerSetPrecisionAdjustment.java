/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
  public Optional<PrecisionAdjustmentResult> prec(AbstractState pState, Precision pPrecision,
      UnmodifiableReachedSet pStates, Function<AbstractState, AbstractState> pStateProjection,
      AbstractState pFullState) throws CPAException, InterruptedException {

    PowerSetState states = (PowerSetState) pState;
    Action action = Action.CONTINUE;

    Set<AbstractState> newStates =
        Sets.newHashSetWithExpectedSize(states.getWrappedStates().size());

    boolean present = false, changed = false;
    Optional<PrecisionAdjustmentResult> wrappedRes;

    for (AbstractState state : states.getWrappedStates()) {
      wrappedRes = wrappedPrec.prec(state, pPrecision, pStates,
          Functions.compose(abstractState -> state, pStateProjection), pFullState);
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

    if (!present) { return Optional.empty(); }

    PowerSetState newState = changed ? new PowerSetState(newStates) : states;
    return Optional.of(PrecisionAdjustmentResult.create(newState, pPrecision, action));
  }

}
