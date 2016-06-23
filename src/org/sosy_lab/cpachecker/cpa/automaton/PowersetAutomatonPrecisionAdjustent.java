/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.automaton;

import java.util.Collection;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;


public class PowersetAutomatonPrecisionAdjustent implements PrecisionAdjustment {

  private final PrecisionAdjustment componentPrec;

  public PowersetAutomatonPrecisionAdjustent(PrecisionAdjustment pPrecisionAdjustment) {
    componentPrec = pPrecisionAdjustment;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(AbstractState pState, Precision pPrecision,
      UnmodifiableReachedSet pStates, Function<AbstractState, AbstractState> pStateProjection, AbstractState pFullState)
          throws CPAException, InterruptedException {

    Preconditions.checkArgument(pState instanceof PowersetAutomatonState);
    PowersetAutomatonState state = (PowersetAutomatonState) pState;

    PrecisionAdjustmentResult.Action action = Action.CONTINUE;
    Collection<AutomatonState> adjustedComponents = Lists.newArrayList();

    boolean adjusted = false;

    for (AutomatonState e: state) {
      Optional<PrecisionAdjustmentResult> ePrime = componentPrec.prec(e, pPrecision, pStates, pStateProjection, pFullState);

      if (ePrime.isPresent()) {
        adjusted = true;

        adjustedComponents.add((AutomatonState) ePrime.get().abstractState());

        switch(ePrime.get().action()) {
        case BREAK: action = Action.BREAK; break;
        case CONTINUE: break;
        default: throw new CPAException("Unsupported precision adjustment ACTION!");
        }

      } else {
        adjustedComponents.add(e);
      }
    }

    if (adjusted) {
      PowersetAutomatonState statePrime = new PowersetAutomatonState(adjustedComponents);
      return Optional.of(PrecisionAdjustmentResult.create(statePrime, pPrecision, action));
    }

    return Optional.absent();
  }

}
