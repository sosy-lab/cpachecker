// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint.test;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.NativeLibraries;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloat;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNative;
import org.sosy_lab.cpachecker.util.floatingpoint.CMyFloat;
import org.sosy_lab.cpachecker.util.floatingpoint.JDouble;
import org.sosy_lab.cpachecker.util.floatingpoint.MpFloat;

@RunWith(Parameterized.class)
public class CMyDoubleTest extends CDoubleUnitTest {
  static {
    NativeLibraries.loadLibrary("mpfr_java");
  }

  public enum ReferenceImpl {
    MPFR,
    JAVA,
    NATIVE
  }

  @Parameters(name = "{0}")
  public static ReferenceImpl[] getReferences() {
    return ReferenceImpl.values();
  }

  @Parameter(0)
  public ReferenceImpl refImpl;

  @Override
  protected int ulpError() {
    return refImpl == ReferenceImpl.MPFR ? 0 : 1;
  }

  @Override
  public CFloat toTestedImpl(String repr, int pFloatType) {
    return new CMyFloat(repr, pFloatType);
  }

  @Override
  public CFloat toReferenceImpl(String repr, int pFloatType) {
    return switch (refImpl) {
      case MPFR -> new MpFloat(repr, pFloatType);
      case JAVA -> new JDouble(repr, pFloatType);
      case NATIVE -> new CFloatNative(repr, pFloatType);
    };
  }

  @Ignore
  @Override
  public void powToIntegralTest() {
    // TODO: Implement 'long double' support to fix
  }

  @Ignore
  @Override
  public void castToTest() {
    // TODO: Implement 'long double' support to fix
  }

  @Ignore
  @Override
  public void castToRoundingTest() {
    // TODO: Implement 'long double' support to fix
  }

  @Ignore
  @Override
  public void castToByteTest() {
    // Disabled
  }

  @Ignore
  @Override
  public void castToShortTest() {
    // Disabled
  }

  @Ignore
  @Override
  public void castToIntTest() {
    // Disabled
  }

  @Ignore
  @Override
  public void castToLongTest() {
    // Disabled
  }

