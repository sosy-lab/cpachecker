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

public class BoundTest {

  @Test
  public void testInvertibleAdaption() {
    Bound bound = new Bound(exp("i"));
    Bound updatedBound =
        bound.adaptForChangedVariableValues(variable("i"), ImmutableSet.of(exp("i", -1)));
    assertThat(updatedBound.expressions()).contains(exp("i", 1));
  }

  @Test
  public void testNonInvertibleAdaption() {
    Bound bound = new Bound(ImmutableSet.of(exp("i"), exp("j")));
    Bound updatedBound =
        bound.adaptForChangedVariableValues(variable("i"), ImmutableSet.of(exp("k")));
    assertThat(updatedBound.expressions()).doesNotContain(exp("i"));
  }

  @Test
  public void testIntroduceEqualVariableAdaption() {
    Bound bound = new Bound(ImmutableSet.of(exp(0)));
    Bound updatedBound =
        bound.adaptForChangedVariableValues(variable("i"), ImmutableSet.of(exp(0)));
    assertThat(updatedBound.expressions()).contains(exp("i"));
    assertThat(updatedBound.expressions()).contains(exp(0));
  }

  @Test
  public void testRelateVariablesAdaption() {
    Bound bound = new Bound(ImmutableSet.of(exp("i", 1), exp("j", 1)));
    Bound updatedBound =
        bound.adaptForChangedVariableValues(variable("i"), ImmutableSet.of(exp("j")));
    assertThat(updatedBound.expressions()).contains(exp("i", 1));
    assertThat(updatedBound.expressions()).contains(exp("j", 1));
  }

  @Test
  public void testIsEmptyOnEmptyBound() {
    Bound bound = new Bound(ImmutableSet.of());
    assertThat(bound.isEmpty()).isTrue();
  }

  @Test
  public void testIsEmptyOnNonEmptyBound() {
    Bound bound = new Bound(exp("i"));
    assertThat(bound.isEmpty()).isFalse();
  }

  @Test
  public void testContainsExpression() {
    Bound bound = new Bound(ImmutableSet.of(exp("i"), exp("j")));
    assertThat(bound.contains(exp("i"))).isTrue();
    assertThat(bound.contains(exp("k"))).isFalse();
  }

  @Test
  public void testContainsPredicate() {
    Bound bound = new Bound(ImmutableSet.of(exp("i", 2), exp("j", -1)));
    assertThat(bound.contains(e -> e.getConstant() > 0)).isTrue();
    assertThat(bound.contains(e -> e.getConstant() > 5)).isFalse();
  }

  @Test
  public void testIncreaseShiftsAllExpressions() {
    Bound bound = new Bound(ImmutableSet.of(exp("i"), exp("j", 1)));
    Bound result = bound.increase(2);
    assertThat(result.expressions()).contains(exp("i", 2));
    assertThat(result.expressions()).contains(exp("j", 3));
  }

  @Test
  public void testRemoveVariableOccurrencesRemovesMatching() {
    Bound bound = new Bound(ImmutableSet.of(exp("i"), exp("j")));
    Bound result = bound.removeVariableOccurrences(variable("i"));
    assertThat(result.expressions()).doesNotContain(exp("i"));
    assertThat(result.expressions()).contains(exp("j"));
  }

  @Test
  public void testRemoveVariableOccurrencesKeepsUnrelated() {
    Bound bound = new Bound(ImmutableSet.of(exp("i"), exp("j")));
    Bound result = bound.removeVariableOccurrences(variable("k"));
    assertThat(result.expressions()).containsExactly(exp("i"), exp("j"));
  }

  @Test
  public void testUnionCombinesExpressions() {
    Bound a = new Bound(exp("i"));
    Bound b = new Bound(exp("j"));
    Bound result = a.union(b);
    assertThat(result.expressions()).containsExactly(exp("i"), exp("j"));
  }

  @Test
  public void testUnionWithOverlap() {
    Bound a = new Bound(ImmutableSet.of(exp("i"), exp("j")));
    Bound b = new Bound(ImmutableSet.of(exp("j"), exp("k")));
    Bound result = a.union(b);
    assertThat(result.expressions()).containsExactly(exp("i"), exp("j"), exp("k"));
  }

  @Test
  public void testStaticUnionOfCollection() {
    Bound a = new Bound(exp("i"));
    Bound b = new Bound(exp("j"));
    Bound c = new Bound(exp("k"));
    Bound result = Bound.union(ImmutableList.of(a, b, c));
    assertThat(result.expressions()).containsExactly(exp("i"), exp("j"), exp("k"));
  }

  @Test
  public void testIntersectionRetainsCommon() {
    Bound a = new Bound(ImmutableSet.of(exp("i"), exp("j")));
    Bound b = new Bound(ImmutableSet.of(exp("j"), exp("k")));
    Bound result = a.intersection(b);
    assertThat(result.expressions()).containsExactly(exp("j"));
  }

  @Test
  public void testIntersectionDisjoint() {
    Bound a = new Bound(exp("i"));
    Bound b = new Bound(exp("j"));
    Bound result = a.intersection(b);
    assertThat(result.isEmpty()).isTrue();
  }

  @Test
  public void testDifferenceRemovesPresent() {
    Bound a = new Bound(ImmutableSet.of(exp("i"), exp("j")));
    Bound b = new Bound(exp("j"));
    Bound result = a.difference(b);
    assertThat(result.expressions()).containsExactly(exp("i"));
  }

  @Test
  public void testDifferenceNothingToRemove() {
    Bound a = new Bound(exp("i"));
    Bound b = new Bound(exp("j"));
    Bound result = a.difference(b);
    assertThat(result.expressions()).containsExactly(exp("i"));
  }

  @Test
  public void testRelativeComplement() {
    Bound a = new Bound(exp("i"));
    Bound b = new Bound(ImmutableSet.of(exp("i"), exp("j")));
    Bound result = a.relativeComplement(b);
    assertThat(result.expressions()).containsExactly(exp("j"));
  }
}
