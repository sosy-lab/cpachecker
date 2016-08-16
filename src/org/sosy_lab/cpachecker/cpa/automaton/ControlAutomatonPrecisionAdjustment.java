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
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.base.Function;
import java.util.Optional;

@Options(prefix="cpa.automaton.prec")
public class ControlAutomatonPrecisionAdjustment implements PrecisionAdjustment {

  private final @Nullable PrecisionAdjustment wrappedPrec;
  private final AutomatonState topState;

  @Option(secure=true, name="topOnFinalSelfLoopingState",
      description="An implicit precision: consider states with a self-loop and no other outgoing edges as TOP.")
  private boolean topOnFinalSelfLoopingState = false;

  public ControlAutomatonPrecisionAdjustment(
      Configuration pConfig,
      AutomatonState pTopState,
      PrecisionAdjustment pWrappedPrecisionAdjustment)
          throws InvalidConfigurationException {

    pConfig.inject(this);

    this.topState = pTopState;
    this.wrappedPrec = pWrappedPrecisionAdjustment;
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