  @Test
  public void hardExpTest() {
    // Example of a "hard to round" input for the exponential function
    // Taken from "Handbook of Floating-Point Arithmetic", chapter 12
    String val = "7.5417527749959590085206221024712557043923055744016892276704E-10";

    CFloat tested = toTestedImpl(val, 1);
    CFloat reference = toReferenceImpl(val, 1);

    CFloat r1 = tested.exp();
    CFloat r2 = reference.exp();

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void mpfr_powBug() {
    // Same as in 32 bits before we increased precision
    String val1 = "1.7976931348623157E308";
    String val2 = "0.5";

    CFloat tested1 = toTestedImpl(val1, 1);
    CFloat tested2 = toTestedImpl(val2, 1);

    CFloat reference1 = toReferenceImpl(val1, 1);
    CFloat reference2 = toReferenceImpl(val2, 1);

    CFloat r1 = tested1.powTo(tested2);
    CFloat r2 = reference1.powTo(reference2);

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void java_expBug1() {
    // One of 94 failed test cases
    // All failed test inputs have small exponents (mostly +/- 3)
    String val = "128";

    CFloat tested = toTestedImpl(val, 1);
    CFloat reference = toReferenceImpl(val, 1);

    CFloat r1 = tested.exp();
    CFloat r2 = reference.exp();

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void java_expBug2() {
    // One of 94 failed tests
    String val = "6.20027091141992";

    CFloat tested = toTestedImpl(val, 1);
    CFloat reference = toReferenceImpl(val, 1);

    CFloat r1 = tested.exp();
    CFloat r2 = reference.exp();

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void java_lnBug() {
    // Only failed test for ln
    String val = "0.9961249915064813";

    CFloat tested = toTestedImpl(val, 1);
    CFloat reference = toReferenceImpl(val, 1);

    CFloat r1 = tested.ln();
    CFloat r2 = reference.ln();

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void java_powBug() {
    // One of 27 failed tests (+ one actual bug: see powMyFloatBug)
    // Most failed values are between 0.1 and 1 for both arguments
    String val1 = "0.7411183464344743";
    String val2 = "0.047265869196129406";

    CFloat tested1 = toTestedImpl(val1, 1);
    CFloat tested2 = toTestedImpl(val2, 1);

    CFloat reference1 = toReferenceImpl(val1, 1);
    CFloat reference2 = toReferenceImpl(val2, 1);

    CFloat r1 = tested1.powTo(tested2);
    CFloat r2 = reference1.powTo(reference2);

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void native_expBug1() {
    // One of 23 failed tests
    // All failed tests have small exponents. Values are generally between 0.1 and 1
    String val = "0.7611478141380555";

    // X1  = 1.1100001011011010100101010100100100100101000100111110 10000000000000000000000000000...
    // X15 = 1.0001001000000011100000010101101101011101100010000000 00010001111110111010010011011...
    // X16 = 1.0001001000000011100000010101101101011101100010000001 01101111101001100100111011101...
    // X28 = 1.0001001000000011100000010101101101011101100010000001 01111111111111101011011111111...
    //                                                                             ^ 53+16 bits

    CFloat tested = toTestedImpl(val, 1);
    CFloat reference = toReferenceImpl(val, 1);

    CFloat r1 = tested.exp();
    CFloat r2 = reference.exp();

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void native_expBug2() {
    // One of 14 failed tests
    String val = "0.01608839922770744";

    // X8 = 1.0000010000100110111001011000010011001100010011001011 011111111111101110010011101111...
    //                                                                          ^ 53+14 bits

    CFloat tested = toTestedImpl(val, 1);
    CFloat reference = toReferenceImpl(val, 1);

    CFloat r1 = tested.exp();
    CFloat r2 = reference.exp();

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void native_lnBug() {
    // One of 11 failed tests
    // All failed values have small exponents. Values are generally between 0.8 and 1
    String val = "0.9151734892115296";

    // X2 = 1.0110101100010011011110110100101010100011010011100110 100000000000000010110101111110...
    //                                                                             ^ 52+16bits

    CFloat tested = toTestedImpl(val, 1);
    CFloat reference = toReferenceImpl(val, 1);

    CFloat r1 = tested.ln();
    CFloat r2 = reference.ln();

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void native_powBug() {
    // One of 15 failed tests (+ one actual bug)
    // Most failed values are between 0.1 and 1 for both arguments.
    String val1 = "0.9412491794821144";
    String val2 = "0.027169061868886568";

    CFloat tested1 = toTestedImpl(val1, 1);
    CFloat tested2 = toTestedImpl(val2, 1);

    CFloat reference1 = toReferenceImpl(val1, 1);
    CFloat reference2 = toReferenceImpl(val2, 1);

    CFloat r1 = tested1.powTo(tested2);
    CFloat r2 = reference1.powTo(reference2);

    assertEqual1Ulp(r1, r2);
  }

  // Fixed by enabling SSE
  @Test
  public void native_divideByBug() {
    // One of 12 failed tests
    // Most failed values are between 0.1 and 1 for both arguments.
    String val1 = "0.24053641567148587";
    String val2 = "0.6839413314680614";

    CFloat tested1 = toTestedImpl(val1, 1);
    CFloat tested2 = toTestedImpl(val2, 1);

    CFloat reference1 = toReferenceImpl(val1, 1);
    CFloat reference2 = toReferenceImpl(val2, 1);

    CFloat r1 = tested1.divideBy(tested2);
    CFloat r2 = reference1.divideBy(reference2);

    assertEqual1Ulp(r1, r2);
  }

  // Fixed by enabling SSE
  @Test
  public void native_multiplyBug() {
    // x87 has issues with double rounding:
    //   1.0110001001110100001110101010000000111001100001011101
    // x 1.0110011011101110100100110110000101101110100101101011           v x87 ends here
    // = 1.11110000111110001101111000011010001101100011100101101000000000000001110100001011001010...
    //   1.1111000011111000110111100001101000110110001110010110               ^ only > 0.5 here
    //
    //   1.1001000011110001011111111011001010010110001001001110
    // x 1.0101011001101100000110010111100100100100100111000000           v x87 ends here
    // = 1.00001100001001011111011100101111111000100111000001010111111111111111111110011111010001...
    //   1.0000110000100101111101110010111111100010011100000110                     ^ only zero here

    // One of 6 failed tests
    // All failed values are between 0.5 and 1 for both arguments.
    String val1 = "0.6922930069529333";
    String val2 = "0.7010389381824046";

    CFloat tested1 = toTestedImpl(val1, 1);
    CFloat tested2 = toTestedImpl(val2, 1);

    CFloat reference1 = toReferenceImpl(val1, 1);
    CFloat reference2 = toReferenceImpl(val2, 1);

    CFloat r1 = tested1.multiply(tested2);
    CFloat r2 = reference1.multiply(reference2);

    assertEqual1Ulp(r1, r2);
  }
}
