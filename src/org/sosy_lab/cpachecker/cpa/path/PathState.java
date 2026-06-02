// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.path;

import com.google.common.collect.ImmutableList;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class PathState implements AbstractState {

  public static final PathState INVALID = new PathState(null, -1, ImmutableList.of(), 0);

  private final @Nullable SegmentedPathCollection paths;

  /** the index of the segment we are in. */
  private final int segmentIndex;

  /** which paths of the current segment are still possible */
  private final ImmutableList<Integer> activePaths;

  /** how far into the current segment we are */
  private final int pathIndex;

  public PathState(SegmentedPathCollection pPaths) {
    this(pPaths, 0, allPathsActive(pPaths.segments().getFirst()), 0);
  }

  private PathState(
      @Nullable SegmentedPathCollection pPaths,
      int pSegmentIndex,
      ImmutableList<Integer> pActiveIndexes,
      int pPathIndex) {
    paths = pPaths;
    segmentIndex = pSegmentIndex;
    activePaths = pActiveIndexes;
    pathIndex = pPathIndex;
  }

  private static ImmutableList<Integer> allPathsActive(
      SegmentedPathCollection.PathSegment pSegment) {
    ImmutableList.Builder<Integer> positions = ImmutableList.builder();
    for (int i = 0; i < pSegment.possiblePaths().size(); i++) {
      positions.add(i);
    }
    return positions.build();
  }

  private boolean isAtEndOfPath() {
    return paths != null && segmentIndex == paths.segments().size();
  }

  public boolean isInvalid() {
    return paths == null;
  }

  public PathState followEdge(CFAEdge pEdge) {
    if (isInvalid() || isAtEndOfPath()) {
      return INVALID;
    }

    SegmentedPathCollection.PathSegment currentSegment = paths.segments().get(segmentIndex);
    ImmutableList.Builder<Integer> newActivePositions = ImmutableList.builder();

    int nextPathIndex = pathIndex + 1;

    for (int activePathIndex : activePaths) {
      SegmentedPathCollection.CFAPath path = currentSegment.possiblePaths().get(activePathIndex);
      ImmutableList<CFAEdge> pathEdges = path.path();
      if (pathEdges.get(pathIndex).equals(pEdge)) {
        if (nextPathIndex == pathEdges.size()) {
          // we reached the end of a segment
          int nextSegmentIndex = segmentIndex + 1;
          if (nextSegmentIndex == paths.segments().size()) {
            return new PathState(paths, nextSegmentIndex, ImmutableList.of(), 0);
          }
          return new PathState(
              paths, nextSegmentIndex, allPathsActive(paths.segments().get(nextSegmentIndex)), 0);

        } else {
          newActivePositions.add(activePathIndex);
        }
      }
    }

    ImmutableList<Integer> positions = newActivePositions.build();

    return positions.isEmpty()
        ? INVALID
        : new PathState(paths, segmentIndex, positions, nextPathIndex);
  }

  @Override
  public int hashCode() {
    return Objects.hash(segmentIndex, paths, activePaths, pathIndex);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof PathState other
        && Objects.equals(paths, other.paths)
        && segmentIndex == other.segmentIndex
        && Objects.equals(activePaths, other.activePaths)
        && pathIndex == other.pathIndex;
  }

  @Override
  public String toString() {
    if (isInvalid()) {
      return "Invalid path state";
    }
    if (isAtEndOfPath()) {
      return "Reached end of path collection";
    }

    SegmentedPathCollection.PathSegment currentSegment = paths.segments().get(segmentIndex);
    ImmutableList.Builder<ImmutableList<CFAEdge>> remainingPaths = ImmutableList.builder();
    for (int position : activePaths) {
      ImmutableList<CFAEdge> path = currentSegment.possiblePaths().get(position).path();
      remainingPaths.add(path.subList(pathIndex, path.size()));
    }
    return "In segment " + segmentIndex + ", remaining paths: " + remainingPaths.build();
  }
}
