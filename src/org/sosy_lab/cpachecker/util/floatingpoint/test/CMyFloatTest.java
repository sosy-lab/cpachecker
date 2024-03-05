// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint.test;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Ignore;
import org.junit.Test;
import org.sosy_lab.common.NativeLibraries;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloat;
import org.sosy_lab.cpachecker.util.floatingpoint.CMyFloat;
import org.sosy_lab.cpachecker.util.floatingpoint.JFloat;

public class CMyFloatTest extends CFloatUnitTest {
  static {
    NativeLibraries.loadLibrary("mpfr_java");
  }

  @Override
  public CFloat toTestedImpl(String repr, int pFloatType) {
    return new CMyFloat(repr, pFloatType);
  }

  @Override
  public CFloat toReferenceImpl(String repr, int pFloatType) {
    return new JFloat(repr, pFloatType);
  }

  protected void assertEqual(CFloat r1, CFloat r2) {
    assertThat(printValue(r1.toFloat())).isEqualTo(printValue(r2.toFloat()));
  }

  @Test
  public void overflowTest() {
    // Should overflow as the exponents add up to 127 in binary and the product of th significands
    // is greater than two. After normalization, this should give us infinity.
    String val1 = "1.3835058e+19";
    String val2 = "2.7670116e+19";

    CFloat myfloat1 = toTestedImpl(val1, 0);
    CFloat myfloat2 = toTestedImpl(val2, 0);

    CFloat jfloat1 = toReferenceImpl(val1, 0);
    CFloat jfloat2 = toReferenceImpl(val2, 0);

    CFloat r1 = myfloat1.multiply(myfloat2);
    CFloat r2 = jfloat1.multiply(jfloat2);
    assertEqual(r1, r2);
  }

  @Test
  public void sqrt2Test() {
    String val = "2.0";
    CFloat myfloat = toTestedImpl(val, 0);
    CFloat jfloat = toReferenceImpl(val, 0);

    CFloat r1 = myfloat.sqrt();
    CFloat r2 = jfloat.sqrt();
    assertEqual(r1, r2);
  }

  @Test
  public void exp1Test() {
    String val = "-10.0";
    CFloat myfloat = toTestedImpl(val, 0);
    CFloat jfloat = toReferenceImpl(val, 0);

    CFloat r1 = myfloat.exp();
    CFloat r2 = jfloat.exp();
    assertEqual(r1, r2);
  }

  @Test
  public void ln_eTest() {
    String val = String.valueOf(Math.E);
    CFloat myfloat = toTestedImpl(val, 0);
    CFloat jfloat = toReferenceImpl(val, 0);

    CFloat r1 = myfloat.ln();
    CFloat r2 = jfloat.ln();
    assertEqual(r1, r2);
  }

  @Test
  public void ln_1Test() {
    // Calculate ln for the next closest value to 1
    String val = "1.00000011920929";
    CFloat myfloat = toTestedImpl(val, 0);
    CFloat jfloat = toReferenceImpl(val, 0);

    CFloat r1 = myfloat.ln();
    CFloat r2 = jfloat.ln();
    assertEqual(r1, r2);
  }

  @Test
  public void addBug1Test() {
    String val1 = "-1.0";
    String val2 = "4.9603518E-8";

    CFloat myfloat1 = toTestedImpl(val1, 0);
    CFloat myfloat2 = toTestedImpl(val2, 0);

    CFloat jfloat1 = toReferenceImpl(val1, 0);
    CFloat jfloat2 = toReferenceImpl(val2, 0);

    CFloat r1 = myfloat1.add(myfloat2);
    CFloat r2 = jfloat1.add(jfloat2);
    assertEqual(r1, r2);
  }

  @Ignore
  @Override
  public void castToByteTest() {
    // Broken in JFloat
  }

  @Ignore
  @Override
  public void castToShortTest() {
    // Broken in JFloat
  }

  @Ignore
  @Override
  public void castToIntTest() {
    // Broken in JFloat
  }

  @Ignore
  @Override
  public void castToLongTest() {
    // Broken in JFloat
  }
}
