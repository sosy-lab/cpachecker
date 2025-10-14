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
import java.util.Collection;
import java.util.List;
import java.util.Set;
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
    Bound boundD = new Bound(Set.of(new NormalFormExpression(1), new NormalFormExpression(2)));

    Interval valA = Interval.ZERO;
    Interval valB = new Interval(-1L, 1L);

    // {0} [0,0] {1}
    FunArray arrayA = new FunArray(List.of(boundA, boundB), List.of(valA), List.of(false));

    // {0} [-1, 1] {1}
    FunArray arrayB = new FunArray(List.of(boundA, boundB), List.of(valB), List.of(false));

    // {0} [0,0] {1} [-1, 1] {2}?
    FunArray arrayC =
        new FunArray(List.of(boundA, boundB, boundC), List.of(valA, valB), List.of(false, true));

    // {0} [-1,1] {1 2}
    FunArray arrayD = new FunArray(List.of(boundA, boundD), List.of(valB), List.of(false));

    // {0} [0,0] {1}?
    FunArray arrayE = new FunArray(List.of(boundA, boundB), List.of(valA), List.of(true));

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
