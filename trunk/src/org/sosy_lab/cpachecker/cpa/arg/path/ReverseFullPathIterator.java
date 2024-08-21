// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.path;

import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocations;

import com.google.common.collect.Iterables;

class ReverseFullPathIterator extends FullPathIterator {

  ReverseFullPathIterator(ARGPath pPath, int pPos, int pOverallOffset) {
    super(pPath, pPos, pOverallOffset);
  }

  ReverseFullPathIterator(ARGPath pPath) {
    this(pPath, pPath.size() - 1, pPath.getFullPath().size());
  }

  @Override
  public void advance() throws IllegalStateException {
    checkState(hasNext(), "No more states in PathIterator.");

    // if we are currently on a position with state and we have a real
    // (non-null) edge then we can directly set the parameters without
    // further checking
    if (path.getInnerEdges().get(pos - 1) != null && currentPositionHasState) {
      pos--;
      overallOffset--;
      currentPositionHasState = true;

    } else {

      boolean nextPositionHasState =
          Iterables.contains(
              extractLocations(getPreviousAbstractState()),
              fullPath.get(overallOffset - 1).getPredecessor());

      if (currentPositionHasState) {
        pos--; // only reduce by one if it was a real node before we are leaving it now
      }

      currentPositionHasState = nextPositionHasState;

      overallOffset--;
    }
  }

  @Override
  public void rewind() throws IllegalStateException {
    checkState(hasPrevious(), "No more states in PathIterator.");

    // if we are currently on a position with state and we have a real
    // (non-null) edge then we can directly set the parameters without
    // further checking
    if (path.getInnerEdges().get(pos) != null && currentPositionHasState) {
      pos++;
      overallOffset++;
      currentPositionHasState = true;

    } else {
      if (Iterables.contains(
          extractLocations(getNextAbstractState()), fullPath.get(overallOffset).getSuccessor())) {
        pos++;
        currentPositionHasState = true;
      } else {
        currentPositionHasState = false;
      }
      overallOffset++;
    }
  }

  @Override
  public boolean hasNext() {
    return overallOffset > 0;
  }

  @Override
  public boolean hasPrevious() {
    return pos < path.size() - 1;
  }
}
