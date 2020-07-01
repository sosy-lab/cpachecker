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
package org.sosy_lab.cpachecker.cpa.smg.evaluator;

import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGUnknownValue;

/**
 * This class represents a simple Pair of an object and a state.
 *
 * There are nested classes for special types of objects (value, address) and lists of such pairs.
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

    private SMGAddressValueAndState(SMGState pState, SMGAddressValue pValue) {
      super(pState, pValue);
    }

    @Override
    public SMGAddressValue getObject() {
      return (SMGAddressValue) super.getObject();
    }

    public static SMGAddressValueAndState of(SMGState pState, SMGAddressValue pValue) {
      return new SMGAddressValueAndState(pState, pValue);
    }

    public static SMGAddressValueAndState of(SMGState pState) {
      return of(pState, SMGUnknownValue.INSTANCE);
    }

    public static SMGAddressValueAndState of(SMGState pState, SMGEdgePointsTo pAddressValue) {
      return of(pState, SMGKnownAddressValue.valueOf(pAddressValue));
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

  public static class SMGValueAndState extends SMGAbstractObjectAndState<SMGSymbolicValue> {

    private SMGValueAndState(SMGState pState, SMGSymbolicValue pValue) {
      super(pState, pValue);
    }

    public static SMGValueAndState withUnknownValue(SMGState pState) {
      return of(pState, SMGUnknownValue.INSTANCE);
    }

    public static SMGValueAndState of(SMGState pState, SMGSymbolicValue pValue) {
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