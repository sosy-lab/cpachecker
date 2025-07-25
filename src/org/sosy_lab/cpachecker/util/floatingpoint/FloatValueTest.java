// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.TruthJUnit.assume;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multiset;
import com.google.common.truth.Correspondence.BinaryPredicate;
import com.google.common.truth.Expect;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.kframework.mpfr.BigFloat;
import org.kframework.mpfr.BinaryMathContext;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CFloatType;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CIntegerType;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue.Format;

/**
 * Abstract test class for the {@link CFloat} interface.
 *
 * <p>The idea behind this class is to compare two different implementations of the CFloat
 * interface. We always use {@link CFloatImpl} as the "tested" implementation and compare it to a
 * second "reference" implementation. There are currently three supported implementations that can
 * be used as a reference:
 *
 * <ul>
 *   <li>{@link CFloatNative}, uses native C code to calculate its results
 *   <li>{@link MpfrFloat}, uses BigFloats to access MPFR
 *   <li>{@link JFloat} ({@link JDouble}), uses normal Java floats (doubles) for its calculations
 * </ul>
 *
 * <p>The test suite will automatically generate test inputs for all methods of the CFloat interface
 * and compare the results of the tested implementation with those of the chosen reference
 * implementation on all of those inputs.
 *
 * <p>The methods {@link FloatValueTest#unaryTestValues()}, {@link
 * FloatValueTest#binaryTestValues()} and {@link FloatValueTest#integerTestValues()} are used to
 * calculate a set of tests inputs. There are four separate generator methods that can be used:
 *
 * <ul>
 *   <li>{@link FloatValueTest#floatConstants(Format)}
 *   <li>{@link FloatValueTest#floatPowers(Format, int, BigFloat, int, BigFloat)}
 *   <li>{@link FloatValueTest#floatRandom(Format, int)}
 *   <li>{@link FloatValueTest#allFloats(Format)}
 * </ul>
 *
 * <p>The default behaviour for both {@link FloatValueTest#unaryTestValues()} and {@link
 * FloatValueTest#binaryTestValues()} is to use a combination of the first three test value classes.
 * For <code>Float8</code> it is possible to calculate all possible inputs for unary and binary
 * operations, and for <code>Float16</code> the same can still be done for unary operations with
 * reasonable runtimes for the tests.
 *
 * <p>The test class is parametrized and can compare results for various floating point precisions
 * and reference implementations. The behaviour depends on the system property <code>
 * enableExpensiveTests</code>. When it is set an exhaustive set of tests will be run for the
 * precisions <code>Float8</code>, <code>Float16</code>, <code>Float32</code>, <code>Float64</code>
 * and <code>FloatExtended</code>. For <code>Float8</code> all possible inputs are tested for each
 * method, and for <code>Float16</code> the tests are exhaustive for unary methods. We use MPFR as a
 * reference implementation for all precisions. In addition we use the native implementation for
 * <code>Float32</code>, <code>Float64</code> and <code>FloatExtended</code>, and the Java
 * implementation on <code>Float32</code> and <code>Float64</code>.
 *
 * <p>When the system property <code>enableExpensiveTests</code> is not set to <code>on</code> a
 * smaller subset of tests is run. In this case we will only consider the precision <code>Float32
 * </code> and test all three implementations with a much smaller number of randomly generated
 * tests.
 */
@SuppressFBWarnings(value = "DMI_RANDOM_USED_ONLY_ONCE")
@RunWith(Parameterized.class)
public class FloatValueTest {

  @BeforeClass
  public static void configureLocale() {
    Locale.setDefault(Locale.US);
  }

  /** Supported reference implementations */
  public enum ReferenceImpl {
    MPFR,
    JAVA,
    NATIVE
  }

  /** Floating point formats for the tests and the number of values to generate for each of them */
  private static final ImmutableMap<Format, Integer> TEST_FORMATS =
      ImmutableMap.of(
          Format.Float8,
          50000,
          Format.Float16,
          50000,
          Format.Float32,
          50000,
          Format.Float64,
          25000,
          Format.Float80,
          15000);

  /** Reference implementations that will be used as oracles in the test */
  private static final Iterable<ReferenceImpl> TEST_REFERENCES =
      ImmutableList.of(ReferenceImpl.MPFR, ReferenceImpl.JAVA, ReferenceImpl.NATIVE);

  /**
   * Test configuration
   *
   * @param format Precision of the values
   * @param reference Reference implementation to compare the results to
   * @param numberOfTests Number of test values to generate
   */
  public record FloatTestOptions(Format format, ReferenceImpl reference, Integer numberOfTests) {
    @Override
    public String toString() {
      String floatFormat =
          format.equals(Format.Float80)
              ? "Extended"
              : String.format("Float%s", 1 + format.expBits() + format.sigBits());
      return reference + ", " + floatFormat;
    }
  }

  /**
   * Enables running more exhaustive tests
   *
   * <p>Use <code>ant tests -DenableExpensiveTests=true</code> to set this flag. The test suite will
   * then generate a much more exhaustive set of input values for the tested methods.
   */
  private static final boolean enableExpensiveTests =
      Boolean.parseBoolean(System.getProperty("enableExpensiveTests"));

  @Parameters(name = "{0}")
  public static FloatTestOptions[] getFloatTestOptions() {
    ImmutableList.Builder<FloatTestOptions> builder = ImmutableList.builder();
    for (Map.Entry<Format, Integer> entry : TEST_FORMATS.entrySet()) {
      Format precision = entry.getKey();
      for (ReferenceImpl reference : TEST_REFERENCES) {
        if (reference.equals(ReferenceImpl.MPFR)
            || precision.equals(Format.Float32)
            || precision.equals(Format.Float64)
            || (reference.equals(ReferenceImpl.NATIVE) && precision.equals(Format.Float80))) {
          if (precision.equals(Format.Float32) || enableExpensiveTests) {
            builder.add(
                new FloatTestOptions(
                    precision, reference, enableExpensiveTests ? entry.getValue() : 100));
          }
        }
      }
    }
    return builder.build().toArray(new FloatTestOptions[0]);
  }

  @Parameter(0)
  public FloatTestOptions floatTestOptions;

