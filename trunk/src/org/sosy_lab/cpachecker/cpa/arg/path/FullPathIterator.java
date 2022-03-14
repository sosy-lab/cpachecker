// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.path;

import static com.google.common.base.Preconditions.checkState;

import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

abstract class FullPathIterator extends PathIterator {
  protected final List<CFAEdge> fullPath;
  protected boolean currentPositionHasState = true;

  /* The position with respect to fullPath */
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
    checkState(overallOffset > 0, "First state in ARGPath has no incoming edge.");
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
    checkState(overallOffset > 0);
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
