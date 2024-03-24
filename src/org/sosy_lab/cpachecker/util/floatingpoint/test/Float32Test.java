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
  public CFloat toTestedImpl(String repr) {
    return new CMyFloat(repr, getFloatType());
  }

  @Override
  public CFloat toReferenceImpl(String repr) {
    return switch (refImpl) {
      case MPFR -> new MpfrFloat(repr, getFloatType());
      case JAVA -> new JFloat(repr);
      case NATIVE -> new CFloatNative(repr, getFloatType());
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
  public void powBugTest() {
    String val1 = "3.4028235E38"; // Works for any precision: maxValue^0.5 = sqrt(maxValue)
    String val2 = "0.5";

    // FIXME: Figure out the right size for the intermediate results to avoid rounding issues
    //  pow(a,x)=a^x is implemented as exp(x*log(a))
    //  We calculate y=x*log(a) for the values in this test and then evaluate the exponential:
    //  exp(101100010111001000010111 11110101110100011100111101111)
    //  1:  100011100100101010011000 01110000110100011001001100001
    //  2:  110110101111011100010100 11101110011100110011100001010
    //  3:  101110000011101001110011 01100100001010101001001000100
    //  4:  101001001000100100000101 00000100111000110111101000001
    //  5:  111010000010010010011110 00100011001101010010001000110
    //  6:  111110110011101010001011 01110011111100010000101111001
    //  7:  111111110010111101001100 00010110100001111100110100101
    //  8:  111111111110000001011011 01110110101011010001100011111
    //  9:  111111111111101110101011 11101110011011010101011010001
    //  10: 111111111111111101110101 10000100001000001010011000011
    //  11: 111111111111111111101111 10110110101011101001101101010
    //  12: 111111111111111111111101 11010100100110010100010001000
    //  13: 111111111111111111111111 01010101111110101101010100110
    //  14: 111111111111111111111111 01111100001001000000001000100
    //  15: 111111111111111111111111 01111111101010101101111100100
    //  16: 111111111111111111111111 01111111111110010001100100100
    //  17: 111111111111111111111111 01111111111111110111101000100
    //  18: 111111111111111111111111 01111111111111111111100000100
    //  19: 111111111111111111111111 10000000000000000000000100010
    //                               ^ we had an overflow here
    //  20: 111111111111111111111111 10000000000000000000001000010
    //  21: 111111111111111111111111 10000000000000000000001000010
    //                               ^ ...and now we need to round up

    CFloat tested1 = toTestedImpl(val1);
    CFloat tested2 = toTestedImpl(val2);

    CFloat reference1 = toReferenceImpl(val1);
    CFloat reference2 = toReferenceImpl(val2);

    CFloat r1 = tested1.powTo(tested2);
    CFloat r2 = reference1.powTo(reference2);

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

  @Test
  public void native_lnBugTest() {
    // One of 4 failed inputs
    // Other values mostly between 0.1 and 1
    String val = "0.0050035235";

    CFloat tested = toTestedImpl(val);
    CFloat reference = toReferenceImpl(val);

    CFloat r1 = tested.ln();
    CFloat r2 = reference.ln();

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void native_powBugTest() {
    // Only failed input (other than powBugTest)
    String val1 = "1.413657E11";
    String val2 = "-0.14661042";

    CFloat tested1 = toTestedImpl(val1);
    CFloat tested2 = toTestedImpl(val2);

    CFloat reference1 = toReferenceImpl(val1);
    CFloat reference2 = toReferenceImpl(val2);

    CFloat r1 = tested1.powTo(tested2);
    CFloat r2 = reference1.powTo(reference2);

    assertEqual1Ulp(r1, r2);
  }
}
