// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interval.funarray;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BinaryOperator;
import org.sosy_lab.cpachecker.cpa.interval.Interval;

/**
 * Aligns two {@link FunArray} instances so their segment bounds coincide, enabling pointwise
 * operations on corresponding segments. The unification algorithm processes the bound lists
 * left-to-right, resolving six possible cases at each index position (Section 11.4 in Cousot,
 * Cousot, and Logozzo (2011)).
 *
 * <p>The class is instantiated with two arrays and mutates internal bound, value, and emptiness
 * lists during {@link #unify}. The result is a {@link UnifyResult} holding two new {@link
 * FunArray}s with identical bound structure, ready for a pointwise operation.
 *
 * <p>See: Patrick Cousot, Radhia Cousot, and Francesco Logozzo. 2011. A parametric segmentation
 * functor for fully automatic and scalable array content analysis. SIGPLAN Not. 46, 1 (January
 * 2011), 105–118. <a href="https://doi.org/10.1145/1925844.1926399">
 * https://doi.org/10.1145/1925844.1926399</a>
 */
public class FunArrayUnification {

  List<Bound> boundsA;
  List<Bound> boundsB;
  List<Interval> valuesA;
  List<Interval> valuesB;
  List<Boolean> emptinessA;
  List<Boolean> emptinessB;

  int currentIndex;

  private BinaryOperator<Interval> collapseOpA = Interval::union;
  private BinaryOperator<Interval> collapseOpB = Interval::union;

  /**
   * Initializes the unification state by copying the bounds, values, and emptiness flags of both
   * arrays into mutable lists that will be updated during {@link #unify}.
   *
   * @param arrayA the first FunArray to unify.
   * @param arrayB the second FunArray to unify.
   */
  public FunArrayUnification(FunArray arrayA, FunArray arrayB) {
    this.boundsA = new ArrayList<>(arrayA.bounds());
    this.boundsB = new ArrayList<>(arrayB.bounds());
    this.valuesA = new ArrayList<>(arrayA.values());
    this.valuesB = new ArrayList<>(arrayB.values());
    this.emptinessA = new ArrayList<>(arrayA.emptiness());
    this.emptinessB = new ArrayList<>(arrayB.emptiness());
  }

  /** Holds the two structurally aligned FunArrays produced by a successful unification. */
  public record UnifyResult(FunArray resultA, FunArray resultB) {}

  /**
   * Variant of {@link #unify} that allows callers to override the binary operator used when
   * collapsing ongoing segments in Case 6. {@code pCollapseOpA} is applied when A is the ongoing
   * array (B exhausted); {@code pCollapseOpB} is applied when B is the ongoing array (A exhausted).
   */
  public UnifyResult unify(
      Interval pNeutralElementA,
      Interval pNeutralElementB,
      BinaryOperator<Interval> pCollapseOpA,
      BinaryOperator<Interval> pCollapseOpB) {
    collapseOpA = pCollapseOpA;
    collapseOpB = pCollapseOpB;
    return unify(pNeutralElementA, pNeutralElementB);
  }

