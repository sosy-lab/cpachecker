// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.path;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.Objects;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class PathState implements AbstractState {

  final ViolationWitness paths;

  /** the index of the segment we are in. */
  final int segmentIndex;

  /** the path of the current segment we have chosen for this abstract state */
  final ImmutableList<String> activePath;

  /** how far into the current path we are */
  final int pathIndex;

  final boolean isInitial;

  PathState(
      ViolationWitness pPaths,
      int pSegmentIndex,
      ImmutableList<String> pActivePath,
      int pPathIndex) {
    this(pPaths, pSegmentIndex, pActivePath, pPathIndex, false);
  }

  public PathState(
      ViolationWitness pPaths,
      int pSegmentIndex,
      ImmutableList<String> pActivePath,
      int pPathIndex,
      boolean pIsInitial) {
    paths = pPaths;
    segmentIndex = pSegmentIndex;
    activePath = pActivePath;
    pathIndex = pPathIndex;
    isInitial = pIsInitial;
  }

  public static PathState initialState(ViolationWitness pPaths) {
    return new PathState(pPaths, 0, null, 0, true);
  }

  public static Iterable<PathState> initialStates(ViolationWitness pPaths) {

    if (pPaths.witness().isEmpty()) {
      return ImmutableList.of(new PathState(pPaths, 0, null, 0));
    }

    return startSegment(pPaths, 0);
  }

  static FluentIterable<PathState> startSegment(ViolationWitness pPaths, int segment) {

    if (segment == pPaths.witness().size()) {
      return FluentIterable.of(new PathState(pPaths, segment, null, -1));
    }

    if (pPaths.witness().get(segment).isEmpty()) {
      return startSegment(pPaths, segment + 1);
    }

    return FluentIterable.from(pPaths.witness().get(segment))
        .transform(nextPath -> new PathState(pPaths, segment, nextPath, 0));
  }

  boolean isAtEndOfPath() {
    return segmentIndex == paths.witness().size();
  }

  @Override
  public int hashCode() {
    return Objects.hash(segmentIndex, paths, activePath, pathIndex);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof PathState other
        && Objects.equals(paths, other.paths)
        && segmentIndex == other.segmentIndex
        && Objects.equals(activePath, other.activePath)
        && pathIndex == other.pathIndex
        && isInitial == other.isInitial;
  }

  @Override
  public String toString() {
    if (isAtEndOfPath()) {
      return "Reached end of path";
    }

    if (isInitial) {
      return "Initial State for: " + paths;
    }

    return "In segment "
        + segmentIndex
        + ", remaining chosen path until next segment: "
        + activePath.subList(pathIndex, activePath.size());
  }
}
