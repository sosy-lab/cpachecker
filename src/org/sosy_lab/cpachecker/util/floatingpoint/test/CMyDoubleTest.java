// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint.test;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.sosy_lab.common.NativeLibraries;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloat;
import org.sosy_lab.cpachecker.util.floatingpoint.CMyFloat;
import org.sosy_lab.cpachecker.util.floatingpoint.JDouble;

public class CMyDoubleTest extends CDoubleUnitTest {
  static {
    NativeLibraries.loadLibrary("mpfr_java");
  }

  @Override
  public CFloat toTestedImpl(String repr, int pFloatType) {
    return new CMyFloat(repr, pFloatType);
  }

  @Override
  public CFloat toReferenceImpl(String repr, int pFloatType) {
    return new JDouble(repr, pFloatType);
  }

  @Test
  public void overflowTest() {
    // Should overflow as the exponents add up to 127 in binary and the product of th significands
    // is greater than two. After normalization, this should give us infinity.
    String val1 = "1.3835058e+19";
    String val2 = "2.7670116e+19";

    CFloat myfloat1 = toTestedImpl(val1, 1);
    CFloat myfloat2 = toTestedImpl(val2, 1);

    CFloat jfloat1 = toReferenceImpl(val1, 1);
    CFloat jfloat2 = toReferenceImpl(val2, 1);

    CFloat r1 = myfloat1.multiply(myfloat2);
    CFloat r2 = jfloat1.multiply(jfloat2);
    assertEqual(r1, r2);
  }

  @Test
  public void sqrt2Test() {
    String val = "2.0";
    CFloat myfloat = toTestedImpl(val, 1);
    CFloat jfloat = toReferenceImpl(val, 1);

    CFloat r1 = myfloat.sqrt();
    CFloat r2 = jfloat.sqrt();
    assertEqual(r1, r2);
  }

  @Test
  public void powBugTest() {
    String val1 = "3.4028235E38";
    String val2 = "0.5";

    CFloat myfloat1 = toTestedImpl(val1, 1);
    CFloat myfloat2 = toTestedImpl(val2, 1);

    CFloat jfloat1 = toReferenceImpl(val1, 1);
    CFloat jfloat2 = toReferenceImpl(val2, 1);

    CFloat r1 = myfloat1.powTo(myfloat2);
    CFloat r2 = jfloat1.powTo(jfloat2);

    assertEqual(r1, r2);
  }

  @Test
  public void hardExp1Test() {
    // Example of a "hard to round" input for the exponential function
    // Taken from "Handbook of Floating-Point Arithmetic", chapter 12
    String val = "7.5417527749959590085206221024712557043923055744016892276704E-10";

    CFloat myfloat = toTestedImpl(val, 1);
    CFloat jfloat = new JDouble(val, 1);

    CFloat r1 = myfloat.exp();
    CFloat r2 = jfloat.exp();

    assertThat(printValue(r1.toDouble())).isEqualTo(printValue(r2.toDouble()));
  }

  @Test
  public void exp1Test() {
    String val = "-10.0";
    CFloat myfloat = toTestedImpl(val, 1);
    CFloat jfloat = toReferenceImpl(val, 1);

    CFloat r1 = myfloat.exp();
    CFloat r2 = jfloat.exp();
    assertEqual(r1, r2);
  }

  @Test
  public void ln_eTest() {
    String val = String.valueOf(Math.E);
    CFloat myfloat = toTestedImpl(val, 1);
    CFloat jfloat = toReferenceImpl(val, 1);

    CFloat r1 = myfloat.ln();
    CFloat r2 = jfloat.ln();
    assertEqual(r1, r2);
  }

  @Test
  public void ln_1Test() {
    // Calculate ln for the next closest value to 1
    String val = "1.00000011920929";
    CFloat myfloat = toTestedImpl(val, 1);
    CFloat jfloat = toReferenceImpl(val, 1);

    CFloat r1 = myfloat.ln();
    CFloat r2 = jfloat.ln();
    assertEqual(r1, r2);
  }
}
