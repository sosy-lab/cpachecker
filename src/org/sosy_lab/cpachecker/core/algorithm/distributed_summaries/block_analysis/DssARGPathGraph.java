// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;

/**
 * Frozen acyclic ARG fragment representing all paths from a block entry to a target.
 *
 * <p>The graph lets violation-condition computation reuse shared paths instead of enumerating them.
 * It extends {@link ARGPath} to fit the existing operator interface; the inherited single path is
 * only a representative for metadata. Computation must use {@link #backwardOrder()} and {@link
 * #incoming(ARGState)} to include every alternative, or enumerate them with {@link #paths()}.
 */
public final class DssARGPathGraph extends ARGPath {
  public record Incoming(ARGState parent, ImmutableList<CFAEdge> edges) {}

  private final ImmutableMap<ARGState, ImmutableList<Incoming>> incoming;
  private final ImmutableList<ARGState> order;

  private DssARGPathGraph(
      List<ARGState> representative, Map<ARGState, ImmutableList<Incoming>> pIncoming) {
    super(representative);
    getFullPath(); // Freeze the representative before a later refinement can mutate the ARG.
    incoming = ImmutableMap.copyOf(pIncoming);
    order = ImmutableList.copyOf(pIncoming.keySet()).reverse();
  }

  public static DssARGPathGraph of(ARGState root, ARGState target) {
    Map<ARGState, ImmutableList<Incoming>> incoming = new LinkedHashMap<>();
    collectIncoming(root, target, incoming);
    List<ARGState> path = new ArrayList<>();
    ARGState current = target;
    path.add(current);
    while (current != root) {
      current = incoming.get(current).getFirst().parent();
      path.add(current);
    }
    return new DssARGPathGraph(path.reversed(), incoming);
  }

  private record Frame(
      ARGState node, Iterator<ARGState> parents, ImmutableList.Builder<Incoming> edges) {}

  /**
   * Depth-first search from the target towards the root that inserts each state into {@code
   * incoming} after all of its parents (post-order). Uses an explicit stack because blocks with
   * long paths overflow the call stack.
   */
  private static void collectIncoming(
      ARGState root, ARGState target, Map<ARGState, ImmutableList<Incoming>> incoming) {
    Set<ARGState> active = new HashSet<>();
    Deque<Frame> stack = new ArrayDeque<>();
    push(root, target, stack, active);
    while (!stack.isEmpty()) {
      Frame frame = stack.peek();
      if (frame.parents().hasNext()) {
        ARGState parent = frame.parents().next();
        ImmutableList<CFAEdge> path = ImmutableList.copyOf(parent.getEdgesToChild(frame.node()));
        Preconditions.checkState(!path.isEmpty(), "Missing ARG transition");
        frame.edges().add(new Incoming(parent, path));
        if (!incoming.containsKey(parent)) {
          push(root, parent, stack, active);
        }
      } else {
        stack.pop();
        incoming.put(frame.node(), frame.edges().build());
        active.remove(frame.node());
      }
    }
  }

  private static void push(ARGState root, ARGState node, Deque<Frame> stack, Set<ARGState> active) {
    Preconditions.checkState(active.add(node), "Cyclic ARG inside a DSS block");
    Iterator<ARGState> parents;
    if (node == root) {
      parents = Collections.emptyIterator();
    } else {
      Preconditions.checkState(!node.getParents().isEmpty(), "Unexpected second ARG root");
      parents = node.getParents().iterator();
    }
    stack.push(new Frame(node, parents, ImmutableList.builder()));
  }

  public ImmutableList<ARGState> backwardOrder() {
    return order;
  }

  public ImmutableList<Incoming> incoming(ARGState node) {
    return incoming.get(node);
  }

  /** A path suffix that ends in the target, sharing its tail with other suffixes. */
  private record Suffix(
      ARGState first, ImmutableList<CFAEdge> edgesToRest, @Nullable Suffix rest) {}

  /**
   * Enumerates every path of this graph, using the edges frozen at construction. The number of
   * paths is exponential in the number of joins.
   */
  public ImmutableList<ARGPath> paths() {
    ImmutableList.Builder<ARGPath> paths = ImmutableList.builder();
    Deque<Suffix> worklist = new ArrayDeque<>();
    worklist.push(new Suffix(getLastState(), ImmutableList.of(), null));
    while (!worklist.isEmpty()) {
      Suffix suffix = worklist.pop();
      if (suffix.first() == getFirstState()) {
        paths.add(toPath(suffix));
        continue;
      }
      for (Incoming parent : incoming.get(suffix.first()).reverse()) {
        worklist.push(new Suffix(parent.parent(), parent.edges(), suffix));
      }
    }
    return paths.build();
  }

  private static ARGPath toPath(Suffix pSuffix) {
    List<ARGState> states = new ArrayList<>();
    List<@Nullable CFAEdge> innerEdges = new ArrayList<>();
    ImmutableList.Builder<CFAEdge> fullPath = ImmutableList.builder();
    for (Suffix suffix = pSuffix; suffix != null; suffix = suffix.rest()) {
      states.add(suffix.first());
      if (suffix.rest() != null) {
        innerEdges.add(suffix.edgesToRest().size() == 1 ? suffix.edgesToRest().getFirst() : null);
        fullPath.addAll(suffix.edgesToRest());
      }
    }
    return new ARGPath(states, Collections.unmodifiableList(innerEdges), fullPath.build());
  }

  public Object graphId() {
    return incoming;
  }
}
