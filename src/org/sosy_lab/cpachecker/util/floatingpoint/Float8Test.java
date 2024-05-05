// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

import java.util.List;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.kframework.mpfr.BigFloat;
import org.kframework.mpfr.BinaryMathContext;

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
  public CFloat toTestedImpl(BigFloat value) {
    return new CFloatImpl(value, getFloatType());
  }

  @Override
  public CFloat toTestedImpl(String repr) {
    return new CFloatImpl(repr, getFloatType());
  }

  @Override
  public CFloat toReferenceImpl(BigFloat value) {
    return new MpfrFloat(value, getFloatType());
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
}
