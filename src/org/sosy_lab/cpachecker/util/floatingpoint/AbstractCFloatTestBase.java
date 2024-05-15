// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.common.truth.TruthJUnit.assume;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Correspondence.BinaryPredicate;
import com.google.common.truth.StringSubject;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Test;
import org.kframework.mpfr.BigFloat;
import org.kframework.mpfr.BinaryMathContext;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CNativeType;

/**
 * Abstract test class for the {@link CFloat} interface.
 *
 * <p>The idea behind this class is to compare two different implementations of the CFloat
 * interface. We generally use {@link FloatP} as the "tested" implementation and compare it to a
 * second "reference" implementation. There are currently 3 supported implementations that can be
 * used as a reference:
 *
 * <ul>
 *   <li>{@link CFloatNative}, uses native C code to calculate its results
 *   <li>{@link MpfrFloat}, uses BigFloats to access MPFR
 *   <li>{@link JFloat} ({@link JDouble}), uses normal Java floats (doubles) for its calculations
 * </ul>
 *
 * <p>Subclasses are expected to overload the abstract methods {@link
 * AbstractCFloatTestBase#getRefImpl()}, {@link AbstractCFloatTestBase#toTestedImpl(String)} and
 * {@link AbstractCFloatTestBase#toReferenceImpl(String)} to select which implementations are
 * supposed to be used in the comparison. The test class will then automatically generate test
 * inputs for all methods of the CFloat interface and compare the results of the tested
 * implementation with those of the reference implementation on all of those inputs.
 *
 * <p>The methods {@link AbstractCFloatTestBase#unaryTestValues()} and {@link
 * AbstractCFloatTestBase#binaryTestValues()} can be overwritten to change the set of test inputs
 * that will be generated. The following classes of test values are supported by the implementation:
 *
 * <ul>
 *   <li>{@link AbstractCFloatTestBase#floatConsts(BinaryMathContext)}
 *   <li>{@link AbstractCFloatTestBase#floatPowers(BinaryMathContext, int, BigFloat, int, BigFloat)}
 *   <li>{@link AbstractCFloatTestBase#floatRandom(BinaryMathContext, int)}
 *   <li>{@link AbstractCFloatTestBase#allFloats(BinaryMathContext)}
 * </ul>
 *
 * <p>The default behaviour for both {@link AbstractCFloatTestBase#unaryTestValues()} and {@link
 * AbstractCFloatTestBase#binaryTestValues()} is to use a combination of the first 3 test value
 * classes.
 *
 * <p>The abstract method {@link AbstractCFloatTestBase#getFloatType()} also needs to be overridden
 * by the subclass to select the bit width of the floating point values that will be generated.
 */
abstract class AbstractCFloatTestBase {
  /** Override to set a floating point width. */
  protected abstract BinaryMathContext getFloatType();

  /** List of all supported reference implementations. */
  enum ReferenceImpl {
    MPFR,
    JAVA,
    NATIVE
  }

  /** Return the reference implementation used by the test. */
  protected abstract ReferenceImpl getRefImpl();

  protected BigFloat toBigFloat(CFloat value) {
    if (value instanceof MpfrFloat val) {
      return val.toBigFloat();
    } else if (value instanceof  CFloatImpl val) {
      int sigBits = val.getValue().getFormat().sigBits();
      int expBits = val.getValue().getFormat().expBits();
      BinaryMathContext context = new BinaryMathContext(sigBits + 1, expBits);
        if (val.isNan()) {
          return val.isNegative()
                 ? BigFloat.NaN(context.precision).negate()
                 : BigFloat.NaN(context.precision);
        }
        if (val.isInfinity()) {
          return val.isNegative()
                 ? BigFloat.negativeInfinity(context.precision)
                 : BigFloat.positiveInfinity(context.precision);
        }
        return new BigFloat(val.isNegative(), val.getValue().extractSigBits(), val.getValue().extractExpBits(), context);
    } else {
      // We have either a CFloatNative or a JFloat/JDouble and only need to support single and
      // double precision
      CNativeType toType = CFloatNativeAPI.toNativeType(value.getType());
      return switch (toType) {
        case SINGLE -> new BigFloat(value.toFloat(), BinaryMathContext.BINARY32);
        case DOUBLE -> new BigFloat(value.toDouble(), BinaryMathContext.BINARY64);
        case LONG_DOUBLE -> throw new UnsupportedOperationException();
        default -> throw new IllegalArgumentException();
      };
    }
  }

