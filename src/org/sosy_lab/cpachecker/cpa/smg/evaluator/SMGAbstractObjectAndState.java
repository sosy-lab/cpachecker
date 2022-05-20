// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.evaluator;

import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGUnknownValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

/**
 * This class represents a simple Pair of an object and a state.
 *
 * <p>There are nested classes for special types of objects (value, address) and lists of such
 * pairs.
 */
public abstract class SMGAbstractObjectAndState<T> {
  private final SMGState smgState;
  private final T object;

  private SMGAbstractObjectAndState(SMGState pState, T pValue) {
    smgState = pState;
    object = pValue;
  }

  public T getObject() {
    return object;
  }

  public SMGState getSmgState() {
    return smgState;
  }

  @Override
  public String toString() {
    return object + " StateId: " + smgState.getId();
  }

  public static class SMGAddressValueAndState extends SMGValueAndState {
    private final SMGValue symbolicValue;

    private SMGAddressValueAndState(
        SMGState pState, SMGAddressValue pValue, SMGValue pSymbolicValue) {
      super(pState, pValue);
      symbolicValue = pSymbolicValue;
    }

    @Override
    public SMGAddressValue getObject() {
      return (SMGAddressValue) super.getObject();
    }

    public SMGValue getValue() {
      return symbolicValue;
    }

    public static SMGAddressValueAndState of(SMGState pState, SMGAddressValue pValue) {
      return new SMGAddressValueAndState(pState, pValue, pValue);
    }

    public static SMGAddressValueAndState of(SMGState pState) {
      return of(pState, SMGUnknownValue.INSTANCE);
    }

    public static SMGAddressValueAndState of(SMGState pState, SMGValue pValue) {
      return new SMGAddressValueAndState(pState, SMGUnknownValue.INSTANCE, pValue);
    }

    public static SMGAddressValueAndState of(SMGState pState, SMGEdgePointsTo pAddressValue) {
      return new SMGAddressValueAndState(
          pState, SMGKnownAddressValue.valueOf(pAddressValue), pAddressValue.getValue());
    }
  }

  public static class SMGAddressAndState extends SMGAbstractObjectAndState<SMGAddress> {

    private SMGAddressAndState(SMGState pState, SMGAddress pAddress) {
      super(pState, pAddress);
    }

    public static SMGAddressAndState withUnknownAddress(SMGState pState) {
      return of(pState, SMGAddress.UNKNOWN);
    }

    public static SMGAddressAndState of(SMGState pState, SMGAddress pAddress) {
      return new SMGAddressAndState(pState, pAddress);
    }
  }

  public static class SMGValueAndState extends SMGAbstractObjectAndState<SMGValue> {

    private SMGValueAndState(SMGState pState, SMGValue pValue) {
      super(pState, pValue);
    }

    public static SMGValueAndState withUnknownValue(SMGState pState) {
      return of(pState, SMGUnknownValue.INSTANCE);
    }

    public static SMGValueAndState of(SMGState pState, SMGValue pValue) {
      return new SMGValueAndState(pState, pValue);
    }
  }

  public static class SMGExplicitValueAndState extends SMGAbstractObjectAndState<SMGExplicitValue> {

    private SMGExplicitValueAndState(SMGState pState, SMGExplicitValue pValue) {
      super(pState, pValue);
    }

    public static SMGExplicitValueAndState of(SMGState pState, SMGExplicitValue pValue) {
      return new SMGExplicitValueAndState(pState, pValue);
    }
  }
}
