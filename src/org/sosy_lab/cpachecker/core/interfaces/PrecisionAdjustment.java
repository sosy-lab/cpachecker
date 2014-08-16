/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Interface for the precision adjustment operator.
 */
public interface PrecisionAdjustment {

  /**
   * The precision adjustment operator can tell the CPAAlgorithm whether
   * to continue with the analysis or whether to break immediately.
   */
  public static enum Action {
    CONTINUE,
    BREAK,
    ;
  }

  /**
   * Represents the result to a call to {@link PrecisionAdjustment#prec(AbstractState, Precision, UnmodifiableReachedSet)}.
   * Contains the (possibly changed) abstract abstractState and precision,
   * and an {@link Action} instance (all are not null).
   */
  @Immutable
  public static final class PrecisionAdjustmentResult {

    private final AbstractState abstractState;
    private final Precision precision;
    private final Action action;

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
      final int prime = 31;
      int result = 1;
      result = prime * result + abstractState.hashCode();
      result = prime * result + action.hashCode();
      result = prime * result + precision.hashCode();
      return result;
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

    @CheckReturnValue
    public PrecisionAdjustmentResult withAbstractState(AbstractState newAbstractState) {
      return new PrecisionAdjustmentResult(newAbstractState, precision, action);
    }

    @CheckReturnValue
    public PrecisionAdjustmentResult withPrecision(Precision newPrecision) {
      return new PrecisionAdjustmentResult(abstractState, newPrecision, action);
    }

    @CheckReturnValue
    public PrecisionAdjustmentResult withAction(Action newAction) {
      return new PrecisionAdjustmentResult(abstractState, precision, newAction);
    }
  }

  /**
   * This method may adjust the current abstractState and precision using information
   * from the current set of reached states.
   *
   * If this method doesn't change anything, it is strongly recommended to return
   * the identical objects for abstractState and precision. This makes it easier for
   * wrapper CPAs.
   *
   * @param abstractState The current abstract abstractState.
   * @param precision The current precision.
   * @param states The current reached set.
   * @return The new abstractState, new precision and the action flag.
   */
  public PrecisionAdjustmentResult prec(
      AbstractState state, Precision precision, UnmodifiableReachedSet states)
      throws CPAException, InterruptedException;
}
