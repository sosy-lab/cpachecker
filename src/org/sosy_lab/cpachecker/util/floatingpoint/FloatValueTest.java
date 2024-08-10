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

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Correspondence.BinaryPredicate;
import com.google.common.truth.Expect;
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.kframework.mpfr.BigFloat;
import org.kframework.mpfr.BinaryMathContext;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CFloatType;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNativeAPI.CIntegerType;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue.Format;

/**
 * Abstract test class for the {@link CFloat} interface.
 *
 * <p>The idea behind this class is to compare two different implementations of the CFloat
 * interface. We always use {@link CFloatImpl} as the "tested" implementation and compare it to a
 * second "reference" implementation. There are currently 3 supported implementations that can be
 * used as a reference:
 *
 * <ul>
 *   <li>{@link CFloatNative}, uses native C code to calculate its results
 *   <li>{@link MpfrFloat}, uses BigFloats to access MPFR
 *   <li>{@link JFloat} ({@link JDouble}), uses normal Java floats (doubles) for its calculations
 * </ul>
 *
 * <p>Subclasses are expected to overload the abstract method {@link
 * FloatValueTest#configuration.pReference} to select which implementations is supposed to be used
 * in the comparison. The test class will then automatically generate test inputs for all methods of
 * the CFloat interface and compare the results of the tested implementation with those of the
 * reference implementation on all of those inputs.
 *
 * <p>The methods {@link FloatValueTest#unaryTestValues()} and {@link
 * FloatValueTest#binaryTestValues()} can be overwritten to change the set of test inputs that will
 * be generated. The following classes of test values are supported by the implementation:
 *
 * <ul>
 *   <li>{@link FloatValueTest#floatConstants(Format)}
 *   <li>{@link FloatValueTest#floatPowers(Format, int, BigFloat, int, BigFloat)}
 *   <li>{@link FloatValueTest#floatRandom(Format, int)}
 *   <li>{@link FloatValueTest#allFloats(Format)}
 * </ul>
 *
 * <p>The default behaviour for both {@link FloatValueTest#unaryTestValues()} and {@link
 * FloatValueTest#binaryTestValues()} is to use a combination of the first 3 test value classes.
 *
 * <p>The abstract method {@link FloatValueTest#configuration.pFormat} also needs to be overridden
 * by the subclass to select the bit width of the floating point values that will be generated.
 */
@SuppressWarnings("deprecation")
@RunWith(Parameterized.class)
public class FloatValueTest {
  /** Supported reference implementations */
  public enum ReferenceImpl {
    MPFR,
    JAVA,
    NATIVE
  }

  /** Supported floating point formats and the number of test values to generate for each of them */
  public static ImmutableMap<Format, Integer> supportedFormats =
      ImmutableMap.of(
          Format.Float8,
          50000,
          Format.Float16,
          50000,
          Format.Float32,
          50000,
          Format.Float64,
          25000,
          Format.Extended,
          15000);

  /**
   * Test configuration
   *
   * @param pFormat Precision of the values
   * @param pReference Reference implementation to compare the results to
   * @param pNumber Number of test values to generate
   */
  public record Configuration(Format pFormat, ReferenceImpl pReference, Integer pNumber) {
    @Override
    public String toString() {
      String floatFormat =
          pFormat.equals(Format.Extended)
              ? "Extended"
              : String.format("Float%s", 1 + pFormat.expBits() + pFormat.sigBits());
      return pReference + ", " + floatFormat;
    }
  }

  @Parameters(name = "{0}")
  public static Configuration[] getConfigurations() {
    ImmutableList.Builder<Configuration> builder = ImmutableList.builder();
    for (Format precision : supportedFormats.keySet()) {
      for (ReferenceImpl reference : ReferenceImpl.values()) {
        if (reference.equals(ReferenceImpl.MPFR)
            || precision.equals(Format.Float32)
            || precision.equals(Format.Float64)
            || (reference.equals(ReferenceImpl.NATIVE) && precision.equals(Format.Extended))) {
          boolean enableExpensiveTests = true;
          // System.getProperty("enableExpensiveTests", "off").equals("on");
          if (precision.equals(Format.Float32) || enableExpensiveTests) {
            builder.add(
                new Configuration(
                    precision, reference,
                    100)); // enableExpensiveTests ? supportedFormats.get(precision) : 100));
          }
        }
      }
    }
    return builder.build().toArray(new FloatValueTest.Configuration[0]);
  }

