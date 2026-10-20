// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.util.AbstractStates;

/** Checks for retaining incoming paths when joining or covering ARG occurrences. */
final class ARGPathPreservation {

  private ARGPathPreservation() {}

  /**
   * Whether the parents of {@code pIncoming} can be attached to {@code pReached} without making a
   * cycle. In this mode coverage is represented by shared suffixes, not by an executable edge
   * between the two states. Analyses requiring cyclic coverage need a separate path-summary
   * interpretation; silently enumerating only simple paths would lose loop iterations.
   */
  static boolean canSharePaths(ARGState pIncoming, ARGState pReached) {
    if (AbstractStates.extractLocation(pIncoming) == null
        || AbstractStates.extractLocation(pIncoming) != AbstractStates.extractLocation(pReached)) {
      return false;
    }
    Set<ARGState> seen = new HashSet<>();
    ArrayDeque<ARGState> todo = new ArrayDeque<>(pIncoming.getParents());
    while (!todo.isEmpty()) {
      ARGState ancestor = todo.removeLast();
      if (ancestor == pReached) {
        return false;
      }
      if (seen.add(ancestor)) {
        todo.addAll(ancestor.getParents());
      }
    }
    return true;
  }
}
