// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

/** Testing that {@link PointerBase#fromFormulaEncoding} is the inverse of the formula encoding. */
public class PointerBaseTest {

  @Test
  public void testRoundTripOfLocalVariable() {
    assertRoundTrip(PointerBase.forVariable("f::x", 3));
  }

  @Test
  public void testRoundTripOfGlobalVariable() {
    assertRoundTrip(PointerBase.forVariable("x", 3));
  }

  @Test
  public void testRoundTripOfDynamicAllocation() {
    assertRoundTrip(new PointerBase("#f_void#1", null));
  }

  @Test
  public void testRoundTripOfNameContainingCallStackDepthSeparator() {
    // the call stack depth is appended at the end, so the last separator is the relevant one
    assertRoundTrip(new PointerBase("f::x__CALL_STACK_DEPTH_5", 3));
  }

  @Test
  public void testNameThatIsNoEncodedBase() {
    assertThat(PointerBase.fromFormulaEncoding("f::x")).isEmpty();
  }

  private void assertRoundTrip(PointerBase base) {
    assertThat(PointerBase.fromFormulaEncoding(base.formulaEncoding())).hasValue(base);
  }
}
