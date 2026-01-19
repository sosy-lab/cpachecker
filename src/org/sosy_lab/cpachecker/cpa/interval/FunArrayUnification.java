// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interval;

import java.util.ArrayList;
import java.util.List;

public class FunArrayUnification {

  List<Bound> boundsA;
  List<Bound> boundsB;
  List<Interval> valuesA;
  List<Interval> valuesB;
  List<Boolean> emptinessA;
  List<Boolean> emptinessB;

  int currentIndex;

  public FunArrayUnification(
      FunArray arrayA,
      FunArray arrayB
  ) {
    this.boundsA = new ArrayList<>(arrayA.bounds());
    this.boundsB = new ArrayList<>(arrayB.bounds());
    this.valuesA = new ArrayList<>(arrayA.values());
    this.valuesB = new ArrayList<>(arrayB.values());
    this.emptinessA = new ArrayList<>(arrayA.emptiness());
    this.emptinessB = new ArrayList<>(arrayB.emptiness());
  }

  public record UnifyResult(FunArray resultA, FunArray resultB) {
  }

  public UnifyResult unify(
      Interval neutralElementA,
      Interval neutralElementB
  ) {
    while (currentIndex <= boundsA.size() && currentIndex <= boundsB.size()) {

      // Case 6
      if (currentIndex == boundsA.size() || currentIndex == boundsB.size()) {
        handleExtremal();
        continue;
      }

      var currentBoundA = boundsA.get(currentIndex);
      var currentBoundB = boundsB.get(currentIndex);

      var intersection = currentBoundA.intersection(currentBoundB);

      // Case 5
      // ┌───┐ ┌───┐
      // │ A │ │ B │
      // └───┘ └───┘
      if (intersection.isEmpty()) {
        handleDisjoint();
        continue;
      }

      var uniqueToA = currentBoundA.difference(currentBoundB);
      var uniqueToB = currentBoundB.difference(currentBoundA);

      if (uniqueToA.isEmpty()) {
        if (uniqueToB.isEmpty()) {
          assert currentBoundA.expressions().containsAll(currentBoundB.expressions());
          assert currentBoundB.expressions().containsAll(currentBoundA.expressions());
          // Case 1
          // ┌───────┐
          // │  A=B  │
          // └───────┘
          handleEqual();
          continue;
        } else {
          assert currentBoundB.expressions().containsAll(currentBoundA.expressions());
          // Case 3
          // ┌───────┐
          // │┌───┐  │
          // ││ A │ B│
          // │└───┘  │
          // └───────┘
          handleBIsSuperset();
          continue;
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
          handleAIsSuperset();
          continue;
        } else {
          assert currentBoundA.expressions().containsAll(intersection.expressions());
          assert currentBoundB.expressions().containsAll(intersection.expressions());
          // Case 4
          // ┌───────┐
          // │A ┌────┼──┐
          // │  │    │  │
          // └──┼────┘ B│
          //    └───────┘
          handlePartiallyOverlapping();
          continue;
        }
      }
    }

    // TODO Hofstetter: What happens here?
    return null;
  }

  // Corresponds to case 1: The bounds are equal.
  private void handleEqual() {
    //TODO
  }

  // Corresponds to case 2: Current bound A is a superset of current bound B.
  private void handleAIsSuperset() {
    //TODO
  }

  // Corresponds to case 3: Current bound B is a superset of current bound A.
  private void handleBIsSuperset() {
    //TODO
  }

  // Corresponds to case 4: The bounds are partially overlapping.
  private void handlePartiallyOverlapping() {
    //TODO
  }

  // Corresponds to case 5: The bounds are entirely disjoint.
  private void handleDisjoint() {
    //TODO
  }

  // Corresponds to case 6: The next bound is the last one in either of the arrays.
  private void handleExtremal() {
    //TODO
  }
}