  /**
   * Convert a CFloat value to BigFloat.
   *
   * <p>This is used in the tests to convert the result of the operation back to a BigFloat value.
   */
  private BigFloat toBigFloat(CFloat pValue) {
    if (pValue instanceof MpfrFloat floatValue) {
      return floatValue.toBigFloat();
    } else if (pValue instanceof CFloatImpl floatValue) {
      return toBigFloat(floatValue.getValue());
    } else if (pValue instanceof JFloat floatValue) {
      return new BigFloat(floatValue.toFloat(), BinaryMathContext.BINARY32);
    } else if (pValue instanceof JDouble floatValue) {
      return new BigFloat(floatValue.toDouble(), BinaryMathContext.BINARY64);
    } else if (pValue instanceof CFloatNative val) {
      CFloatWrapper wrapper = val.getWrapper();
      return switch (pValue.getType()) {
        case SINGLE -> {
          long exponent = wrapper.getExponent() << Format.Float32.sigBits();
          long mantissa = wrapper.getMantissa();
          yield new BigFloat(
              Float.intBitsToFloat((int) (exponent + mantissa)), BinaryMathContext.BINARY32);
        }
        case DOUBLE -> {
          long exponent = wrapper.getExponent() << Format.Float64.sigBits();
          long mantissa = wrapper.getMantissa();
          yield new BigFloat(
              Double.longBitsToDouble(exponent + mantissa), BinaryMathContext.BINARY64);
        }
        case LONG_DOUBLE -> {
          BinaryMathContext context = new BinaryMathContext(64, 15);
          if (val.isNan()) {
            yield val.isNegative()
                ? BigFloat.NaN(context.precision).negate()
                : BigFloat.NaN(context.precision);
          } else if (val.isInfinity()) {
            yield val.isNegative()
                ? BigFloat.negativeInfinity(context.precision)
                : BigFloat.positiveInfinity(context.precision);
          } else {
            long exponent = (wrapper.getExponent() & 0x7FFF) - Format.Float80.bias();
            BigInteger significand = new BigInteger(Long.toUnsignedString(wrapper.getMantissa()));
            yield new BigFloat(val.isNegative(), significand, exponent, context);
          }
        }
      };
    } else {
      throw new UnsupportedOperationException(
          String.format("Unsupported CFloat class \"%s\"", pValue.getClass().getSimpleName()));
    }
  }

  /**
   * Convert a FloatValue value to BigFloat.
   *
   * <p>Used in the implementation of {@link #toBigFloat(CFloat)}.
   */
  private BigFloat toBigFloat(FloatValue pValue) {
    int sigBits = pValue.getFormat().sigBits();
    int expBits = pValue.getFormat().expBits();
    BinaryMathContext context = new BinaryMathContext(sigBits + 1, expBits);
    if (pValue.isNan()) {
      return pValue.isNegative()
          ? BigFloat.NaN(context.precision).negate()
          : BigFloat.NaN(context.precision);
    }
    if (pValue.isInfinite()) {
      return pValue.isNegative()
          ? BigFloat.negativeInfinity(context.precision)
          : BigFloat.positiveInfinity(context.precision);
    }
    return new BigFloat(
        pValue.isNegative(),
        pValue.getSignificandWithoutHiddenBit(),
        pValue.getExponentWithoutBias(),
        context);
  }

  /** Construct a CFloatImpl from a BigFloat test value. */
  private CFloatImpl testValueToCFloatImpl(BigFloat value, Format format) {
    if (value.isNaN()) {
      return new CFloatImpl(
          value.sign() ? FloatValue.nan(format).negate() : FloatValue.nan(format));
    } else if (value.isInfinite()) {
      return new CFloatImpl(
          value.sign() ? FloatValue.negativeInfinity(format) : FloatValue.infinity(format));
    } else {
      long exp = value.exponent(format.minExp(), format.maxExp());
      BigInteger sig = value.significand(format.minExp(), format.maxExp());
      return new CFloatImpl(new FloatValue(format, value.sign(), exp, sig));
    }
  }

