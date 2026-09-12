// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;

/**
 * Immutable snapshot of the paths to a block's results. Shared prefixes and suffixes are stored
 * once, including distinct edge sequences connecting the same pair of ARG states. Explicit paths
 * are materialized only while a consumer needs them. A later refinement or analysis reset cannot
 * change this snapshot's topology.
 *
 * <p>The representation is acyclic. Cyclic coverage requires a summary interpretation and must not
 * be approximated by silently dropping revisited nodes during path enumeration.
 */
final class DssBlockPathGraph {

  private record Incoming(ARGState parent, ImmutableList<CFAEdge> edges) {}

  private record Visit(ARGState state, boolean finished) {}

  private record Trace(ARGState state, ImmutableList<CFAEdge> edges, @Nullable Trace suffix) {}

  private final ARGState root;
  private final ImmutableMap<ARGState, ImmutableList<Incoming>> incoming;
  private final ImmutableMap<ARGState, BigInteger> pathCounts;

  DssBlockPathGraph(ARGState pRoot, Iterable<ARGState> pTargets) {
    root = pRoot;
    checkArgument(root.getParents().isEmpty(), "Expected an ARG root");
    Map<ARGState, ImmutableList<Incoming>> edges = new HashMap<>();
    Map<ARGState, BigInteger> counts = new HashMap<>();
    counts.put(root, BigInteger.ONE);
    edges.put(root, ImmutableList.of());
    Set<ARGState> active = new HashSet<>();
    ArrayDeque<Visit> todo = new ArrayDeque<>();
    for (ARGState target : pTargets) {
      todo.add(new Visit(target, false));
    }
    while (!todo.isEmpty()) {
      Visit visit = todo.removeLast();
      ARGState state = visit.state();
      if (counts.containsKey(state)) {
        continue;
      }
      if (visit.finished()) {
        BigInteger count = BigInteger.ZERO;
        for (Incoming edge : edges.get(state)) {
          count = count.add(counts.get(edge.parent()));
        }
        counts.put(state, count);
        active.remove(state);
        continue;
      }
      checkArgument(active.add(state), "Cyclic ARG paths require a block summary");
      checkArgument(!state.getParents().isEmpty(), "Block path does not reach its analysis root");
      ImmutableList.Builder<Incoming> parents = ImmutableList.builder();
      todo.add(new Visit(state, true));
      for (ARGState parent : state.getParents()) {
        for (ImmutableList<CFAEdge> path : state.getPathsFromParent(parent)) {
          checkState(!path.isEmpty(), "Missing CFA edges on a block path");
          parents.add(new Incoming(parent, path));
        }
        todo.add(new Visit(parent, false));
      }
      edges.put(state, parents.build());
    }
    incoming = ImmutableMap.copyOf(edges);
    pathCounts = ImmutableMap.copyOf(counts);
  }

  BigInteger pathCount(ARGState pTarget) {
    return pathCounts.get(pTarget);
  }

  Iterable<ARGPath> pathsTo(ARGState pTarget) {
    checkArgument(incoming.containsKey(pTarget), "State is not in this path snapshot");
    return () -> new PathIterator(pTarget);
  }

  private final class PathIterator extends AbstractIterator<ARGPath> {
    private final ArrayDeque<Trace> todo = new ArrayDeque<>();

    PathIterator(ARGState pTarget) {
      todo.add(new Trace(pTarget, ImmutableList.of(), null));
    }

    @Override
    protected ARGPath computeNext() {
      while (!todo.isEmpty()) {
        Trace current = todo.removeLast();
        if (current.state() == root) {
          return materialize(current);
        }
        for (Incoming edge : incoming.get(current.state()).reverse()) {
          todo.add(new Trace(edge.parent(), edge.edges(), current));
        }
      }
      return endOfData();
    }
  }

  private static ARGPath materialize(Trace pTrace) {
    ImmutableList.Builder<ARGState> states = ImmutableList.builder();
    List<@Nullable CFAEdge> innerEdges = new ArrayList<>();
    ImmutableList.Builder<CFAEdge> fullEdges = ImmutableList.builder();
    for (Trace trace = pTrace; trace != null; trace = trace.suffix()) {
      states.add(trace.state());
      if (trace.suffix() != null) {
        innerEdges.add(trace.edges().size() == 1 ? trace.edges().getFirst() : null);
        fullEdges.addAll(trace.edges());
      }
    }
    return new ARGPath(states.build(), innerEdges, fullEdges.build());
  }
}
