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
import java.util.List;
import java.util.Random;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import org.junit.Test;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloat;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CNativeType;

// Like CFloatUnitTest, but for double precision
public abstract class CDoubleUnitTest {
  protected int floatType = CFloatNativeAPI.FP_TYPE_DOUBLE; // TODO: Add other float types

  // Convert floating point value to its decimal representation
  private static String toPlainString(double fpVal) {
    if (Double.isNaN(fpVal)) {
      return "nan";
    }
    if (Double.isInfinite(fpVal)) {
      return Double.compare(fpVal, 0.0f) < 0 ? "-inf" : "inf";
    }
    if (fpVal == -0.0f) {
      return "-0.0";
    }
    return new BigDecimal(fpVal).toPlainString();
  }

  // Generate a list of floating point constants that cover all special case values
  protected static List<Double> floatConsts() {
    ImmutableList.Builder<Double> builder = ImmutableList.builder();
    builder.add(
        Double.NEGATIVE_INFINITY,
        -Double.MAX_VALUE,
        -17.0d,
        -1.0d,
        -0.1d,
        -Double.MIN_NORMAL,
        -Double.MIN_VALUE,
        -0.0d,
        Double.NaN,
        0.0d,
        Double.MIN_VALUE,
        Double.MIN_NORMAL,
        0.1d,
        1.0d,
        17.0d,
        Double.MAX_VALUE,
        Double.POSITIVE_INFINITY);
    return builder.build();
  }

  // Generate a list of powers ca^px where c,p are incremented starting from 1 and a,x are constants
  protected static List<Double> floatPowers(int c, Double a, int p, Double x) {
    ImmutableList.Builder<Double> builder = ImmutableList.builder();
    for (int i = 1; i <= c; i++) {
      for (int j = -p; j <= p; j++) {
        Double val = Math.pow(i * a, j * x);
        if (val != 1.0f) {
          builder.add(val);
        }
      }
    }
    return builder.build();
  }

  // Generate n random floating point values
  protected static List<Double> floatRandom(int n) {
    ImmutableList.Builder<Double> builder = ImmutableList.builder();
    Random randomNumbers = new Random(0);
    int i = 0;
    while (i < n) {
      Double flt = randomNumbers.nextDouble();
      if (!Double.isNaN(flt) && !Double.isInfinite(flt)) {
        builder.add(flt);
        i++;
      }
    }
    return builder.build();
  }

  protected static List<Double> binaryTestValues() {
    ImmutableList.Builder<Double> builder = ImmutableList.builder();
    builder.addAll(floatConsts());
    builder.addAll(floatPowers(3, 0.5d, 3, 0.5d));
    builder.addAll(floatRandom(200));
    return builder.build();
  }

  protected static List<Double> unaryTestValues() {
    ImmutableList.Builder<Double> builder = ImmutableList.builder();
    builder.addAll(floatConsts());
    builder.addAll(floatPowers(14, 0.5d, 20, 0.5d));
    builder.addAll(floatRandom(50000));
    return builder.build();
  }

  // Wraps a generated input value along with the result of the operation
  private static class TestValue<T> {
    private final Double[] args;
    private final T expected;

    public TestValue(Double arg, T result) {
      args = new Double[] {arg};
      expected = result;
    }

    public TestValue(Double arg1, Double arg2, T result) {
      args = new Double[] {arg1, arg2};
      expected = result;
    }

    Double arg1() {
      return args[0];
    }

    Double arg2() {
      return args[1];
    }

    T result() {
      return expected;
    }
  }

  private String toBits(Double value) {
    String repr = Long.toUnsignedString(Double.doubleToLongBits(value), 2);
    repr = "0".repeat(64 - repr.length()) + repr;
    return String.format(
        "%s %s %s", repr.substring(0, 1), repr.substring(1, 12), repr.substring(12));
  }

  protected String printValue(Double value) {
    return String.format("%s [%s]", toBits(value), value);
  }

