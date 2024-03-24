// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint.test;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.List;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.kframework.mpfr.BigFloat;
import org.kframework.mpfr.BinaryMathContext;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloat;
import org.sosy_lab.cpachecker.util.floatingpoint.CMyFloat;
import org.sosy_lab.cpachecker.util.floatingpoint.MpfrFloat;

@RunWith(Parameterized.class)
public class Float16Test extends CFloatUnitTest {
  @Override
  protected BinaryMathContext getFloatType() {
    return BinaryMathContext.BINARY16;
  }

  @Parameters(name = "{0}")
  public static ReferenceImpl[] getReferences() {
    return new ReferenceImpl[] {ReferenceImpl.MPFR};
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
    return new MpfrFloat(repr, getFloatType());
  }

  // List *all* possible floating point values for the given precision
  private static List<BigFloat> allFloats(BinaryMathContext format) {
    // FIXME: We seem to be missing 2048 values?
    ImmutableList.Builder<BigFloat> builder = ImmutableList.builder();
    for (long exponent = format.minExponent - 1; exponent <= format.maxExponent + 1; exponent++) {
      BigInteger leading = BigInteger.ONE.shiftLeft(format.precision - 1);
      if (exponent < format.minExponent) { // Special case for subnormal numbers
        exponent = format.minExponent;
        leading = BigInteger.ZERO;
      }
      int maxValue = (2 << (format.precision - 2));
      for (int i=0; i < maxValue; i++) {
        BigInteger significand = leading.add(BigInteger.valueOf(i));
        builder.add(new BigFloat(false, significand, exponent, format));
        builder.add(new BigFloat(true, significand, exponent, format));
      }
    }
    return builder.build();
  }

  @Override
  protected List<BigFloat> unaryTestValues() {
    return allFloats(getFloatType());
  }

  @Override
  protected int ulpError() {
    return 0;
  }

  @Ignore
  @Override
  public void powToIntegralTest() {
    // TODO: Not implemented in BigFloat
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
}
