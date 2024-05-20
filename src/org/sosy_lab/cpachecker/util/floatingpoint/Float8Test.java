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
import org.kframework.mpfr.BigFloat;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatP.Format;

/**
 * Tests the CFloat interface for 8 bit floating point values.
 *
 * <p>Uses only MPFR as reference implementation as both the C and the Java implementation lack
 * support for 8 bit float. All methods are tested exhaustively: that is we try all possible input
 * values.
 */
public class Float8Test extends AbstractCFloatTestBase {
  @Override
  Format getFloatType() {
    return Format.Float8;
  }

  @Override
  protected ReferenceImpl getRefImpl() {
    return ReferenceImpl.MPFR;
  }

  @Override
  protected List<BigFloat> unaryTestValues() {
    return allFloats(getFloatType());
  }

  @Override
  protected List<BigFloat> binaryTestValues() {
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
}
