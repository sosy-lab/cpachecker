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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

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

    boolean previousPositionHasState =
        Iterables.contains(
            extractLocations(getPreviousAbstractState()),
            fullPath.get(overallOffset - 1).getPredecessor());

    if (currentPositionHasState) {
      pos--; // only reduce by one if it was a real node before we are leaving it now
    }

    currentPositionHasState = previousPositionHasState;

    overallOffset--;
  }

  @Override
  public void rewind() throws IllegalStateException {
    checkState(hasPrevious(), "No more states in PathIterator.");

    CFAEdge nextEdge = fullPath.get(overallOffset);
    Iterable<CFANode> nextLocation = extractLocations(getNextAbstractState());
    boolean nextPositionHasState =
        Iterables.contains(nextLocation, nextEdge.getSuccessor()) // forward analysis
            || Iterables.contains(nextLocation, nextEdge.getPredecessor()); // backward analysis

    if (nextPositionHasState) {
      pos++;
    }

    currentPositionHasState = nextPositionHasState;
    overallOffset++;
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
