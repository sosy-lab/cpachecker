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

import java.util.List;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

abstract class FullPathIterator extends PathIterator {
  protected final List<CFAEdge> fullPath;
  protected boolean currentPositionHasState = true;
  protected int overallOffset;

  protected FullPathIterator(ARGPath pPath, int pPos, int pOverallOffset) {
    super(pPath, pPos);
    fullPath = pPath.getFullPath();
    overallOffset = pOverallOffset;
  }

  @Override
  public int getIndex() {
    return overallOffset;
  }

  @Override
  public PathPosition getPosition() {
    return new FullPathPosition(path, pos, overallOffset);
  }

  /**
   * {@inheritDoc} May only be called on positions of the iterator where we have an {@link ARGState}
   * not in the edges that fill up holes between them.
   */
  @Override
  public ARGState getAbstractState() {
    checkState(currentPositionHasState);
    return path.asStatesList().get(pos);
  }

  @Override
  public boolean isPositionWithState() {
    return currentPositionHasState;
  }

  @Override
  public @Nullable CFAEdge getIncomingEdge() {
    checkState(pos > 0, "First state in ARGPath has no incoming edge.");
    return fullPath.get(overallOffset - 1);
  }

  @Override
  public @Nullable CFAEdge getOutgoingEdge() {
    checkState(pos < path.size() - 1, "Last state in ARGPath has no outgoing edge.");
    return fullPath.get(overallOffset);
  }

  /**
   * {@inheritDoc} Returns the directly previous AbstractState that can be found, thus this is the
   * appropriate replacement for {@code FullPathIterator#getAbstractState()} if the iterator is
   * currently in a hole in the path that was filled with additional edges.
   */
  @Override
  public ARGState getPreviousAbstractState() {
    checkState(pos - 1 >= 0);
    if (currentPositionHasState) {
      return path.asStatesList().get(pos - 1);
    } else {
      return path.asStatesList().get(pos);
    }
  }

  /**
   * {@inheritDoc} While in the hole of a path prefix inclusive returns the prefix inclusive the
   * state following the hole of this path.
   */
  @Override
  public ARGPath getPrefixInclusive() {
    if (currentPositionHasState) {
      return new ARGPath(
          path.asStatesList().subList(0, pos + 1), path.getInnerEdges().subList(0, pos));
    } else {
      return new ARGPath(
          path.asStatesList().subList(0, pos + 2), path.getInnerEdges().subList(0, pos + 1));
    }
  }

  /**
   * {@inheritDoc} While in the hole of a path prefix exclusive returns the prefix exclusive the
   * state following the hole of this path. (But inclusive the last found state)
   */
  @Override
  public ARGPath getPrefixExclusive() {
    checkState(
        !currentPositionHasState || pos > 0,
        "Exclusive prefix of first state in path would be empty.");
    if (currentPositionHasState) {
      checkState(pos != 0);
      return new ARGPath(
          path.asStatesList().subList(0, pos), path.getInnerEdges().subList(0, pos - 1));
    } else {
      return new ARGPath(
          path.asStatesList().subList(0, pos + 1), path.getInnerEdges().subList(0, pos));
    }
  }
}
