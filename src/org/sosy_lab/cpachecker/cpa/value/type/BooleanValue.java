// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.type;

import org.sosy_lab.cpachecker.cfa.types.c.CType;

import java.util.Optional;

/**
 * This class represents a boolean value.
 * It may store the values <code>false</code> and <code>true</code>.
 */
public class BooleanValue implements Value {

  private static final long serialVersionUID = -35132790150256304L;

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
   * Returns an instance of a <code>BooleanValue</code> object representing the
   * boolean meaning of the given value, if one exists.
   * If none exists, an <code>Optional</code> with no contained reference is returned.
   *
   * @param pValue the {@link Value} whose boolean meaning should be returned
   * @return an <code>Optional</code> instance containing a reference to the
   * <code>BooleanValue</code> object representing the boolean meaning of the given value,
   * if one exists. An empty <code>Optional</code> instance, otherwise.
   */
  public static Optional<BooleanValue> valueOf(Value pValue) {
    if (pValue.isUnknown()) {
      return Optional.empty();

    } else if (pValue.isNumericValue()) {
      return valueOf((NumericValue) pValue);

    } else if (pValue instanceof BooleanValue) {
      return Optional.of((BooleanValue) pValue);

    } else {
      return Optional.empty();
    }
  }

  private static Optional<BooleanValue> valueOf(NumericValue pValue) {
    if (pValue.equals(new NumericValue(0L))) {
      return Optional.of(valueOf(false));
    } else if (pValue.equals(new NumericValue(1L))) {
      return Optional.of(valueOf(true));
    } else {
      return Optional.empty();
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

  @Override
  public <T> T accept(ValueVisitor<T> pVisitor) {
    return pVisitor.visit(this);
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
