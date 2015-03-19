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

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.google.common.base.Objects;

/**
 * Represents the result to a call to {@link org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment#prec(org.sosy_lab.cpachecker.core.interfaces.AbstractState, org.sosy_lab.cpachecker.core.interfaces.Precision, org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet, org.sosy_lab.cpachecker.core.interfaces.AbstractState)}.
 * Contains the (possibly changed) abstract abstractState and precision,
 * and an {@link org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action} instance (all are not null).
 *
 * NOTE: implemented as an abstract state because Java7 does not allow static
 * methods on an interface.
 */
public abstract class PrecisionAdjustmentResult {
  private PrecisionAdjustmentResult(){}

  /**
   * The precision adjustment operator can tell the CPAAlgorithm whether
   * to continue with the analysis or whether to break immediately.
   */
  public static enum Action {
    CONTINUE,
    BREAK,
    ;
  }

  public abstract AbstractState abstractState();

  public abstract Precision precision();

  public abstract Action action();

  public abstract boolean isBottom();

  @CheckReturnValue
  public abstract PrecisionAdjustmentResult withAbstractState(AbstractState newAbstractState);

  @CheckReturnValue
  public abstract PrecisionAdjustmentResult withPrecision(Precision newPrecision);

  @CheckReturnValue
  public abstract PrecisionAdjustmentResult withAction(Action newAction);

  public static PrecisionAdjustmentResult create(AbstractState pState,
      Precision pPrecision, Action pAction) {
    return new PrecisionAdjustmentResultImpl(pState, pPrecision, pAction);
  }

  public static PrecisionAdjustmentResult bottom() {
    return BottomPrecisionAdjustmentResult.getInstance();
  }


  @Immutable
  private static final class PrecisionAdjustmentResultImpl extends
      PrecisionAdjustmentResult {

    private final AbstractState abstractState;
    private final Precision precision;
    private final Action action;

    private PrecisionAdjustmentResultImpl(AbstractState pState, Precision pPrecision,
        Action pAction) {
      abstractState = checkNotNull(pState);
      precision = checkNotNull(pPrecision);
      action = checkNotNull(pAction);
    }


    @Override
    public AbstractState abstractState() {
      return abstractState;
    }

    @Override
    public Precision precision() {
      return precision;
    }

    @Override
    public Action action() {
      return action;
    }

    public boolean isBottom() {
      return false;
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
      if (!(obj instanceof PrecisionAdjustmentResultImpl)) {
        return false;
      }
      PrecisionAdjustmentResultImpl other = (PrecisionAdjustmentResultImpl) obj;
      return (abstractState.equals(other.abstractState))
          && (precision.equals(other.precision))
          && (action.equals(other.action))
          ;
    }

    @Override
    public PrecisionAdjustmentResult withAbstractState(AbstractState newAbstractState) {
      return new PrecisionAdjustmentResultImpl(newAbstractState, precision, action);
    }

    @Override
    public PrecisionAdjustmentResult withPrecision(Precision newPrecision) {
      return new PrecisionAdjustmentResultImpl(abstractState, newPrecision, action);
    }

    @Override
    public PrecisionAdjustmentResult withAction(Action newAction) {
      return new PrecisionAdjustmentResultImpl(abstractState, precision, newAction);
    }
  }

  @Immutable
  private static class BottomPrecisionAdjustmentResult extends PrecisionAdjustmentResult {
    private final static BottomPrecisionAdjustmentResult INSTANCE =
        new BottomPrecisionAdjustmentResult();

    private BottomPrecisionAdjustmentResult() {}

    public static BottomPrecisionAdjustmentResult getInstance() {
      return INSTANCE;
    }

    @Override
    public AbstractState abstractState() {
      throw new UnsupportedOperationException(
          "Bottom state has no associated state");
    }

    @Override
    public Precision precision() {
      throw new UnsupportedOperationException(
          "Bottom state has no associated precision");
    }

    @Override
    public Action action() {
      throw new UnsupportedOperationException(
          "Bottom state has no associated action");
    }

    @Override
    public PrecisionAdjustmentResult withAbstractState(
        AbstractState newAbstractState) {
      throw new UnsupportedOperationException(
          "Bottom state has no associated state");
    }

    @Override
    public PrecisionAdjustmentResult withPrecision(
        Precision newPrecision) {
      throw new UnsupportedOperationException(
          "Bottom state has no associated precision");
    }

    @Override
    public PrecisionAdjustmentResult withAction(
        Action newAction) {
      throw new UnsupportedOperationException(
          "Bottom state has no associated action");
    }

    @Override
    public boolean isBottom() {
      return true;
    }
  }
}
