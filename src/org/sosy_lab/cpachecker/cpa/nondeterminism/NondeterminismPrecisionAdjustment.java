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
package org.sosy_lab.cpachecker.cpa.nondeterminism;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.nondeterminism.NondeterminismState.NondeterminismAbstractionState;
import org.sosy_lab.cpachecker.cpa.nondeterminism.NondeterminismState.NondeterminismNonAbstractionState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public enum NondeterminismPrecisionAdjustment implements PrecisionAdjustment {
  INSTANCE;

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      AbstractState pState,
      Precision pPrecision,
      UnmodifiableReachedSet pStates,
      Function<AbstractState, AbstractState> pStateProjection,
      AbstractState pFullState)
      throws CPAException, InterruptedException {
    AbstractState state = prec(pState, AbstractStates.asIterable(pFullState));
    return Optional.of(PrecisionAdjustmentResult.create(state, pPrecision, Action.CONTINUE));
  }

  @Override
  public Optional<? extends AbstractState> strengthen(
      AbstractState pState, Precision pPrecision, List<AbstractState> pOtherStates) {
    return Optional.of(prec(pState, pOtherStates));
  }

  private AbstractState prec(AbstractState pState, Iterable<AbstractState> pOtherStates) {
    AbstractState result = pState;
    if (pState instanceof NondeterminismNonAbstractionState
        && FluentIterable.from(pOtherStates)
            .anyMatch(
                s ->
                    s instanceof PredicateAbstractState
                        && PredicateAbstractState.CONTAINS_ABSTRACTION_STATE.apply(s))) {
      result =
          new NondeterminismAbstractionState(
              ((NondeterminismNonAbstractionState) pState).getNondetVariables());
    }
    return result;
  }
}