  protected Integer toInteger(CFloat value) {
    return toBigFloat(value).intValue();
  }

  /** Pretty printer for BigFloat type */
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

  /** Convert floating point value to its decimal representation. */
  public static String toPlainString(BigFloat value) {
    String r = printBigFloat(value);
    if (r.contains("e")) {
      r = new BigDecimal(r).toPlainString();
    }
    return r;
  }

  /** Generate a list of floating point constants that cover all special case values. */
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

  /**
   * Generate a list of powers ca^px where c,p are incremented starting from 1 and a,x are
   * constants.
   */
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

  /** Generate n random floating point values. */
  protected static List<BigFloat> floatRandom(BinaryMathContext format, int n) {
    ImmutableList.Builder<BigFloat> builder = ImmutableList.builder();
    Random random = new Random(0);
    int i = 0;
    while (i < n) {
      boolean sign = random.nextBoolean();
      long exponent = random.nextLong(2 * format.maxExponent) - format.maxExponent;
      BigInteger leading = BigInteger.ONE.shiftLeft(format.precision - 1);
      if (exponent < format.minExponent) { // Special case for subnormal numbers
        leading = BigInteger.ZERO;
      }
      BigInteger significand = leading.add(new BigInteger(format.precision - 1, random));
      BigFloat value = new BigFloat(sign, significand, exponent, format);
      if (!value.isPositiveZero() || !value.isNegativeZero()) {
        builder.add(new BigFloat(sign, significand, exponent, format));
        i++;
      }
    }
    return builder.build();
  }

  /** Generate all possible floating point values for a given precision. */
  protected static List<BigFloat> allFloats(BinaryMathContext format) {
    ImmutableList.Builder<BigFloat> builder = ImmutableList.builder();
    for (long exponent = format.minExponent - 1; exponent <= format.maxExponent + 1; exponent++) {
      BigInteger leading = BigInteger.ONE.shiftLeft(format.precision - 1);
      if (exponent < format.minExponent || exponent > format.maxExponent) {
        leading = BigInteger.ZERO;
      }
      int maxValue = (2 << (format.precision - 2));
      for (int i = 0; i < maxValue; i++) {
        if (exponent > format.maxExponent && i > 1) {
          // Only generate one NaN value
          continue;
        }
        BigInteger significand = leading.add(BigInteger.valueOf(i));
        builder.add(new BigFloat(false, significand, exponent, format));
        builder.add(new BigFloat(true, significand, exponent, format));
      }
    }
    return builder.build();
  }

  /** The set of test inputs that should be used for unary operations in the CFloat interface. */
  protected List<BigFloat> unaryTestValues() {
    BinaryMathContext format = getFloatType();
    BigFloat constant = new BigFloat(0.5f, format);
    return ImmutableList.<BigFloat>builder()
        .addAll(floatConsts(format))
        .addAll(floatPowers(format, 14, constant, 20, constant))
        .addAll(floatRandom(format, 50000))
        .build();
  }

  /**
   * The set of test inputs that should be used for binary operations in the CFloat interface. The
   * values will be used for both arguments separately. So if there are k test values in this set,
   * the number of test runs for a binary operation will be k^2.
   */
  protected List<BigFloat> binaryTestValues() {
    BinaryMathContext format = getFloatType();
    BigFloat constant = new BigFloat(0.5f, format);
    return ImmutableList.<BigFloat>builder()
        .addAll(floatConsts(format))
        .addAll(floatPowers(format, 3, constant, 3, constant))
        .addAll(floatRandom(format, 200))
        .build();
  }

