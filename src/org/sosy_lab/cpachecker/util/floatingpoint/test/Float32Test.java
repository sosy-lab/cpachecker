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
import org.kframework.mpfr.BigFloat;
import org.kframework.mpfr.BinaryMathContext;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloat;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNative;
import org.sosy_lab.cpachecker.util.floatingpoint.CMyFloat;
import org.sosy_lab.cpachecker.util.floatingpoint.JFloat;
import org.sosy_lab.cpachecker.util.floatingpoint.MpfrFloat;

@SuppressWarnings("deprecation")
@RunWith(Parameterized.class)
public class Float32Test extends CFloatUnitTest {
  @Override
  protected BinaryMathContext getFloatType() {
    return BinaryMathContext.BINARY32;
  }

  @Parameters(name = "{0}")
  public static ReferenceImpl[] getReferences() {
    return ReferenceImpl.values();
  }

  @Parameter(0)
  public ReferenceImpl refImpl;

  @Override
  protected ReferenceImpl getRefImpl() {
    return refImpl;
  }

  @Override
  public CFloat toTestedImpl(BigFloat value) {
    return new CMyFloat(value, getFloatType());
  }

  @Override
  public CFloat toReferenceImpl(BigFloat value) {
    return switch (refImpl) {
      case MPFR -> new MpfrFloat(value, getFloatType());
      case JAVA -> new JFloat(value.floatValue());
      case NATIVE -> new CFloatNative(value.toString(), getFloatType());
    };
  }

  @Override
  protected int ulpError() {
    return refImpl == ReferenceImpl.MPFR ? 0 : 1;
  }

  @Ignore
  @Override
  @Test
  public void powToIntegralTest() {
    // TODO: Not implemented in BigFloat
  }

  @Ignore
  @Override
  @Test
  public void castToByteTest() {
    // Disabled
  }

  @Ignore
  @Override
  @Test
  public void castToShortTest() {
    // Disabled
  }

  @Ignore
  @Override
  @Test
  public void castToIntTest() {
    // Disabled
  }

  @Ignore
  @Override
  @Test
  public void castToLongTest() {
    // Disabled
  }

  @Test
  public void overflowTest() {
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
    String val = "2.0";

    CFloat tested = toTestedImpl(val);
    CFloat reference = toReferenceImpl(val);

    CFloat r1 = tested.sqrt();
    CFloat r2 = reference.sqrt();

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void ln_eTest() {
    String val = String.valueOf(Math.E);

    CFloat tested = toTestedImpl(val);
    CFloat reference = toReferenceImpl(val);

    CFloat r1 = tested.ln();
    CFloat r2 = reference.ln();

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void ln_1Test() {
    // Calculate ln for the next closest value to 1
    String val = "1.00000011920929";

    CFloat tested = toTestedImpl(val);
    CFloat reference = toReferenceImpl(val);

    CFloat r1 = tested.ln();
    CFloat r2 = reference.ln();

    assertEqual1Ulp(r1, r2);
  }
}
