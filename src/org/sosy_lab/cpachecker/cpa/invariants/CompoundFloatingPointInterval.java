// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.invariants;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

public class CompoundFloatingPointInterval implements CompoundInterval, FloatingPointType {

  private final FloatingPointTypeInfo typeInfo;

  public CompoundFloatingPointInterval(FloatingPointTypeInfo pTypeInfo) {
    typeInfo = pTypeInfo;
  }

  @Override
  public boolean isSingleton() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean hasLowerBound() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean hasUpperBound() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public BigInteger getLowerBound() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public BigInteger getUpperBound() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public BigInteger getValue() {
    // TODO Auto-generated method stub
    return BigInteger.ZERO;
  }

  @Override
  public boolean isDefinitelyFalse() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isDefinitelyTrue() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isBottom() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean contains(BigInteger pBigInteger) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public CompoundInterval extendToMinValue() {
    // TODO Auto-generated method stub
    return this;
  }

  @Override
  public CompoundInterval extendToMaxValue() {
    // TODO Auto-generated method stub
    return this;
  }

  @Override
  public CompoundInterval invert() {
    // TODO Auto-generated method stub
    return this;
  }

  @Override
  public CompoundInterval span() {
    // TODO Auto-generated method stub
    return this;
  }

  @Override
  public boolean containsAllPossibleValues() {
    return true;
  }

  @Override
  public boolean containsNegative() {
    return true;
  }

  @Override
  public boolean containsPositive() {
    return true;
  }

  @Override
  public CompoundInterval signum() {
    // TODO Auto-generated method stub
    return this;
  }

  @Override
  public List<? extends CompoundInterval> splitIntoIntervals() {
    // TODO Auto-generated method stub
    return Collections.singletonList(this);
  }

  @Override
  public List<SimpleInterval> getIntervals() {
    // TODO Auto-generated method stub
    return Collections.singletonList(SimpleInterval.infinite());
  }

  @Override
  public FloatingPointTypeInfo getTypeInfo() {
    return typeInfo;
  }
}
