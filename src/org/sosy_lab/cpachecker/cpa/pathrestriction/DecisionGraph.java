// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pathrestriction;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/** An immutable acyclic automaton of decision-edge sequences, with shared suffixes. */
public final class DecisionGraph {
  private record Arc(String edge, Node next) {}

  private static final class Node {
    private final ImmutableList<Arc> arcs;
    private final int length;

    private Node(Collection<Arc> pArcs) {
      arcs = ImmutableList.copyOf(pArcs);
      length =
          arcs.stream().mapToInt(a -> a.next.length + (a.edge.isEmpty() ? 0 : 1)).max().orElse(0);
    }
  }

  private static final Node END = new Node(ImmutableList.of());
  public static final DecisionGraph EMPTY = new DecisionGraph(END);
  private final Node root;
  private @Nullable String encoding;

  private DecisionGraph(Node pRoot) {
    root = pRoot;
  }

  public int size() {
    return root.length;
  }

  public static DecisionGraph fromPaths(Collection<? extends List<String>> paths) {
    List<DecisionGraph> choices = new ArrayList<>();
    for (List<String> path : paths) {
      Node root = END;
      for (String edge : path.reversed()) {
        root = new Node(ImmutableList.of(new Arc(edge, root)));
      }
      choices.add(new DecisionGraph(root));
    }
    return choices.isEmpty() ? EMPTY : union(choices);
  }

  public DecisionGraph prepend(List<CFAEdge> edges) {
    Node result = root;
    for (CFAEdge edge : edges.reversed()) {
      if (SegmentedPaths.isDecisionEdge(edge)) {
        result = new Node(ImmutableList.of(new Arc(SegmentedPaths.edgeToString(edge), result)));
      }
    }
    return new DecisionGraph(result);
  }

  public static DecisionGraph union(Collection<DecisionGraph> choices) {
    Preconditions.checkArgument(!choices.isEmpty());
    Set<Node> roots = new LinkedHashSet<>();
    choices.forEach(graph -> roots.add(graph.root));
    return roots.size() == 1
        ? new DecisionGraph(roots.iterator().next())
        : new DecisionGraph(new Node(roots.stream().map(n -> new Arc("", n)).toList()));
  }

  public DecisionGraph then(DecisionGraph suffix) {
    return new DecisionGraph(copy(root, suffix.root, ImmutableMap.of(), new IdentityHashMap<>()));
  }

  public DecisionGraph transformEdges(Map<String, String> replacements) {
    return new DecisionGraph(copy(root, END, replacements, new IdentityHashMap<>()));
  }

  private static List<Node> postorder(Node root) {
    record Visit(Node node, java.util.Iterator<Arc> remaining) {}
    List<Node> result = new ArrayList<>();
    Set<Node> finished = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    finished.add(END);
    Deque<Visit> waiting = new ArrayDeque<>();
    waiting.push(new Visit(root, root.arcs.iterator()));
    while (!waiting.isEmpty()) {
      Visit visit = waiting.peek();
      if (finished.contains(visit.node())) {
        waiting.pop();
      } else if (visit.remaining().hasNext()) {
        Node next = visit.remaining().next().next();
        if (!finished.contains(next)) {
          waiting.push(new Visit(next, next.arcs.iterator()));
        }
      } else {
        waiting.pop();
        finished.add(visit.node());
        result.add(visit.node());
      }
    }
    return result;
  }

  private static Node copy(
      Node node, Node end, Map<String, String> replacements, IdentityHashMap<Node, Node> memo) {
    memo.put(END, end);
    for (Node current : postorder(node)) {
      memo.put(
          current,
          new Node(
              current.arcs.stream()
                  .map(
                      a ->
                          new Arc(
                              replacements.getOrDefault(a.edge, a.edge),
                              java.util.Objects.requireNonNull(memo.get(a.next))))
                  .toList()));
    }
    return java.util.Objects.requireNonNull(memo.get(node));
  }

  /** Positions after epsilon closure. A terminal position permits the remainder of the CFA. */
  public static final class Cursor {
    private final ImmutableSet<Node> positions;

    private Cursor(Collection<Node> nodes) {
      Set<Node> closed = new LinkedHashSet<>();
      Set<Node> visited = new LinkedHashSet<>();
      Deque<Node> waiting = new ArrayDeque<>(nodes);
      while (!waiting.isEmpty()) {
        Node node = waiting.removeFirst();
        if (!visited.add(node)) {
          continue;
        }
        if (node == END || node.arcs.stream().anyMatch(a -> !a.edge.isEmpty())) {
          closed.add(node);
        }
        node.arcs.stream().filter(a -> a.edge.isEmpty()).forEach(a -> waiting.add(a.next));
      }
      positions = ImmutableSet.copyOf(closed);
    }

    public boolean isEmpty() {
      return positions.isEmpty();
    }

    public Cursor advance(CFAEdge edge) {
      if (positions.contains(END) || !SegmentedPaths.isDecisionEdge(edge)) {
        return this;
      }
      String token = SegmentedPaths.edgeToString(edge);
      List<Node> next = new ArrayList<>();
      for (Node node : positions) {
        node.arcs.stream().filter(a -> a.edge.equals(token)).forEach(a -> next.add(a.next));
      }
      return new Cursor(next);
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof Cursor cursor && positions.equals(cursor.positions);
    }

    @Override
    public int hashCode() {
      return positions.hashCode();
    }
  }

  public Cursor cursor() {
    return new Cursor(ImmutableList.of(root));
  }

  public String serialize() {
    if (encoding == null) {
      IdentityHashMap<Node, Integer> ids = new IdentityHashMap<>();
      ids.put(END, 0);
      List<String> nodes = new ArrayList<>();
      nodes.add("D");
      encode(root, ids, nodes);
      encoding = String.join("/", nodes);
    }
    return encoding;
  }

  private static int encode(Node node, IdentityHashMap<Node, Integer> ids, List<String> nodes) {
    for (Node current : postorder(node)) {
      List<String> arcs = new ArrayList<>();
      for (Arc arc : current.arcs) {
        arcs.add(ids.get(arc.next) + ":" + arc.edge);
      }
      ids.put(current, nodes.size());
      nodes.add(String.join(",", arcs));
    }
    return ids.get(node);
  }

  public static DecisionGraph deserialize(String value) {
    String[] nodes = value.split("/", -1);
    Preconditions.checkArgument(nodes[0].equals("D"));
    List<Node> built = new ArrayList<>();
    built.add(END);
    for (int i = 1; i < nodes.length; i++) {
      List<Arc> arcs = new ArrayList<>();
      for (String arc : nodes[i].split(",", -1)) {
        int colon = arc.indexOf(':');
        Preconditions.checkArgument(colon > 0);
        int target = Integer.parseInt(arc.substring(0, colon));
        Preconditions.checkArgument(target >= 0 && target < i);
        arcs.add(new Arc(arc.substring(colon + 1), built.get(target)));
      }
      built.add(new Node(arcs));
    }
    return new DecisionGraph(built.getLast());
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof DecisionGraph graph && serialize().equals(graph.serialize());
  }

  @Override
  public int hashCode() {
    return serialize().hashCode();
  }
}
