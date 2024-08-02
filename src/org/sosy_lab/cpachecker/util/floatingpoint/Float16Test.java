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
import org.kframework.mpfr.BigFloat;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue.Format;

/**
 * Tests the CFloat interface for 16 bit floating point values.
 *
 * <p>Uses only MPFR as reference implementation as both the C and the Java implementation lack
 * support for 16 bit floats. Unary methods are tested exhaustively, for binary methods we generate
 * around 50k test values.
 */
public class Float16Test extends AbstractCFloatTestBase {
  @Override
  Format getFloatType() {
    return Format.Float16;
  }

  @Override
  protected ReferenceImpl getRefImpl() {
    return ReferenceImpl.MPFR;
  }

  @Override
  protected Iterable<BigFloat> unaryTestValues() {
    return allFloats(getFloatType());
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
