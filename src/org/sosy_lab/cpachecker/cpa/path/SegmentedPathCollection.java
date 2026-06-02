// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.path;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public record SegmentedPathCollection(ImmutableList<PathSegment> segments) {

  public SegmentedPathCollection {
    Preconditions.checkNotNull(segments);
    Preconditions.checkArgument(!segments.isEmpty(), "empty path collection");

    for (int i = 0; i < segments.size() - 1; i++) {
      Preconditions.checkArgument(
          segments.get(i).getFirstNode().equals(segments.get(i + 1).getLastNode()),
          "SegmentedPathCollection %s is not valid, as its segments do not follow each other",
          segments);
    }
  }

  public record CFAPath(ImmutableList<CFAEdge> path) {

    public CFAPath {
      Preconditions.checkNotNull(path);
      for (int i = 0; i < path.size() - 1; i++) {
        Preconditions.checkArgument(
            path.get(i).getSuccessor().equals(path.get(i + 1).getPredecessor()),
            "Path %s is not valid, as its edges do not follow each other",
            path);
      }
    }

    @Override
    public String toString() {
      return path.toString();
    }
  }

  public record PathSegment(ImmutableList<CFAPath> possiblePaths) {

    public PathSegment {
      Preconditions.checkNotNull(possiblePaths);
      Preconditions.checkArgument(!possiblePaths.isEmpty(), "empty path segment");

      CFAPath any = possiblePaths.getFirst();

      Preconditions.checkArgument(
          possiblePaths.stream()
              .allMatch(
                  p ->
                      !p.path.isEmpty()
                          && p.path
                              .getFirst()
                              .getPredecessor()
                              .equals(any.path.getFirst().getPredecessor())
                          && p.path
                              .getLast()
                              .getSuccessor()
                              .equals(any.path.getLast().getSuccessor())),
          "Paths in segment have different first and last nodes");

      // ensure lastSucc does not appear as any intermediate successor or predecessor
      Preconditions.checkArgument(
          possiblePaths.stream()
              .allMatch(
                  p -> {
                    ImmutableList<CFAEdge> edges = p.path();
                    // no intermediate edge's successor equals lastSucc
                    for (int i = 0; i < edges.size() - 1; i++) {
                      if (edges.get(i).getSuccessor().equals(any.path.getLast().getSuccessor())) {
                        return false;
                      }
                    }
                    return true;
                  }),
          "a path contains the segment's last node before its end");
    }

    @Override
    public String toString() {
      return "{" + possiblePaths + "}";
    }

    public CFANode getLastNode() {
      return possiblePaths.getFirst().path.getLast().getSuccessor();
    }

    public CFANode getFirstNode() {
      return possiblePaths.getFirst().path.getFirst().getPredecessor();
    }
  }
}
