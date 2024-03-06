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
import org.sosy_lab.cpachecker.util.floatingpoint.CFloat;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNative;
import org.sosy_lab.cpachecker.util.floatingpoint.JFloat;

@SuppressWarnings("deprecation")
public class CFloatNativeTest extends CFloatUnitTest {
  @Override
  public CFloat toTestedImpl(String repr, int pFloatType) {
    return new CFloatNative(repr, pFloatType);
  }

  @Override
  public CFloat toReferenceImpl(String repr, int pFloatType) {
    return new JFloat(repr, pFloatType);
  }

  @Test
  public void addManyBugTest() {
    String val1 = "1.6777216E7"; // 1.0 x 2^24
    String val2 = "1.0";
    String val_ = "1.6777218E7"; // 1.00...1 x 2^24

    CFloat nativeFloat1 = toTestedImpl(val1, 0);
    CFloat nativeFloat2 = toTestedImpl(val2, 0);
    CFloat nativeFloat_ = toTestedImpl(val_, 0);

    // If the calculation is done with a larger precision than float we'd expect the two val2 to
    // carry over into the last bit of the result
    assertEqual(nativeFloat1.add(nativeFloat2, nativeFloat2), nativeFloat_);
  }

  @Ignore
  @Override
  public void powToIntegralTest() {
    // FIXME: floatingPoints.c lacks support for negative exponents in powToIntegral
  }
}