  @Parameter(0)
  public Configuration configuration;

  /**
   * Convert a CFloat value to BigFloat.
   *
   * <p>This is used in the tests to convert the result of the operation back to a BigFloat value.
   */
  BigFloat toBigFloat(CFloat value) {
    if (value instanceof MpfrFloat val) {
      return val.toBigFloat();
    } else if (value instanceof CFloatImpl val) {
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
      return new BigFloat(
          val.isNegative(), val.getValue().getSignificand(), val.getValue().getExponent(), context);
    } else {
      CFloatType toType = value.getType();
      if (value instanceof CFloatNative val && toType == CFloatType.LONG_DOUBLE) {
        // Special case for "extended precision" with CFloatNative
        BinaryMathContext context = new BinaryMathContext(64, 15);
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
        CFloatWrapper wrapper = val.getWrapper();

        long exponent = (wrapper.getExponent() & 0x7FFF) - Format.Extended.bias();
        BigInteger significand = new BigInteger(Long.toUnsignedString(wrapper.getMantissa()));

        return new BigFloat(val.isNegative(), significand, exponent, context);
      } else {
        // We have either a CFloatNative or a JFloat/JDouble and only need to support single and
        // double precision
        return switch (toType) {
          case SINGLE -> new BigFloat(value.toFloat(), BinaryMathContext.BINARY32);
          case DOUBLE -> new BigFloat(value.toDouble(), BinaryMathContext.BINARY64);
          case LONG_DOUBLE -> throw new UnsupportedOperationException();
          default -> throw new IllegalArgumentException();
        };
      }
    }
  }

  /** Convert a {@link CFloat} value to an Integer. */
  Integer toInteger(CFloat value) {
    return toBigFloat(value).intValue();
  }

