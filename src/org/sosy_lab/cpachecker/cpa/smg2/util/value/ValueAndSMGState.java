// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util.value;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;

public class ValueAndSMGState {

  private final Value value;
  private final SMGState state;

  private ValueAndSMGState(Value pValue, SMGState pState) {
    value = pValue;
    state = pState;
  }

  public static ValueAndSMGState of(Value pValue, SMGState pState) {
    Preconditions.checkNotNull(pState, pValue);
    return new ValueAndSMGState(pValue, pState);
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

  public Value getValue() {
    return value;
  }

  public SMGState getState() {
    return state;
  }
}
