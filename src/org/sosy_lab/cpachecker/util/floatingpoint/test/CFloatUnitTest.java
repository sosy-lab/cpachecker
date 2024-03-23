// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint.test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.Correspondence.BinaryPredicate;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Random;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import org.junit.Test;
import org.kframework.mpfr.BigFloat;
import org.kframework.mpfr.BinaryMathContext;
import org.sosy_lab.common.NativeLibraries;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloat;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CNativeType;

public abstract class CFloatUnitTest {
  static {
    NativeLibraries.loadLibrary("mpfr_java");
  }

  // Override to set a floating point width
  protected abstract BinaryMathContext getFloatType();

  // List of all supported reference implementations
  public enum ReferenceImpl {
    MPFR,
    JAVA,
    NATIVE
  }

  // Return the reference implementation used by the test
  protected abstract ReferenceImpl getRefImpl();

  // Pretty printer for BigFloat type
  private static String printBigFloat(BigFloat value) {
    if (value.isNaN()) {
      return "nan";
    }
    if (value.isInfinite()) {
      return value.sign() ? "-inf" : "inf";
    }
    if (value.isNegativeZero()) {
      return "-0.0";
    }
    if (value.isPositiveZero()) {
      return "0.0";
    }
    return value.toString().replaceAll(",", ".");
  }

  // Convert floating point value to its decimal representation
  private static String toPlainString(BigFloat value) {
    String r = printBigFloat(value);
    if (r.contains("e")) {
      r = new BigDecimal(r).toPlainString();
    }
    return r;
  }

  // Generate a list of floating point constants that cover all special case values
  protected static List<BigFloat> floatConsts(BinaryMathContext format) {
    ImmutableList.Builder<BigFloat> builder = ImmutableList.builder();
    builder.add(
        BigFloat.negativeInfinity(format.precision),
        BigFloat.maxValue(format.precision, format.maxExponent).negate(),
        new BigFloat(-17.0f, format),
        new BigFloat(-1.0f, format),
        new BigFloat(-0.1f, format),
        BigFloat.minNormal(format.precision, format.minExponent).negate(),
        BigFloat.minValue(format.precision, format.minExponent).negate(),
        BigFloat.negativeZero(format.precision),
        BigFloat.NaN(format.precision),
        BigFloat.zero(format.precision),
        BigFloat.minValue(format.precision, format.minExponent),
        BigFloat.minNormal(format.precision, format.minExponent),
        new BigFloat(0.1f, format),
        new BigFloat(1.0f, format),
        new BigFloat(17.0f, format),
        BigFloat.maxValue(format.precision, format.maxExponent),
        BigFloat.positiveInfinity(format.precision));
    return builder.build();
  }

  // Generate a list of powers ca^px where c,p are incremented starting from 1 and a,x are constants
  protected static List<BigFloat> floatPowers(
      BinaryMathContext format, int c, BigFloat a, int p, BigFloat x) {
    ImmutableList.Builder<BigFloat> builder = ImmutableList.builder();
    for (int i = 1; i <= c; i++) {
      for (int j = -p; j <= p; j++) {
        BigFloat t1 = new BigFloat(i, format).multiply(a, format);
        BigFloat t2 = new BigFloat(p, format).multiply(x, format);
        BigFloat r = t1.pow(t2, format);
        if (!r.equalTo(new BigFloat(1, format))) {
          builder.add(r);
        }
      }
    }
    return builder.build();
  }

  // Generate n random floating point values
  protected static List<BigFloat> floatRandom(BinaryMathContext format, int n) {
    ImmutableList.Builder<BigFloat> builder = ImmutableList.builder();
    Random random = new Random(0);
    for (int i = 0; i < n; i++) {
      boolean sign = random.nextBoolean();
      long exponent = random.nextLong(2 * format.maxExponent) - format.maxExponent;
      BigInteger leading = BigInteger.ONE.shiftLeft(format.precision - 1);
      if (exponent < format.minExponent) { // Special case for subnormal numbers
        exponent = format.minExponent;
        leading = BigInteger.ZERO;
      }
      BigInteger significand = leading.add(new BigInteger(format.precision - 1, random));
      builder.add(new BigFloat(sign, significand, exponent, format));
    }
    return builder.build();
  }

