// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.nondeterminism;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Function;
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
      AbstractState pState, Precision pPrecision, Iterable<AbstractState> pOtherStates) {
    return Optional.of(prec(pState, pOtherStates));
  }

  private AbstractState prec(AbstractState pState, Iterable<AbstractState> pOtherStates) {
    AbstractState result = pState;
    if (pState instanceof NondeterminismNonAbstractionState
        && from(pOtherStates)
            .filter(PredicateAbstractState.class)
            .anyMatch(PredicateAbstractState::isAbstractionState)) {
      result =
          new NondeterminismAbstractionState(
              ((NondeterminismNonAbstractionState) pState).getNondetVariables());
    }
    return result;
  }
}
