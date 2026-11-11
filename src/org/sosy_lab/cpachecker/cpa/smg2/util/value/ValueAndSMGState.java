// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util.value;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGException;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;

public class ValueAndSMGState {

  private final @Nullable Value value;
  private final SMGState state;

  private ValueAndSMGState(@Nullable Value pValue, SMGState pState) {
    value = pValue;
    state = pState;
  }

  public static ValueAndSMGState of(Value pValue, SMGState pState) {
    checkNotNull(pValue);
    checkNotNull(pState);
    return new ValueAndSMGState(pValue, pState);
  }

  /**
   * Creates a {@link ValueAndSMGState} that throws a {@link SMGException} should {@link
   * #getValue()} be used on it. Must only be used if there is no {@link Value} by design, and it is
   * guaranteed that {@link #getValue()} is never used, e.g. for returns from function calls
   * returning void.
   */
  public static ValueAndSMGState ofStateWithoutValue(SMGState pState) {
    checkNotNull(pState);
    return new ValueAndSMGState(null, pState);
  }

  /**
   * Returns the entered state with a newly created unknown value. Only use this for internal
   * unknown values that don't influence the analysis or logging that has already been handled.
   */
  public static ValueAndSMGState ofUnknownValue(SMGState pState) {
    Preconditions.checkNotNull(pState);
    return new ValueAndSMGState(UnknownValue.getInstance(), pState);
  }

  /**
   * Returns the entered state with a newly created unknown value, logging the returned unknown with
   * the edge given.
   */
  public static ValueAndSMGState ofUnknownValue(SMGState pState, CFAEdge edge) {
    Preconditions.checkNotNull(pState);
    pState.logUnknownValue(edge);
    return new ValueAndSMGState(UnknownValue.getInstance(), pState);
  }

  /**
   * Returns the entered state with a newly created unknown value, logging the returned unknown with
   * the edge and message given.
   */
  public static ValueAndSMGState ofUnknownValue(SMGState pState, String msg, CFAEdge edge) {
    Preconditions.checkNotNull(pState);
    pState.logUnknownValue(msg, edge);
    return new ValueAndSMGState(UnknownValue.getInstance(), pState);
  }

  /**
   * Returns the entered state with a newly created unknown value, logging the returned unknown with
   * the message given.
   */
  public static ValueAndSMGState ofUnknownValue(SMGState pState, String msg) {
    Preconditions.checkNotNull(pState);
    pState.logUnknownValue(msg);
    return new ValueAndSMGState(UnknownValue.getInstance(), pState);
  }

  /**
   * Returns the {@link Value} of this {@link ValueAndSMGState}.
   *
   * @throws SMGException if there is no value. This can happen for returns that only require a
   *     state and must never be associated by a value, e.g. function-calls that return void.
   */
  public Value getValue() throws SMGException {
    if (value != null) {
      return value;
    }
    throw new SMGException("Error: trying to access value returned by a void operation");
  }

  public SMGState getState() {
    return state;
  }
}