  private String printTestHeader(String name, Double arg) {
    return String.format("%n%nTestcase %s(%s): ", name, printValue(arg));
  }

  private String printTestHeader(String name, Double arg1, Double arg2) {
    return String.format("%n%nTestcase %s(%s, %s): ", name, printValue(arg1), printValue(arg2));
  }

  protected void testOperator(String name, UnaryOperator<CFloat> operator) {
    ImmutableList.Builder<TestValue<Double>> testBuilder = ImmutableList.builder();
    for (Double arg : unaryTestValues()) {
      CFloat ref = toReferenceImpl(toPlainString(arg), floatType);
      Double result = operator.apply(ref).toDouble();
      testBuilder.add(new TestValue<>(arg, result));
    }
    ImmutableList<TestValue<Double>> testCases = testBuilder.build();
    ImmutableList.Builder<String> logBuilder = ImmutableList.builder();
    for (TestValue<Double> test : testCases) {
      try {
        String testHeader = printTestHeader(name, test.arg1());
        CFloat tested = toTestedImpl(toPlainString(test.arg1()), floatType);
        Double result = Double.NaN;
        try {
          result = operator.apply(tested).toDouble();
        } catch (Throwable t) {
          assertWithMessage(testHeader + t).fail();
        }
        assertWithMessage(testHeader).that(printValue(result)).isEqualTo(printValue(test.result()));
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

  protected void testOperator(String name, BinaryOperator<CFloat> operator) {
    ImmutableList.Builder<TestValue<Double>> testBuilder = ImmutableList.builder();
    for (Double arg1 : binaryTestValues()) {
      for (Double arg2 : binaryTestValues()) {
        CFloat ref1 = toReferenceImpl(toPlainString(arg1), floatType);
        CFloat ref2 = toReferenceImpl(toPlainString(arg2), floatType);
        Double result = operator.apply(ref1, ref2).toDouble();
        testBuilder.add(new TestValue<>(arg1, arg2, result));
      }
    }
    ImmutableList<TestValue<Double>> testCases = testBuilder.build();
    ImmutableList.Builder<String> logBuilder = ImmutableList.builder();
    for (TestValue<Double> test : testCases) {
      try {
        String testHeader = printTestHeader(name, test.arg1(), test.arg2());
        CFloat tested1 = toTestedImpl(toPlainString(test.arg1()), floatType);
        CFloat tested2 = toTestedImpl(toPlainString(test.arg2()), floatType);
        Double result = Double.NaN;
        try {
          result = operator.apply(tested1, tested2).toDouble();
        } catch (Throwable t) {
          assertWithMessage(testHeader + t).fail();
        }
        assertWithMessage(testHeader).that(printValue(result)).isEqualTo(printValue(test.result()));
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
    for (Double arg : unaryTestValues()) {
      CFloat ref = toReferenceImpl(toPlainString(arg), floatType);
      boolean result = predicate.test(ref);
      testBuilder.add(new TestValue<>(arg, result));
    }
    ImmutableList<TestValue<Boolean>> testCases = testBuilder.build();
    ImmutableList.Builder<String> logBuilder = ImmutableList.builder();
    for (TestValue<Boolean> test : testCases) {
      try {
        String testHeader = printTestHeader(name, test.arg1());
        CFloat tested = toTestedImpl(toPlainString(test.arg1()), floatType);
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
    for (Double arg1 : binaryTestValues()) {
      for (Double arg2 : binaryTestValues()) {
        CFloat ref1 = toReferenceImpl(toPlainString(arg1), floatType);
        CFloat ref2 = toReferenceImpl(toPlainString(arg2), floatType);
        boolean result = predicate.apply(ref1, ref2);
        testBuilder.add(new TestValue<>(arg1, arg2, result));
      }
    }
    ImmutableList<TestValue<Boolean>> testCases = testBuilder.build();
    ImmutableList.Builder<String> logBuilder = ImmutableList.builder();
    for (TestValue<Boolean> test : testCases) {
      try {
        String testHeader = printTestHeader(name, test.arg1(), test.arg2());
        CFloat tested1 = toTestedImpl(toPlainString(test.arg1()), floatType);
        CFloat tested2 = toTestedImpl(toPlainString(test.arg2()), floatType);
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
    for (Double arg : unaryTestValues()) {
      CFloat ref = toReferenceImpl(toPlainString(arg), floatType);
      Number result = function.apply(ref);
      testBuilder.add(new TestValue<>(arg, result));
    }
    ImmutableList<TestValue<Number>> testCases = testBuilder.build();
    ImmutableList.Builder<String> logBuilder = ImmutableList.builder();
    for (TestValue<Number> test : testCases) {
      try {
        String testHeader = printTestHeader(name, test.arg1());
        CFloat tested = toTestedImpl(toPlainString(test.arg1()), floatType);
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

  protected void assertEqual(CFloat r1, CFloat r2) {
    assertThat(printValue(r1.toDouble())).isEqualTo(printValue(r2.toDouble()));
  }

  public abstract CFloat toTestedImpl(String repr, int pFloatType);

  public abstract CFloat toReferenceImpl(String repr, int pFloatType);

  // (This test is here to check that parsing and printing of values is handles correctly by the
  // implementation)
  @Test
  public void constTest() {
    testOperator("id", (CFloat a) -> a);
  }

  @Test
  public void addTest() {
    testOperator("add", (CFloat a, CFloat b) -> a.add(b));
  }

  @Test
  public void addManyTest() {
    testOperator("addManyTest", (CFloat a, CFloat b) -> b.add(a, b));
  }

  @Test
  public void multiplyTest() {
    testOperator("multiply", (CFloat a, CFloat b) -> a.multiply(b));
  }

  @Test
  public void multiplyManyTest() {
    testOperator("multiplyManyTest", (CFloat a, CFloat b) -> b.multiply(a, b));
  }

  @Test
  public void subtractTest() {
    testOperator("subtract", (CFloat a, CFloat b) -> a.subtract(b));
  }

  @Test
  public void divideByTest() {
    testOperator("divideBy", (CFloat a, CFloat b) -> a.divideBy(b));
  }

  @Test
  public void lnTest() {
    testOperator("ln", (CFloat a) -> a.ln());
  }

  @Test
  public void expTest() {
    testOperator("exp", (CFloat a) -> a.exp());
  }

  @Test
  public void powToTest() {
    testOperator("powTo", (CFloat a, CFloat b) -> a.powTo(b));
  }

  @Test
  public void powToIntegralTest() {
    testOperator("powToInteger", (CFloat a, CFloat b) -> a.powToIntegral(b.toInteger()));
  }

  @Test
  public void sqrtTest() {
    testOperator("sqrt", (CFloat a) -> a.sqrt());
  }

  @Test
  public void roundTest() {
    testOperator("round", (CFloat a) -> a.round());
  }

  @Test
  public void truncTest() {
    testOperator("trunc", (CFloat a) -> a.trunc());
  }

  @Test
  public void ceilTest() {
    testOperator("ceil", (CFloat a) -> a.ceil());
  }

  @Test
  public void floorTest() {
    testOperator("floor", (CFloat a) -> a.floor());
  }

  @Test
  public void absTest() {
    testOperator("abs", (CFloat a) -> a.abs());
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
    testOperator("copySignFrom", (CFloat a, CFloat b) -> a.copySignFrom(b));
  }

  @Test
  public void castToTest() {
    testOperator(
        "castToTest", (CFloat a) -> a.castTo(CNativeType.LONG_DOUBLE).castTo(CNativeType.DOUBLE));
  }

  @Test
  public void castToRoundingTest() {
    testOperator(
        "castToRoundingTest",
        (CFloat a) -> a.castTo(CNativeType.LONG_DOUBLE).sqrt().castTo(CNativeType.DOUBLE));
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
