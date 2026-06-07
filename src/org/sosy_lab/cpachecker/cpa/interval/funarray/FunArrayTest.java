// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interval.funarray;

import static com.google.common.truth.Truth.assertThat;
import static org.sosy_lab.cpachecker.cpa.interval.funarray.FunArrayBuilder.exp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.interval.Interval;
import org.sosy_lab.cpachecker.cpa.interval.funarray.FunArrayBuilder.FunArrayBuilderException;

public class FunArrayTest {

  @Test
  public void ofInitializerListWithEmptyListReturnsBottom() {
    FunArray result = FunArray.ofInitializerList(ImmutableList.of(), null);
    assertThat(result).isEqualTo(FunArray.BOTTOM);
  }

  @Test
  public void removeEmptyBoundsWithEmptyLastBoundReturnsBottom() {
    Bound b0 = new Bound(new NormalFormExpression(0));
    Bound b1 = new Bound(new NormalFormExpression(1));
    Bound b2 = new Bound(new NormalFormExpression(2));
    Bound emptyLastBound = new Bound(ImmutableSet.of());

    FunArray array =
        new FunArray(
            ImmutableList.of(b0, b1, b2, emptyLastBound),
            ImmutableList.of(Interval.ZERO, Interval.ZERO, Interval.ZERO),
            ImmutableList.of(false, false, false));

    assertThat(array.removeEmptyBounds()).isEqualTo(FunArray.BOTTOM);
  }

  @Test
  public void testAssignAllSegmentsJoinsValueIntoEverySegment() throws FunArrayBuilderException {
    // {0} [5,5] {i} [20,20] {n}
    FunArray array =
        FunArrayBuilder.firstBound(exp(0))
            .value(5L, 5L)
            .bound(exp("i"))
            .value(20L, 20L)
            .bound(exp("n"))
            .build();

    // Write [10,10] to unknown index — every segment must include 10.
    FunArray result = array.assignAllSegments(new Interval(10L, 10L));

    // Expected: {0} [5,10] {i} [10,20] {n}
    FunArray expected =
        FunArrayBuilder.firstBound(exp(0))
            .value(5L, 10L)
            .bound(exp("i"))
            .value(10L, 20L)
            .bound(exp("n"))
            .build();
    assertThat(result).isEqualTo(expected);
  }
}