  /** Construct a CFloatImpl from a BigFloat test value. */
  CFloatImpl testValueToCFloatImpl(BigFloat value, Format format) {
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
  static String toPlainString(BigFloat value) {
    String r = printBigFloat(value);
    if (r.contains("e")) {
      r = new BigDecimal(r).toPlainString();
    }
    return r;
  }

  /** Generate a list of floating point constants that cover all special case values. */
  static Iterable<BigFloat> floatConstants(Format format) {
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
  static Iterable<BigFloat> floatPowers(Format format, int c, BigFloat a, int p, BigFloat x) {
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
  static Iterable<BigFloat> floatRandom(Format format, int n) {
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
  static Iterable<BigFloat> allFloats(Format format) {
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
  Iterable<BigFloat> unaryTestValues() {
    Format format = configuration.pFormat;
    if (format.equals(Format.Float8) || format.equals(Format.Float16)) {
      return allFloats(format);
    } else {
      BinaryMathContext context = new BinaryMathContext(format.sigBits() + 1, format.expBits());
      BigFloat constant = new BigFloat(0.5f, context);
      return FluentIterable.concat(
          floatConstants(format),
          floatPowers(format, 14, constant, 20, constant),
          floatRandom(format, configuration.pNumber));
    }
  }

  /**
   * The set of test inputs that should be used for binary operations in the CFloat interface. The
   * values will be used for both arguments separately. So if there are k test values in this set,
   * the number of test runs for a binary operation will be k^2.
   */
  Iterable<BigFloat> binaryTestValues() {
    Format format = configuration.pFormat;
    if (format.equals(Format.Float8)) {
      return allFloats(format);
    } else {
      BinaryMathContext context = new BinaryMathContext(format.sigBits() + 1, format.expBits());
      BigFloat constant = new BigFloat(0.5f, context);
      return FluentIterable.concat(
          floatConstants(format),
          floatPowers(format, 3, constant, 3, constant),
          floatRandom(format, (int) Math.sqrt(configuration.pNumber)));
    }
  }

  /** Generate a list of special case integer values. */
  static Iterable<BigFloat> integerConstants(Format format) {
    BinaryMathContext context = new BinaryMathContext(format.sigBits() + 1, format.expBits());
    return ImmutableList.of(
        new BigFloat(Integer.MIN_VALUE, context),
        new BigFloat(-1.0, context),
        BigFloat.zero(context.precision),
        new BigFloat(1.0, context),
        new BigFloat(Integer.MAX_VALUE, context)
            .nextDown(context.minExponent, context.maxExponent) // Avoid overflow issues
        );
  }

  /** Generate n random integer values. */
  static Iterable<BigFloat> integerRandom(Format format, int n) {
    ImmutableList.Builder<BigFloat> builder = ImmutableList.builder();
    BinaryMathContext context = new BinaryMathContext(format.sigBits() + 1, format.expBits());

    long maxValue = FloatValue.maxValue(format).toInt().orElse(Integer.MAX_VALUE);
    long minValue = FloatValue.maxValue(format).negate().toInt().orElse(Integer.MIN_VALUE);

    Random random = new Random(0);

    for (int c = 0; c < n; c++) {
      long r = random.nextLong(maxValue - minValue);
      builder.add(new BigFloat(r + minValue, context));
    }
    return builder.build();
  }

  /** Generate integer test inputs that include both special cases and random values. */
  Iterable<BigFloat> integerTestValues() {
    Format format = configuration.pFormat;
    return FluentIterable.concat(
        integerConstants(format), integerRandom(format, (int) Math.sqrt(configuration.pNumber)));
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
  static String toBits(Format format, BigFloat value) {
    String sign = value.sign() ? "1" : "0";

    // Print the exponent
    long valueExp = value.exponent(format.minExp(), format.maxExp()) + format.maxExp();
    String exponent = Long.toString(valueExp, 2);
    exponent = "0".repeat(calculateExpWidth(format) - exponent.length()) + exponent;

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

  String printValue(BigFloat value) {
    return String.format("%s [%s]", toBits(configuration.pFormat, value), printBigFloat(value));
  }

  String printTestHeader(String name, BigFloat arg) {
    return String.format("Testcase %s(%s): ", name, printValue(arg));
  }

  String printTestHeader(String name, BigFloat arg1, BigFloat arg2) {
    return String.format("Testcase %s(%s, %s): ", name, printValue(arg1), printValue(arg2));
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
    return configuration.pReference == ReferenceImpl.MPFR ? 0 : 1;
  }

  /** Returns a list of all float values in the error range. */
  private List<String> errorRange(int pDistance, BigFloat pValue) {
    if (pDistance == 0 || pValue.isNaN()) {
      return ImmutableList.of(printValue(pValue));
    }
    if (pDistance == 1) {
      BigFloat minus1Ulp =
          pValue.nextDown(configuration.pFormat.minExp(), configuration.pFormat.maxExp());
      BigFloat plus1Ulp =
          pValue.nextUp(configuration.pFormat.minExp(), configuration.pFormat.maxExp());

      return ImmutableList.of(printValue(minus1Ulp), printValue(pValue), printValue(plus1Ulp));
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
          String testHeader = printTestHeader(name, arg);
          if (ulps == 0) {
            expect
                .withMessage(testHeader)
                .that(printValue(resultTested))
                .isEqualTo(printValue(resultReference));
          } else {
            expect
                .withMessage(testHeader)
                .that(printValue(resultTested))
                .isIn(errorRange(ulps, resultReference));
          }
        }
      } catch (Throwable t) {
        throw new RuntimeException(printTestHeader(name, arg), t);
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
            String testHeader = printTestHeader(name, arg1, arg2);
            if (ulps == 0) {
              expect
                  .withMessage(testHeader)
                  .that(printValue(resultTested))
                  .isEqualTo(printValue(resultReference));
            } else {
              expect
                  .withMessage(testHeader)
                  .that(printValue(resultTested))
                  .isIn(errorRange(ulps, resultReference));
            }
          }
        } catch (Throwable t) {
          throw new RuntimeException(printTestHeader(name, arg1, arg2), t);
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
          String testHeader = printTestHeader(name, arg);
          expect.withMessage(testHeader).that(resultTested).isEqualTo(resultReference);
        }
      } catch (Throwable t) {
        throw new RuntimeException(printTestHeader(name, arg), t);
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
            String testHeader = printTestHeader(name, arg1, arg2);
            expect.withMessage(testHeader).that(resultTested).isEqualTo(resultReference);
          }
        } catch (Throwable t) {
          throw new RuntimeException(printTestHeader(name, arg1, arg2), t);
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
          String testHeader = printTestHeader(name, arg);
          expect.withMessage(testHeader).that(resultsTested).isEqualTo(resultReference);
        }
      } catch (Throwable t) {
        throw new RuntimeException(printTestHeader(name, arg), t);
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
          String testHeader = printTestHeader(name, arg);
          expect.withMessage(testHeader).that(resultTested).isEqualTo(resultReference);
        }
      } catch (Throwable t) {
        throw new RuntimeException(printTestHeader(name, arg), t);
      }
    }
  }

