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
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue.Format;

/** Stores a numeric value that can be tracked by the ValueAnalysisCPA. */
public record NumericValue(Number number) implements Value {

  @Serial private static final long serialVersionUID = -3829943575180448170L;

  /** Returns the number stored in the container. */
  public Number getNumber() {
    return number;
  }

  /**
   * Convert to long
   *
   * <p>Warning: This silently truncates and rounds the value to fit into a long. Use {@link
   * #bigIntegerValue()} instead.
   */
  public long longValue() {
    return number.longValue();
  }

  /**
   * Convert to float
   *
   * <p>Warning: This silently truncates and rounds the value to fit into a float.
   */
  public float floatValue() {
    return number.floatValue();
  }

  /**
   * Convert to double
   *
   * <p>Warning: This silently truncates and rounds the value to fit into a double.
   */
  public double doubleValue() {
    return number.doubleValue();
  }

  /**
   * Return value as a {@link FloatValue}
   *
   * <p>Throws an exception if the value does not have a floating point type already. Use {@link
   * NumericValue#hasFloatType()} to check first, and {@link
   * NumericValue#floatingPointValue(Format)} for conversion.
   */
  public FloatValue getFloatValue() {
    if (number instanceof FloatValue floatValue) {
      return floatValue;
    } else if (number instanceof Double doubleValue) {
      return FloatValue.fromDouble(doubleValue);
    } else if (number instanceof Float floatValue) {
      return FloatValue.fromFloat(floatValue);
    } else {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Convert to {@link FloatValue]
   *
   * @param format The target format for the conversion
   */
  public FloatValue floatingPointValue(FloatValue.Format format) {
    if (hasFloatType()) {
      return getFloatValue().withPrecision(format);
    } else if (hasIntegerType()) {
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
   * Return value as a {@link BigInteger}
   *
   * <p>Throws an exception if the value does not have an integer type already. Use {@link
   * NumericValue#hasIntegerType()} to check first, and {@link NumericValue#bigIntegerValue(Format)}
   * for conversion.
   */
  public BigInteger getIntegerValue() {
    if (number instanceof BigInteger bigInt) {
      return bigInt;
    } else if (number instanceof Long
        || number instanceof Integer
        || number instanceof Short
        || number instanceof Byte) {
      return BigInteger.valueOf(number.longValue());
    } else {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Convert to {@link BigInteger}
   *
   * <p>WARNING: This silently rounds decimal numbers.
   */
  public BigInteger bigIntegerValue() {
    if (hasIntegerType()) {
      return getIntegerValue();
    } else if (number instanceof Rational rationalValue) {
      BigInteger num = rationalValue.getNum();
      BigInteger den = rationalValue.getDen();
      return num.divide(den);
    } else if (number instanceof Double || number instanceof Float) {
      return FloatValue.fromDouble(number.doubleValue()).integerValue();
    } else if (number instanceof FloatValue floatValue) {
      return floatValue.integerValue();
    } else {
      throw new IllegalArgumentException();
    }
  }

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
        return new NumericValue(FloatValue.nan(FloatValue.Format.Float32).negate().floatValue());
      } else {
        return new NumericValue(-numberToNegate);
      }
    } else if (number instanceof Double numberToNegate) {
      if (Double.isNaN(numberToNegate)) {
        // If the number is NaN we need to convert to FloatValue to handle the sign
        return new NumericValue(FloatValue.nan(FloatValue.Format.Float64).negate().floatValue());
      } else {
        return new NumericValue(-numberToNegate);
      }
    } else if (number instanceof FloatValue floatValue) {
      return new NumericValue(floatValue.negate());
    } else if (hasIntegerType()) {
      // FIXME: This might be broken for -MAX_VALUE if the type is not BigInteger
      return new NumericValue(bigIntegerValue().negate());
    } else if (number instanceof Rational rat) {
      return new NumericValue(rat.negate());
    } else {
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
