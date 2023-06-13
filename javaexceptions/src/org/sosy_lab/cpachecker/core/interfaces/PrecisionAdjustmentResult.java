// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

/**
 * Represents the result to a call to {@link PrecisionAdjustment#prec(AbstractState, Precision,
 * UnmodifiableReachedSet, Function, AbstractState)}. Contains the (possibly changed) abstract
 * abstractState and precision, and an {@link PrecisionAdjustmentResult.Action} instance (all are
 * not null).
 */
@javax.annotation.concurrent.Immutable // cannot prove deep immutability
public record PrecisionAdjustmentResult(
    AbstractState abstractState, Precision precision, Action action) {

  /**
   * The precision adjustment operator can tell the CPAAlgorithm whether to continue with the
   * analysis or whether to break immediately.
   */
  public enum Action {
    CONTINUE,
    BREAK,
  }

  public PrecisionAdjustmentResult {
    checkNotNull(abstractState);
    checkNotNull(precision);
    checkNotNull(action);
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