  protected List<BigFloat> unaryTestValues() {
    BinaryMathContext format = getFloatType();
    BigFloat constant = new BigFloat(0.5f, format);
    ImmutableList.Builder<BigFloat> builder = ImmutableList.builder();
    builder.addAll(floatConsts(format));
    builder.addAll(floatPowers(format, 14, constant, 20, constant));
    builder.addAll(floatRandom(format, 50000));
    return builder.build();
  }

  protected List<BigFloat> binaryTestValues() {
    BinaryMathContext format = getFloatType();
    BigFloat constant = new BigFloat(0.5f, format);
    ImmutableList.Builder<BigFloat> builder = ImmutableList.builder();
    builder.addAll(floatConsts(format));
    builder.addAll(floatPowers(format, 3, constant, 3, constant));
    builder.addAll(floatRandom(format, 200));
    return builder.build();
  }

  // Wraps a generated input value along with the result of the operation
  private static class TestValue<T> {
    private final BigFloat[] args;
    private final T expected;

    public TestValue(BigFloat arg, T result) {
      args = new BigFloat[] {arg};
      expected = result;
    }

    public TestValue(BigFloat arg1, BigFloat arg2, T result) {
      args = new BigFloat[] {arg1, arg2};
      expected = result;
    }

    BigFloat arg1() {
      return args[0];
    }

    BigFloat arg2() {
      return args[1];
    }

    T result() {
      return expected;
    }
  }

  private double lb(double number) {
    return Math.log(number) / Math.log(2);
  }

  private int calculateExpWidth(BinaryMathContext pFormat) {
    return (int) Math.ceil(lb(2*pFormat.maxExponent+1));
  }

  private String toBits(BigFloat value) {
    String sign = value.sign() ? "1" : "0";
    long valueExp =
        value.exponent(getFloatType().minExponent, getFloatType().maxExponent)
            + getFloatType().maxExponent;
    String exponent = Long.toString(valueExp, 2);
    exponent = "0".repeat(calculateExpWidth(getFloatType()) - exponent.length()) + exponent;
    String significand = BigInteger.ONE.shiftLeft(getFloatType().precision - 2).toString(2);
    if (!value.isNaN()) {
      // Get the actual significand if the value is not NaN
      String repr =
          value.significand(getFloatType().minExponent, getFloatType().maxExponent).toString(2);
      repr = "0".repeat(getFloatType().precision + 1 - repr.length()) + repr;
      significand = repr.substring(1);
    }
    return String.format("%s %s %s", sign, exponent, significand);
  }

  protected String printValue(BigFloat value) {
    return String.format("%s [%s]", toBits(value), printBigFloat(value));
  }

  private String printTestHeader(String name, BigFloat arg) {
    return String.format("%n%nTestcase %s(%s): ", name, printValue(arg));
  }

  private String printTestHeader(String name, BigFloat arg1, BigFloat arg2) {
    return String.format("%n%nTestcase %s(%s, %s): ", name, printValue(arg1), printValue(arg2));
  }

  // Defines the maximum error (in ULPs) for transcendent functions
  protected int ulpError() {
    return getRefImpl() == ReferenceImpl.MPFR ? 0 : 1;
  }

  // Returns a list of all float values in the error range
  private List<String> errorRange(int pDistance, BigFloat pValue) {
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    builder.add(printValue(pValue));
    return builder.build();

    /* FIXME: Translate to BigFloat
    Preconditions.checkArgument(pDistance >= 0);
    if (pValue.isNaN()) {
      return ImmutableList.of(printValue(pValue));
    }
    float ulp = Math.ulp(pValue);
    if (pValue == 0.0f) { // for zero we look at the closest subnormal numbers
      ulp = Float.MIN_VALUE;
    }
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    for (int p = -pDistance; p <= pDistance; p++) {
      float value = p == 0 ? pValue : pValue + p * ulp; // adding 0 messes up the sign for -0.0
      builder.add(printValue(value));
    }
    return builder.build();
    */
  }

