// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.type;

import static com.google.common.truth.Truth.assertThat;

import java.math.BigInteger;
import org.junit.Test;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue;

public class NumericValueTest {

  @Test
  public void longValue_conversionFromPositiveLong() {
    NumericValue val = new NumericValue(5L);
    long result = val.longValue();
    assertThat(result).isEqualTo(5L);
  }

  @Test
  public void longValue_conversionFromDouble() {
    NumericValue val = new NumericValue(5.3d);
    long result = val.longValue();
    assertThat(result).isEqualTo(5L);
  }

  @Test
  public void longValue_conversionFromFloat() {
    NumericValue val = new NumericValue(5.3f);
    long result = val.longValue();
    assertThat(result).isEqualTo(5L);
  }

  @Test
  public void longValue_conversionFromRationalWithDecimals() {
    Rational input = Rational.of(BigInteger.ONE, BigInteger.TWO);
    NumericValue val = new NumericValue(input);
    long result = val.longValue();
    assertThat(result).isEqualTo(0L);
  }

  @Test
  public void longValue_conversionFromRationalWithoutDecimals() {
    Rational input = Rational.of(BigInteger.TEN, BigInteger.TWO);
    NumericValue val = new NumericValue(input);
    long result = val.longValue();
    assertThat(result).isEqualTo(5L);
  }

  @Test
  public void longValue_conversionFromInteger() {
    NumericValue val = new NumericValue(10);
    long result = val.longValue();
    assertThat(result).isEqualTo(10L);
  }

  @Test
  public void floatingPointValue_conversionFromPositiveLong() {
    NumericValue val = new NumericValue(5L);
    FloatValue.Format precision = FloatValue.Format.Float64;
    FloatValue result = val.floatingPointValue(precision);
    assertThat(result).isEqualTo(FloatValue.fromString(precision, "5"));
  }

  @Test
  public void floatingPointValue_conversionFromDouble() {
    NumericValue val = new NumericValue(5.3d);
    FloatValue.Format precision = FloatValue.Format.Float64;
    FloatValue result = val.floatingPointValue(precision);
    assertThat(result).isEqualTo(FloatValue.fromString(precision, "5.3"));
  }

  @Test
  public void floatingPointValue_conversionFromFloat() {
    NumericValue val = new NumericValue(5.3f);
    FloatValue.Format precision = FloatValue.Format.Float32;
    FloatValue result = val.floatingPointValue(precision);
    assertThat(result).isEqualTo(FloatValue.fromString(precision, "5.3"));
  }

  @Test
  public void floatingPointValue_conversionFromRationalWithDecimals() {
    Rational input = Rational.of(BigInteger.ONE, BigInteger.TWO);
    NumericValue val = new NumericValue(input);
    FloatValue.Format precision = FloatValue.Format.Float64;
    FloatValue result = val.floatingPointValue(precision);
    assertThat(result).isEqualTo(FloatValue.fromString(precision, "0.5"));
  }

  @Test
  public void floatingPointValue_conversionFromRationalWithInfiniteDecimals() {
    Rational input = Rational.of(BigInteger.ONE, BigInteger.valueOf(3));
    NumericValue val = new NumericValue(input);
    FloatValue.Format precision = FloatValue.Format.Float64;
    FloatValue result = val.floatingPointValue(precision);
    assertThat(result).isEqualTo(FloatValue.fromString(precision, "0.3333333333333333"));
  }

  @Test
  public void floatingPointValue_conversionFromRationalWithoutDecimals() {
    Rational input = Rational.of(BigInteger.TEN, BigInteger.TWO);
    NumericValue val = new NumericValue(input);
    FloatValue.Format precision = FloatValue.Format.Float64;
    FloatValue result = val.floatingPointValue(precision);
    assertThat(result).isEqualTo(FloatValue.fromString(precision, "5"));
  }

  @Test
  public void floatingPointValue_conversionFromInteger() {
    NumericValue val = new NumericValue(10);
    FloatValue.Format precision = FloatValue.Format.Float64;
    FloatValue result = val.floatingPointValue(precision);
    assertThat(result).isEqualTo(FloatValue.fromString(precision, "10"));
  }

  @Test
  public void bigInteger_conversionFromPositiveLong() {
    NumericValue val = new NumericValue(5L);
    BigInteger result = val.bigIntegerValue();
    assertThat(result).isEqualTo(BigInteger.valueOf(5));
  }

  @Test
  public void bigInteger_conversionFromDouble() {
    NumericValue val = new NumericValue(5.3d);
    BigInteger result = val.bigIntegerValue();
    assertThat(result).isEqualTo(BigInteger.valueOf(5));
  }

  @Test
  public void bigInteger_conversionFromFloat() {
    NumericValue val = new NumericValue(5.3f);
    BigInteger result = val.bigIntegerValue();
    assertThat(result).isEqualTo(BigInteger.valueOf(5));
  }

  @Test
  public void bigInteger_conversionFromRationalWithDecimals() {
    Rational input = Rational.of(BigInteger.ONE, BigInteger.TWO);
    NumericValue val = new NumericValue(input);
    BigInteger result = val.bigIntegerValue();
    assertThat(result).isEqualTo(BigInteger.valueOf(0));
  }

  @Test
  public void bigInteger_conversionFromRationalWithoutDecimals() {
    Rational input = Rational.of(BigInteger.TEN, BigInteger.TWO);
    NumericValue val = new NumericValue(input);
    BigInteger result = val.bigIntegerValue();
    assertThat(result).isEqualTo(BigInteger.valueOf(5));
  }

  @Test
  public void bigInteger_conversionFromInteger() {
    NumericValue val = new NumericValue(10);
    BigInteger result = val.bigIntegerValue();
    assertThat(result).isEqualTo(BigInteger.TEN);
  }

  @Test
  public void doubleValue_conversionFromPositiveLong() {
    NumericValue val = new NumericValue(5L);
    double result = val.doubleValue();
    assertThat(result).isEqualTo(5);
  }

  @Test
  public void doubleValue_conversionFromDouble() {
    NumericValue val = new NumericValue(5.3d);
    double result = val.doubleValue();
    assertThat(result).isEqualTo(5.3d);
  }

  @Test
  public void doubleValue_conversionFromFloat() {
    // Note that this test will fail if one sets val = new NumericValue(5.3f) and check for 5.3d
    NumericValue val = new NumericValue(5.5f);
    double result = val.doubleValue();
    assertThat(result).isEqualTo(5.5d);
  }

  @Test
  public void doubleValue_conversionFromRationalWithDecimals() {
    Rational input = Rational.of(BigInteger.ONE, BigInteger.TWO);
    NumericValue val = new NumericValue(input);
    double result = val.doubleValue();
    assertThat(result).isEqualTo(0.5);
  }

  @Test
  public void doubleValue_conversionFromRationalWithoutDecimals() {
    Rational input = Rational.of(BigInteger.TEN, BigInteger.TWO);
    NumericValue val = new NumericValue(input);
    double result = val.doubleValue();
    assertThat(result).isEqualTo(5);
  }

  @Test
  public void doubleValue_conversionFromInteger() {
    NumericValue val = new NumericValue(10);
    double result = val.doubleValue();
    assertThat(result).isEqualTo(10d);
  }
}
