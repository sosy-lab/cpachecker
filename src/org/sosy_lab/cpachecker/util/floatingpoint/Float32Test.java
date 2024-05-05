// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.kframework.mpfr.BigFloat;
import org.kframework.mpfr.BinaryMathContext;

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
    return new CFloatImpl(value, getFloatType());
  }

  @Override
  public CFloat toTestedImpl(String repr) {
    return new CFloatImpl(repr, getFloatType());
  }

  @Override
  public CFloat toReferenceImpl(BigFloat value) {
    return switch (refImpl) {
      case MPFR -> new MpfrFloat(value, getFloatType());
      case JAVA -> new JFloat(value.floatValue());
      case NATIVE -> new CFloatNative(toPlainString(value), getFloatType());
    };
  }

  @Override
  protected int ulpError() {
    return refImpl == ReferenceImpl.MPFR ? 0 : 1;
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

  @Ignore
  @Test
  public void roundingBugLnTest() {
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
    // String val = "1.6777217e+07";
    String val = "16777217";

    CFloat tested = toTestedImpl(val);
    CFloat reference = toReferenceImpl(val);

    CFloat r1 = tested;
    CFloat r2 = reference;

    assertEqual1Ulp(r1, r2);
  }
}
