// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.type;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/** Stores a numeric value that can be tracked by the ValueAnalysisCPA. */
public class NumericValue implements Value, Serializable {

  private static final long serialVersionUID = -3829943575180448170L;

  private Number number;

  /**
   * Creates a new <code>NumericValue</code>.
   *
   * @param pNumber the value of the number
   */
  public NumericValue(Number pNumber) {
    number = pNumber;
  }

  /**
   * Returns the number stored in the container.
   *
   * @return the number stored in the container
   */
  public Number getNumber() {
    return number;
  }

  /**
   * Returns the integer stored in the container as long. Before calling this function, it must be
   * ensured using `getType()` that this container contains an integer.
   */
  public long longValue() {
    return number.longValue();
  }

  /** Returns the floating point stored in the container as float. */
  public float floatValue() {
    return number.floatValue();
  }

  /** Returns the floating point stored in the container as double. */
  public double doubleValue() {
    return number.doubleValue();
  }

  /** Returns a BigDecimal value representing the stored number. */
  public BigDecimal bigDecimalValue() {
    if (number instanceof Double || number instanceof Float) {
      // if we use number.toString() for float values, the toString() method
      // will not print the full double but only the number of digits
      // necessary to distinguish it from the surrounding double-values.
      // This will result in an incorrect value of the BigDecimal.
      // Instead, use the floats themselves to get the precise value.
      //
      // cf. https://docs.oracle.com/javase/8/docs/api/java/lang/Double.html#toString-double-
      return BigDecimal.valueOf(number.doubleValue());
    } else if (number instanceof Rational) {
      Rational rat = (Rational) number;
      return new BigDecimal(rat.getNum())
          .divide(new BigDecimal(rat.getDen()), 100, RoundingMode.HALF_UP);
    } else {
      return new BigDecimal(number.toString());
    }
  }

  /** Returns a {@link BigInteger} value representing the stored number. */
  public BigInteger bigInteger() {
    if (number instanceof BigInteger) {
      return (BigInteger) number;
    }
    if (number instanceof Double || number instanceof Float) {
      long x = (long) number.doubleValue();
      return BigInteger.valueOf(x);
    }
    if (number instanceof BigDecimal) {
      return ((BigDecimal) number).toBigInteger();
    }
    if (number instanceof Rational) {
      return new NumericValue(number).bigDecimalValue().toBigInteger();
    }
    return new BigInteger(number.toString());
  }

  @Override
  public String toString() {
    return "NumericValue [number=" + number + "]";
  }

  /**
   * Returns whether this object and a given object are equal. Two <code>NumericValue</code> objects
   * are equal if and only if their stored values are equal.
   *
   * @param other the <code>Object</code> to compare to this object
   * @return <code>true</code> if the given object equals this object, <code>false</code> otherwise
   */
  @Override
  public boolean equals(Object other) {
    if (other instanceof NumericValue) {
      return getNumber().equals(((NumericValue) other).getNumber());
    } else {
      return false;
    }
  }

  /**
   * Always returns <code>true</code>.
   *
   * @return always <code>true</code>
   */
  @Override
  public boolean isNumericValue() {
    return true;
  }

  /**
   * Returns a <code>NumericValue</code> object that holds the negation of this object's value.
   *
   * @return the negation of this objects value
   */
  public NumericValue negate() {
    // TODO explicitfloat: handle the remaining different implementations of Number properly
    final Number numberToNegate = getNumber();

    // check if number is infinite or NaN
    if (numberToNegate instanceof Float) {
      if (numberToNegate.equals(Float.POSITIVE_INFINITY)) {
        return new NumericValue(Float.NEGATIVE_INFINITY);

      } else if (numberToNegate.equals(Float.NEGATIVE_INFINITY)) {
        return new NumericValue(Float.POSITIVE_INFINITY);

      } else if (numberToNegate.equals(Float.NaN)) {
        return new NumericValue(NegativeNaN.VALUE);
      }
    } else if (numberToNegate instanceof Double) {
      if (numberToNegate.equals(Double.POSITIVE_INFINITY)) {
        return new NumericValue(Double.NEGATIVE_INFINITY);

      } else if (numberToNegate.equals(Double.NEGATIVE_INFINITY)) {
        return new NumericValue(Double.POSITIVE_INFINITY);

      } else if (numberToNegate.equals(Double.NaN)) {
        return new NumericValue(NegativeNaN.VALUE);
      }
    } else if (numberToNegate instanceof Rational) {
      return new NumericValue(((Rational) numberToNegate).negate());
    } else if (NegativeNaN.VALUE.equals(numberToNegate)) {
      return new NumericValue(Double.NaN);
    }

    if (numberToNegate instanceof BigDecimal) {
      BigDecimal bd = (BigDecimal) numberToNegate;
      if (bd.signum() == 0) {
        return new NumericValue(-bd.doubleValue());
      }
    }

    // if the stored number is a 'casual' number, just negate it
    return new NumericValue(bigDecimalValue().negate());
  }

  @Override
  public NumericValue asNumericValue() {
    return this;
  }

  @Override
  public @Nullable Long asLong(CType type) {
    checkNotNull(type);
    type = type.getCanonicalType();
    if (!(type instanceof CSimpleType)) {
      return null;
    }

    if (((CSimpleType) type).getType() == CBasicType.INT) {
      return longValue();
    } else {
      return null;
    }
  }

  @Override
  public <T> T accept(ValueVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

  /**
   * Always returns <code>false</code> as each <code>NumericValue</code> holds one specific value.
   *
   * @return always <code>false</code>
   */
  @Override
  public boolean isUnknown() {
    return false;
  }

  /**
   * Always returns <code>true</code> as each <code>NumericValue</code> holds one specific value.
   *
   * @return always <code>true</code>
   */
  @Override
  public boolean isExplicitlyKnown() {
    return true;
  }

  @Override
  public int hashCode() {
    // fulfills contract that if this.equals(other),
    // then this.hashCode() == other.hashCode()
    return number.hashCode();
  }

  public static class NegativeNaN extends Number {

    private static final long serialVersionUID = 1L;

    public static final Number VALUE = new NegativeNaN();

    private NegativeNaN() {}

    @Override
    public double doubleValue() {
      return Double.NaN;
    }

    @Override
    public float floatValue() {
      return Float.NaN;
    }

    @Override
    public int intValue() {
      return (int) Double.NaN;
    }

    @Override
    public long longValue() {
      return (long) Double.NaN;
    }

    @Override
    public String toString() {
      return "-NaN";
    }

    @Override
    public boolean equals(Object pObj) {
      return pObj == this || pObj instanceof NegativeNaN;
    }

    @Override
    public int hashCode() {
      return -1;
    }
  }
}
