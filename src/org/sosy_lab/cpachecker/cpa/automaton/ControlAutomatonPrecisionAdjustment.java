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

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.base.Function;
import com.google.common.base.Optional;

@Options(prefix="cpa.automaton.prec")
public class ControlAutomatonPrecisionAdjustment implements PrecisionAdjustment {

  private final AutomatonState bottomState;
  private final AutomatonState inactiveState;

  @Option(secure=true, name="limit.violations",
      description="Handle at most k (> 0) violation of one property.")
  private int violationsLimit = 1;


  enum TargetStateVisitBehaviour {
    SIGNAL, // Signal the target state (default)
    BOTTOM, // Change to the automata state BOTTOM (when splitting states on a violation)
    INACTIVE, // Change to the automata state INACTIVE (when NOT splitting states on a violation)
  }
  @Option(secure=true, description="Behaviour on a property that has already been fully handled.")
  private TargetStateVisitBehaviour onHandledTarget = TargetStateVisitBehaviour.SIGNAL;

  public ControlAutomatonPrecisionAdjustment(
      LogManager pLogger,
      Configuration pConfig,
      AutomatonState pTopState,
      AutomatonState pBottomState,
      AutomatonState pInactiveState)
          throws InvalidConfigurationException {

    pConfig.inject(this);

    this.bottomState = pBottomState;
    this.inactiveState = pInactiveState;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pState,
      Precision pPrecision,
      UnmodifiableReachedSet pStates,
      Function<AbstractState, AbstractState> pStateProjection,
      AbstractState pFullState)
    throws CPAException, InterruptedException {

    // Casts to the AutomataCPA-specific types
    final AutomatonPrecision pi = (AutomatonPrecision) pPrecision;
    final AutomatonInternalState internalState = ((AutomatonState) pState).getInternalState();
    final AutomatonState state = (AutomatonState) pState;

    // Specific handling of potential target states!!!
    if (state.isTarget()) {

      final AutomatonState stateOnHandledTarget;
      switch (onHandledTarget) {
        case BOTTOM: stateOnHandledTarget = bottomState; break;
        case INACTIVE: stateOnHandledTarget = inactiveState; break;
        default: stateOnHandledTarget = state;
      }

      // A property might have already been disabled!
      //    Handling of blacklisted (disabled) states:
      if (pi.getBlacklist().containsAll(state.getViolatedProperties())) {
        return Optional.of(PrecisionAdjustmentResult.create(
            stateOnHandledTarget,
            pi, Action.CONTINUE));
      }

    }

    // Handle the BREAK state
    if (internalState.getName().equals(AutomatonInternalState.BREAK.getName())) {
      return Optional.of(PrecisionAdjustmentResult.create(pState, pPrecision, Action.BREAK));
    }

    // No precision adjustment
    return Optional.of(PrecisionAdjustmentResult.create(pState, pPrecision, Action.CONTINUE));
  }

}
