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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

class DefaultFullPathIterator extends FullPathIterator {

  DefaultFullPathIterator(ARGPath pPath, int pPos, int pOverallOffset) {
    super(pPath, pPos, pOverallOffset);
  }

  DefaultFullPathIterator(ARGPath pPath) {
    this(pPath, 0, 0);
  }

  @Override
  public void advance() throws IllegalStateException {
    checkState(hasNext(), "No more states in PathIterator.");

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
  public void rewind() throws IllegalStateException {
    checkState(hasPrevious(), "No more states in PathIterator.");

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
  public boolean hasNext() {
    return pos < path.size() - 1;
  }

  @Override
  public boolean hasPrevious() {
    return overallOffset > 0;
  }
}
