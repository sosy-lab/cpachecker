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
import org.sosy_lab.cpachecker.cpa.interval.ExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.interval.Interval;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisState;
import org.sosy_lab.cpachecker.cpa.interval.funarray.FunArrayBuilder.FunArrayBuilderException;

public class FunArraySatisfyTest {

  @Test
  public void testSatisfyStrictLessThanMarksSegmentNonEmpty() throws FunArrayBuilderException {
    // {0} ⊤ {1}? ⊤ {n}
    // first segment may be empty
    FunArray initial =
        FunArrayBuilder.firstBound(exp(0))
            .value(Interval.UNBOUND)
            .bound(exp(1))
            .mayBeEmpty()
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build();

    FunArray result = initial.satisfyStrictLessThan(exp(0), exp(1));

    FunArray expected =
        FunArrayBuilder.firstBound(exp(0))
            .value(Interval.UNBOUND)
            .bound(exp(1))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build();

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testSatisfyStrictLessThanContradictionRemovesBounds()
      throws FunArrayBuilderException {
    // {0} ⊤ {1} ⊤ {n}
    // assert 1 < 0: contradiction
    FunArray initial =
        FunArrayBuilder.firstBound(exp(0))
            .value(Interval.UNBOUND)
            .bound(exp(1))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build();

    FunArray result = initial.satisfyStrictLessThan(exp(1), exp(0));

    FunArray expected =
        new FunArray(
            ImmutableList.of(new Bound(ImmutableSet.of()), new Bound(exp("n"))),
            ImmutableList.of(Interval.UNBOUND),
            ImmutableList.of(false));

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testSatisfyStrictLessThanAlreadySatisfied() throws FunArrayBuilderException {
    // {0} ⊤ {2} ⊤ {n}
    // assert 0 < 2: already satisfied, no change
    FunArray initial =
        FunArrayBuilder.firstBound(exp(0))
            .value(Interval.UNBOUND)
            .bound(exp(2))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build();

    FunArray result = initial.satisfyStrictLessThan(exp(0), exp(2));

    assertThat(result).isEqualTo(initial);
  }

  @Test
  public void testSatisfyLessEqualNoChange() throws FunArrayBuilderException {
    // {0} ⊤ {1} ⊤ {n}
    // assert 0 <= 1: already ordered, no change
    FunArray initial =
        FunArrayBuilder.firstBound(exp(0))
            .value(Interval.UNBOUND)
            .bound(exp(1))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build();

    FunArray result = initial.satisfyLessEqual(exp(0), exp(1));

    assertThat(result).isEqualTo(initial);
  }

  @Test
  public void testSatisfyLessEqualSquashesBoundsWhenMayBeEqual() throws FunArrayBuilderException {
    // {0} ⊤ {1}? ⊤ {n}
    // assert 1 <= 0: squash bounds
    FunArray initial =
        FunArrayBuilder.firstBound(exp(0))
            .value(Interval.UNBOUND)
            .mayBeEmpty()
            .bound(exp(1))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build();

    FunArray result = initial.satisfyLessEqual(exp(1), exp(0));

    FunArray expected =
        FunArrayBuilder.firstBound(exp(0), exp(1)).value(Interval.UNBOUND).bound(exp("n")).build();

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testSatisfyStrictLessThanProducesBottom() throws FunArrayBuilderException {
    // {0} ⊤ {n}?
    // assert n < 0: contradiction, n is right of 0 in bounds
    FunArray initial =
        FunArrayBuilder.firstBound(exp(0))
            .value(Interval.UNBOUND)
            .mayBeEmpty()
            .bound(exp("n"))
            .build();

    FunArray result = initial.satisfyStrictLessThan(exp("n"), exp(0));

    assertThat(result).isEqualTo(FunArray.BOTTOM);
  }

  @Test
  public void testSatisfyLessEqualSquashEntireArrayReturnsBottom() throws FunArrayBuilderException {
    // {0} ⊤ {n}?
    // assert n <= 0: squash collapses all bounds into one → BOTTOM
    FunArray initial =
        FunArrayBuilder.firstBound(exp(0))
            .value(Interval.UNBOUND)
            .mayBeEmpty()
            .bound(exp("n"))
            .build();

    FunArray result = initial.satisfyLessEqual(exp("n"), exp(0));

    assertThat(result).isEqualTo(FunArray.BOTTOM);
  }

  @Test
  public void testNarrowElementNarrowsWideSegment() throws FunArrayBuilderException {
    // {0} ⊤ {5}   (single unbounded segment)
    FunArray initial =
        FunArrayBuilder.firstBound(exp(0)).value(Interval.UNBOUND).bound(exp(5)).build();

    ExpressionValueVisitor visitor =
        new ExpressionValueVisitor(new IntervalAnalysisState(null), null);

    // narrow at index 2 to [0,0]:  [−∞,+∞] ∩ [0,0] = [0,0]
    FunArray result = initial.narrowElement(exp(2), new Interval(0L, 0L), visitor);

    // narrowElement delegates to insert(), which marks overhangs as mayBeEmpty
    // because insert does not evaluate whether the index equals the adjacent bound
    // expected: {0} ⊤ {2}? [0,0] {3} ⊤ {5}?
    FunArray expected =
        FunArrayBuilder.firstBound(exp(0))
            .value(Interval.UNBOUND)
            .bound(exp(2))
            .mayBeEmpty()
            .value(0L, 0L)
            .bound(exp(3))
            .value(Interval.UNBOUND)
            .mayBeEmpty()
            .bound(exp(5))
            .build();

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testNarrowElementValueAlreadyContained() throws FunArrayBuilderException {
    FunArray initial =
        FunArrayBuilder.firstBound(exp(0)).value(0L, 0L).bound(exp(5)).build();

    ExpressionValueVisitor visitor =
        new ExpressionValueVisitor(new IntervalAnalysisState(null), null);

    FunArray result = initial.narrowElement(exp(2), new Interval(0L, 0L), visitor);

    assertThat(result).isNotEqualTo(FunArray.BOTTOM);
    assertThat(result.get(exp(2), visitor)).isEqualTo(new Interval(0L, 0L));
  }

  @Test
  public void testNarrowElementEmptyIntersectionReturnsBottom()
      throws FunArrayBuilderException {
    FunArray initial =
        FunArrayBuilder.firstBound(exp(0)).value(5L, 10L).bound(exp(20)).build();

    ExpressionValueVisitor visitor =
        new ExpressionValueVisitor(new IntervalAnalysisState(null), null);

    FunArray result = initial.narrowElement(exp(3), new Interval(0L, 0L), visitor);

    assertThat(result).isEqualTo(FunArray.BOTTOM);
  }

  @Test
  public void testSatisfyLessEqualContradictionRemovesBounds() throws FunArrayBuilderException {
    // {0} ⊤ {1} ⊤ {n}
    // assert 1 <= 0: non-empty segment, contradiction
    FunArray initial =
        FunArrayBuilder.firstBound(exp(0))
            .value(Interval.UNBOUND)
            .bound(exp(1))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build();

    FunArray result = initial.satisfyLessEqual(exp(1), exp(0));

    FunArray expected =
        new FunArray(
            ImmutableList.of(new Bound(ImmutableSet.of()), new Bound(exp("n"))),
            ImmutableList.of(Interval.UNBOUND),
            ImmutableList.of(false));

    assertThat(result).isEqualTo(expected);
  }
}
