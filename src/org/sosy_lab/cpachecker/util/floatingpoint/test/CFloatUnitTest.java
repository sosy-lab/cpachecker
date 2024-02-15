// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint.test;

import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.common.truth.TruthJUnit.assume;

import com.google.common.collect.ImmutableList;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloat;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI;

@RunWith(Parameterized.class)
public abstract class CFloatUnitTest {
  protected int floatType = CFloatNativeAPI.FP_TYPE_SINGLE; // TODO: Add other float types

  // Convert floating point value to its decimal representation
  private static String toPlainString(float fpVal) {
    if (Float.isNaN(fpVal)) {
      return "nan";
    }
    if (Float.isInfinite(fpVal)) {
      return Float.compare(fpVal, 0.0f) < 0 ? "-inf" : "inf";
    }
    if (fpVal == -0.0f) {
      return "-0.0";
    }
    return new BigDecimal(fpVal).toPlainString();
  }

  // Generate test values
  protected static List<Float> floatConsts() {
    ImmutableList.Builder<Float> builder = ImmutableList.builder();
    builder.add(
        Float.NEGATIVE_INFINITY,
        -Float.MAX_VALUE,
        -Float.MIN_NORMAL,
        -Float.MIN_VALUE,
        -0.0f,
        Float.NaN,
        0.0f,
        Float.MIN_VALUE,
        Float.MIN_NORMAL,
        Float.MAX_VALUE,
        Float.POSITIVE_INFINITY);

    for (int c = 1; c <= 2; c++) {
      for (int e = 1; e <= 5; e++) {
        builder.add((float) Math.pow(c * 0.1f, e));
      }
    }

    Random randomNumbers = new Random();
    int i = 0;
    while (i < 10) {
      float flt = Float.intBitsToFloat(randomNumbers.nextInt());
      if (!Float.isNaN(flt) && !Float.isInfinite(flt)) {
        if (!builder.build().contains(flt)) {
          builder.add(flt);
          i++;
        }
      }
    }
    return builder.build();
  }

  @Parameters(name = "a:{0} b:{1}")
  public static Collection<Object[]> floatArgs() {
    ImmutableList.Builder<Object[]> builder = ImmutableList.builder();
    for (Float f1 : floatConsts()) {
      for (Float f2 : floatConsts()) {
        builder.add(new Object[] {f1, f2});
      }
    }
    return builder.build();
  }

  private final Float arg1;
  private final Float arg2;

  private final CFloat nat1;
  private final CFloat nat2;

  private final CFloat jav1;
  private final CFloat jav2;

  public CFloatUnitTest(Float pArg1, Float pArg2) {
    String arg1Str = toPlainString(pArg1);
    nat1 = toTestedImpl(arg1Str, floatType);
    jav1 = toReferenceImpl(arg1Str, floatType);
    arg1 = pArg1;

    String arg2Str = toPlainString(pArg2);
    nat2 = toTestedImpl(arg2Str, floatType);
    jav2 = toReferenceImpl(arg2Str, floatType);
    arg2 = pArg2;
  }

  public abstract CFloat toTestedImpl(String repr, int pFloatType);

  public abstract CFloat toReferenceImpl(String repr, int pFloatType);

  private String toBinary(long number) {
    return BigInteger.valueOf(number).toString(2);
  }

  private String toBits(CFloat fp) {
    long sign = (fp.getExponent() & 0x100) >> 8;
    long exponent = fp.getExponent() & 0xFF;
    return toBinary(sign) + " " + toBinary(exponent) + " " + toBinary(fp.getMantissa());
  }

  protected void assumeOneArgument() {
    assume().that(arg1.equals(arg2) || (arg1.isNaN() && arg2.isNaN())).isTrue();
  }

  protected void assertEqualValue(CFloat fp1, CFloat fp2) {
    assertWithMessage(
            "tested impl: %s (%s)\nreference  : %s (%s)", fp1, toBits(fp1), fp2, toBits(fp2))
        .that(fp1.equals(fp2) || (fp1.isNan() && fp2.isNan()))
        .isTrue();
  }

  protected void assertEqualRepr(CFloat fp1, CFloat fp2) {
    assertWithMessage(
            "tested impl: %s (%s)\nreference  : %s (%s)", fp1, toBits(fp1), fp2, toBits(fp2))
        .that(Objects.equals(fp1.toString(), fp2.toString()))
        .isTrue();
  }

  protected void assertEqualP(boolean r1, boolean r2) {
    assertWithMessage("tested impl: %s\nreference  : %s", r1, r2).that(r1 == r2).isTrue();
  }

