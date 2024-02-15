// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint.test;

import org.junit.Ignore;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloat;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatImpl;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatInf;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloatNaN;
import org.sosy_lab.cpachecker.util.floatingpoint.JFloat;

public class CFloatImplTest extends CFloatUnitTest {
  public CFloatImplTest(Float pArg1, Float pArg2) {
    super(pArg1, pArg2);
  }

  @Override
  public CFloat makeCFloatType1(String repr, int pFloatType) {
    if (repr == "nan") {
      return new CFloatNaN(floatType);
    }
    if (repr == "-inf") {
      return new CFloatInf(true, floatType);
    }
    if (repr == "inf") {
      return new CFloatInf(false, floatType);
    }
    return new CFloatImpl(repr, floatType);
  }

  @Override
  public CFloat makeCFloatType2(String repr, int pFloatType) {
    return new JFloat(repr, pFloatType);
  }

  @Ignore
  @Override
  public void powToTest() {
    // FIXME: CFloatImpl does not implement powTo
  }

  @Ignore
  @Override
  public void powToIntegralTest() {
    // FIXME: CFloatImpl does not implement powToIntegral
  }

  @Ignore
  @Override
  public void sqrtTest() {
    // FIXME: CFloatImpl does not implement sqrt
  }
}