  void assertEqual1Ulp(CFloat r1, CFloat r2) {
    assertThat(printValue(toBigFloat(r1))).isIn(errorRange(ulpError(), toBigFloat(r2)));
  }

  /** Create a test value for the tested implementation. */
  CFloat toTestedImpl(BigFloat value) {
    return testValueToCFloatImpl(value, configuration.pFormat);
  }

  /** Create a test value for the tested implementation by parsing a String. */
  CFloat toTestedImpl(String repr) {
    return new CFloatImpl(repr, configuration.pFormat);
  }

  /** Convert Format to a matching native floating point type */
  private static CFloatType toNativeType(Format pFormat) {
    if (pFormat.equals(Format.Float32)) {
      return CFloatType.SINGLE;
    } else if (pFormat.equals(Format.Float64)) {
      return CFloatType.DOUBLE;
    } else if (pFormat.equals(Format.Extended)) {
      return CFloatType.LONG_DOUBLE;
    } else {
      throw new IllegalArgumentException();
    }
  }

  /** Create a test value for the reference implementation. */
  CFloat toReferenceImpl(BigFloat value) {
    checkState(
        configuration.pFormat.equals(Format.Float32)
            || configuration.pFormat.equals(Format.Float64)
            || (configuration.pReference == ReferenceImpl.NATIVE
                && configuration.pFormat.equals(Format.Extended))
            || configuration.pReference == ReferenceImpl.MPFR,
        "Backend %s only support float32 and float64 as format",
        configuration.pReference);
    return switch (configuration.pReference) {
      case MPFR -> new MpfrFloat(value, configuration.pFormat);
      case JAVA ->
          configuration.pFormat.equals(Format.Float32)
              ? (value.isNaN()
                  ? new JFloat(value.sign() ? Float.intBitsToFloat(0xFFC00000) : Float.NaN)
                  : new JFloat(value.floatValue()))
              : (value.isNaN()
                  ? new JDouble(
                      value.sign() ? Double.longBitsToDouble(0xFFF8000000000000L) : Float.NaN)
                  : new JDouble(value.doubleValue()));
      case NATIVE -> new CFloatNative(toPlainString(value), toNativeType(configuration.pFormat));
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
  CFloat toReferenceImpl(String repr) {
    BinaryMathContext context =
        new BinaryMathContext(configuration.pFormat.sigBits() + 1, configuration.pFormat.expBits());
    return toReferenceImpl(parseBigFloat(context, repr));
  }

  @Test
  public void constTest() {
    // This test checks that test values are correctly convert to values in the implementation.
    testOperator("const", 0, (CFloat a) -> a);
  }

  @Test
  public void fromStringTest() {
    for (BigFloat arg : unaryTestValues()) {
      try {
        // Calculate result with the reference implementation
        BigFloat resultReference = toBigFloat(toReferenceImpl(printBigFloat(arg)));

        // Calculate result with the tested implementation
        BigFloat resultTested = toBigFloat(toTestedImpl(printBigFloat(arg)));

        // Calculate result with the reference implementation
        if (!resultTested.equals(resultReference)) {
          String testHeader = printTestHeader("fromString", arg);
          expect
              .withMessage(testHeader)
              .that(printValue(resultTested))
              .isEqualTo(printValue(resultReference));
        }
      } catch (Throwable t) {
        throw new RuntimeException(printTestHeader("fromString", arg), t);
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
  public void moduloTest() {
    testOperator("modulo", 0, (CFloat a, CFloat b) -> a.modulo(b));
  }

  @Test
  public void remainderTest() {
    testOperator("remainder", 0, (CFloat a, CFloat b) -> a.remainder(b));
  }

  private static int findClosest(Map<Integer, Float> accum, float p) {
    for (Integer k : accum.keySet().stream().sorted().toList()) {
      if (accum.get(k) >= p) {
        return k;
      }
    }
    throw new IllegalArgumentException();
  }

  // Print statistics about the required bit width in ln, exp and pow
  @SuppressWarnings("unused")
  private static String printStatistics(Map<Integer, Integer> stats) {
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
    Iterable<BigFloat> integers = integerTestValues();
    Iterable<BigFloat> positiveIntegers =
        FluentIterable.from(integers).filter((BigFloat a) -> !a.sign());

    // Native implementation does not support negative exponents
    Iterable<BigFloat> expValues =
        configuration.pReference.equals(ReferenceImpl.NATIVE) ? positiveIntegers : integers;

    testOperator(
        "powToInteger",
        0,
        (CFloat a, CFloat b) -> a.powToIntegral(toInteger(b)),
        binaryTestValues(),
        expValues);
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
  public void notEqualToTest() {
    testPredicate("notEqualTo", (CFloat a, CFloat b) -> a.notEqualTo(b));
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
    assume().that(configuration.pFormat).isNoneOf(Format.Float8, Format.Float16);
    Format other = configuration.pFormat.equals(Format.Float32) ? Format.Float64 : Format.Float32;
    testOperator(
        "castToTest",
        0,
        (CFloat a) -> a.castTo(toNativeType(other)).castTo(toNativeType(configuration.pFormat)));
  }

  @Test
  public void castToRoundingTest() {
    assume().that(configuration.pFormat).isNoneOf(Format.Float8, Format.Float16);
    Format other = configuration.pFormat.equals(Format.Float32) ? Format.Float64 : Format.Float32;
    testOperator(
        "castToRoundingTest",
        0,
        (CFloat a) ->
            a.castTo(toNativeType(other)).sqrt().castTo(toNativeType(configuration.pFormat)));
  }

  @Test
  public void castToByteTest() {
    testIntegerFunction("castToByteTest", (CFloat a) -> a.castToOther(CIntegerType.CHAR).orElse(0));
  }

  @Test
  public void castToShortTest() {
    testIntegerFunction(
        "castToShortTest", (CFloat a) -> a.castToOther(CIntegerType.SHORT).orElse(0));
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
  public void hardestExpTest() {
    assume().that(configuration.pFormat).isEqualTo(Format.Float16);
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

    CFloat tested = toTestedImpl(val);
    CFloat reference = toReferenceImpl(val);

    CFloat r1 = tested.exp();
    CFloat r2 = reference.exp();

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void overflowTest() {
    assume().that(configuration.pFormat).isEqualTo(Format.Float32);
    // Should overflow as the exponents add up to 127 in binary and the product of th significands
    // is greater than two. After normalization, this should give us infinity.
    String val1 = "1.3835058e+19";
    String val2 = "2.7670116e+19";

    CFloat tested1 = toTestedImpl(val1);
    CFloat tested2 = toTestedImpl(val2);

    CFloat reference1 = toReferenceImpl(val1);
    CFloat reference2 = toReferenceImpl(val2);

    CFloat r1 = tested1.multiply(tested2);
    CFloat r2 = reference1.multiply(reference2);

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void sqrt2Test() {
    assume().that(configuration.pFormat).isEqualTo(Format.Float32);
    String val = "2.0";

    CFloat tested = toTestedImpl(val);
    CFloat reference = toReferenceImpl(val);

    CFloat r1 = tested.sqrt();
    CFloat r2 = reference.sqrt();

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void ln_eTest() {
    assume().that(configuration.pFormat).isEqualTo(Format.Float32);
    String val = String.valueOf(Math.E);

    CFloat tested = toTestedImpl(val);
    CFloat reference = toReferenceImpl(val);

    CFloat r1 = tested.ln();
    CFloat r2 = reference.ln();

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void ln_1Test() {
    assume().that(configuration.pFormat).isEqualTo(Format.Float32);
    // Calculate ln for the next closest value to 1
    String val = "1.00000011920929";

    CFloat tested = toTestedImpl(val);
    CFloat reference = toReferenceImpl(val);

    CFloat r1 = tested.ln();
    CFloat r2 = reference.ln();

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void roundingBugLnTest() {
    assume().that(configuration.pFormat).isEqualTo(Format.Float32);
    // Example of a value that is not correctly rounded by logf
    String val = "1.10175121e+00";

    CFloat tested = toTestedImpl(val);
    CFloat reference = toReferenceImpl(val);

    CFloat r1 = tested.ln();
    CFloat r2 = reference.ln();

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void fromStringBugTest() {
    assume().that(configuration.pFormat).isEqualTo(Format.Float32);
    // Taken from CFloatTest.createTest:
    // 16777217 = 1000000000000000000000001
    // The number is too large for a float and the last bit needs to be rounded off
    // This causes the rounding test to fail as it keeps looking for another 1 before rounding
    String val = "16777217";

    CFloat tested = toTestedImpl(val);
    CFloat reference = toReferenceImpl(val);

    assertEqual1Ulp(tested, reference);
  }

  @Test
  public void hardExpTest() {
    assume().that(configuration.pFormat).isEqualTo(Format.Float64);
    // Example of a "hard to round" input for the exponential function
    // Taken from "Handbook of Floating-Point Arithmetic", chapter 12
    String val = "7.5417527749959590085206221024712557043923055744016892276704E-10";

    CFloat tested = toTestedImpl(val);
    CFloat reference = toReferenceImpl(val);

    CFloat r1 = tested.exp();
    CFloat r2 = reference.exp();

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void roundingBugExpTest() {
    assume().that(configuration.pFormat).isEqualTo(Format.Float64);
    // Example of a value that is not correctly rounded by either Math.exp() or exp() from math.h
    String val = "-2.920024588250959e-01";

    CFloat tested = toTestedImpl(val);
    CFloat reference = toReferenceImpl(val);

    CFloat r1 = tested.exp();
    CFloat r2 = reference.exp();

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void roundingBugPowTest() {
    assume().that(configuration.pFormat).isEqualTo(Format.Float64);
    // This value is not correctly rounded by C, but works in Java
    String val1 = "3.5355339059327379e-01";
    String val2 = "-2.2021710233624257e+00";

    CFloat tested1 = toTestedImpl(val1);
    CFloat tested2 = toTestedImpl(val2);

    CFloat reference1 = toReferenceImpl(val1);
    CFloat reference2 = toReferenceImpl(val2);

    CFloat r1 = tested1.powTo(tested2);
    CFloat r2 = reference1.powTo(reference2);

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void toStringBugTest() {
    assume().that(configuration.pFormat).isEqualTo(Format.Float64);
    String val = "1.000001";

    CFloat tested = toTestedImpl(val);
    CFloat reference = toReferenceImpl(val);

    String r1 = tested.toString();
    String r2 = reference.toString();

    assertThat(r1).isEqualTo(r2);
  }
}
