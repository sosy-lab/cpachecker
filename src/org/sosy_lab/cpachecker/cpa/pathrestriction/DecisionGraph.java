// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pathrestriction;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

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

  /** The number of decisions on the longest path through this graph. */
  public int size() {
    return root.length;
  }

  static String edgeToString(CFAEdge edge) {
    return "N" + edge.getPredecessor().getNodeNumber() + "N" + edge.getSuccessor().getNodeNumber();
  }

  /** Whether another relevant edge leaves the predecessor of {@code edge}. */
  static boolean isDecisionEdge(CFAEdge edge) {
    return relevantEdge(edge)
        && edge.getPredecessor().getAllLeavingEdges().filter(DecisionGraph::relevantEdge).size()
            > 1;
  }

  private static boolean relevantEdge(CFAEdge e) {
    return !(e instanceof BlankEdge || e instanceof FunctionSummaryEdge);
  }

  /**
   * Returns a graph that first takes the decision edges among {@code edges} and then continues like
   * this graph. Edges where no other edge could have been taken are not stored.
   */
  public DecisionGraph prepend(List<CFAEdge> edges) {
    Node result = root;
    for (CFAEdge edge : edges.reversed()) {
      if (isDecisionEdge(edge)) {
        result = new Node(ImmutableList.of(new Arc(edgeToString(edge), result)));
      }
    }
    return new DecisionGraph(result);
  }

  /** Returns a graph that permits exactly the paths permitted by any of {@code choices}. */
  public static DecisionGraph union(Collection<DecisionGraph> choices) {
    Preconditions.checkArgument(!choices.isEmpty());
    if (choices.size() == 1) {
      return Iterables.getOnlyElement(choices);
    }
    Set<Node> roots = new LinkedHashSet<>();
    choices.forEach(graph -> roots.add(graph.root));
    return roots.size() == 1
        ? new DecisionGraph(roots.iterator().next())
        : new DecisionGraph(new Node(roots.stream().map(n -> new Arc("", n)).toList()));
  }

  /** Returns a graph that first follows this graph and then continues like {@code suffix}. */
  public DecisionGraph then(DecisionGraph suffix) {
    return new DecisionGraph(copy(root, suffix.root, ImmutableMap.of(), new IdentityHashMap<>()));
  }

  /**
   * Returns a graph where every edge that is a key in {@code oldToNew} is replaced by its
   * corresponding value. Edges that do not appear as a key in {@code oldToNew} are left unchanged.
   */
  public DecisionGraph transformEdges(Map<CFAEdge, CFAEdge> oldToNew) {
    Map<String, String> replacements = new HashMap<>(oldToNew.size());
    for (Map.Entry<CFAEdge, CFAEdge> entry : oldToNew.entrySet()) {
      replacements.put(edgeToString(entry.getKey()), edgeToString(entry.getValue()));
    }
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

  /**
   * The nodes of a {@link DecisionGraph} that the decisions taken so far lead to, after following
   * all epsilon arcs. A position at the end of the graph permits the remainder of the CFA. As the
   * state of {@link PathRestrictionCPA}, it restricts an analysis to the paths of the graph.
   */
  public static final class PathPosition implements AbstractState {

    /**
     * The initial state of a {@link PathRestrictionCPA} whose paths have not been set yet. It must
     * never be advanced: an analysis that explores from it would silently ignore the paths.
     */
    static final PathPosition UNINITIALIZED = new PathPosition();

    /** {@code null} only for {@link #UNINITIALIZED}. */
    private final @Nullable ImmutableSet<Node> positions;

    private PathPosition() {
      positions = null;
    }

    private PathPosition(Collection<Node> nodes) {
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

    private ImmutableSet<Node> positions() {
      Preconditions.checkState(
          positions != null,
          "Uninitialized paths, PathRestrictionCPA.getInitialState needs to be called after"
              + " PathRestrictionCPA.init");
      return positions;
    }

    /** Whether no path of the graph is compatible with the decisions taken so far. */
    public boolean isEmpty() {
      return positions().isEmpty();
    }

    /** The position after taking {@code edge}, which {@link #isEmpty} if the graph forbids it. */
    public PathPosition advance(CFAEdge edge) {
      ImmutableSet<Node> current = positions();
      if (current.contains(END) || !isDecisionEdge(edge)) {
        return this;
      }
      String token = edgeToString(edge);
      List<Node> next = new ArrayList<>();
      for (Node node : current) {
        node.arcs.stream().filter(a -> a.edge.equals(token)).forEach(a -> next.add(a.next));
      }
      return new PathPosition(next);
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof PathPosition position
          && java.util.Objects.equals(positions, position.positions);
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hashCode(positions);
    }

    @Override
    public String toString() {
      if (positions == null) {
        return "Uninitialized path restriction";
      }
      return positions.contains(END)
          ? "Reached end of path"
          : "At " + positions.size() + " position(s) of the path";
    }
  }

  /** The position before any decision has been taken. */
  public PathPosition start() {
    return new PathPosition(ImmutableList.of(root));
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
    List<String> nodes = Splitter.on('/').splitToList(value);
    Preconditions.checkArgument(nodes.getFirst().equals("D"));
    List<Node> built = new ArrayList<>();
    built.add(END);
    for (int i = 1; i < nodes.size(); i++) {
      List<Arc> arcs = new ArrayList<>();
      for (String arc : Splitter.on(',').split(nodes.get(i))) {
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

  @Override
  public String toString() {
    return serialize();
  }
}