  /** Wraps a generated input value along with the result of the operation. */
  private record TestValue<T>(BigFloat arg1, @Nullable BigFloat arg2, T result) {
    public TestValue(BigFloat pArg1, T pResult) {
      this(pArg1, null, pResult);
    }
  }

  private static int calculateExpWidth(BinaryMathContext pFormat) {
    BigInteger val = BigInteger.valueOf(2 * pFormat.maxExponent + 1);
    int r = val.bitLength() - 1;
    return val.bitCount() > 1 ? r + 1 : r;
  }

  private static String toBits(BinaryMathContext format, BigFloat value) {
    String sign = value.sign() ? "1" : "0";
    long valueExp = value.exponent(format.minExponent, format.maxExponent) + format.maxExponent;
    String exponent = Long.toString(valueExp, 2);
    exponent = "0".repeat(calculateExpWidth(format) - exponent.length()) + exponent;
    String significand = BigInteger.ONE.shiftLeft(format.precision - 2).toString(2);
    if (!value.isNaN()) {
      // Get the actual significand if the value is not NaN
      String repr = value.significand(format.minExponent, format.maxExponent).toString(2);
      repr = "0".repeat(format.precision - repr.length()) + repr;
      significand = repr.substring(1);
    }
    return String.format("%s %s %s", sign, exponent, significand);
  }

  protected String printValue(BigFloat value) {
    return String.format("%s [%s]", toBits(getFloatType(), value), printBigFloat(value));
  }

  private String printTestHeader(String name, BigFloat arg) {
    return String.format("%n%nTestcase %s(%s): ", name, printValue(arg));
  }

  private String printTestHeader(String name, BigFloat arg1, BigFloat arg2) {
    return String.format("%n%nTestcase %s(%s, %s): ", name, printValue(arg1), printValue(arg2));
  }

  /** Defines the maximum error (in ULPs) for transcendental functions. */
  protected int ulpError() {
    return getRefImpl() == ReferenceImpl.MPFR ? 0 : 1;
  }

  /** Returns a list of all float values in the error range. */
  private List<String> errorRange(int pDistance, BigFloat pValue) {
    if (pDistance == 0 || pValue.isNaN()) {
      return ImmutableList.of(printValue(pValue));
    }
    if (pDistance == 1) {
      BigFloat minus1Ulp = pValue.nextDown(getFloatType().minExponent, getFloatType().maxExponent);
      BigFloat plus1Ulp = pValue.nextUp(getFloatType().minExponent, getFloatType().maxExponent);

      return ImmutableList.of(printValue(minus1Ulp), printValue(pValue), printValue(plus1Ulp));
    }
    throw new IllegalArgumentException();
  }

