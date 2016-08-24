/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.core.interfaces;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

import com.google.common.base.Function;
import com.google.common.base.Objects;

/**
 * Represents the result to a call to
 * {@link PrecisionAdjustment#prec(AbstractState, Precision, UnmodifiableReachedSet, Function, AbstractState)}.
 * Contains the (possibly changed) abstract abstractState and precision,
 * and an {@link PrecisionAdjustmentResult.Action} instance (all are not null).
 */
@Immutable
public class PrecisionAdjustmentResult {

  private final AbstractState abstractState;
  private final Precision precision;
  private final Action action;

  /**
   * The precision adjustment operator can tell the CPAAlgorithm whether
   * to continue with the analysis or whether to break immediately.
   */
  public enum Action {
    CONTINUE,
    BREAK,
    ;
  }

  private PrecisionAdjustmentResult(AbstractState pState, Precision pPrecision,
      Action pAction) {
    abstractState = checkNotNull(pState);
    precision = checkNotNull(pPrecision);
    action = checkNotNull(pAction);
  }


  public static PrecisionAdjustmentResult create(AbstractState pState,
      Precision pPrecision, Action pAction) {
    return new PrecisionAdjustmentResult(pState, pPrecision, pAction);
  }

  public AbstractState abstractState() {
    return abstractState;
  }

  public Precision precision() {
    return precision;
  }

  public Action action() {
    return action;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(abstractState, action, precision);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PrecisionAdjustmentResult)) {
      return false;
    }
    PrecisionAdjustmentResult other = (PrecisionAdjustmentResult) obj;
    return (abstractState.equals(other.abstractState))
        && (precision.equals(other.precision))
        && (action.equals(other.action))
        ;
  }

  public PrecisionAdjustmentResult withAbstractState(AbstractState newAbstractState) {
    return new PrecisionAdjustmentResult(newAbstractState, precision, action);
  }

  public PrecisionAdjustmentResult withPrecision(Precision newPrecision) {
    return new PrecisionAdjustmentResult(abstractState, newPrecision, action);
  }

  public PrecisionAdjustmentResult withAction(Action newAction) {
    return new PrecisionAdjustmentResult(abstractState, precision, newAction);
  }
}
