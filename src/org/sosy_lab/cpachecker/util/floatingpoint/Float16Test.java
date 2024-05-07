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

/**
 * Tests the CFloat interface for 16 bit floating point values.
 *
 * <p>Uses only MPFR as reference implementation as both the C and the Java implementation lack
 * support for 16 bit floats. Unary methods are tested exhaustively, for binary methods we generate
 * around 50k test values.
 */
@RunWith(Parameterized.class)
public class Float16Test extends AbstractCFloatTestBase {
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
  protected int ulpError() {
    return 0;
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
  public void hardExpTest() {
    // Hardest instance for exp(...) in float16
    /* {1=23042,
        2=1541,
        3=262,
        4=140,
        5=121,
        6=2684,
        7=3916,
        8=4224,
        9=4456,
        10=4728,
        11=5193,
        12=5207,
        13=2160,
        14=2113,
        15=1715,
        16=1033,
        17=457,
        18=231,
        19=133,
        20=71,
        21=36,
        22=11,
        23=9,
        24=4,
        25=2,
        26=1 <- here
    }*/
    String val = "1.0969e+01";

    CFloat tested = toTestedImpl(val);
    CFloat reference = toReferenceImpl(val);

    CFloat r1 = tested.exp();
    CFloat r2 = reference.exp();

    assertEqual1Ulp(r1, r2);
  }
}
