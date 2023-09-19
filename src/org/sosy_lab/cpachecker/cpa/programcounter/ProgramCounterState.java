// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.programcounter;

import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class ProgramCounterState
    implements AbstractState, LatticeAbstractState<ProgramCounterState> {

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

  @Override
  public boolean isLessOrEqual(ProgramCounterState other) {
    return other.containsAll(this);
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
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof ProgramCounterState other && values == other.values;
  }

  @Override
  public int hashCode() {
    return Objects.hash(values);
  }

  @Override
  public String toString() {
    return isTop() ? "TOP" : values.toString();
  }

  public ProgramCounterState insert(BigInteger pValue) {
    if (containsValue(pValue)) {
      return this;
    }
    return new ProgramCounterState(
        ImmutableSet.<BigInteger>builder().addAll(values).add(pValue).build());
  }

  @Override
  public ProgramCounterState join(ProgramCounterState pOther) {
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
    builder.addAll(pValues);
    return new ProgramCounterState(builder.build());
  }
}
