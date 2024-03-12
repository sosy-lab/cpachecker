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
  public static ReferenceImpl[] getReferences() { return ReferenceImpl.values(); }

  @Parameter(0)
  public ReferenceImpl refImpl;

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
  public void hardExp1Test() {
    // Example of a "hard to round" input for the exponential function
    // Taken from "Handbook of Floating-Point Arithmetic", chapter 12
    String val = "7.5417527749959590085206221024712557043923055744016892276704E-10";

    CFloat tested = toTestedImpl(val, 1);
    CFloat reference = toReferenceImpl(val, 1);

    CFloat r1 = tested.exp();
    CFloat r2 = reference.exp();

    assertEqual(r1, r2);
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

    assertEqual(r1, r2);
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

    assertEqual(r1, r2);
  }

  @Test
  public void java_expBug2() {
    // One of 94 failed tests
    String val = "6.20027091141992";

    CFloat tested = toTestedImpl(val, 1);
    CFloat reference = toReferenceImpl(val, 1);

    CFloat r1 = tested.exp();
    CFloat r2 = reference.exp();

    assertEqual(r1, r2);
  }

  @Test
  public void java_lnBug() {
    // Only failed test for ln
    String val = "0.9961249915064813";

    CFloat tested = toTestedImpl(val, 1);
    CFloat reference = toReferenceImpl(val, 1);

    CFloat r1 = tested.ln();
    CFloat r2 = reference.ln();

    assertEqual(r1, r2);
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

    assertEqual(r1, r2);
  }

  @Test
  public void native_expBug1() {
    // One of 23 failed tests
    // All failed tests have small exponents. Values are generally between 0.1 and 1
    String val = "386.0202221885229";

    CFloat tested = toTestedImpl(val, 1);
    CFloat reference = toReferenceImpl(val, 1);

    CFloat r1 = tested.exp();
    CFloat r2 = reference.exp();

    assertEqual(r1, r2);
  }

  @Test
  public void native_expBug2() {
    // One of 23 failed tests
    String val = "0.7805213540453771";

    CFloat tested = toTestedImpl(val, 1);
    CFloat reference = toReferenceImpl(val, 1);

    CFloat r1 = tested.exp();
    CFloat r2 = reference.exp();

    assertEqual(r1, r2);
  }

  @Test
  public void native_lnBug() {
    // One of 47 failed tests
    // All failed values have small exponents. Values are generally between 0.8 and 1
    String val = "0.9151734892115296";

    CFloat tested = toTestedImpl(val, 1);
    CFloat reference = toReferenceImpl(val, 1);

    CFloat r1 = tested.ln();
    CFloat r2 = reference.ln();

    assertEqual(r1, r2);
  }

  @Test
  public void native_multiplyBug() {
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

    assertEqual(r1, r2);
  }

  @Test
  public void native_powBug() {
    // One of 41 failed tests (+ one actual bug)
    // Most failed values are between 0.1 and 1 for both arguments.
    String val1 = "0.1";
    String val2 = "0.8388903500470183";

    CFloat tested1 = toTestedImpl(val1, 1);
    CFloat tested2 = toTestedImpl(val2, 1);

    CFloat reference1 = toReferenceImpl(val1, 1);
    CFloat reference2 = toReferenceImpl(val2, 1);

    CFloat r1 = tested1.powTo(tested2);
    CFloat r2 = reference1.powTo(reference2);

    assertEqual(r1, r2);
  }

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

    assertEqual(r1, r2);
  }
}
