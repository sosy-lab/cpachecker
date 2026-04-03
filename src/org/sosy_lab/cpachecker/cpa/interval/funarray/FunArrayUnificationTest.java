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

public class FunArrayUnificationTest {

  private void testUnification(
      FunArray initialA, FunArray initialB, FunArray expectedResultA, FunArray expectedResultB) {
    var unification = new FunArrayUnification(initialA, initialB);
    var result = unification.unify(Interval.EMPTY, Interval.EMPTY);

    FunArray resultA = result.resultA();
    FunArray resultB = result.resultB();

    assertThat(resultA).isEqualTo(expectedResultA);
    assertThat(resultB).isEqualTo(expectedResultB);
  }

  /*
   * Example 8 from the Cousout, Cousot and Logozzo Paper.
   */
  @Test
  public void testExample8FromPaper() throws FunArrayBuilderException {
    testUnification(
        // {0 i} ⊤ {n}
        FunArrayBuilder.firstBound(exp(0), exp("i"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build(),
        // {0 i-1} 0 {1 i} ⊤ {n}?
        FunArrayBuilder.firstBound(exp(0), exp("i", -1))
            .value(0)
            .bound(exp(1), exp("i"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .mayBeEmpty()
            .build(),
        // {0} ⊥ {i}? ⊤ {n}
        FunArrayBuilder.firstBound(exp(0))
            .value(Interval.EMPTY)
            .bound(exp("i"))
            .mayBeEmpty()
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build(),
        // {0} 0 {i} ⊤ {n}?
        FunArrayBuilder.firstBound(exp(0))
            .value(0)
            .bound(exp("i"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .mayBeEmpty()
            .build());
  }

  /*
   * Dropping disjoint middle bounds. When bounds have absolutely no symbolic expressions in common,
   * they are safely dropped and their adjacent abstract values are joined.
   */
  @Test
  public void testDisjointMiddleBounds() throws FunArrayBuilderException {
    testUnification(
        // Initial A: {0} 0 {x} ⊤ {n}
        FunArrayBuilder.firstBound(exp(0))
            .value(0)
            .bound(exp("x"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build(),
        // Initial B: {0} 0 {y} ⊤ {n}
        FunArrayBuilder.firstBound(exp(0))
            .value(0)
            .bound(exp("y"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build(),
        // Expected A: {0} ⊤ {n} (Values are joined: 0 ∪ ⊤ = ⊤)
        FunArrayBuilder.firstBound(exp(0)).value(Interval.UNBOUND).bound(exp("n")).build(),
        // Expected B: {0} ⊤ {n}
        FunArrayBuilder.firstBound(exp(0)).value(Interval.UNBOUND).bound(exp("n")).build());
  }

  /*
   * Superset and subset bounds. Extracts common knowledge from overlapping bound sets. The unique,
   * un-anticipated expression 'j' is dropped to unify the segmentation.
   */
  @Test
  public void testSupersetAndSubsetBounds() throws FunArrayBuilderException {
    testUnification(
        // Initial A: {0} 0 {i j} ⊤ {n}
        FunArrayBuilder.firstBound(exp(0))
            .value(0)
            .bound(exp("i"), exp("j"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build(),
        // Initial B: {0} 0 {i} ⊤ {n}
        FunArrayBuilder.firstBound(exp(0))
            .value(0)
            .bound(exp("i"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build(),
        // Expected A: {0} 0 {i} ⊤ {n}
        FunArrayBuilder.firstBound(exp(0))
            .value(0)
            .bound(exp("i"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build(),
        // Expected B: {0} 0 {i} ⊤ {n}
        FunArrayBuilder.firstBound(exp(0))
            .value(0)
            .bound(exp("i"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build());
  }

  /*
   * Partially overlapping bounds with anticipation. The expression 'i' is unique to A at the
   * current index, but is anticipated in B's future bounds, causing A to split a new segment.
   */
  @Test
  public void testPartiallyOverlappingWithAnticipation() throws FunArrayBuilderException {
    testUnification(
        // Initial A: {0} 0 {i k} ⊤ {n}
        FunArrayBuilder.firstBound(exp(0))
            .value(0)
            .bound(exp("i"), exp("k"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build(),
        // Initial B: {0} 0 {j k} ⊤ {i} ⊤ {n}
        FunArrayBuilder.firstBound(exp(0))
            .value(0)
            .bound(exp("j"), exp("k"))
            .value(Interval.UNBOUND)
            .bound(exp("i"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build(),
        // Expected A: {0} 0 {k} ⊥ {i}? ⊤ {n}
        FunArrayBuilder.firstBound(exp(0))
            .value(0)
            .bound(exp("k"))
            .value(Interval.EMPTY)
            .bound(exp("i"))
            .mayBeEmpty()
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build(),
        // Expected B: {0} 0 {k} ⊤ {i} ⊤ {n}
        FunArrayBuilder.firstBound(exp(0))
            .value(0)
            .bound(exp("k"))
            .value(Interval.UNBOUND)
            .bound(exp("i"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build());
  }

  /*
   * Handling early termination of one array's bounds. Array A terminates at {n} while Array B still
   * has an intermediate bound {i}. The algorithm merges the remaining bounds ({n} from A, {i} and
   * {n} from B) into a single final bound and removes the extra intermediate segment from B.
   */
  @Test
  public void testHandleLastEarlyTermination() throws FunArrayBuilderException {
    testUnification(
        // Initial A: {0} 0 {n}
        FunArrayBuilder.firstBound(exp(0)).value(0).bound(exp("n")).build(),
        // Initial B: {0} 0 {i} ⊤ {n}
        FunArrayBuilder.firstBound(exp(0))
            .value(0)
            .bound(exp("i"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build(),
        // Expected A: {0} 0 {n i}
        FunArrayBuilder.firstBound(exp(0)).value(0).bound(exp("n"), exp("i")).build(),
        // Expected B: {0} 0 {n i}
        FunArrayBuilder.firstBound(exp(0)).value(0).bound(exp("n"), exp("i")).build());
  }

  /*
   * Superset bounds with anticipation. Array A contains the superset bound {i k}, while Array B
   * only has {k} at this index. However, 'i' is anticipated in Array B's future bounds. Therefore,
   * 'i' cannot be dropped. Array A must split its segment to preserve 'i' for future alignment,
   * injecting an empty, optional segment.
   */
  @Test
  public void testSupersetWithAnticipation() throws FunArrayBuilderException {
    testUnification(
        // Initial A: {0} 0 {i k} ⊤ {n}
        FunArrayBuilder.firstBound(exp(0))
            .value(0)
            .bound(exp("i"), exp("k"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build(),
        // Initial B: {0} 0 {k} ⊤ {i} ⊤ {n}
        FunArrayBuilder.firstBound(exp(0))
            .value(0)
            .bound(exp("k"))
            .value(Interval.UNBOUND)
            .bound(exp("i"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build(),
        // Expected A: {0} 0 {k} ⊥ {i}? ⊤ {n}
        FunArrayBuilder.firstBound(exp(0))
            .value(0)
            .bound(exp("k"))
            .value(Interval.EMPTY)
            .bound(exp("i"))
            .mayBeEmpty()
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build(),
        // Expected B: {0} 0 {k} ⊤ {i} ⊤ {n}
        FunArrayBuilder.firstBound(exp(0))
            .value(0)
            .bound(exp("k"))
            .value(Interval.UNBOUND)
            .bound(exp("i"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .build());
  }

  @Test
  public void testUnificationForWidening() throws FunArrayBuilderException {
    testUnification(
        // {0} 0 {i} ⊤ {n}?
        FunArrayBuilder.firstBound(exp(0))
            .value(0)
            .bound(exp("i"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .mayBeEmpty()
            .build(),
        // {0} 0 {i-1} 0 {i} ⊤ {n}?
        FunArrayBuilder.firstBound(exp(0))
            .value(0)
            .bound(exp("i-1", -1))
            .value(0)
            .bound(exp("i"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .mayBeEmpty()
            .build(),
        // {0} 0 {i} ⊤ {n}?
        FunArrayBuilder.firstBound(exp(0))
            .value(0)
            .bound(exp("i"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .mayBeEmpty()
            .build(),
        // {0} 0 {i} ⊤ {n}?
        FunArrayBuilder.firstBound(exp(0))
            .value(0)
            .bound(exp("i"))
            .value(Interval.UNBOUND)
            .bound(exp("n"))
            .mayBeEmpty()
            .build());
  }

  @Test
  public void testOpposingOrder() throws FunArrayBuilderException {
    testUnification(
        // {0} 0 {a} 0 {b} 0 {n}
        FunArrayBuilder.firstBound(exp(0))
            .value(0)
            .bound(exp("a"))
            .value(0)
            .bound(exp("b"))
            .value(0)
            .bound(exp("n"))
            .build(),
        // {0} 0 {b} 0 {a} 0 {n}
        FunArrayBuilder.firstBound(exp(0))
            .value(0)
            .bound(exp("b"))
            .value(0)
            .bound(exp("a"))
            .value(0)
            .bound(exp("n"))
            .build(),
        // {0} 0 {n}
        FunArrayBuilder.firstBound(exp(0)).value(0).bound(exp("n")).build(),
        // {0} 0 {n}
        FunArrayBuilder.firstBound(exp(0)).value(0).bound(exp("n")).build());
  }
}
