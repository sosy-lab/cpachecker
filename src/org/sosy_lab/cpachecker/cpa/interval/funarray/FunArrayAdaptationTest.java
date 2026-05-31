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
import static org.sosy_lab.cpachecker.cpa.interval.funarray.FunArrayBuilder.variable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.interval.Interval;
import org.sosy_lab.cpachecker.cpa.interval.funarray.FunArrayBuilder.FunArrayBuilderException;

public class FunArrayAdaptationTest {

  @Test
  public void testAdaptInvertibleAssignment() throws FunArrayBuilderException {
    // {0} ⊤ {i} ⊤ {n}
    // assign i = i - 1 → bound {i} shifts to {i+1}
    FunArray initial =
        FunArrayBuilder.firstBound(exp(0))
            .value(Interval.UNBOUND)
            .bound(exp("i"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build();

    FunArray result =
        initial.adaptToVariableAssignment(variable("i"), ImmutableSet.of(exp("i", -1)));

    FunArray expected =
        FunArrayBuilder.firstBound(exp(0))
            .value(Interval.UNBOUND)
            .bound(exp("i", 1))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build();

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testAdaptNonInvertibleAssignment() throws FunArrayBuilderException {
    // {0} ⊤ {i} ⊤ {n}
    // assign i = k → {i} cannot be expressed in terms of k, removed
    FunArray initial =
        FunArrayBuilder.firstBound(exp(0))
            .value(Interval.UNBOUND)
            .bound(exp("i"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build();

    FunArray adapted =
        initial.adaptToVariableAssignment(variable("i"), ImmutableSet.of(exp("k")));
    FunArray result = adapted.removeEmptyBounds();

    FunArray expected =
        FunArrayBuilder.firstBound(exp(0))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build();

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testAdaptIntroducesEquivalentExpression() throws FunArrayBuilderException {
    // {0} ⊤ {n}
    // assign i = 0 → bound {0} gains equivalent expression {i}
    FunArray initial =
        FunArrayBuilder.firstBound(exp(0)).value(Interval.UNBOUND).bound(exp("n")).build();

    FunArray result =
        initial.adaptToVariableAssignment(variable("i"), ImmutableSet.of(exp(0)));

    FunArray expected =
        new FunArray(
            ImmutableList.of(
                new Bound(ImmutableSet.of(exp(0), exp("i"))), new Bound(exp("n"))),
            ImmutableList.of(Interval.UNBOUND),
            ImmutableList.of(false));

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testRemoveEmptyBoundsJoinsAdjacentSegments() throws FunArrayBuilderException {
    // Manually build {0} [0,5] {} [1,3] {n} — middle bound is empty
    FunArray withEmptyBound =
        new FunArray(
            ImmutableList.of(
                new Bound(exp(0)), new Bound(ImmutableSet.of()), new Bound(exp("n"))),
            ImmutableList.of(new Interval(0L, 5L), new Interval(1L, 3L)),
            ImmutableList.of(false, false));

    FunArray result = withEmptyBound.removeEmptyBounds();

    // Empty bound removed; [0,5] ∪ [1,3] = [0,5] stored in the remaining segment
    FunArray expected =
        FunArrayBuilder.firstBound(exp(0)).value(0, 5).bound(exp("n")).build();

    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void testFindIndexFirstBound() throws FunArrayBuilderException {
    FunArray array =
        FunArrayBuilder.firstBound(exp(0))
            .value(Interval.UNBOUND)
            .bound(exp("i"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build();

    assertThat(array.findIndex(exp(0))).isEqualTo(0);
  }

  @Test
  public void testFindIndexMiddleBound() throws FunArrayBuilderException {
    FunArray array =
        FunArrayBuilder.firstBound(exp(0))
            .value(Interval.UNBOUND)
            .bound(exp("i"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build();

    assertThat(array.findIndex(exp("i"))).isEqualTo(1);
  }

  @Test
  public void testFindIndexLastBound() throws FunArrayBuilderException {
    FunArray array =
        FunArrayBuilder.firstBound(exp(0))
            .value(Interval.UNBOUND)
            .bound(exp("i"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build();

    assertThat(array.findIndex(exp("n"))).isEqualTo(2);
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void testFindIndexNotPresent() throws FunArrayBuilderException {
    FunArray array =
        FunArrayBuilder.firstBound(exp(0)).value(Interval.UNBOUND).bound(exp("n")).build();

    array.findIndex(exp("k"));
  }

  @Test
  public void testGetExpressionsReturnsAllBoundExpressions() throws FunArrayBuilderException {
    FunArray array =
        FunArrayBuilder.firstBound(exp(0))
            .value(Interval.UNBOUND)
            .bound(exp("i"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build();

    assertThat(array.getExpressions()).containsExactly(exp(0), exp("i"), exp("n"));
  }

  @Test
  public void testIsReachableNormalArray() throws FunArrayBuilderException {
    FunArray array =
        FunArrayBuilder.firstBound(exp(0)).value(Interval.UNBOUND).bound(exp("n")).build();

    assertThat(array.isReachable()).isTrue();
  }

  @Test
  public void testIsReachableWithEmptySegment() throws FunArrayBuilderException {
    FunArray array =
        FunArrayBuilder.firstBound(exp(0)).value(Interval.EMPTY).bound(exp("n")).build();

    assertThat(array.isReachable()).isFalse();
  }

  @Test
  public void testIsReachableBottom() {
    assertThat(FunArray.BOTTOM.isReachable()).isFalse();
  }

  @Test
  public void testRemoveEmptyBoundsProducesBottom() {
    FunArray array =
        new FunArray(
            ImmutableList.of(new Bound(exp(0)), new Bound(ImmutableSet.of())),
            ImmutableList.of(Interval.UNBOUND),
            ImmutableList.of(false));
    assertThat(array.removeEmptyBounds()).isEqualTo(FunArray.BOTTOM);
  }
}
