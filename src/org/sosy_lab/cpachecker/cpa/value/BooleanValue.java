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
package org.sosy_lab.cpachecker.cpa.value;

import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * This class represents a boolean value.
 * It may store the values <code>false</code> and <code>true</code>.
 */
public class BooleanValue implements Value {

  // static objects for singleton pattern
  private static final BooleanValue TRUE_VALUE = new BooleanValue(true);
  private static final BooleanValue FALSE_VALUE = new BooleanValue(false);

  private final boolean value;

  private BooleanValue(boolean value) {
    this.value = value;
  }

  /**
   * Returns an instance of a <code>BooleanValue</code> object
   * with the specified value.
   *
   * @param value the value the returned object should hold
   * @return an instance of <code>BooleanValue</code> with the specified value
   */
  public static BooleanValue valueOf(boolean value) {
    if (value) {
      return TRUE_VALUE;
    } else {
      return FALSE_VALUE;
    }
  }

  /**
   * Returns whether this object represents the boolean value
   * <code>true</code>.
   *
   * @return <code>true</code> if this object represents <code>true</code>,
   *         false otherwise
   */
  public boolean isTrue() {
    return value;
  }

  /**
   * Returns the negation of this <code>BooleanValue</code>.
   *
   * @return a <code>BooleanValue</code> object representing <code>true</code>
   *         if this object represents <code>false</code>.
   *         An object representing <code>false</code> otherwise.
   */
  public BooleanValue negate() {
    return value ? FALSE_VALUE : TRUE_VALUE;
  }

  /**
   * Always returns <code>false</code> because <code>BooleanValue</code>
   * always stores a boolean and never a number.
   *
   * @return always <code>false</code>
   */
  @Override
  public boolean isNumericValue() {
    return false;
  }

  /**
   * Always returns <code>false</code>. <code>BooleanValue</code>
   * always stores either <code>true</code> or <code>false</code> and
   * never an unknown value.
   *
   * @return always <code>false</code>
   */
  @Override
  public boolean isUnknown() {
    return false;
  }

  /**
   * Always returns <code>true</code>. <code>BooleanValue</code>
   * always stores a specific value.
   *
   * @return always <code>true</code>
   */
  @Override
  public boolean isExplicitlyKnown() {
    return true;
  }

  /**
   * Returns a {@link NumericValue} object holding the numeric representation of this object's value.
   *
   * @return <p>Returns a <code>NumericValue</code> object with value
   *         <code>1</code>, if this object's value is <code>true</code>.
   *         Returns an object with value <code>0</code> otherwise.
   */
  @Override
  public NumericValue asNumericValue() {
    if (value) {
      return new NumericValue(1L);
    } else {
      return new NumericValue(0L);
    }
  }

  /**
   * Always throws an <code>AssertionError</code>.
   *
   * <p>There is no use for this method in the case of boolean values.</p>
   */
  @Override
  public Long asLong(CType pType) {
    throw new AssertionError("This method is not implemented");
  }

  /**
   * Returns whether the given object and this object are equal.
   *
   * Two <code>BooleanValue</code> objects are equal when they represent
   * the same boolean value.
   *
   * @param other the object to compare to this object
   * @return <code>true</code> if the objects are equal, <code>false</code>
   *         otherwise
   */
  @Override
  public boolean equals(Object other) {
    if (other instanceof BooleanValue) {
      return ((BooleanValue) other).value == value;

    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return value ? 1 : 0;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
}
