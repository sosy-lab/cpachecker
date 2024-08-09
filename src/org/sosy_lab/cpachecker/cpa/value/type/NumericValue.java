// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.type;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import java.math.BigInteger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue;

/** Stores a numeric value that can be tracked by the ValueAnalysisCPA. */
public record NumericValue(Number number) implements Value {

  @Serial private static final long serialVersionUID = -3829943575180448170L;

  /** Returns the number stored in the container. Same as {@link #number()} for consistency. */
  public Number getNumber() {
    return number;
  }

  /**
   * Returns the integer stored in the container as long. Before calling this function, it must be
   * ensured using `getType()` that this container contains an integer.
   *
   * <p>Warning: This silently truncates and rounds the value to fit into a long. Use {@link
   * #bigIntegerValue()} instead.
   */
  public long longValue() {
    return number.longValue();
  }

  /**
   * Returns the floating point stored in the container as float.
   *
   * <p>Warning: This silently truncates and rounds the value to fit into a float. Use {@link
   * #bigIntegerValue()} instead.
   */
  public float floatValue() {
    return number.floatValue();
  }

  /**
   * Returns the floating point stored in the container as double. *
   *
   * <p>Warning: This silently truncates and rounds the value to fit into a double. Use {@link
   * #floatingPointValue() or #bigIntegerValue()} instead.
   */
  public double doubleValue() {
    return number.doubleValue();
  }

  public FloatValue floatingPointValue() {
    // TODO: Add a parameter for the target precision
    FloatValue.Format format = FloatValue.Format.Float64;
    if (number instanceof FloatValue floatValue) {
      return floatValue;
    } else if (number instanceof Double doubleValue) {
      return FloatValue.fromDouble(doubleValue);
    } else if (number instanceof Float floatValue) {
      return FloatValue.fromFloat(floatValue);
    } else if (number instanceof BigInteger
        || number instanceof Long
        || number instanceof Integer
        || number instanceof Short
        || number instanceof Byte) {
      return FloatValue.fromInteger(format, bigIntegerValue());
    } else if (number instanceof Rational rat) {
      FloatValue n = FloatValue.fromInteger(format, rat.getNum());
      FloatValue d = FloatValue.fromInteger(format, rat.getDen());
      return n.divide(d);
    } else {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Returns a {@link BigInteger} value representing the stored number.
   *
   * <p>WARNING: This silently rounds decimal numbers.
   */
  public BigInteger bigIntegerValue() {
    if (number instanceof BigInteger bigInt) {
      return bigInt;
    } else if (number instanceof Long
        || number instanceof Integer
        || number instanceof Short
        || number instanceof Byte) {
      return BigInteger.valueOf(number.longValue());
    } else if (number instanceof Double
        || number instanceof Float
        || number instanceof Rational
        || number instanceof FloatValue) {
      return floatingPointValue().integerValue();
    } else {
      throw new IllegalArgumentException();
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
   * Check if the value has an integer type.
   *
   * <p>Note that this will not check if the actual value is integer. For types like Double or
   * Rational this method always returns <code>false</code>.
   */
  public boolean hasIntegerType() {
    return number instanceof BigInteger
        || number instanceof Long
        || number instanceof Integer
        || number instanceof Short
        || number instanceof Byte;
  }

  /** Check if the value has a floating point type. */
  public boolean hasFloatType() {
    return number instanceof FloatValue || number instanceof Double || number instanceof Float;
  }

  /**
   * Check if the type has a fixed size.
   *
   * <p>Returns <code>false</code> for Rational and BigInteger as they can represent arbitrary sized
   * values.
   */
  public boolean hasFixedSize() {
    return !(number instanceof BigInteger || number instanceof Rational);
  }

  /**
   * Returns a <code>NumericValue</code> object that holds the negation of this object's value.
   *
   * @return the negation of this objects value
   */
  public NumericValue negate() {
    if (number instanceof Float numberToNegate) {
      if (Float.isNaN(numberToNegate)) {
        // If the number is NaN we need to convert to FloatValue to handle the sign
        return new NumericValue(floatingPointValue().negate().floatValue());
      } else {
        return new NumericValue(-numberToNegate);
      }
    } else if (number instanceof Double numberToNegate) {
      if (Double.isNaN(numberToNegate)) {
        // If the number is NaN we need to convert to FloatValue to handle the sign
        return new NumericValue(floatingPointValue().negate().floatValue());
      } else {
        return new NumericValue(-numberToNegate);
      }
    } else if (number instanceof BigInteger
        || number instanceof Long
        || number instanceof Integer
        || number instanceof Short
        || number instanceof Byte) {
      return new NumericValue(bigIntegerValue().negate());
    } else if (number instanceof Rational rat) {
      return new NumericValue(rat.negate());
    } else if (number instanceof FloatValue floatValue) {
      return new NumericValue(floatValue.negate());
    } else {
      // TODO explicitfloat: handle the remaining different implementations of Number properly
      throw new IllegalArgumentException();
    }
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
}