  protected void testOperator(String name, int ulps, UnaryOperator<CFloat> operator) {
    ImmutableList.Builder<TestValue<BigFloat>> testBuilder = ImmutableList.builder();
    for (BigFloat arg : unaryTestValues()) {
      CFloat ref = toReferenceImpl(toPlainString(arg));
      BigFloat result = operator.apply(ref).toBigFloat();
      testBuilder.add(new TestValue<>(arg, result));
    }
    ImmutableList<TestValue<BigFloat>> testCases = testBuilder.build();
    ImmutableList.Builder<String> logBuilder = ImmutableList.builder();
    for (TestValue<BigFloat> test : testCases) {
      try {
        String testHeader = printTestHeader(name, test.arg1());
        CFloat tested = toTestedImpl(toPlainString(test.arg1()));
        BigFloat result = BigFloat.NaN(getFloatType().precision);
        try {
          result = operator.apply(tested).toBigFloat();
        } catch (Throwable t) {
          assertWithMessage(testHeader + t).fail();
        }
        assertWithMessage(testHeader)
            .that(printValue(result))
            .isIn(errorRange(ulps, test.result()));
      } catch (AssertionError e) {
        logBuilder.add(e.getMessage());
      }
    }
    ImmutableList<String> errorLog = logBuilder.build();
    if (!errorLog.isEmpty()) {
      assertWithMessage(
              "Failed on %s (out of %s) test inputs:%s",
              errorLog.size(), testCases.size(), errorLog)
          .fail();
    }
  }

  protected void testOperator(String name, int ulps, BinaryOperator<CFloat> operator) {
    ImmutableList.Builder<TestValue<BigFloat>> testBuilder = ImmutableList.builder();
    for (BigFloat arg1 : binaryTestValues()) {
      for (BigFloat arg2 : binaryTestValues()) {
        CFloat ref1 = toReferenceImpl(toPlainString(arg1));
        CFloat ref2 = toReferenceImpl(toPlainString(arg2));
        BigFloat result = operator.apply(ref1, ref2).toBigFloat();
        testBuilder.add(new TestValue<>(arg1, arg2, result));
      }
    }
    ImmutableList<TestValue<BigFloat>> testCases = testBuilder.build();
    ImmutableList.Builder<String> logBuilder = ImmutableList.builder();
    for (TestValue<BigFloat> test : testCases) {
      try {
        String testHeader = printTestHeader(name, test.arg1(), test.arg2());
        CFloat tested1 = toTestedImpl(toPlainString(test.arg1()));
        CFloat tested2 = toTestedImpl(toPlainString(test.arg2()));
        BigFloat result = BigFloat.NaN(getFloatType().precision);
        try {
          result = operator.apply(tested1, tested2).toBigFloat();
        } catch (Throwable t) {
          assertWithMessage(testHeader + t).fail();
        }
        assertWithMessage(testHeader)
            .that(printValue(result))
            .isIn(errorRange(ulps, test.result()));
      } catch (AssertionError e) {
        logBuilder.add(e.getMessage());
      }
    }
    ImmutableList<String> errorLog = logBuilder.build();
    if (!errorLog.isEmpty()) {
      assertWithMessage(
              "Failed on %s (out of %s) test inputs:%s",
              errorLog.size(), testCases.size(), errorLog)
          .fail();
    }
  }

  protected void testPredicate(String name, Predicate<CFloat> predicate) {
    ImmutableList.Builder<TestValue<Boolean>> testBuilder = ImmutableList.builder();
    for (BigFloat arg : unaryTestValues()) {
      CFloat ref = toReferenceImpl(toPlainString(arg));
      boolean result = predicate.test(ref);
      testBuilder.add(new TestValue<>(arg, result));
    }
    ImmutableList<TestValue<Boolean>> testCases = testBuilder.build();
    ImmutableList.Builder<String> logBuilder = ImmutableList.builder();
    for (TestValue<Boolean> test : testCases) {
      try {
        String testHeader = printTestHeader(name, test.arg1());
        CFloat tested = toTestedImpl(toPlainString(test.arg1()));
        boolean result = true;
        try {
          result = predicate.test(tested);
        } catch (Throwable t) {
          assertWithMessage(testHeader + t).fail();
        }
        assertWithMessage(testHeader).that(result).isEqualTo(test.result());
      } catch (AssertionError e) {
        logBuilder.add(e.getMessage());
      }
    }
    ImmutableList<String> errorLog = logBuilder.build();

    if (!errorLog.isEmpty()) {
      assertWithMessage(
              "Failed on %s (out of %s) test inputs:%s",
              errorLog.size(), testCases.size(), errorLog)
          .fail();
    }
  }

