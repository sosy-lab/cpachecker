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
package org.sosy_lab.cpachecker.cpa.programcounter;

import java.math.BigInteger;
import java.util.Set;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

import com.google.common.collect.ImmutableSet;


public class ProgramCounterState implements AbstractState {

  private static final ProgramCounterState TOP = new ProgramCounterState();

  private final Set<BigInteger> values;

  private ProgramCounterState() {
    this(null);
  }

  private ProgramCounterState(ImmutableSet<BigInteger> pValues) {
    assert pValues != null || TOP == null;
    this.values = pValues;
  }

  public boolean isTop() {
    assert values != null || this == TOP;
    return values == null;
  }

  public boolean isBottom() {
    return !isTop() && values.isEmpty();
  }

  public boolean containsValue(BigInteger pValue) {
    return isTop() || this.values.contains(pValue);
  }

  public boolean containsAll(ProgramCounterState pOther) {
    if (pOther.isTop()) {
      return isTop();
    }
    if (isTop() || this == pOther) {
      return true;
    }
    return values.containsAll(pOther.values);
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO instanceof ProgramCounterState) {
      ProgramCounterState other = (ProgramCounterState) pO;
      return values == other.values || values != null && values.equals(other.values);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return values.hashCode();
  }

  @Override
  public String toString() {
    return isTop() ? "TOP" : values.toString();
  }

  public ProgramCounterState insert(BigInteger pValue) {
    if (containsValue(pValue)) {
      return this;
    }
    return new ProgramCounterState(ImmutableSet.<BigInteger>builder().addAll(values).add(pValue).build());
  }

  public ProgramCounterState insertAll(ProgramCounterState pOther) {
    if (isTop() || pOther.isTop()) {
      return TOP;
    }
    ImmutableSet.Builder<BigInteger> builder = null;
    for (BigInteger value : pOther.values) {
      if (!containsValue(value)) {
        if (builder == null) {
          builder = ImmutableSet.builder();
          builder.addAll(values);
        }
        builder.add(value);
      }
    }
    if (builder == null) {
      return this;
    }
    return new ProgramCounterState(builder.build());
  }

  public ProgramCounterState remove(BigInteger pValue) {
    if (isTop() || !containsValue(pValue)) {
      return this;
    }
    ImmutableSet.Builder<BigInteger> builder = ImmutableSet.builder();
    for (BigInteger value : values) {
      if (!value.equals(pValue)) {
        builder.add(value);
      }
    }
    return new ProgramCounterState(builder.build());
  }

  public static ProgramCounterState getTopState() {
    return TOP;
  }

  public static ProgramCounterState getStateForValue(BigInteger pPCValue) {
    return new ProgramCounterState(ImmutableSet.of(pPCValue));
  }

  public static AbstractState getStateForValues(Iterable<BigInteger> pValues) {
    ImmutableSet.Builder<BigInteger> builder = ImmutableSet.builder();
    for (BigInteger value : pValues) {
      builder.add(value);
    }
    return new ProgramCounterState(builder.build());
  }

}