  /**
   * Runs the unification algorithm to produce two {@link FunArray}s with identical bound structure.
   * The algorithm steps through the bound lists left-to-right, applying one of the six cases from
   * Section 11.4 of Cousot, Cousot, and Logozzo (2011) at each position until both lists are
   * exhausted. The resulting arrays are cleaned up with {@link FunArray#removeEmptyBounds()} before
   * being returned.
   *
   * @param neutralElementA the interval inserted into newly created segments in array A.
   * @param neutralElementB the interval inserted into newly created segments in array B.
   * @return a {@link UnifyResult} containing two structurally aligned FunArrays.
   */
  public UnifyResult unify(Interval neutralElementA, Interval neutralElementB) {
    while (currentIndex < boundsA.size() - 1 || currentIndex < boundsB.size() - 1) {

      // Case 6
      if (currentIndex == boundsA.size() - 1) {
        handleLastInA();
        continue;
      }
      if (currentIndex == boundsB.size() - 1) {
        handleLastInB();
        continue;
      }

      Bound currentBoundA = boundsA.get(currentIndex);
      Bound currentBoundB = boundsB.get(currentIndex);

      Bound intersection = currentBoundA.intersection(currentBoundB);

      Bound uniqueToA = currentBoundA.difference(currentBoundB);
      Bound uniqueToB = currentBoundB.difference(currentBoundA);

      if (uniqueToA.isEmpty()) {
        if (uniqueToB.isEmpty()) {
          assert currentBoundA.expressions().containsAll(currentBoundB.expressions());
          assert currentBoundB.expressions().containsAll(currentBoundA.expressions());
          // Case 1
          // ┌───────┐
          // │  A=B  │
          // └───────┘
          handleEqual();
        } else {
          assert currentBoundB.expressions().containsAll(currentBoundA.expressions());
          // Case 3
          // ┌───────┐
          // │┌───┐  │
          // ││ A │ B│
          // │└───┘  │
          // └───────┘
          handleBIsSuperset(uniqueToB, intersection, neutralElementB);
        }
      } else {
        if (uniqueToB.isEmpty()) {
          assert currentBoundA.expressions().containsAll(currentBoundB.expressions());
          // Case 2
          // ┌───────┐
          // │  ┌───┐│
          // │A │ B ││
          // │  └───┘│
          // └───────┘
          handleAIsSuperset(uniqueToA, intersection, neutralElementA);
        } else {
          assert currentBoundA.expressions().containsAll(intersection.expressions());
          assert currentBoundB.expressions().containsAll(intersection.expressions());
          // Case 4
          // ┌───────┐
          // │A ┌────┼──┐
          // │  │    │  │
          // └──┼────┘ B│
          //    └───────┘
          handlePartiallyOverlapping(
              uniqueToA, uniqueToB, intersection, neutralElementA, neutralElementB);
        }
      }
    }

    return new UnifyResult(
        new FunArray(boundsA, valuesA, emptinessA).removeEmptyBounds(),
        new FunArray(boundsB, valuesB, emptinessB).removeEmptyBounds());
  }

  /**
   * Handles Case 1 of the unification algorithm: the current bounds of A and B are equal. Advances
   * the index without modifying either array.
   */
  private void handleEqual() {
    currentIndex++;
  }

  /**
   * Handles Case 2 of the unification algorithm: the current bound of A is a strict superset of the
   * current bound of B. Delegates to {@link #handleSuperset} with A as the superset array.
   *
   * @param uniqueToA the expressions present in A's bound but not B's.
   * @param intersection the expressions common to both bounds.
   * @param neutralElementA the neutral interval for newly inserted A segments.
   */
  private void handleAIsSuperset(Bound uniqueToA, Bound intersection, Interval neutralElementA) {
    handleSuperset(uniqueToA, intersection, boundsA, valuesA, emptinessA, boundsB, neutralElementA);
  }

  /**
   * Handles Case 3 of the unification algorithm: the current bound of B is a strict superset of the
   * current bound of A. Delegates to {@link #handleSuperset} with B as the superset array.
   *
   * @param uniqueToB the expressions present in B's bound but not A's.
   * @param intersection the expressions common to both bounds.
   * @param neutralElementB the neutral interval for newly inserted B segments.
   */
  private void handleBIsSuperset(Bound uniqueToB, Bound intersection, Interval neutralElementB) {
    handleSuperset(uniqueToB, intersection, boundsB, valuesB, emptinessB, boundsA, neutralElementB);
  }

  /**
   * Generalizes the superset handling for Cases 2 and 3. If none of the unique expressions of the
   * superset bound appear later in the opposite array's bounds (case 2.1 / 3.1), the superset bound
   * is shrunk to the intersection. Otherwise (case 2.2 / 3.2), the superset bound is split: the
   * intersection becomes the lower bound at the current index, and a new possibly-empty segment
   * with value {@code neutralElement} is inserted between the intersection and the unique
   * expressions, which are placed as the upper bound of that new segment.
   *
   * @param uniqueToSuperset the expressions unique to the superset bound.
   * @param intersection the expressions shared by both bounds.
   * @param bounds the mutable bound list of the superset array.
   * @param values the mutable value list of the superset array.
   * @param emptiness the mutable emptiness list of the superset array.
   * @param oppositeBounds the mutable bound list of the other array.
   * @param neutralElement the interval inserted for the new segment.
   */
  // TODO Hofstetter: This is propably just a special case of case 4. Replace it.
  private void handleSuperset(
      Bound uniqueToSuperset,
      Bound intersection,
      List<Bound> bounds,
      List<Interval> values,
      List<Boolean> emptiness,
      List<Bound> oppositeBounds,
      Interval neutralElement) {
    Set<NormalFormExpression> anticipatedInOppositeBounds =
        filterAnticipatedInOppositeBounds(uniqueToSuperset, oppositeBounds);
    if (anticipatedInOppositeBounds.isEmpty()) {
      // Corresponds to case 2.1 (or 3.1)
      bounds.set(currentIndex, intersection);
    } else {
      // Corresponds to case 2.2 (or 3.2)
      values.add(currentIndex, neutralElement);
      emptiness.add(currentIndex, true);
      bounds.set(currentIndex, uniqueToSuperset);
      bounds.add(currentIndex, intersection);
    }
  }