  protected void testPredicate(String name, BinaryPredicate<CFloat, CFloat> predicate) {
    ImmutableList.Builder<TestValue<Boolean>> testBuilder = ImmutableList.builder();
    for (BigFloat arg1 : binaryTestValues()) {
      for (BigFloat arg2 : binaryTestValues()) {
        CFloat ref1 = toReferenceImpl(toPlainString(arg1));
        CFloat ref2 = toReferenceImpl(toPlainString(arg2));
        boolean result = predicate.apply(ref1, ref2);
        testBuilder.add(new TestValue<>(arg1, arg2, result));
      }
    }
    ImmutableList<TestValue<Boolean>> testCases = testBuilder.build();
    ImmutableList.Builder<String> logBuilder = ImmutableList.builder();
    for (TestValue<Boolean> test : testCases) {
      try {
        String testHeader = printTestHeader(name, test.arg1(), test.arg2());
        CFloat tested1 = toTestedImpl(toPlainString(test.arg1()));
        CFloat tested2 = toTestedImpl(toPlainString(test.arg2()));
        boolean result = true;
        try {
          result = predicate.apply(tested1, tested2);
        } catch (Throwable t) {
          assertWithMessage(testHeader + t).fail();
        }
        assertWithMessage(testHeader).that(result).isEqualTo(test.result());
      } catch (AssertionError e) {
        logBuilder.add(e.getMessage());
      }
    }
    ImmutableList<String> errorLog = logBuilder.build();
    if (!errorLog.isEmpty()) {
      assertWithMessage(
              "Failed on %s (out of %s) test inputs:%s",
              errorLog.size(), testCases.size(), errorLog)
          .fail();
    }
  }

  protected void testIntegerFunction(String name, Function<CFloat, Number> function) {
    ImmutableList.Builder<TestValue<Number>> testBuilder = ImmutableList.builder();
    for (BigFloat arg : unaryTestValues()) {
      CFloat ref = toReferenceImpl(toPlainString(arg));
      Number result = function.apply(ref);
      testBuilder.add(new TestValue<>(arg, result));
    }
    ImmutableList<TestValue<Number>> testCases = testBuilder.build();
    ImmutableList.Builder<String> logBuilder = ImmutableList.builder();
    for (TestValue<Number> test : testCases) {
      try {
        String testHeader = printTestHeader(name, test.arg1());
        CFloat tested = toTestedImpl(toPlainString(test.arg1()));
        Number result = null;
        try {
          result = function.apply(tested);
        } catch (Throwable t) {
          assertWithMessage(testHeader + t).fail();
        }
        assertWithMessage(testHeader).that(result).isEqualTo(test.result());
      } catch (AssertionError e) {
        logBuilder.add(e.getMessage());
      }
    }
    ImmutableList<String> errorLog = logBuilder.build();

    if (!errorLog.isEmpty()) {
      assertWithMessage(
              "Failed on %s (out of %s) test inputs:%s",
              errorLog.size(), testCases.size(), errorLog)
          .fail();
    }
  }

  protected void assertEqual1Ulp(CFloat r1, CFloat r2) {
    assertThat(printValue(r1.toBigFloat())).isIn(errorRange(ulpError(), r2.toBigFloat()));
  }

  protected abstract CFloat toTestedImpl(String repr);

  protected abstract CFloat toReferenceImpl(String repr);

  // (This test is here to check that parsing and printing of values is handles correctly by the
  // implementation)
  @Test
  public void constTest() {
    testOperator("id", 0, (CFloat a) -> a);
  }

