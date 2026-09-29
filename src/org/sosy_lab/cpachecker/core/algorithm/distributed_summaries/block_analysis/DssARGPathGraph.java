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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;

/** Frozen acyclic ARG fragment; the inherited path is only a representative for legacy metadata. */
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
    visit(root, target, incoming, new HashSet<>());
    List<ARGState> path = new ArrayList<>();
    ARGState current = target;
    path.add(current);
    while (current != root) {
      current = incoming.get(current).getFirst().parent();
      path.add(current);
    }
    return new DssARGPathGraph(path.reversed(), incoming);
  }

  private static void visit(
      ARGState root,
      ARGState node,
      Map<ARGState, ImmutableList<Incoming>> incoming,
      Set<ARGState> active) {
    if (incoming.containsKey(node)) {
      return;
    }
    Preconditions.checkState(active.add(node), "Cyclic ARG inside a DSS block");
    var edges = ImmutableList.<Incoming>builder();
    if (node != root) {
      Preconditions.checkState(!node.getParents().isEmpty(), "Unexpected second ARG root");
      for (ARGState parent : node.getParents()) {
        visit(root, parent, incoming, active);
        var path = ImmutableList.copyOf(parent.getEdgesToChild(node));
        Preconditions.checkState(!path.isEmpty(), "Missing ARG transition");
        edges.add(new Incoming(parent, path));
      }
    }
    incoming.put(node, edges.build());
    active.remove(node);
  }

  public ImmutableList<ARGState> backwardOrder() {
    return order;
  }

  public ImmutableList<Incoming> incoming(ARGState node) {
    return incoming.get(node);
  }

  public Object graphId() {
    return incoming;
  }
}
