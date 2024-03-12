// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.NativeLibraries;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloat;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNative;
import org.sosy_lab.cpachecker.util.floatingpoint.CMyFloat;
import org.sosy_lab.cpachecker.util.floatingpoint.JFloat;
import org.sosy_lab.cpachecker.util.floatingpoint.MpFloat;

@RunWith(Parameterized.class)
public class CMyFloatTest extends CFloatUnitTest {
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
    return refImpl == CMyFloatTest.ReferenceImpl.MPFR ? 0 : 1;
  }

  @Override
  public CFloat toTestedImpl(String repr, int pFloatType) {
    return new CMyFloat(repr, pFloatType);
  }

  @Override
  public CFloat toReferenceImpl(String repr, int pFloatType) {
    return switch (refImpl) {
      case MPFR -> new MpFloat(repr, pFloatType);
      case JAVA -> new JFloat(repr, pFloatType);
      case NATIVE -> new CFloatNative(repr, pFloatType);
    };
  }

  @Test
  public void overflowTest() {
    // Should overflow as the exponents add up to 127 in binary and the product of th significands
    // is greater than two. After normalization, this should give us infinity.
    String val1 = "1.3835058e+19";
    String val2 = "2.7670116e+19";

    CFloat tested1 = toTestedImpl(val1, 0);
    CFloat tested2 = toTestedImpl(val2, 0);

    CFloat reference1 = toReferenceImpl(val1, 0);
    CFloat reference2 = toReferenceImpl(val2, 0);

    CFloat r1 = tested1.multiply(tested2);
    CFloat r2 = reference1.multiply(reference2);

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void sqrt2Test() {
    String val = "2.0";

    CFloat tested = toTestedImpl(val, 0);
    CFloat reference = toReferenceImpl(val, 0);

    CFloat r1 = tested.sqrt();
    CFloat r2 = reference.sqrt();

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void powBugTest() {
    String val1 = "3.4028235E38";
    String val2 = "0.5";

    CFloat tested1 = toTestedImpl(val1, 0);
    CFloat tested2 = toTestedImpl(val2, 0);

    CFloat reference1 = toReferenceImpl(val1, 0);
    CFloat reference2 = toReferenceImpl(val2, 0);

    CFloat r1 = tested1.powTo(tested2);
    CFloat r2 = reference1.powTo(reference2);

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void ln_eTest() {
    String val = String.valueOf(Math.E);

    CFloat tested = toTestedImpl(val, 0);
    CFloat reference = toReferenceImpl(val, 0);

    CFloat r1 = tested.ln();
    CFloat r2 = reference.ln();

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void ln_1Test() {
    // Calculate ln for the next closest value to 1
    String val = "1.00000011920929";

    CFloat tested = toTestedImpl(val, 0);
    CFloat reference = toReferenceImpl(val, 0);

    CFloat r1 = tested.ln();
    CFloat r2 = reference.ln();

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void native_lnBugTest() {
    // One of 4 failed inputs
    // Other values mostly between 0.1 and 1
    String val = "0.0050035235";

    CFloat tested = toTestedImpl(val, 0);
    CFloat reference = toReferenceImpl(val, 0);

    CFloat r1 = tested.ln();
    CFloat r2 = reference.ln();

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void native_powBugTest() {
    // Only failed input (other than powBugTest)
    String val1 = "1.413657E11";
    String val2 = "-0.14661042";

    CFloat tested1 = toTestedImpl(val1, 0);
    CFloat tested2 = toTestedImpl(val2, 0);

    CFloat reference1 = toReferenceImpl(val1, 0);
    CFloat reference2 = toReferenceImpl(val2, 0);

    CFloat r1 = tested1.powTo(tested2);
    CFloat r2 = reference1.powTo(reference2);

    assertEqual1Ulp(r1, r2);
  }
}