  /**
   * Returns the subset of expressions in {@code bound} that also appear in {@code oppositeBounds}
   * at or after the current index position. These are the expressions that will eventually be
   * encountered in the other array, and should therefore be preserved by anticipating them earlier.
   *
   * @param bound the bound whose expressions are tested.
   * @param oppositeBounds the other array's bound list.
   * @return the expressions from {@code bound} anticipated in {@code oppositeBounds}.
   */
  private Set<NormalFormExpression> filterAnticipatedInOppositeBounds(
      Bound bound, List<Bound> oppositeBounds) {
    Set<NormalFormExpression> anticipatedInOppositeBounds =
        oppositeBounds.stream()
            .skip(currentIndex)
            .flatMap(b -> b.expressions().stream())
            .collect(ImmutableSet.toImmutableSet());

    return bound.expressions().stream()
        .filter(e -> anticipatedInOppositeBounds.contains(e))
        .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * Handles Case 4 of the unification algorithm: the current bounds of A and B partially overlap.
   * For each array's unique expressions that are anticipated in the opposite array, new segments
   * are inserted at the current position. If all unique expressions on both sides are fully
   * anticipated, the case degenerates to Case 5 ({@link #handleDisjoint}).
   *
   * @param uniqueToA expressions exclusive to A's bound.
   * @param uniqueToB expressions exclusive to B's bound.
   * @param intersection expressions shared by both bounds.
   * @param neutralElementA the neutral interval inserted for new segments in A.
   * @param neutralElementB the neutral interval inserted for new segments in B.
   */
  private void handlePartiallyOverlapping(
      Bound uniqueToA,
      Bound uniqueToB,
      Bound intersection,
      Interval neutralElementA,
      Interval neutralElementB) {
    Set<NormalFormExpression> anticipatedFromA =
        filterAnticipatedInOppositeBounds(uniqueToA, boundsB);
    Set<NormalFormExpression> anticipatedFromB =
        filterAnticipatedInOppositeBounds(uniqueToB, boundsA);

    if (anticipatedFromA.containsAll(uniqueToA.expressions())
        && anticipatedFromB.containsAll(uniqueToB.expressions())) {
      handleDisjoint();
      return;
    }

    boundsA.set(currentIndex, intersection);
    boundsB.set(currentIndex, intersection);

    if (!anticipatedFromA.isEmpty()) {
      boundsA.add(currentIndex + 1, new Bound(anticipatedFromA));
      valuesA.add(currentIndex, neutralElementA);
      emptinessA.add(currentIndex, true);
    }

    if (!anticipatedFromB.isEmpty()) {
      boundsB.add(currentIndex + 1, new Bound(anticipatedFromB));
      valuesB.add(currentIndex, neutralElementB);
      emptinessB.add(currentIndex, true);
    }
  }

  /**
   * Handles Case 5 of the unification algorithm: the current bounds of A and B are entirely
   * disjoint. Both bounds at the current index are dropped, and their segment values are joined
   * into the preceding segment in each array.
   */
  private void handleDisjoint() {
    // A prerequisite for array unification is that the two arrays must have the same extremal
    // bounds. Therefore, if the current bounds are entirely disjoint, this cannot be the first
    // bound.
    assert currentIndex > 0;

    dropBound(boundsA, valuesA, emptinessA, currentIndex);
    dropBound(boundsB, valuesB, emptinessB, currentIndex);
  }

  /**
   * Removes the bound at {@code index} from a bound list and merges its associated segment value
   * and emptiness flag into the preceding segment via a union operation.
   *
   * @param bounds the mutable bound list to modify.
   * @param values the mutable value list to modify.
   * @param emptiness the mutable emptiness list to modify.
   * @param index the index of the bound to drop.
   */
  private void dropBound(
      List<Bound> bounds, List<Interval> values, List<Boolean> emptiness, int index) {
    bounds.remove(index);
    joinElementWithPredecessor(values, index, (a, b) -> a.union(b));
    joinElementWithPredecessor(emptiness, index, FunArrayUnification::joinEmptiness);
  }

  /**
   * Joins the element at {@code index} with its predecessor in {@code list} using {@code join},
   * then removes the element at {@code index}, leaving the combined result at {@code index - 1}.
   *
   * @param <T> the element type.
   * @param list the mutable list to modify.
   * @param index the index of the element to join with its predecessor.
   * @param join the binary operator used to combine the two elements.
   */
  private static <T> void joinElementWithPredecessor(
      List<T> list, int index, BinaryOperator<T> join) {
    assert index < list.size();
    assert index > 0;

    T union = join.apply(list.get(index - 1), list.get(index));
    list.set(index, union);
    list.remove(index - 1);
  }

  /**
   * Joins two emptiness flags for adjacent segments that are being merged into one. The merged
   * segment {@code [a, c)} can only be empty if both constituent segments {@code [a, b)} and {@code
   * [b, c)} may be empty simultaneously (i.e., {@code a == b == c}). If either segment is
   * definitely non-empty, the merged segment is also definitely non-empty.
   *
   * @param a the emptiness flag of the predecessor segment.
   * @param b the emptiness flag of the segment being merged in.
   * @return {@code true} only if both segments may be empty.
   */
  private static boolean joinEmptiness(boolean a, boolean b) {
    return a && b;
  }

  /** Delegates to {@link #handleLast} when array A is exhausted (Case 6). */
  private void handleLastInA() {
    handleLast(boundsA, boundsB, valuesB, emptinessB, currentIndex, collapseOpB);
  }

  /** Delegates to {@link #handleLast} when array B is exhausted (Case 6). */
  private void handleLastInB() {
    handleLast(boundsB, boundsA, valuesA, emptinessA, currentIndex, collapseOpA);
  }

  /**
   * Handles Case 6 of the unification algorithm: one array has reached its last bound while the
   * other still has remaining bounds. The current bound of the exhausted array is merged with the
   * current and next bound of the ongoing array into a single joint bound, and the now-redundant
   * segment in the ongoing array is joined into its predecessor.
   *
   * @param pExhaustedBounds the mutable bound list of the array that has reached its last bound.
   * @param pOngoingBounds the mutable bound list of the array that still has remaining bounds.
   * @param pOngoingValues the mutable value list of the ongoing array.
   * @param pOngoingEmptiness the mutable emptiness list of the ongoing array.
   * @param pCurrentIndex the current index position in both bound lists.
   * @param pCollapseOp the binary operator used to combine values when collapsing ongoing segments.
   */
  private static void handleLast(
      List<Bound> pExhaustedBounds,
      List<Bound> pOngoingBounds,
      List<Interval> pOngoingValues,
      List<Boolean> pOngoingEmptiness,
      int pCurrentIndex,
      BinaryOperator<Interval> pCollapseOp) {
    assert pOngoingBounds.size() > pCurrentIndex + 1;
    assert pOngoingBounds.size() != pExhaustedBounds.size();

    Bound currentBoundExhausted = pExhaustedBounds.get(pCurrentIndex);
    Bound currentBoundOngoing = pOngoingBounds.get(pCurrentIndex);
    Bound nextBoundOngoing = pOngoingBounds.get(pCurrentIndex + 1);

    Bound joinedBound = currentBoundExhausted.union(currentBoundOngoing).union(nextBoundOngoing);

    pExhaustedBounds.set(pCurrentIndex, joinedBound);
    pOngoingBounds.set(pCurrentIndex, joinedBound);
    pOngoingBounds.remove(pCurrentIndex + 1);
    joinElementWithPredecessor(pOngoingValues, pCurrentIndex, pCollapseOp);
    joinElementWithPredecessor(
        pOngoingEmptiness, pCurrentIndex, FunArrayUnification::joinEmptiness);
  }
}
