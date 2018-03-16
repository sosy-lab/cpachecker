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

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGExplicitValue;
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
    return object.toString() + " StateId: " + smgState.getId();
  }

  public static class SMGAddressValueAndState extends SMGValueAndState {

    private SMGAddressValueAndState(SMGState pState, SMGAddressValue pValue) {
      super(pState, pValue);
    }

    public SMGAddressAndState asSMGAddressAndState() {
      return SMGAddressAndState.of(getSmgState(), getObject().getAddress());
    }

    @Override
    public SMGAddressValue getObject() {
      return (SMGAddressValue) super.getObject();
    }

    public static SMGAddressValueAndState of(SMGState pState, SMGAddressValue pValue) {
      return new SMGAddressValueAndState(pState, pValue);
    }

    public static SMGAddressValueAndState of(SMGState pState) {
      return new SMGAddressValueAndState(pState, SMGUnknownValue.getInstance());
    }
  }

  public static class SMGAddressAndState extends SMGAbstractObjectAndState<SMGAddress> {

    private SMGAddressAndState(SMGState pState, SMGAddress pAddress) {
      super(pState, pAddress);
    }

    public static List<SMGAddressAndState> listOf(SMGState pInitialSmgState, SMGAddress pValueOf) {
      return ImmutableList.of(of(pInitialSmgState, pValueOf));
    }

    public static List<SMGAddressAndState> listOf(SMGState pInitialSmgState) {
      return ImmutableList.of(of(pInitialSmgState));
    }

    public static SMGAddressAndState of(SMGState pState) {
      return new SMGAddressAndState(pState, SMGAddress.getUnknownInstance());
    }

    public static SMGAddressAndState of(SMGState pState, SMGAddress pAddress) {
      return new SMGAddressAndState(pState, pAddress);
    }
  }

  public static class SMGValueAndStateList {

    private final List<? extends SMGValueAndState> valueAndStateList;

    public SMGValueAndStateList(List<? extends SMGValueAndState> list) {
      valueAndStateList = ImmutableList.copyOf(list);
    }

    public SMGValueAndStateList(SMGValueAndState pE) {
      valueAndStateList = ImmutableList.of(pE);
    }

    public int size() {
      return valueAndStateList.size();
    }

    @Override
    public String toString() {
      return valueAndStateList.toString();
    }

    @Override
    public boolean equals(Object pObj) {
      return valueAndStateList.equals(pObj);
    }

    @Override
    public int hashCode() {
      return valueAndStateList.hashCode();
    }

    public List<? extends SMGValueAndState> getValueAndStateList() {
      return valueAndStateList;
    }

    public static SMGValueAndStateList of(SMGValueAndState pE) {
      return new SMGValueAndStateList(pE);
    }

    public static SMGValueAndStateList of(SMGState smgState) {
      return of(SMGValueAndState.of(smgState));
    }

    public static SMGValueAndStateList of(SMGState smgState, SMGSymbolicValue val) {
      return of(SMGValueAndState.of(smgState, val));
    }

    public static SMGValueAndStateList copyOf(List<SMGValueAndState> pE) {
      return new SMGValueAndStateList(pE);
    }

    public List<SMGState> asSMGStateList() {
      return Lists.transform(valueAndStateList, SMGValueAndState::getSmgState);
    }

    public static SMGValueAndStateList copyOfUnknownValue(List<SMGState> pNewStates) {
      return copyOf(Lists.transform(pNewStates, SMGValueAndState::of));
    }
  }

  public static class SMGAddressValueAndStateList extends SMGValueAndStateList {

    private SMGAddressValueAndStateList(List<SMGAddressValueAndState> pList) {
      super(ImmutableList.copyOf(pList));
    }

    public List<SMGAddressAndState> asAddressAndStateList() {
      List<SMGAddressAndState> result = new ArrayList<>();
      for (SMGValueAndState valueAndState : getValueAndStateList()) {
        SMGAddressValueAndState addressValueAndState = (SMGAddressValueAndState) valueAndState;
        SMGAddressValue addressValue = addressValueAndState.getObject();
        SMGState newState = addressValueAndState.getSmgState();
        result.add(addressValue.isUnknown() ? SMGAddressAndState.of(newState)
                : SMGAddressAndState.of(newState, addressValue.getAddress()));
      }
      return result;
    }

    private SMGAddressValueAndStateList(SMGAddressValueAndState pE) {
      super(pE);
    }

    public List<SMGAddressValueAndState> asAddressValueAndStateList() {
      return FluentIterable.from(getValueAndStateList()).filter(SMGAddressValueAndState.class).toList();
    }

    public static SMGAddressValueAndStateList of(SMGAddressValueAndState pE) {
      return new SMGAddressValueAndStateList(pE);
    }

    public static SMGAddressValueAndStateList of(SMGState smgState) {
      return of(SMGAddressValueAndState.of(smgState));
    }

    public static SMGAddressValueAndStateList copyOfAddressValueList(List<SMGAddressValueAndState> pList) {
      return new SMGAddressValueAndStateList(pList);
    }
  }

  public static class SMGValueAndState extends SMGAbstractObjectAndState<SMGSymbolicValue> {

    private SMGValueAndState(SMGState pState, SMGSymbolicValue pValue) {
      super(pState, pValue);
    }

    public static SMGValueAndState of(SMGState pState) {
      return of(pState, SMGUnknownValue.getInstance());
    }

    public static SMGValueAndState of(SMGState pState, SMGSymbolicValue pValue) {
      return new SMGValueAndState(pState, pValue);
    }
  }

  public static class SMGExplicitValueAndState extends SMGAbstractObjectAndState<SMGExplicitValue> {

    private SMGExplicitValueAndState(SMGState pState, SMGExplicitValue pValue) {
      super(pState, pValue);
    }

    public static SMGExplicitValueAndState of(SMGState pState) {
      return of(pState, SMGUnknownValue.getInstance());
    }

    public static SMGExplicitValueAndState of(SMGState pState, SMGExplicitValue pValue) {
      return new SMGExplicitValueAndState(pState, pValue);
    }
  }
}