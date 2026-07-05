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

import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.interval.Interval;
import org.sosy_lab.cpachecker.cpa.interval.funarray.FunArrayBuilder.FunArrayBuilderException;

public class FunArrayWideningTest {

  @Test
  public void wideningCase6_preservesDroppedSegmentValue() throws FunArrayBuilderException {
    // {0} [0,50] {i} [51,100] {n}?
    FunArray existing =
        FunArrayBuilder.firstBound(exp(0))
            .value(new Interval(0L, 50L))
            .bound(exp("i"))
            .value(new Interval(51L, 100L))
            .bound(exp("n"))
            .mayBeEmpty()
            .build();

    // {0} [0,30] {n}?
    FunArray newer =
        FunArrayBuilder.firstBound(exp(0))
            .value(new Interval(0L, 30L))
            .bound(exp("n"))
            .mayBeEmpty()
            .build();

    FunArray result = existing.widen(newer);

    // The single merged segment must cover BOTH [0,50] (from {0}-{i}) AND [51,100] (from {i}-{n}).
    assertThat(result.values()).containsExactly(new Interval(0L, 100L));
  }

  @Test
  public void testWideningForInitialization() throws FunArrayBuilderException {

    // {0} 0 {i} ⊤ {n}?
    FunArray leftSide =
        FunArrayBuilder.firstBound(exp(0))
            .value(0)
            .bound(exp("i"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .mayBeEmpty()
            .build();

    // {0} 0 {i-1} 0 {i} ⊤ {n}?
    FunArray rightSide =
        FunArrayBuilder.firstBound(exp(0))
            .value(0)
            .bound(exp("j", -1))
            .value(0)
            .bound(exp("i"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .mayBeEmpty()
            .build();

    FunArray result = leftSide.widen(rightSide);

    assertThat(result).isEqualTo(leftSide);
  }
}
