/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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
