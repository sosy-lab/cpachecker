// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.dependencegraph;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class SystemDependenceGraph<T, V> {

  private final ImmutableMap<Node<T, V>, GraphNode<T, V>> nodes;

  private SystemDependenceGraph(ImmutableMap<Node<T, V>, GraphNode<T, V>> pNodes) {
    nodes = pNodes;
  }

  public static <T, V> Builder<T, V> builder() {
    return new Builder<>();
  }

  public ImmutableSet<Node<T, V>> getNodes() {
    return nodes.keySet();
  }

  public Node<T, V> getNode(NodeType pType, T pStatement, Optional<V> pVariable) {

    Objects.requireNonNull(pType, "pType must not be null");
    Objects.requireNonNull(pStatement, "pStatement must not be null");
    Objects.requireNonNull(pVariable, "pVariable must not be null");

    return new Node<>(pType, pStatement, pVariable);
  }

  public ImmutableSet<V> getDefs(Node<T, V> pNode) {

    Objects.requireNonNull(pNode, "pNode must not be null");

    GraphNode<T, V> graphNode = nodes.get(pNode);

    return graphNode != null ? ImmutableSet.copyOf(graphNode.getDefs()) : ImmutableSet.of();
  }

  public ImmutableSet<V> getUses(Node<T, V> pNode) {

    Objects.requireNonNull(pNode, "pNode must not be null");

    GraphNode<T, V> graphNode = nodes.get(pNode);

    return graphNode != null ? ImmutableSet.copyOf(graphNode.getUses()) : ImmutableSet.of();
  }

  private void traverse(
      Direction pDirection,
      Collection<Node<T, V>> pStartNodes,
      Visitor<T, V> pVisitor,
      boolean pOnce) {

    Objects.requireNonNull(pDirection, "pDirection must not be null");
    Objects.requireNonNull(pStartNodes, "pStartNodes must not be null");
    Objects.requireNonNull(pVisitor, "pVisitor must not be null");

    Deque<GraphNode<T, V>> waitlist = new ArrayDeque<>();
    Set<GraphNode<T, V>> waitlisted = new HashSet<>();

    for (Node<T, V> node : pStartNodes) {

      GraphNode<T, V> graphNode = nodes.get(node);
      if (graphNode == null) {
        throw new IllegalArgumentException("start node is not part of this graph: " + node);
      }

      if (!pOnce || waitlisted.add(graphNode)) {
        waitlist.add(graphNode);
      }
    }

    while (!waitlist.isEmpty()) {

      GraphNode<T, V> graphNode = waitlist.remove();
      VisitResult nodeVisitResult = pVisitor.visitNode(graphNode.getNode());

      if (nodeVisitResult == VisitResult.CONTINUE) {

        List<GraphEdge<T, V>> edges =
            pDirection == Direction.FORWARDS
                ? graphNode.getLeavingEdges()
                : graphNode.getEnteringEdges();

        for (GraphEdge<T, V> edge : edges) {

          VisitResult edgeVisitResult =
              pVisitor.visitEdge(
                  edge.getType(), edge.getPredecessor().getNode(), edge.getSuccessor().getNode());

          if (edgeVisitResult == VisitResult.CONTINUE) {

            GraphNode<T, V> next =
                pDirection == Direction.FORWARDS ? edge.getSuccessor() : edge.getPredecessor();

            if (!pOnce || waitlisted.add(next)) {
              waitlist.add(next);
            }

          } else if (nodeVisitResult == VisitResult.TERMINATE) {
            return;
          }
        }

      } else if (nodeVisitResult == VisitResult.TERMINATE) {
        return;
      }
    }
  }

  public void traverse(
      Direction pDirection, Collection<Node<T, V>> pStartNodes, Visitor<T, V> pVisitor) {
    traverse(pDirection, pStartNodes, pVisitor, false);
  }

  public void traverseOnce(
      Direction pDirection, Collection<Node<T, V>> pStartNodes, Visitor<T, V> pVisitor) {
    traverse(pDirection, pStartNodes, pVisitor, true);
  }

  public enum NodeType {
    STATEMENT,
    FORMAL_IN,
    FORMAL_OUT,
    ACTUAL_IN,
    ACTUAL_OUT;
  }

  public enum EdgeType {
    FLOW_DEPENDENCY,
    CONTROL_DEPENDENCY;
  }

  public static final class Node<T, V> {

    private final NodeType type;
    private final T statement;
    private final Optional<V> variable;

    private Node(NodeType pType, T pStatement, Optional<V> pVariable) {

      type = pType;
      statement = pStatement;
      variable = pVariable;
    }

    public NodeType getType() {
      return type;
    }

    public T getStatement() {
      return statement;
    }

    public Optional<V> getVariable() {
      return variable;
    }

    @Override
    public int hashCode() {
      return Objects.hash(type, statement, variable);
    }

    @Override
    public boolean equals(Object pObject) {

      if (this == pObject) {
        return true;
      }

      if (pObject == null) {
        return false;
      }

      if (getClass() != pObject.getClass()) {
        return false;
      }

      Node<?, ?> other = (Node<?, ?>) pObject;

      return type == other.type
          && Objects.equals(statement, other.statement)
          && Objects.equals(variable, other.variable);
    }

    @Override
    public String toString() {
      return String.format(
          Locale.ENGLISH,
          "%s[type=%s, statement=%s, variable=%s]",
          getClass().getName(),
          type,
          statement,
          variable);
    }
  }

  private static final class GraphNode<T, V> {

    private final Node<T, V> node;

    private List<GraphEdge<T, V>> enteringEdges;
    private List<GraphEdge<T, V>> leavingEdges;

    private Set<V> defs;
    private Set<V> uses;

    private GraphNode(Node<T, V> pNode) {

      node = pNode;

      enteringEdges = new ArrayList<>();
      leavingEdges = new ArrayList<>();

      defs = new HashSet<>();
      uses = new HashSet<>();
    }

    private Node<T, V> getNode() {
      return node;
    }

    private List<GraphEdge<T, V>> getEnteringEdges() {
      return enteringEdges;
    }

    private void addEnteringEdge(GraphEdge<T, V> pEdge) {
      enteringEdges.add(pEdge);
    }

    private List<GraphEdge<T, V>> getLeavingEdges() {
      return leavingEdges;
    }

    private void addLeavingEdge(GraphEdge<T, V> pEdge) {
      leavingEdges.add(pEdge);
    }

    private Set<V> getDefs() {
      return defs;
    }

    private void addDef(V pVariable) {
      defs.add(pVariable);
    }

    private Set<V> getUses() {
      return uses;
    }

    private void addUse(V pVariable) {
      uses.add(pVariable);
    }

    private void finish() {

      enteringEdges = ImmutableList.copyOf(enteringEdges);
      leavingEdges = ImmutableList.copyOf(leavingEdges);

      defs = ImmutableSet.copyOf(defs);
      uses = ImmutableSet.copyOf(uses);
    }

    @Override
    public int hashCode() {
      return Objects.hash(node);
    }

    @Override
    public boolean equals(Object obj) {

      if (this == obj) {
        return true;
      }

      if (obj == null) {
        return false;
      }

      if (getClass() != obj.getClass()) {
        return false;
      }

      GraphNode<?, ?> other = (GraphNode<?, ?>) obj;
      return Objects.equals(node, other.node);
    }

    @Override
    public String toString() {
      return String.format(
          Locale.ENGLISH,
          "%s[node=%s, enteringEdges=%s, leavingEdges=%s, defs=%s, uses=%s]",
          getClass().getName(),
          node,
          enteringEdges,
          leavingEdges,
          defs,
          uses);
    }
  }

  private static final class GraphEdge<T, V> {

    private final EdgeType type;

    private final GraphNode<T, V> predecessor;
    private final GraphNode<T, V> successor;

    private GraphEdge(EdgeType pType, GraphNode<T, V> pPredecessor, GraphNode<T, V> pSuccessor) {

      type = pType;

      predecessor = pPredecessor;
      successor = pSuccessor;
    }

    private EdgeType getType() {
      return type;
    }

    private GraphNode<T, V> getPredecessor() {
      return predecessor;
    }

    private GraphNode<T, V> getSuccessor() {
      return successor;
    }

    @Override
    public int hashCode() {
      return Objects.hash(type, predecessor, successor);
    }

    @Override
    public boolean equals(Object pObject) {

      if (this == pObject) {
        return true;
      }

      if (pObject == null) {
        return false;
      }

      if (getClass() != pObject.getClass()) {
        return false;
      }

      GraphEdge<?, ?> other = (GraphEdge<?, ?>) pObject;
      return type == other.type
          && Objects.equals(predecessor, other.predecessor)
          && Objects.equals(successor, other.successor);
    }

    @Override
    public String toString() {
      return String.format(
          Locale.ENGLISH,
          "%s[type=%s, predecessor=%s, successor=%s]",
          getClass().getName(),
          type,
          predecessor,
          successor);
    }
  }

  public static final class Builder<T, V> {

    private final Map<Node<T, V>, GraphNode<T, V>> nodes;

    private Builder() {
      nodes = new HashMap<>();
    }

    private GraphNode<T, V> graphNode(Node<T, V> pNode) {
      return nodes.computeIfAbsent(pNode, key -> new GraphNode<>(pNode));
    }

    private void insertEdge(
        Node<T, V> pPredecessor, Node<T, V> pSuccessor, EdgeType pType, Optional<V> pCause) {

      GraphNode<T, V> predecessorGraphNode = graphNode(pPredecessor);
      GraphNode<T, V> successorGraphNode = graphNode(pSuccessor);
      GraphEdge<T, V> edge = new GraphEdge<>(pType, predecessorGraphNode, successorGraphNode);

      predecessorGraphNode.addLeavingEdge(edge);
      successorGraphNode.addEnteringEdge(edge);

      if (pCause.isPresent()) {
        V variable = pCause.orElseThrow();
        predecessorGraphNode.addDef(variable);
        successorGraphNode.addUse(variable);
      }
    }

    public EdgeChooser node(NodeType pType, T pStatement, Optional<V> pVariable) {

      Objects.requireNonNull(pType, "pType must not be null");
      Objects.requireNonNull(pStatement, "pStatement must not be null");
      Objects.requireNonNull(pVariable, "pVariable must not be null");

      return new EdgeChooser(new Node<>(pType, pStatement, pVariable));
    }

    public SystemDependenceGraph<T, V> build() {

      for (GraphNode<T, V> graphNode : nodes.values()) {
        graphNode.finish();
      }

      return new SystemDependenceGraph<>(ImmutableMap.copyOf(nodes));
    }

    public final class EdgeChooser {

      private final Node<T, V> node;

      private EdgeChooser(Node<T, V> pNode) {
        node = pNode;
      }

      public DependencyChooser depends(EdgeType pType, Optional<V> pCause) {

        Objects.requireNonNull(pType, "pType must not be null");
        Objects.requireNonNull(pCause, "pCause must not be null");

        return new DependencyChooser(node, pType, pCause);
      }
    }

    public final class DependencyChooser {

      private final Node<T, V> node;
      private final EdgeType edgeType;
      private final Optional<V> cause;

      private DependencyChooser(Node<T, V> pNode, EdgeType pEdgeType, Optional<V> pCause) {
        node = pNode;
        edgeType = pEdgeType;
        cause = pCause;
      }

      public void on(NodeType pType, T pStatement, Optional<V> pVariable) {

        Objects.requireNonNull(pType, "pType must not be null");
        Objects.requireNonNull(pStatement, "pStatement must not be null");
        Objects.requireNonNull(pVariable, "pVariable must not be null");

        insertEdge(new Node<>(pType, pStatement, pVariable), node, edgeType, cause);
      }
    }
  }

  public enum Direction {
    FORWARDS,
    BACKWARDS;
  }

  public enum VisitResult {
    CONTINUE,
    TERMINATE,
    SKIP;
  }

  public interface Visitor<T, V> {

    public VisitResult visitNode(Node<T, V> pNode);

    public VisitResult visitEdge(EdgeType pType, Node<T, V> pPredecessor, Node<T, V> pSuccessor);
  }
}
