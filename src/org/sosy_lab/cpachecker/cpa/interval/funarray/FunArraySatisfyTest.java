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
        FunArrayBuilder.firstBound(exp(0), exp(1))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build();

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
