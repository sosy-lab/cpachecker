// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint.test;

import java.util.List;
import org.junit.Ignore;
import org.junit.Test;
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
public class Float8Test extends CFloatUnitTest {
  @Override
  protected BinaryMathContext getFloatType() {
    return new BinaryMathContext(4, 4);
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

  @Override
  protected List<BigFloat> unaryTestValues() {
    return allFloats(getFloatType());
  }

  @Override
  protected List<BigFloat> binaryTestValues() {
    return allFloats(getFloatType());
  }

  @Override
  protected int ulpError() {
    return 0;
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
  public void castToTest() {
    // Not implemented
  }

  @Ignore
  @Override
  @Test
  public void castToRoundingTest() {
    // Not implemented
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

  // TODO: Fix failing powToTest cases:
  //  Failed on 11 (out of 59536) test inputs

  @Test
  public void powBug1Test() {
    // First kind of bug: The exponent is Z/2, Z/4 ... Z/2^k and the base is a square
    // We need a special test for these values
    String val1 = "3.91e-03"; // 1/16
    String val2 = "7.5e-01";

    // expected: 0 0001 000 [1.56e-02]
    // but was : 0 1111 100 [nan],

    CFloat tested1 = toTestedImpl(val1);
    CFloat tested2 = toTestedImpl(val2);

    CFloat reference1 = toReferenceImpl(val1);
    CFloat reference2 = toReferenceImpl(val2);

    CFloat r1 = tested1.powTo(tested2);
    CFloat r2 = reference1.powTo(reference2);

    assertEqual1Ulp(r1, r2);
  }
}
