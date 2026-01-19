// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interval;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

public class FunArrayUnificationTest {
  @Test
  public void testMisaligned() {
    Bound boundA = new Bound(new NormalFormExpression(0));
    Bound boundB = new Bound(new NormalFormExpression(5));
    Bound boundC = new Bound(new NormalFormExpression(10));

    Interval valueA = new Interval(1L, 1L);
    Interval valueB = new Interval(2L, 2L);

    // {0} [1,1] {10}
    FunArray arrayA =
        new FunArray(
            ImmutableList.of(boundA, boundC), ImmutableList.of(valueA), ImmutableList.of(false));

    // {0} [2,2] {5} [2,2] {10}
    FunArray arrayB =
        new FunArray(
            ImmutableList.of(boundA, boundB, boundC),
            ImmutableList.of(valueB, valueB),
            ImmutableList.of(false, false));

    FunArray.UnifyResult result = arrayA.unify(arrayB, Interval.EMPTY, Interval.EMPTY);
    FunArray resultA = result.resultThis();
    FunArray resultB = result.resultOther();

    assertThat(resultA.bounds()).containsExactly(boundA, boundB, boundC);
    assertThat(resultB.bounds()).containsExactly(boundA, boundB, boundC);
  }
}
