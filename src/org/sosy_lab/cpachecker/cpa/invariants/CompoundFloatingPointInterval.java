/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.invariants;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;


public class CompoundFloatingPointInterval implements CompoundInterval, FloatingPointType {

  private final FloatingPointTypeInfo typeInfo;

  public CompoundFloatingPointInterval(FloatingPointTypeInfo pTypeInfo) {
    this.typeInfo = pTypeInfo;
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
