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

import javax.annotation.Nullable;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Multiset;

@Options(prefix="cpa.automaton.prec")
public class ControlAutomatonPrecisionAdjustment implements PrecisionAdjustment {

  private final @Nullable PrecisionAdjustment wrappedPrec;
  private final AutomatonState topState;
  private final AutomatonState bottomState;

  @Option(secure=true, description="An implicit precision: consider states with a self-loop and no other outgoing edges as TOP.")
  private boolean topOnFinalSelfLoopingState = false;

  @Option(secure=true, description="Handle at most k (> 0) violation of one property.")
  private int targetHandledAfter = 1;

  enum TargetStateVisitBehaviour {
    SIGNAL, // Signal the target state (default)
    BOTTOM, // Change to the automata state BOTTOM (when splitting states on a violation)
  }
  @Option(secure=true, description="Behaviour on a property that has already been fully handled.")
  private TargetStateVisitBehaviour onHandledTarget = TargetStateVisitBehaviour.SIGNAL;

  public ControlAutomatonPrecisionAdjustment(
      Configuration pConfig,
      AutomatonState pTopState,
      AutomatonState pBottomState,
      PrecisionAdjustment pWrappedPrecisionAdjustment)
          throws InvalidConfigurationException {

    pConfig.inject(this);

    this.topState = pTopState;
    this.bottomState = pBottomState;
    this.wrappedPrec = pWrappedPrecisionAdjustment;
  }

  private int timesEqualTargetInReached(UnmodifiableReachedSet pStates, AbstractState pFullState) {
    assert pFullState instanceof Targetable;
    assert ((Targetable) pFullState).isTarget();

    Collection<Property> props = AbstractStates.extractViolatedProperties(pFullState);

    ARGCPA argCpa = CPAs.retrieveCPA(GlobalInfo.getInstance().getCPA().get(), ARGCPA.class);
    Multiset<Property> reachedViolations = argCpa.getCexSummary().getFeasiblePropertyViolations();

    int result = Integer.MAX_VALUE;
    for (Property p: props) {
      result = Math.min(result, reachedViolations.count(p));
    }

    return result;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pState,
      Precision pPrecision,
      UnmodifiableReachedSet pStates,
      Function<AbstractState, AbstractState> pStateProjection,
      AbstractState pFullState)
    throws CPAException, InterruptedException {

    Optional<PrecisionAdjustmentResult> wrappedPrecResult = wrappedPrec.prec(pState,
        pPrecision, pStates, pStateProjection, pFullState);

    if (!wrappedPrecResult.isPresent()) {
      return wrappedPrecResult;
    }

    AutomatonInternalState internalState = ((AutomatonState) pState).getInternalState();

    // Handle a target state
    //    We might disable certain target states
    //      (they should not be considered as target states)
    if (onHandledTarget == TargetStateVisitBehaviour.BOTTOM) {
      assert targetHandledAfter > 0;

      if (((Targetable) pFullState).isTarget()) {

        int timesHandled = timesEqualTargetInReached(pStates, pFullState);
        if (timesHandled >= targetHandledAfter) { // the new state is the ith+1

          Precision adjustedPrecision = pPrecision; // TODO!

          return Optional.of(PrecisionAdjustmentResult.create(
              bottomState,
              adjustedPrecision, Action.CONTINUE));
        }
      }
    }

    // Handle the BREAK state
    if (internalState.getName().equals(AutomatonInternalState.BREAK.getName())) {
      return Optional.of(wrappedPrecResult.get().withAction(Action.BREAK));
    }

    // Handle SINK state
    if (topOnFinalSelfLoopingState
        && internalState.isFinalSelfLoopingState()) {

      AbstractState adjustedSate = topState;
      Precision adjustedPrecision = pPrecision;
      return Optional.of(PrecisionAdjustmentResult.create(
          adjustedSate,
          adjustedPrecision, Action.CONTINUE));
    }

    return wrappedPrecResult;
  }

}
