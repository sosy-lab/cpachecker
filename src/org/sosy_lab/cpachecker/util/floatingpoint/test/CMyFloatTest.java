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
import org.sosy_lab.common.NativeLibraries;
import org.sosy_lab.cpachecker.util.floatingpoint.CFloat;
import org.sosy_lab.cpachecker.util.floatingpoint.CMyFloat;
import org.sosy_lab.cpachecker.util.floatingpoint.JFloat;

public class CMyFloatTest extends CFloatUnitTest {
  static {
    NativeLibraries.loadLibrary("mpfr_java");
  }

  @Override
  public CFloat toTestedImpl(String repr, int pFloatType) {
    return new CMyFloat(repr, pFloatType);
  }

  @Override
  public CFloat toReferenceImpl(String repr, int pFloatType) {
    return new JFloat(repr, pFloatType);
  }

  @Ignore
  @Test
  @Override
  public void divideByTest() {
    // FIXME: Not implemented
  }

  @Ignore
  @Test
  @Override
  public void powToTest() {
    // FIXME: Not implemented
  }

  @Ignore
  @Test
  @Override
  public void powToIntegralTest() {
    // FIXME: Not implemented
  }

  @Ignore
  @Test
  @Override
  public void sqrtTest() {
    // FIXME: Not implemented
  }

  @Ignore
  @Test
  @Override
  public void roundTest() {
    // FIXME: Not implemented
  }

  @Ignore
  @Test
  @Override
  public void truncTest() {
    // FIXME: Not implemented
  }

  @Ignore
  @Test
  @Override
  public void ceilTest() {
    // FIXME: Not implemented
  }

  @Ignore
  @Test
  @Override
  public void floorTest() {
    // FIXME: Not implemented
  }

  @Ignore
  @Test
  @Override
  public void absTest() {
    // FIXME: Not implemented
  }

  @Ignore
  @Test
  @Override
  public void greaterThanTest() {
    // FIXME: Not implemented
  }

  @Ignore
  @Test
  @Override
  public void copySignFromTest() {
    // FIXME: Not implemented
  }
}
