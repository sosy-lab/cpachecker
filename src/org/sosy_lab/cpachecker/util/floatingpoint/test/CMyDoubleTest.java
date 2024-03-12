// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint.test;

import org.junit.Test;
import org.sosy_lab.common.NativeLibraries;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloat;
import org.sosy_lab.cpachecker.util.floatingpoint.CMyFloat;
import org.sosy_lab.cpachecker.util.floatingpoint.JDouble;
import org.sosy_lab.cpachecker.util.floatingpoint.MpFloat;

public class CMyDoubleTest extends CDoubleUnitTest {
  static {
    NativeLibraries.loadLibrary("mpfr_java");
  }

  @Override
  public CFloat toTestedImpl(String repr, int pFloatType) {
    return new CMyFloat(repr, pFloatType);
  }

  @Override
  public CFloat toReferenceImpl(String repr, int pFloatType) {
    return new JDouble(repr, pFloatType);
  }

  @Test
  public void pow64Bug() {
    // We've already seen this bug for 32 bits. Now it's back...
    String val1 = "1.7976931348623157E308";
    String val2 = "0.5";

    CFloat myfloat1 = toTestedImpl(val1, 1);
    CFloat myfloat2 = toTestedImpl(val2, 1);

    CFloat jfloat1 = new MpFloat(val1, 1);
    CFloat jfloat2 = new MpFloat(val2, 1);

    CFloat r1 = myfloat1.powTo(myfloat2);
    CFloat r2 = jfloat1.powTo(jfloat2);

    assertEqual(r1, r2);
  }
}
