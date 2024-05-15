// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

import java.util.List;
import java.util.Map;
import org.junit.Ignore;
import org.junit.Test;
import org.kframework.mpfr.BigFloat;
import org.kframework.mpfr.BinaryMathContext;

/**
 * Tests the CFloat interface for 8 bit floating point values.
 *
 * <p>Uses only MPFR as reference implementation as both the C and the Java implementation lack
 * support for 8 bit float. All methods are tested exhaustively: that is we try all possible input
 * values.
 */
public class Float8Test extends AbstractCFloatTestBase {
  @Override
  protected BinaryMathContext getFloatType() {
    return new BinaryMathContext(4, 4);
  }

  @Override
  protected ReferenceImpl getRefImpl() {
    return ReferenceImpl.MPFR;
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
  protected CFloat toTestedImpl(String repr, Map<Integer, Integer> fromStringStats) {
    return new CFloatImpl(repr, getFloatType(), fromStringStats);
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