  @Test
  public void addTest() {
    testOperator("add", 0, (CFloat a, CFloat b) -> a.add(b));
  }

  @Test
  public void addManyTest() {
    testOperator("addManyTest", 0, (CFloat a, CFloat b) -> b.add(a, b));
  }

  @Test
  public void multiplyTest() {
    testOperator("multiply", 0, (CFloat a, CFloat b) -> a.multiply(b));
  }

  @Test
  public void multiplyManyTest() {
    testOperator("multiplyManyTest", 0, (CFloat a, CFloat b) -> b.multiply(a, b));
  }

  @Test
  public void subtractTest() {
    testOperator("subtract", 0, (CFloat a, CFloat b) -> a.subtract(b));
  }

  @Test
  public void divideByTest() {
    testOperator("divideBy", 0, (CFloat a, CFloat b) -> a.divideBy(b));
  }

  @Test
  public void lnTest() {
    testOperator("ln", ulpError(), (CFloat a) -> a.ln());
  }

  @Test
  public void expTest() {
    testOperator("exp", ulpError(), (CFloat a) -> a.exp());
  }

  @Test
  public void powToTest() {
    testOperator("powTo", ulpError(), (CFloat a, CFloat b) -> a.powTo(b));
  }

  @Test
  public void powToIntegralTest() {
    testOperator("powToInteger", 0, (CFloat a, CFloat b) -> a.powToIntegral(b.toInteger()));
  }

  @Test
  public void sqrtTest() {
    testOperator("sqrt", 0, (CFloat a) -> a.sqrt());
  }

  @Test
  public void roundTest() {
    testOperator("round", 0, (CFloat a) -> a.round());
  }

  @Test
  public void truncTest() {
    testOperator("trunc", 0, (CFloat a) -> a.trunc());
  }

  @Test
  public void ceilTest() {
    testOperator("ceil", 0, (CFloat a) -> a.ceil());
  }

  @Test
  public void floorTest() {
    testOperator("floor", 0, (CFloat a) -> a.floor());
  }

  @Test
  public void absTest() {
    testOperator("abs", 0, (CFloat a) -> a.abs());
  }

  @Test
  public void isZeroTest() {
    testPredicate("isZero", (CFloat a) -> a.isZero());
  }

  @Test
  public void isOneTest() {
    testPredicate("isOne", (CFloat a) -> a.isOne());
  }

  @Test
  public void isNanTest() {
    testPredicate("isNan", (CFloat a) -> a.isNan());
  }

  @Test
  public void isInfinityTest() {
    testPredicate("isInfinity", (CFloat a) -> a.isInfinity());
  }

  @Test
  public void isNegativeTest() {
    testPredicate("isNegative", (CFloat a) -> a.isNegative());
  }

  @Test
  public void greaterThanTest() {
    testPredicate("greaterThan", (CFloat a, CFloat b) -> a.greaterThan(b));
  }

  @Test
  public void copySignFromTest() {
    testOperator("copySignFrom", 0, (CFloat a, CFloat b) -> a.copySignFrom(b));
  }

  @Test
  public void castToTest() {
    testOperator(
        "castToTest", 0, (CFloat a) -> a.castTo(CNativeType.DOUBLE).castTo(CNativeType.SINGLE));
  }

  @Test
  public void castToRoundingTest() {
    testOperator(
        "castToRoundingTest",
        0,
        (CFloat a) -> a.castTo(CNativeType.DOUBLE).sqrt().castTo(CNativeType.SINGLE));
  }

  @Test
  public void castToByteTest() {
    testIntegerFunction("castToByteTest", (CFloat a) -> a.castToOther(CNativeType.CHAR));
  }

  @Test
  public void castToShortTest() {
    testIntegerFunction("castToShortTest", (CFloat a) -> a.castToOther(CNativeType.SHORT));
  }

  @Test
  public void castToIntTest() {
    testIntegerFunction("castToIntTest", (CFloat a) -> a.castToOther(CNativeType.INT));
  }

  @Test
  public void castToLongTest() {
    testIntegerFunction("castToLongTest", (CFloat a) -> a.castToOther(CNativeType.LONG));
  }
}