  /** Pretty printer for BigFloat type */
  private static String printBigFloat(BigFloat value) {
    if (value.isNaN()) {
      return value.sign() ? "-nan" : "nan";
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
  private static String toPlainString(BigFloat value) {
    String r = printBigFloat(value);
    if (r.contains("e")) {
      r = new BigDecimal(r).toPlainString();
    }
    return r;
  }

  /** Generate a list of floating point constants that cover all special case values. */
  private static Iterable<BigFloat> floatConstants(Format format) {
    int precision = format.sigBits() + 1;
    BinaryMathContext context = new BinaryMathContext(precision, format.expBits());
    return ImmutableList.of(
        BigFloat.NaN(precision).negate(),
        BigFloat.negativeInfinity(precision),
        BigFloat.maxValue(precision, format.maxExp()).negate(),
        new BigFloat(-17.0f, context),
        new BigFloat(-1.0f, context),
        new BigFloat(-0.1f, context),
        BigFloat.minNormal(precision, format.minExp()).negate(),
        BigFloat.minValue(precision, format.minExp()).negate(),
        BigFloat.negativeZero(precision),
        BigFloat.zero(precision),
        BigFloat.minValue(precision, format.minExp()),
        BigFloat.minNormal(precision, format.minExp()),
        new BigFloat(0.1f, context),
        new BigFloat(1.0f, context),
        new BigFloat(17.0f, context),
        BigFloat.maxValue(precision, format.maxExp()),
        BigFloat.positiveInfinity(precision),
        BigFloat.NaN(precision));
  }

  /**
   * Generate a list of powers ca^px where c,p are incremented starting from 1 and a,x are
   * constants.
   */
  private static Iterable<BigFloat> floatPowers(
      Format format, int c, BigFloat a, int p, BigFloat x) {
    ImmutableList.Builder<BigFloat> builder = ImmutableList.builder();
    BinaryMathContext context = new BinaryMathContext(format.sigBits() + 1, format.expBits());
    for (int i = 1; i <= c; i++) {
      for (int j = -p; j <= p; j++) {
        BigFloat t1 = new BigFloat(i, context).multiply(a, context);
        BigFloat t2 = new BigFloat(p, context).multiply(x, context);
        BigFloat r = t1.pow(t2, context);
        if (!r.equalTo(new BigFloat(1, context))) {
          builder.add(r);
        }
      }
    }
    return builder.build();
  }

  /** Generate n random floating point values. */
  private static Iterable<BigFloat> floatRandom(Format format, int n) {
    ImmutableList.Builder<BigFloat> builder = ImmutableList.builder();
    BinaryMathContext context = new BinaryMathContext(format.sigBits() + 1, format.expBits());
    Random random = new Random(0);
    int i = 0;
    while (i < n) {
      boolean sign = random.nextBoolean();
      long exponent = random.nextLong(2 * format.maxExp()) - format.maxExp();
      BigInteger leading = BigInteger.ONE.shiftLeft(format.sigBits());
      if (exponent < format.minExp()) { // Special case for subnormal numbers
        leading = BigInteger.ZERO;
      }
      BigInteger significand = leading.add(new BigInteger(format.sigBits(), random));
      BigFloat value = new BigFloat(sign, significand, exponent, context);
      if (!value.isPositiveZero() || !value.isNegativeZero()) {
        builder.add(new BigFloat(sign, significand, exponent, context));
        i++;
      }
    }
    return builder.build();
  }

  /** Generate all possible floating point values for a given precision. */
  private static Iterable<BigFloat> allFloats(Format format) {
    ImmutableList.Builder<BigFloat> builder = ImmutableList.builder();
    BinaryMathContext context = new BinaryMathContext(format.sigBits() + 1, format.expBits());
    for (long exponent = format.minExp() - 1; exponent <= format.maxExp() + 1; exponent++) {
      BigInteger leading = BigInteger.ONE.shiftLeft(format.sigBits());
      if (exponent < format.minExp() || exponent > format.maxExp()) {
        leading = BigInteger.ZERO;
      }
      int maxValue = (2 << (format.sigBits() - 1));
      for (int i = 0; i < maxValue; i++) {
        if (exponent > format.maxExp() && i > 1) {
          // Only generate one NaN value
          continue;
        }
        BigInteger significand = leading.add(BigInteger.valueOf(i));
        builder.add(new BigFloat(false, significand, exponent, context));
        builder.add(new BigFloat(true, significand, exponent, context));
      }
    }
    return builder.build();
  }

  /** The set of test inputs that should be used for unary operations in the CFloat interface. */
  private Iterable<BigFloat> unaryTestValues() {
    Format format = floatTestOptions.format;
    if (enableExpensiveTests && (format.equals(Format.Float8) || format.equals(Format.Float16))) {
      return allFloats(format);
    } else {
      BinaryMathContext context = new BinaryMathContext(format.sigBits() + 1, format.expBits());
      BigFloat constant = new BigFloat(0.5f, context);
      return FluentIterable.concat(
          floatConstants(format),
          floatPowers(format, 14, constant, 20, constant),
          floatRandom(format, floatTestOptions.numberOfTests));
    }
  }

  /**
   * The set of test inputs that should be used for binary operations in the CFloat interface. The
   * values will be used for both arguments separately. So if there are k test values in this set,
   * the number of test runs for a binary operation will be k^2.
   */
  private Iterable<BigFloat> binaryTestValues() {
    Format format = floatTestOptions.format;
    if (enableExpensiveTests && format.equals(Format.Float8)) {
      return allFloats(format);
    } else {
      BinaryMathContext context = new BinaryMathContext(format.sigBits() + 1, format.expBits());
      BigFloat constant = new BigFloat(0.5f, context);

      return FluentIterable.concat(
          floatConstants(format),
          floatPowers(format, 3, constant, 3, constant),
          floatRandom(format, (int) Math.sqrt(floatTestOptions.numberOfTests)));
    }
  }

  /** Generate a list of special case integer values. */
  private static Iterable<Integer> integerConstants(Format format) {
    int maxValue = Integer.MAX_VALUE;
    int minValue = Integer.MIN_VALUE;
    if (format.sigBits() + 1 < 32) {
      maxValue = (1 << (format.sigBits() + 1)) - 1;
      minValue = -maxValue;
    }
    return ImmutableList.of(minValue, -1, 0, 1, maxValue);
  }

  /** Generate n random integer values. */
  private static Iterable<Integer> integerRandom(Format format, int n) {
    ImmutableList.Builder<Integer> builder = ImmutableList.builder();
    Random random = new Random(0);
    for (int c = 0; c < n; c++) {
      int maxValue = Integer.MAX_VALUE;
      int minValue = Integer.MIN_VALUE;
      if (format.sigBits() + 1 < 32) {
        maxValue = (1 << (format.sigBits() + 1)) - 1;
        minValue = -maxValue;
      }
      builder.add(random.nextInt(minValue, maxValue));
    }
    return builder.build();
  }

  /** Generate integer test inputs that include both special cases and random values. */
  private Iterable<Integer> integerTestValues() {
    Format format = floatTestOptions.format;
    return FluentIterable.concat(
        integerConstants(format),
        integerRandom(format, (int) Math.sqrt(floatTestOptions.numberOfTests)));
  }

  private static int calculateExpWidth(Format pFormat) {
    BigInteger val = BigInteger.valueOf(2 * pFormat.maxExp() + 1);
    int r = val.bitLength() - 1;
    return val.bitCount() > 1 ? r + 1 : r;
  }

  /**
   * Print a BigFloat value as a string of bits.
   *
   * <p>The output will consist of 3 parts: one bit for the sign, followed by the exponent, and then
   * the significand written as bit strings. The value is printed in IEEE notation, that is, with a
   * bias added to the exponent to make it non-negative, and the hidden bit removed from the
   * significand.
   *
   * <p>As an example the output for 1.0969e+01 in float16 is 0 10010 0101111100. Put differently
   * the float value can be written as 1.0101111100 * 2^(10010 - 15 = 3) where 15 is the bias for
   * 16bit floats.
   */
  private static String toBits(Format format, BigFloat value) {
    String sign = value.sign() ? "1" : "0";

    // Print the exponent
    long valueExp = value.exponent(format.minExp(), format.maxExp()) + format.maxExp();
    String exponent = Long.toString(valueExp, 2);
    exponent = "0".repeat(calculateExpWidth(format) - exponent.length()) + exponent;
    exponent = exponent.substring(0, format.expBits());

    // Print the significand
    // We use NaN as default value and then only extract the actual bits if the value was different
    // from NaN. This is necessary because MPFR uses a canonical representation for NaN that leaves
    // the bits effectively unspecified. Trying to extract them will cause an exception.
    String significand = BigInteger.ONE.shiftLeft(format.sigBits() - 1).toString(2);
    if (!value.isNaN()) {
      // Get the actual significand if the value is not NaN
      String repr = value.significand(format.minExp(), format.maxExp()).toString(2);
      repr = "0".repeat(format.sigBits() + 1 - repr.length()) + repr;
      significand = repr.substring(1);
    }
    return String.format("%s %s %s", sign, exponent, significand);
  }

  private static String printValue(Format fmt, BigFloat value) {
    return String.format("%s [%s]", toBits(fmt, value), printBigFloat(value));
  }

  private static String printTestHeader(String name, Format fmt, BigFloat arg) {
    return String.format("Testcase %s(%s): ", name, printValue(fmt, arg));
  }

  private static String printTestHeader(String name, Format fmt, BigFloat arg1, BigFloat arg2) {
    return String.format(
        "Testcase %s(%s, %s): ", name, printValue(fmt, arg1), printValue(fmt, arg2));
  }

  private static String printTestHeader(String name, Format fmt, BigFloat arg1, Integer arg2) {
    return String.format("Testcase %s(%s, %s): ", name, printValue(fmt, arg1), arg2);
  }

  private static String printTestHeader(String name, Integer arg1, Integer arg2) {
    return String.format("Testcase %s(%s, %s): ", name, arg1, arg2);
  }

  /**
   * Defines the maximum error (in ULPs) for transcendental functions.
   *
   * <p>Needed as actual math libraries don't guarantee exact results for transcendental functions.
   * If this method returns anything greater than 0 tests will not fail if the result is imprecise,
   * but still within the specified error bound.
   *
   * @see <a
   *     href="https://www.gnu.org/software/libc/manual/html_node/Errors-in-Math-Functions.html">
   *     "Known Maximum Errors in Math Functions" (from the glibc documentation)</a>
   */
  private int ulpError() {
    return ImmutableList.of(ReferenceImpl.JAVA, ReferenceImpl.NATIVE)
            .contains(floatTestOptions.reference)
        ? 1
        : 0;
  }

  /** Returns a list of all float values in the error range. */
  private static List<String> errorRange(int pDistance, Format fmt, BigFloat pValue) {
    if (pDistance == 0 || pValue.isNaN()) {
      return ImmutableList.of(printValue(fmt, pValue));
    }
    if (pDistance == 1) {
      BigFloat minus1Ulp = pValue.nextDown(fmt.minExp(), fmt.maxExp());
      BigFloat plus1Ulp = pValue.nextUp(fmt.minExp(), fmt.maxExp());

      return ImmutableList.of(
          printValue(fmt, minus1Ulp), printValue(fmt, pValue), printValue(fmt, plus1Ulp));
    }
    throw new IllegalArgumentException();
  }

  @Rule public final Expect expect = Expect.create();

  private void testOperator(String name, int ulps, UnaryOperator<CFloat> operator) {
    for (BigFloat arg : unaryTestValues()) {
      try {
        //  Calculate result with the reference implementation
        CFloat ref = toReferenceImpl(arg);
        BigFloat resultReference = toBigFloat(operator.apply(ref));

        // Calculate result with the tested implementation
        CFloat tested = toTestedImpl(arg);
        BigFloat resultTested = toBigFloat(operator.apply(tested));

        // Compare the two results
        if (!resultTested.equals(resultReference)) {
          String testHeader = printTestHeader(name, floatTestOptions.format, arg);
          if (ulps == 0) {
            expect
                .withMessage(testHeader)
                .that(printValue(floatTestOptions.format, resultTested))
                .isEqualTo(printValue(floatTestOptions.format, resultReference));
          } else {
            expect
                .withMessage(testHeader)
                .that(printValue(floatTestOptions.format, resultTested))
                .isIn(errorRange(ulps, floatTestOptions.format, resultReference));
          }
        }
      } catch (Throwable t) {
        throw new RuntimeException(printTestHeader(name, floatTestOptions.format, arg), t);
      }
    }
  }

  private void testOperator(String name, int ulps, BinaryOperator<CFloat> operator) {
    testOperator(name, ulps, operator, binaryTestValues(), binaryTestValues());
  }

  private void testOperator(
      String name,
      int ulps,
      BinaryOperator<CFloat> operator,
      Iterable<BigFloat> values1,
      Iterable<BigFloat> values2) {
    for (BigFloat arg1 : values1) {
      for (BigFloat arg2 : values2) {
        try { // Calculate result with the reference implementation
          CFloat ref1 = toReferenceImpl(arg1);
          CFloat ref2 = toReferenceImpl(arg2);
          BigFloat resultReference = toBigFloat(operator.apply(ref1, ref2));

          // Calculate result with the tested implementation
          CFloat tested1 = toTestedImpl(arg1);
          CFloat tested2 = toTestedImpl(arg2);
          BigFloat resultTested = toBigFloat(operator.apply(tested1, tested2));

          // Compare the two results
          if (!resultTested.equals(resultReference)) {
            String testHeader = printTestHeader(name, floatTestOptions.format, arg1, arg2);
            if (ulps == 0) {
              expect
                  .withMessage(testHeader)
                  .that(printValue(floatTestOptions.format, resultTested))
                  .isEqualTo(printValue(floatTestOptions.format, resultReference));
            } else {
              expect
                  .withMessage(testHeader)
                  .that(printValue(floatTestOptions.format, resultTested))
                  .isIn(errorRange(ulps, floatTestOptions.format, resultReference));
            }
          }
        } catch (Throwable t) {
          throw new RuntimeException(printTestHeader(name, floatTestOptions.format, arg1, arg2), t);
        }
      }
    }
  }

  private void testPredicate(String name, Predicate<CFloat> predicate) {
    for (BigFloat arg : unaryTestValues()) {
      try {
        // Calculate result with the reference implementation
        CFloat ref = toReferenceImpl(arg);
        boolean resultReference = predicate.test(ref);

        // Calculate result with the tested implementation
        CFloat tested = toTestedImpl(arg);
        boolean resultTested = predicate.test(tested);

        // Compare the two results
        if (resultTested != resultReference) {
          String testHeader = printTestHeader(name, floatTestOptions.format, arg);
          expect.withMessage(testHeader).that(resultTested).isEqualTo(resultReference);
        }
      } catch (Throwable t) {
        throw new RuntimeException(printTestHeader(name, floatTestOptions.format, arg), t);
      }
    }
  }

  private void testPredicate(String name, BinaryPredicate<CFloat, CFloat> predicate) {
    for (BigFloat arg1 : binaryTestValues()) {
      for (BigFloat arg2 : binaryTestValues()) {
        try {
          // Calculate result with the reference implementation
          CFloat ref1 = toReferenceImpl(arg1);
          CFloat ref2 = toReferenceImpl(arg2);
          boolean resultReference = predicate.apply(ref1, ref2);

          // Calculate result with the tested implementation
          CFloat tested1 = toTestedImpl(arg1);
          CFloat tested2 = toTestedImpl(arg2);
          boolean resultTested = predicate.apply(tested1, tested2);

          // Compare the two results
          if (resultTested != resultReference) {
            String testHeader = printTestHeader(name, floatTestOptions.format, arg1, arg2);
            expect.withMessage(testHeader).that(resultTested).isEqualTo(resultReference);
          }
        } catch (Throwable t) {
          throw new RuntimeException(printTestHeader(name, floatTestOptions.format, arg1, arg2), t);
        }
      }
    }
  }

  private void testIntegerFunction(String name, Function<CFloat, Number> function) {
    for (BigFloat arg : unaryTestValues()) {
      try {
        // Calculate result with the reference implementation
        CFloat ref = toReferenceImpl(arg);
        Number resultReference = function.apply(ref);

        // Calculate result with the tested implementation
        CFloat tested = toTestedImpl(arg);
        Number resultsTested = function.apply(tested);

        // Compare the two results
        if (!Objects.equals(resultsTested, resultReference)) {
          String testHeader = printTestHeader(name, floatTestOptions.format, arg);
          expect.withMessage(testHeader).that(resultsTested).isEqualTo(resultReference);
        }
      } catch (Throwable t) {
        throw new RuntimeException(printTestHeader(name, floatTestOptions.format, arg), t);
      }
    }
  }

  private void testStringFunction(String name, Function<CFloat, String> function) {
    for (BigFloat arg : unaryTestValues()) {
      try {
        // Calculate result with the reference implementation
        CFloat ref = toReferenceImpl(arg);
        String resultReference = function.apply(ref);

        // Calculate result with the tested implementation
        CFloat tested = toTestedImpl(arg);
        String resultTested = function.apply(tested);

        // Compare the two results
        if (!Objects.equals(resultTested, resultReference)) {
          String testHeader = printTestHeader(name, floatTestOptions.format, arg);
          expect.withMessage(testHeader).that(resultTested).isEqualTo(resultReference);
        }
      } catch (Throwable t) {
        throw new RuntimeException(printTestHeader(name, floatTestOptions.format, arg), t);
      }
    }
  }

  private void assertEqual1Ulp(FloatValue result, CFloat expected) {
    assertThat(printValue(result.getFormat(), toBigFloat(result)))
        .isIn(errorRange(ulpError(), result.getFormat(), toBigFloat(expected)));
  }

  /** Create a test value for the tested implementation. */
  private CFloat toTestedImpl(BigFloat value) {
    return testValueToCFloatImpl(value, floatTestOptions.format);
  }

  /** Convert Format to a matching native floating point type */
  private static CFloatType toNativeType(Format pFormat) {
    if (pFormat.equals(Format.Float32)) {
      return CFloatType.SINGLE;
    } else if (pFormat.equals(Format.Float64)) {
      return CFloatType.DOUBLE;
    } else if (pFormat.equals(Format.Float80)) {
      return CFloatType.LONG_DOUBLE;
    } else {
      throw new IllegalArgumentException();
    }
  }

  /** Create a test value for the reference implementation. */
  private CFloat toReferenceImpl(BigFloat value) {
    checkState(
        floatTestOptions.format.equals(Format.Float32)
            || floatTestOptions.format.equals(Format.Float64)
            || (floatTestOptions.reference == ReferenceImpl.NATIVE
                && floatTestOptions.format.equals(Format.Float80))
            || floatTestOptions.reference == ReferenceImpl.MPFR,
        "Backend %s only support float32 and float64 as format",
        floatTestOptions.reference);
    return switch (floatTestOptions.reference) {
      case MPFR -> new MpfrFloat(value, floatTestOptions.format);
      case JAVA ->
          floatTestOptions.format.equals(Format.Float32)
              ? (value.isNaN()
                  ? new JFloat(value.sign() ? Float.intBitsToFloat(0xFFC00000) : Float.NaN)
                  : new JFloat(value.floatValue()))
              : (value.isNaN()
                  ? new JDouble(
                      value.sign() ? Double.longBitsToDouble(0xFFF8000000000000L) : Float.NaN)
                  : new JDouble(value.doubleValue()));
      case NATIVE -> new CFloatNative(toPlainString(value), toNativeType(floatTestOptions.format));
    };
  }

  private BigFloat parseBigFloat(BinaryMathContext context, String repr) {
    if ("nan".equals(repr)) {
      return BigFloat.NaN(context.precision);
    } else if ("-nan".equals(repr)) {
      return BigFloat.NaN(context.precision).negate();
    } else if ("-inf".equals(repr)) {
      return BigFloat.negativeInfinity(context.precision);
    } else if ("inf".equals(repr)) {
      return BigFloat.positiveInfinity(context.precision);
    } else {
      return new BigFloat(repr, context);
    }
  }

  /** Create a test value for the reference implementation by parsing a String. */
  private CFloat toReferenceImpl(String repr) {
    BinaryMathContext context =
        new BinaryMathContext(
            floatTestOptions.format.sigBits() + 1, floatTestOptions.format.expBits());
    return toReferenceImpl(parseBigFloat(context, repr));
  }

  @Test
  public void constTest() {
    // This test doesn't test anything of the (actual) implementation. It tests the conversion from
    // and to BigFloat in the test class.
    testOperator("const", 0, (CFloat a) -> a);
  }

  @Test
  public void fromStringTest() {
    for (BigFloat arg : unaryTestValues()) {
      try {
        // Read the String with FloatValue.fromString
        BigFloat resultTested =
            toBigFloat(FloatValue.fromString(floatTestOptions.format, printBigFloat(arg)));

        // Compare the result to the original value
        if (!resultTested.equals(arg)) {
          String testHeader = printTestHeader("fromString", floatTestOptions.format, arg);
          expect
              .withMessage(testHeader)
              .that(printValue(floatTestOptions.format, resultTested))
              .isEqualTo(printValue(floatTestOptions.format, arg));
        }
      } catch (Throwable t) {
        throw new RuntimeException(printTestHeader("fromString", floatTestOptions.format, arg), t);
      }
    }
  }

  @Test
  public void fromStringRoundedTest() {
    // Similar to fromStringTest, but here we read a floating point number that has too many digits
    // for the format and needs to be rounded
    assume().that(floatTestOptions.format).isEqualTo(Format.Float32);
    assume().that(floatTestOptions.reference).isEqualTo(ReferenceImpl.MPFR);

    for (BigFloat arg : unaryTestValues()) {
      try {
        // Parse the String with MPFR (= the reference implementation for this test)
        BigFloat resultReference = new BigFloat(printBigFloat(arg), BinaryMathContext.BINARY16);

        // Parse the String with FloatValue.fromString
        BigFloat resultTested =
            toBigFloat(FloatValue.fromString(Format.Float16, printBigFloat(arg)));

        // Compare both values
        if (!resultTested.equals(resultReference)) {
          String testHeader = printTestHeader("fromStringRounded", Format.Float32, arg);
          expect
              .withMessage(testHeader)
              .that(printValue(Format.Float16, resultTested))
              .isEqualTo(printValue(Format.Float16, resultReference));
        }
      } catch (Throwable t) {
        throw new RuntimeException(printTestHeader("fromStringRounded", Format.Float32, arg), t);
      }
    }
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
  public void multiplyTest() {
    testOperator("multiply", 0, (CFloat a, CFloat b) -> a.multiply(b));
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
  public void moduloTest() {
    testOperator("modulo", 0, (CFloat a, CFloat b) -> a.modulo(b));
  }

  @Test
  public void remainderTest() {
    testOperator("remainder", 0, (CFloat a, CFloat b) -> a.remainder(b));
  }

  private static int findClosest(Map<Integer, Float> accum, float p) {
    return FluentIterable.from(accum.entrySet())
        .firstMatch(entry -> entry.getValue() >= p)
        .get()
        .getKey();
  }

  // Print statistics about the required bit width in ln, exp and pow
  @SuppressWarnings("unused")
  private static String printStatistics(Multiset<Integer> stats) {
    int total = stats.entrySet().stream().mapToInt(e -> e.getCount()).sum();

    ImmutableMap.Builder<Integer, Float> accum = ImmutableMap.builder();
    int sum = 0;
    for (Integer k : ImmutableList.sortedCopyOf(stats.elementSet())) {
      sum += stats.count(k);
      accum.put(k, ((float) sum) / total);
    }
    ImmutableMap<Integer, Float> accumMap = accum.buildOrThrow();

    ImmutableList<Float> quantiles =
        ImmutableList.of(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f).reverse();

    return quantiles.stream()
        .map(p -> String.format("%s: %7s", p.toString(), findClosest(accumMap, p)))
        .collect(Collectors.joining("\n"));
  }

  @Test
  public void lnTest() {
    Multiset<Integer> lnStats = HashMultiset.create();
    testOperator(
        "ln",
        ulpError(),
        (CFloat a) ->
            (a instanceof CFloatImpl cFloatImpl) ? cFloatImpl.lnWithStats(lnStats) : a.ln());
    // println(printStatistics(lnStats));
  }

  @Test
  public void expTest() {
    Multiset<Integer> expStats = HashMultiset.create();
    testOperator(
        "exp",
        ulpError(),
        (CFloat a) ->
            (a instanceof CFloatImpl cFloatImpl) ? cFloatImpl.expWithStats(expStats) : a.exp());
    // println(printStatistics(expStats));
  }

  @Test
  public void powToTest() {
    Multiset<Integer> powStats = HashMultiset.create();
    testOperator(
        "powTo",
        ulpError(),
        (CFloat a, CFloat b) ->
            (a instanceof CFloatImpl cFloatImpl)
                ? cFloatImpl.powToWithStats(b, powStats)
                : a.powTo(b));
    // println(printStatistics(powStats));
  }

  @Test
  public void powToIntegralTest() {
    // Native implementation does not support negative exponents and produces rounding errors
    assume().that(floatTestOptions.reference).isNotEqualTo(ReferenceImpl.NATIVE);

    for (BigFloat arg1 : binaryTestValues()) {
      for (Integer arg2 : integerTestValues()) {
        try { // Calculate result with the reference implementation
          CFloat ref1 = toReferenceImpl(arg1);
          BigFloat resultReference = toBigFloat(ref1.powToIntegral(arg2));

          // Calculate result with the tested implementation
          CFloat tested1 = toTestedImpl(arg1);
          BigFloat resultTested = toBigFloat(tested1.powToIntegral(arg2));

          // Compare the two results
          if (!resultTested.equals(resultReference)) {
            String testHeader =
                printTestHeader("powToInteger", floatTestOptions.format, arg1, arg2);
            expect
                .withMessage(testHeader)
                .that(printValue(floatTestOptions.format, resultTested))
                .isEqualTo(printValue(floatTestOptions.format, resultReference));
          }
        } catch (Throwable t) {
          throw new RuntimeException(
              printTestHeader("powToInteger", floatTestOptions.format, arg1, arg2), t);
        }
      }
    }
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
  public void equalToTest() {
    testPredicate("equalTo", (CFloat a, CFloat b) -> a.equalTo(b));
  }

  @Test
  public void lessOrGreaterTest() {
    testPredicate("lessOrGreater", (CFloat a, CFloat b) -> a.lessOrGreater(b));
  }

  @Test
  public void greaterThanTest() {
    testPredicate("greaterThan", (CFloat a, CFloat b) -> a.greaterThan(b));
  }

  @Test
  public void greaterOrEqualTest() {
    testPredicate("greaterOrEqual", (CFloat a, CFloat b) -> a.greaterOrEqual(b));
  }

  @Test
  public void lessThanTest() {
    testPredicate("lessThan", (CFloat a, CFloat b) -> a.lessThan(b));
  }

  @Test
  public void lessOrEqualTest() {
    testPredicate("lessOrEqual", (CFloat a, CFloat b) -> a.lessOrEqual(b));
  }

  @Test
  public void equalsTest() {
    testPredicate("equals", (CFloat a, CFloat b) -> a.equals(b));
  }

  @Test
  public void compareToTestLT() {
    testPredicate("compareTo<", (CFloat a, CFloat b) -> a.compareTo(b) < 0);
  }

  @Test
  public void compareToTestEq() {
    testPredicate("compareTo=", (CFloat a, CFloat b) -> a.compareTo(b) == 0);
  }

  @Test
  public void compareToTestGT() {
    testPredicate("compareTo<", (CFloat a, CFloat b) -> a.compareTo(b) > 0);
  }

  @Test
  public void copySignFromTest() {
    testOperator("copySignFrom", 0, (CFloat a, CFloat b) -> a.copySignFrom(b));
  }

  @Test
  public void castToTest() {
    assume().that(floatTestOptions.format).isNoneOf(Format.Float8, Format.Float16);
    Format other = floatTestOptions.format.equals(Format.Float32) ? Format.Float64 : Format.Float32;
    testOperator(
        "castToTest",
        0,
        (CFloat a) -> a.castTo(toNativeType(other)).castTo(toNativeType(floatTestOptions.format)));
  }

  @Test
  public void castToRoundingTest() {
    assume().that(floatTestOptions.format).isNoneOf(Format.Float8, Format.Float16);
    Format other = floatTestOptions.format.equals(Format.Float32) ? Format.Float64 : Format.Float32;
    testOperator(
        "castToRoundingTest",
        0,
        (CFloat a) ->
            a.castTo(toNativeType(other)).sqrt().castTo(toNativeType(floatTestOptions.format)));
  }

  @Test
  public void castToIntTest() {
    testIntegerFunction("castToIntTest", (CFloat a) -> a.castToOther(CIntegerType.INT).orElse(0));
  }

  @Test
  public void castToLongTest() {
    testIntegerFunction("castToLongTest", (CFloat a) -> a.castToOther(CIntegerType.LONG).orElse(0));
  }

  @Test
  public void fromRationalTest() {
    assume().that(floatTestOptions.format).isEqualTo(Format.Float32);

    for (Integer arg1 : integerTestValues()) {
      for (Integer arg2 : integerTestValues()) {
        if (arg2 != 0) {
          try {
            // Build a fraction from two randomly chosen integers
            Rational rational = Rational.ofLongs(arg1, arg2);

            // Convert the fraction to floating-point, once with Rational.floatValue and once with
            // FloatValue.fromRational
            FloatValue expected = FloatValue.fromFloat(rational.floatValue());
            FloatValue result = FloatValue.fromRational(FloatValue.Format.Float32, rational);

            // Check that the results are the same
            if (!result.equals(expected)) {
              String testHeader = printTestHeader("fromRational", arg1, arg2);
              expect
                  .withMessage(testHeader)
                  .that(printValue(FloatValue.Format.Float32, toBigFloat(result)))
                  .isEqualTo(printValue(FloatValue.Format.Float32, toBigFloat(expected)));
            }
          } catch (Throwable t) {
            throw new RuntimeException(printTestHeader("fromRational", arg1, arg2));
          }
        }
      }
    }
  }

  @Test
  public void fromRationalRoundingTest() {
    // Similar to fromRationalTest, but this time we use 64bit integer values to trigger a rounding
    // error when converting to Float32
    assume().that(floatTestOptions.format).isEqualTo(Format.Float32);

    for (long arg1 : integerTestValues()) {
      for (long arg2 : integerTestValues()) {
        if (arg2 != 0) {
          try {
            // Build a fraction from two randomly chosen integers.
            // We multiply the numerator by a constant that is not just a power of 2 to get our
            // rounding errors.
            Rational rational = Rational.ofLongs(arg1 * 3, arg2);

            // Convert the fraction to floating-point, once with Rational.floatValue and once with
            // FloatValue.fromRational
            FloatValue expected = FloatValue.fromFloat(rational.floatValue());
            FloatValue result = FloatValue.fromRational(FloatValue.Format.Float32, rational);

            // Check that the results are the same
            if (!result.equals(expected)) {
              String testHeader = printTestHeader("fromRational", (int) arg1, (int) arg2);
              expect
                  .withMessage(testHeader)
                  .that(printValue(FloatValue.Format.Float32, toBigFloat(result)))
                  .isEqualTo(printValue(FloatValue.Format.Float32, toBigFloat(expected)));
            }
          } catch (Throwable t) {
            throw new RuntimeException(printTestHeader("fromRational", (int) arg1, (int) arg2));
          }
        }
      }
    }
  }

  @Test
  public void toRationalTest() {
    assume().that(floatTestOptions.format).isEqualTo(Format.Float32);

    for (BigFloat arg : unaryTestValues()) {
      if (!arg.isNegativeZero() && !arg.isInfinite() && !arg.isNaN()) {
        try {
          // Randomly choose a floating-point value. Convert the value to rational, and then back to
          // floating-point again
          FloatValue expected = FloatValue.fromFloat(arg.floatValue());
          FloatValue result =
              FloatValue.fromFloat(expected.toRational().orElseThrow().floatValue());

          // The result should be the same as the initial value
          if (!result.equals(expected)) {
            String testHeader = printTestHeader("fromRational", floatTestOptions.format, arg);
            expect
                .withMessage(testHeader)
                .that(printValue(FloatValue.Format.Float32, toBigFloat(result)))
                .isEqualTo(printValue(FloatValue.Format.Float32, toBigFloat(expected)));
          }
        } catch (Throwable t) {
          throw new RuntimeException(printTestHeader("fromRational", floatTestOptions.format, arg));
        }
      }
    }
  }

  @Test
  public void hardestExpTest() {
    assume().that(floatTestOptions.format).isEqualTo(Format.Float16);
    /* Hardest instance for exp(...) in float16
       {1=23042,
        2=1541,
        3=262,
        4=140,
        5=121,
        6=2684,
        7=3916,
        8=4224,
        9=4456,
        10=4728,
        11=5193,
        12=5207,
        13=2160,
        14=2113,
        15=1715,
        16=1033,
        17=457,
        18=231,
        19=133,
        20=71,
        21=36,
        22=11,
        23=9,
        24=4,
        25=2,
        26=1 <- here
    }*/
    String val = "1.0969e+01";

    FloatValue tested = FloatValue.fromString(floatTestOptions.format, val);
    FloatValue r1 = tested.exp();

    CFloat reference = toReferenceImpl(val);
    CFloat r2 = reference.exp();

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void overflowTest() {
    assume().that(floatTestOptions.format).isEqualTo(Format.Float32);
    // Should overflow as the exponents add up to 127 in binary and the product of the significands
    // is greater than two. After normalization, this should give us infinity.
    String val1 = "1.3835058e+19";
    String val2 = "2.7670116e+19";

    FloatValue tested1 = FloatValue.fromString(floatTestOptions.format, val1);
    FloatValue tested2 = FloatValue.fromString(floatTestOptions.format, val2);
    FloatValue r1 = tested1.multiply(tested2);

    CFloat reference1 = toReferenceImpl(val1);
    CFloat reference2 = toReferenceImpl(val2);
    CFloat r2 = reference1.multiply(reference2);

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void sqrt2Test() {
    assume().that(floatTestOptions.format).isEqualTo(Format.Float32);
    String val = "2.0";

    FloatValue tested = FloatValue.fromString(floatTestOptions.format, val);
    FloatValue r1 = tested.sqrt();

    CFloat reference = toReferenceImpl(val);
    CFloat r2 = reference.sqrt();

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void ln_eTest() {
    assume().that(floatTestOptions.format).isEqualTo(Format.Float32);
    String val = String.valueOf(Math.E);

    FloatValue tested = FloatValue.fromString(floatTestOptions.format, val);
    FloatValue r1 = tested.ln();

    CFloat reference = toReferenceImpl(val);
    CFloat r2 = reference.ln();

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void ln_1Test() {
    assume().that(floatTestOptions.format).isEqualTo(Format.Float32);
    // Calculate ln for the next closest value to 1
    String val = "1.00000011920929";

    FloatValue tested = FloatValue.fromString(floatTestOptions.format, val);
    FloatValue r1 = tested.ln();

    CFloat reference = toReferenceImpl(val);
    CFloat r2 = reference.ln();

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void roundingBugLnTest() {
    assume().that(floatTestOptions.format).isEqualTo(Format.Float32);
    // Example of a value that is not correctly rounded by logf
    String val = "1.10175121e+00";

    FloatValue tested = FloatValue.fromString(floatTestOptions.format, val);
    FloatValue r1 = tested.ln();

    CFloat reference = toReferenceImpl(val);
    CFloat r2 = reference.ln();

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void fromStringBugTest() {
    assume().that(floatTestOptions.format).isEqualTo(Format.Float32);
    // Taken from CFloatTest.createTest:
    // 16777217 = 1000000000000000000000001
    // The number is too large for a float and the last bit needs to be rounded off
    // This causes the rounding test to fail as it keeps looking for another 1 before rounding
    String val = "16777217.0";

    FloatValue tested = FloatValue.fromString(floatTestOptions.format, val);
    CFloat reference = toReferenceImpl(val);

    assertEqual1Ulp(tested, reference);
  }

  @Test
  public void hardExpTest() {
    assume().that(floatTestOptions.format).isEqualTo(Format.Float64);
    // Example of a "hard to round" input for the exponential function
    // Taken from "Handbook of Floating-Point Arithmetic", chapter 12
    String val = "7.5417527749959590085206221024712557043923055744016892276704E-10";

    FloatValue tested = FloatValue.fromString(floatTestOptions.format, val);
    FloatValue r1 = tested.exp();

    CFloat reference = toReferenceImpl(val);
    CFloat r2 = reference.exp();

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void roundingBugExpTest() {
    assume().that(floatTestOptions.format).isEqualTo(Format.Float64);
    // Example of a value that is not correctly rounded by either Math.exp() or exp() from math.h
    String val = "-2.920024588250959e-01";

    FloatValue tested = FloatValue.fromString(floatTestOptions.format, val);
    FloatValue r1 = tested.exp();

    CFloat reference = toReferenceImpl(val);
    CFloat r2 = reference.exp();

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void roundingBugPowTest() {
    assume().that(floatTestOptions.format).isEqualTo(Format.Float64);
    // This value is not correctly rounded by C, but works in Java
    String val1 = "3.5355339059327379e-01";
    String val2 = "-2.2021710233624257e+00";

    FloatValue tested1 = FloatValue.fromString(floatTestOptions.format, val1);
    FloatValue tested2 = FloatValue.fromString(floatTestOptions.format, val2);
    FloatValue r1 = tested1.pow(tested2);

    CFloat reference1 = toReferenceImpl(val1);
    CFloat reference2 = toReferenceImpl(val2);
    CFloat r2 = reference1.powTo(reference2);

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void toStringBugTest() {
    assume().that(floatTestOptions.format).isEqualTo(Format.Float64);
    String val = "1.000001";

    FloatValue tested = FloatValue.fromString(floatTestOptions.format, val);
    String r1 = tested.toString();

    CFloat reference = toReferenceImpl(val);
    String r2 = reference.toString();

    assertThat(r1).isEqualTo(r2);
  }

  @Test
  public void parserValidTest() {
    assume().that(floatTestOptions.format).isEqualTo(Format.Float32);

    Map<String, FloatValue> testValues =
        ImmutableMap.of(
            "nan",
            FloatValue.nan(floatTestOptions.format),
            "inf",
            FloatValue.infinity(floatTestOptions.format),
            ".0",
            FloatValue.zero(floatTestOptions.format),
            "0.",
            FloatValue.zero(floatTestOptions.format));

    for (Entry<String, FloatValue> testValue : testValues.entrySet()) {
      assertThat(FloatValue.fromString(floatTestOptions.format, testValue.getKey()))
          .isEqualTo(testValue.getValue());
    }
  }

  @Test
  public void parserInvalidTest() {
    assume().that(floatTestOptions.format).isEqualTo(Format.Float32);

    for (String input :
        ImmutableList.of(
            "+nan", // Only "nan" and "-nan" are valid. The same goes for "inf" and "-inf"
            "+inf",
            "-+inf",
            "NaN",
            "Inf",
            "infinity",
            "0", // No plain integers. We need a decimal point and/or an exponent
            "00",
            "0,0", // No commas. We use the "C" locale.
            ",0",
            "0,",
            "0.0f", // No "f" and "l" suffixes
            "0x0.1f", // Hexadecimal floats must have an exponent...
            "0x0.1e43", // that starts with a "p", and not "e"...
            "0x0.1pab" // and is a decimal number
            )) {
      assertThrows(
          IllegalArgumentException.class,
          () -> FloatValue.fromString(floatTestOptions.format, input));
    }
  }

  @Test
  public void divisionBugTest() {
    assume().that(floatTestOptions.format).isEqualTo(Format.Float16);
    String val1 = "6.2042e-02";
    String val2 = "1.9456e+04";

    FloatValue tested1 = FloatValue.fromString(floatTestOptions.format, val1);
    FloatValue tested2 = FloatValue.fromString(floatTestOptions.format, val2);
    FloatValue r1 = tested1.divide(tested2);

    CFloat reference1 = toReferenceImpl(val1);
    CFloat reference2 = toReferenceImpl(val2);
    CFloat r2 = reference1.divideBy(reference2);

    assertEqual1Ulp(r1, r2);
  }
}