  protected void testOperator(String name, int ulps, UnaryOperator<CFloat> operator) {
    ImmutableList.Builder<TestValue<BigFloat>> testBuilder = ImmutableList.builder();
    for (BigFloat arg : unaryTestValues()) {
      CFloat ref = toReferenceImpl(arg);
      BigFloat result = toBigFloat(operator.apply(ref));
      testBuilder.add(new TestValue<>(arg, result));
    }
    ImmutableList<TestValue<BigFloat>> testCases = testBuilder.build();
    ImmutableList.Builder<String> logBuilder = ImmutableList.builder();
    for (TestValue<BigFloat> test : testCases) {
      try {
        CFloat tested = toTestedImpl(test.arg1());
        BigFloat result = BigFloat.NaN(getFloatType().precision);
        try {
          result = toBigFloat(operator.apply(tested));
        } catch (Throwable t) {
          String testHeader = printTestHeader(name, test.arg1());
          assertWithMessage(testHeader + t).fail();
        }
        if (!result.equals(test.result())) {
          String testHeader = printTestHeader(name, test.arg1());
          StringSubject value = assertWithMessage(testHeader).that(printValue(result));
          if (ulps == 0) {
            value.isEqualTo(printValue(test.result()));
          } else {
            value.isIn(errorRange(ulps, test.result()));
          }
        }
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
        CFloat ref1 = toReferenceImpl(arg1);
        CFloat ref2 = toReferenceImpl(arg2);
        BigFloat result = toBigFloat(operator.apply(ref1, ref2));
        testBuilder.add(new TestValue<>(arg1, arg2, result));
      }
    }
    ImmutableList<TestValue<BigFloat>> testCases = testBuilder.build();
    ImmutableList.Builder<String> logBuilder = ImmutableList.builder();
    for (TestValue<BigFloat> test : testCases) {
      try {
        CFloat tested1 = toTestedImpl(test.arg1());
        CFloat tested2 = toTestedImpl(test.arg2());
        BigFloat result = BigFloat.NaN(getFloatType().precision);
        try {
          result = toBigFloat(operator.apply(tested1, tested2));
        } catch (Throwable t) {
          String testHeader = printTestHeader(name, test.arg1(), test.arg2());
          assertWithMessage(testHeader + t).fail();
        }
        if (!result.equals(test.result())) {
          String testHeader = printTestHeader(name, test.arg1(), test.arg2());
          StringSubject value = assertWithMessage(testHeader).that(printValue(result));
          if (ulps == 0) {
            value.isEqualTo(printValue(test.result()));
          } else {
            value.isIn(errorRange(ulps, test.result()));
          }
        }
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
      CFloat ref = toReferenceImpl(arg);
      boolean result = predicate.test(ref);
      testBuilder.add(new TestValue<>(arg, result));
    }
    ImmutableList<TestValue<Boolean>> testCases = testBuilder.build();
    ImmutableList.Builder<String> logBuilder = ImmutableList.builder();
    for (TestValue<Boolean> test : testCases) {
      try {
        CFloat tested = toTestedImpl(test.arg1());
        boolean result = true;
        try {
          result = predicate.test(tested);
        } catch (Throwable t) {
          String testHeader = printTestHeader(name, test.arg1());
          assertWithMessage(testHeader + t).fail();
        }
        if (result != test.result()) {
          String testHeader = printTestHeader(name, test.arg1());
          assertWithMessage(testHeader).that(result).isEqualTo(test.result());
        }
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
        CFloat ref1 = toReferenceImpl(arg1);
        CFloat ref2 = toReferenceImpl(arg2);
        boolean result = predicate.apply(ref1, ref2);
        testBuilder.add(new TestValue<>(arg1, arg2, result));
      }
    }
    ImmutableList<TestValue<Boolean>> testCases = testBuilder.build();
    ImmutableList.Builder<String> logBuilder = ImmutableList.builder();
    for (TestValue<Boolean> test : testCases) {
      try {
        CFloat tested1 = toTestedImpl(test.arg1());
        CFloat tested2 = toTestedImpl(test.arg2());
        boolean result = true;
        try {
          result = predicate.apply(tested1, tested2);
        } catch (Throwable t) {
          String testHeader = printTestHeader(name, test.arg1(), test.arg2());
          assertWithMessage(testHeader + t).fail();
        }
        if (result != test.result()) {
          String testHeader = printTestHeader(name, test.arg1(), test.arg2());
          assertWithMessage(testHeader).that(result).isEqualTo(test.result());
        }
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
      CFloat ref = toReferenceImpl(arg);
      Number result = function.apply(ref);
      testBuilder.add(new TestValue<>(arg, result));
    }
    ImmutableList<TestValue<Number>> testCases = testBuilder.build();
    ImmutableList.Builder<String> logBuilder = ImmutableList.builder();
    for (TestValue<Number> test : testCases) {
      try {
        CFloat tested = toTestedImpl(test.arg1());
        Number result = null;
        try {
          result = function.apply(tested);
        } catch (Throwable t) {
          String testHeader = printTestHeader(name, test.arg1());
          assertWithMessage(testHeader + t).fail();
        }
        if (!Objects.equals(result, test.result())) {
          String testHeader = printTestHeader(name, test.arg1());
          assertWithMessage(testHeader).that(result).isEqualTo(test.result());
        }
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

  protected void testStringFunction(String name, Function<CFloat, String> function) {
    ImmutableList.Builder<TestValue<String>> testBuilder = ImmutableList.builder();
    for (BigFloat arg : unaryTestValues()) {
      CFloat ref = toReferenceImpl(arg);
      String result = function.apply(ref);
      testBuilder.add(new TestValue<>(arg, result));
    }
    ImmutableList<TestValue<String>> testCases = testBuilder.build();
    ImmutableList.Builder<String> logBuilder = ImmutableList.builder();
    for (TestValue<String> test : testCases) {
      try {
        CFloat tested = toTestedImpl(test.arg1());
        String result = null;
        try {
          result = function.apply(tested);
        } catch (Throwable t) {
          String testHeader = printTestHeader(name, test.arg1());
          assertWithMessage(testHeader + t).fail();
        }
        if (!Objects.equals(result, test.result())) {
          String testHeader = printTestHeader(name, test.arg1());
          assertWithMessage(testHeader).that(result).isEqualTo(test.result());
        }
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
    assertThat(printValue(toBigFloat(r1))).isIn(errorRange(ulpError(), toBigFloat(r2)));
  }

  protected abstract CFloat toTestedImpl(BigFloat value);

  protected abstract CFloat toTestedImpl(String repr);

  protected abstract CFloat toTestedImpl(String repr, Map<Integer, Integer> fromStringStats);

  protected abstract CFloat toReferenceImpl(BigFloat value);

  protected CFloat toReferenceImpl(String repr) {
    return toReferenceImpl(new BigFloat(repr, getFloatType()));
  }

  @Test
  public void constTest() {
    // This test checks that test values are correctly convert to values in the implementation.
    testOperator("const", 0, (CFloat a) -> a);
  }

  @Test
  public void fromStringTest() {
    ImmutableList.Builder<TestValue<BigFloat>> testBuilder = ImmutableList.builder();
    for (BigFloat arg : unaryTestValues()) {
      BigFloat result = toBigFloat(toReferenceImpl(printBigFloat(arg)));
      testBuilder.add(new TestValue<>(arg, result));
    }
    ImmutableList<TestValue<BigFloat>> testCases = testBuilder.build();
    ImmutableList.Builder<String> logBuilder = ImmutableList.builder();
    Map<Integer, Integer> fromStringStats = new HashMap<>();
    for (TestValue<BigFloat> test : testCases) {
      try {
        BigFloat result = BigFloat.NaN(getFloatType().precision);
        try {
          result = toBigFloat(toTestedImpl(printBigFloat(test.arg1()), fromStringStats));
        } catch (Throwable t) {
          String testHeader = printTestHeader("fromString", test.arg1());
          assertWithMessage(testHeader + t).fail();
        }
        if (!result.equals(test.result())) {
          String testHeader = printTestHeader("fromString", test.arg1());
          assertWithMessage(testHeader)
              .that(printValue(result))
              .isEqualTo(printValue(test.result()));
        }

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
    // printStatistics(fromStringStats);
  }

  @Test
  public void toStringTest() {
    testStringFunction("toString", (CFloat a) -> a.toString());
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

  protected static int findClosest(Map<Integer, Float> accum, float p) {
    for (Integer k : accum.keySet().stream().sorted().toList()) {
      if (accum.get(k) >= p) {
        return k;
      }
    }
    throw new IllegalArgumentException();
  }

  // Print statistics about the required bit width in ln, exp and pow
  protected static String printStatistics(Map<Integer, Integer> stats) {
    int total = stats.values().stream().reduce(0, (acc, v) -> acc + v);

    ImmutableMap.Builder<Integer, Float> accum = ImmutableMap.builder();
    int sum = 0;
    for (Integer k : stats.keySet().stream().sorted().toList()) {
      sum += stats.get(k);
      accum.put(k, ((float) sum) / total);
    }
    ImmutableMap<Integer, Float> accumMap = accum.buildOrThrow();

    ImmutableList<Float> quantiles =
        ImmutableList.of(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f);
    StringBuilder stdout = new StringBuilder();
    for (Float p : quantiles) { // Header
      String s = p.toString();
      stdout.append(" ".repeat(7 - s.length()) + s);
    }
    stdout.append("\n");
    for (Float p : quantiles) { // Values
      String s = String.valueOf(findClosest(accumMap, p));
      stdout.append(" ".repeat(7 - s.length()) + s);
    }
    return stdout.toString();
  }

  @Test
  public void lnTest() {
    Map<Integer, Integer> lnStats = new HashMap<>();
    testOperator(
        "ln",
        ulpError(),
        (CFloat a) -> (a instanceof CFloatImpl) ? ((CFloatImpl) a).lnWithStats(lnStats) : a.ln());
    // printStatistics(lnStats);
  }

  @Test
  public void expTest() {
    Map<Integer, Integer> expStats = new HashMap<>();
    testOperator(
        "exp",
        ulpError(),
        (CFloat a) ->
            (a instanceof CFloatImpl) ? ((CFloatImpl) a).expWithStats(expStats) : a.exp());
    // printStatistics(expStats);
  }

  @Test
  public void powToTest() {
    Map<Integer, Integer> powStats = new HashMap<>();
    testOperator(
        "powTo",
        ulpError(),
        (CFloat a, CFloat b) ->
            (a instanceof CFloatImpl) ? ((CFloatImpl) a).powToWithStats(b, powStats) : a.powTo(b));
    // printStatistics(powStats);
  }

  @Test
  public void powToIntegralTest() {
    // Native implementation does not support negative exponents
    assume().that(getRefImpl()).isNotEqualTo(ReferenceImpl.NATIVE);
    testOperator(
        "powToInteger",
        0,
        // FIXME: Find a better way to skip the test if x is NaN or infinite
        (CFloat a, CFloat b) -> (b.isNan() || b.isInfinity()) ? a : a.powToIntegral(toInteger(b)));
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

  private CNativeType toNativeType(BinaryMathContext pFormat) {
    int r = -1;
    if (pFormat.equals(BinaryMathContext.BINARY16)) {
      r = CNativeType.HALF.getOrdinal();
    }
    if (pFormat.equals(BinaryMathContext.BINARY32)) {
      r = CNativeType.SINGLE.getOrdinal();
    }
    if (pFormat.equals(BinaryMathContext.BINARY64)) {
      r = CNativeType.DOUBLE.getOrdinal();
    }
    checkArgument(r >= 0);
    return CFloatNativeAPI.toNativeType(r);
  }

  @Test
  public void castToTest() {
    BinaryMathContext other =
        getFloatType().equals(BinaryMathContext.BINARY32)
            ? BinaryMathContext.BINARY64
            : BinaryMathContext.BINARY32;
    testOperator(
        "castToTest",
        0,
        (CFloat a) -> a.castTo(toNativeType(other)).castTo(toNativeType(getFloatType())));
  }

  @Test
  public void castToRoundingTest() {
    BinaryMathContext other =
        getFloatType().equals(BinaryMathContext.BINARY32)
            ? BinaryMathContext.BINARY64
            : BinaryMathContext.BINARY32;
    testOperator(
        "castToRoundingTest",
        0,
        (CFloat a) -> a.castTo(toNativeType(other)).sqrt().castTo(toNativeType(getFloatType())));
  }

  @Test
  public void castToByteTest() {
    assume().that(getRefImpl()).isEqualTo(ReferenceImpl.NATIVE);
    testIntegerFunction("castToByteTest", (CFloat a) -> a.castToOther(CNativeType.CHAR));
  }

  @Test
  public void castToShortTest() {
    assume().that(getRefImpl()).isEqualTo(ReferenceImpl.NATIVE);
    testIntegerFunction("castToShortTest", (CFloat a) -> a.castToOther(CNativeType.SHORT));
  }

  @Test
  public void castToIntTest() {
    assume().that(getRefImpl()).isEqualTo(ReferenceImpl.NATIVE);
    testIntegerFunction("castToIntTest", (CFloat a) -> a.castToOther(CNativeType.INT));
  }

  @Test
  public void castToLongTest() {
    assume().that(getRefImpl()).isEqualTo(ReferenceImpl.NATIVE);
    testIntegerFunction("castToLongTest", (CFloat a) -> a.castToOther(CNativeType.LONG));
  }
}
