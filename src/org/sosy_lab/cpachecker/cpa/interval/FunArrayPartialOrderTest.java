// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interval;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class FunArrayPartialOrderTest {

  @Parameter(0)
  public FunArray lesser;

  @Parameter(1)
  public FunArray greater;

  @Parameters(name = "{0} is less or equal than {1}")
  public static Collection<Object[]> parameters() {
    Bound boundA = new Bound(new NormalFormExpression(0));
    Bound boundB = new Bound(new NormalFormExpression(1));
    Bound boundC = new Bound(new NormalFormExpression(2));
    Bound boundD = new Bound(ImmutableSet.of(new NormalFormExpression(1), new NormalFormExpression(2)));

    Interval valA = Interval.ZERO;
    Interval valB = new Interval(-1L, 1L);

    // {0} [0,0] {1}
    FunArray arrayA = new FunArray(ImmutableList.of(boundA, boundB), ImmutableList.of(valA), ImmutableList.of(false));

    // {0} [-1, 1] {1}
    FunArray arrayB = new FunArray(ImmutableList.of(boundA, boundB), ImmutableList.of(valB), ImmutableList.of(false));

    // {0} [0,0] {1} [-1, 1] {2}?
    FunArray arrayC =
        new FunArray(ImmutableList.of(boundA, boundB, boundC), ImmutableList.of(valA, valB), ImmutableList.of(false, true));

    // {0} [-1,1] {1 2}
    FunArray arrayD = new FunArray(ImmutableList.of(boundA, boundD), ImmutableList.of(valB), ImmutableList.of(false));

    // {0} [0,0] {1}?
    FunArray arrayE = new FunArray(ImmutableList.of(boundA, boundB), ImmutableList.of(valA), ImmutableList.of(true));

    return ImmutableList.of(
        new Object[] {arrayA, arrayA},
        new Object[] {arrayA, arrayB},
        new Object[] {arrayC, arrayD},
        new Object[] {arrayE, arrayA});
  }

  @Test
  public void testPartialOrder() {
    assertThat(lesser.isLessOrEqual(greater)).isTrue();
    if (!lesser.equals(greater)) {
      assertThat(greater.isLessOrEqual(lesser)).isFalse();
    }
  }
}
