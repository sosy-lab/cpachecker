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
import org.sosy_lab.cpachecker.util.floatingpoint.JFloat;
import org.sosy_lab.cpachecker.util.floatingpoint.MpFloat;

public class MpFloatTest extends CFloatUnitTest {
  @Override
  public CFloat toTestedImpl(String repr, int pFloatType) {
    return new MpFloat(repr, pFloatType);
  }

  @Override
  public CFloat toReferenceImpl(String repr, int pFloatType) {
    return new JFloat(repr, pFloatType);
  }

  @Ignore
  @Override
  public void powToIntegralTest() {
    // FIXME: Not implemented
  }
}
