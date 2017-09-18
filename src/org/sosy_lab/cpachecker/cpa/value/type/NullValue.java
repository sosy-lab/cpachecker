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
package org.sosy_lab.cpachecker.cpa.value.type;

import java.io.Serializable;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.interval.NumberInterface;

/**
 * Singleton class for the special value <code>null</code>.
 */
public class NullValue implements NumberInterface, Serializable {

  private static final long serialVersionUID = 49593725475613735L;

  private static final NullValue SINGLETON = new NullValue();

  private NullValue() {
    // private constructor for singleton pattern
  }

  /**
   * Returns an instance of a <code>NullValue</code> object.
   *
   * @return an instance of this object
   */
  public static NumberInterface getInstance() {
    return SINGLETON;
  }

  /**
   * Always returns <code>false</code> since <code>null</code> is no numeric value.
   *
   * @return always returns <code>false</code>
   */
  @Override
  public boolean isNumericValue() {
    return false;
  }

  /**
   * Always returns <code>false</code> since <code>null</code> is a specific value.
   *
   * @return always returns <code>false</code>
   */
  @Override
  public boolean isUnknown() {
    return false;
  }

  /**
   * Always returns <code>true</code> since <code>null</code> is a specific value.
   */
  @Override
  public boolean isExplicitlyKnown() {
    return true;
  }

  /**
   * This method always returns <code>null</code>.
   *
   * <p>This object always represents <code>null</code>, which can't be
   * represented by a specific numeric value.</p>
   *
   */
  @Override
  public NumericValue asNumericValue() {
    return null;
  }

  /**
   * This method is not implemented and will lead to an <code>AssertionError</code>.
   * <code>Null</code> can't be represented by a specific number.
   */
  @Override
  public Long asLong(CType pType) {
    throw new AssertionError("Null cannot be represented as Long");
  }

  @Override
  public boolean equals(Object other) {

    // all NullValue objects are equal
    return other instanceof NullValue;
  }

  @Override
  public int hashCode() {
    return 1; // singleton without any values
  }

  @Override
  public String toString() {
    return "NULL";
  }

  @Override
  public NumberInterface EMPTY() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface UNBOUND() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface BOOLEAN_INTERVAL() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface ZERO() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface ONE() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean intersects(NumberInterface pOther) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Number getLow() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Number getHigh() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isGreaterThan(NumberInterface pOther) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isGreaterOrEqualThan(NumberInterface pOther) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public NumberInterface plus(NumberInterface pInterval) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface minus(NumberInterface pOther) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface times(NumberInterface pOther) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface divide(NumberInterface pOther) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface shiftLeft(NumberInterface pOffset) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface shiftRight(NumberInterface pOffset) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface unsignedDivide(NumberInterface pOther) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface unsignedModulo(NumberInterface pOther) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface unsignedShiftRight(NumberInterface pOther) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface modulo(NumberInterface pOther) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isUnbound() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public NumberInterface union(NumberInterface pOther) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean contains(NumberInterface pOther) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isEmpty() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public NumberInterface negate() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface intersect(NumberInterface pOther) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface limitUpperBoundBy(NumberInterface pOther) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface limitLowerBoundBy(NumberInterface pOther) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface asDecimal() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public NumberInterface asInteger() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Number getNumber() {
    // TODO Auto-generated method stub
    return null;
  }

@Override
public NumberInterface binaryAnd(NumberInterface pRNum) {
    // TODO Auto-generated method stub
    return null;
}

@Override
public NumberInterface binaryOr(NumberInterface pRNum) {
    // TODO Auto-generated method stub
    return null;
}

@Override
public NumberInterface binaryXor(NumberInterface pRNum) {
    // TODO Auto-generated method stub
    return null;
}

@Override
public boolean covers(NumberInterface pSign) {
    // TODO Auto-generated method stub
    return false;
}

@Override
public NumberInterface evaluateNonCommutativePlusOperator(NumberInterface pRight) {
    // TODO Auto-generated method stub
    return null;
}

@Override
public NumberInterface evaluateMulOperator(NumberInterface pRight) {
    // TODO Auto-generated method stub
    return null;
}

@Override
public NumberInterface evaluateNonCommutativeMulOperator(NumberInterface pRight) {
    // TODO Auto-generated method stub
    return null;
}

@Override
public NumberInterface evaluateDivideOperator(NumberInterface pRight) {
    // TODO Auto-generated method stub
    return null;
}

@Override
public NumberInterface evaluateModuloOperator(NumberInterface pRight) {
    // TODO Auto-generated method stub
    return null;
}

@Override
public NumberInterface evaluateAndOperator(NumberInterface pRight) {
    // TODO Auto-generated method stub
    return null;
}

@Override
public NumberInterface evaluateLessOperator(NumberInterface pRight) {
    // TODO Auto-generated method stub
    return null;
}

@Override
public NumberInterface evaluateLessEqualOperator(NumberInterface pRight) {
    // TODO Auto-generated method stub
    return null;
}

@Override
public NumberInterface evaluateEqualOperator(NumberInterface pRight) {
    // TODO Auto-generated method stub
    return null;
}
}