  @Test
  public void constTest() {
    assumeOneArgument();
    assertEqualValue(nat1, jav1);
  }

  @Test
  public void toStringTest() {
    assumeOneArgument();
    assertEqualRepr(nat1, jav1);
  }

  @Test
  public void addTest() {
    CFloat nat_ = nat1.add(nat2);
    CFloat jav_ = jav1.add(jav2);
    assertEqualValue(nat_, jav_);
  }

  @Test
  public void multiplyTest() {
    CFloat nat_ = nat1.multiply(nat2);
    CFloat jav_ = jav1.multiply(jav2);
    assertEqualValue(nat_, jav_);
  }

  @Test
  public void subtractTest() {
    CFloat nat_ = nat1.subtract(nat2);
    CFloat jav_ = jav1.subtract(jav2);
    assertEqualValue(nat_, jav_);
  }

  @Test
  public void divideByTest() {
    CFloat nat_ = nat1.divideBy(nat2);
    CFloat jav_ = jav1.divideBy(jav2);
    assertEqualValue(nat_, jav_);
  }

  @Test
  public void powToTest() {
    CFloat nat_ = nat1.powTo(nat2);
    CFloat jav_ = jav1.powTo(jav2);
    assertEqualValue(nat_, jav_);
  }

  @Test
  public void powToIntegralTest() {
    float rounded = Float.parseFloat(String.valueOf(Math.round(arg2)));
    assume().that(arg2).isEqualTo(rounded);
    CFloat nat_ = nat1.powToIntegral(Math.round(arg2));
    CFloat jav_ = jav1.powToIntegral(Math.round(arg2));
    assertEqualValue(nat_, jav_);
  }

  @Test
  public void sqrtTest() {
    assumeOneArgument();
    CFloat nat_ = nat1.sqrt();
    CFloat jav_ = jav1.sqrt();
    assertEqualValue(nat_, jav_);
  }

  @Test
  public void roundTest() {
    assumeOneArgument();
    CFloat nat_ = nat1.round();
    CFloat jav_ = jav1.round();
    assertEqualValue(nat_, jav_);
  }

  @Test
  public void truncTest() {
    assumeOneArgument();
    CFloat nat_ = nat1.trunc();
    CFloat jav_ = jav1.trunc();
    assertEqualValue(nat_, jav_);
  }

  @Test
  public void ceilTest() {
    assumeOneArgument();
    CFloat nat_ = nat1.ceil();
    CFloat jav_ = jav1.ceil();
    assertEqualValue(nat_, jav_);
  }

  @Test
  public void floorTest() {
    assumeOneArgument();
    CFloat nat_ = nat1.floor();
    CFloat jav_ = jav1.floor();
    assertEqualValue(nat_, jav_);
  }

  @Test
  public void absTest() {
    assumeOneArgument();
    CFloat nat_ = nat1.abs();
    CFloat jav_ = jav1.abs();
    assertEqualValue(nat_, jav_);
  }

  @Test
  public void isZeroTest() {
    assumeOneArgument();
    boolean nat_ = nat1.isZero();
    boolean jav_ = jav1.isZero();
    assertEqualP(nat_, jav_);
  }

  @Test
  public void isOneTest() {
    assumeOneArgument();
    boolean nat_ = nat1.isOne();
    boolean jav_ = jav1.isOne();
    assertEqualP(nat_, jav_);
  }

  @Test
  public void isNanTest() {
    assumeOneArgument();
    boolean nat_ = nat1.isNan();
    boolean jav_ = jav1.isNan();
    assertEqualP(nat_, jav_);
  }

  @Test
  public void isInfinityTest() {
    assumeOneArgument();
    boolean nat_ = nat1.isInfinity();
    boolean jav_ = jav1.isInfinity();
    assertEqualP(nat_, jav_);
  }

  @Test
  public void isNegativeTest() {
    assumeOneArgument();
    boolean nat_ = nat1.isNegative();
    boolean jav_ = jav1.isNegative();
    assertEqualP(nat_, jav_);
  }

  @Test
  public void greaterThanTest() {
    boolean nat_ = nat1.greaterThan(nat2);
    boolean jav_ = jav1.greaterThan(jav2);
    assertEqualP(nat_, jav_);
  }

  @Test
  public void copySignFromTest() {
    CFloat nat_ = nat1.copySignFrom(nat2);
    CFloat jav_ = jav1.copySignFrom(jav2);
    assertEqualValue(nat_, jav_);
  }

  // FIXME: Implement castTo
  /*
  public CFloat castTo(int toType) {
    return null;
  }

  public Number castToOther(int toType) {
    return null;
  }
  */
}
