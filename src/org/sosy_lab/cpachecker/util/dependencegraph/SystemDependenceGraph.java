// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.dependencegraph;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
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

public class SystemDependenceGraph<P, T, V> {

  private static final int UNDEFINED_ID = -1;

  private final ImmutableList<Node<P, T, V>> nodes;
  private final ImmutableList<GraphNode<P, T, V>> graphNodes;

  private final ImmutableMultimap<T, Node<P, T, V>> nodesPerStatement;

  private SystemDependenceGraph(
      ImmutableList<Node<P, T, V>> pNodes,
      ImmutableList<GraphNode<P, T, V>> pGraphNodes,
      ImmutableMultimap<T, Node<P, T, V>> pNodesPerStatement) {

    nodes = pNodes;
    graphNodes = pGraphNodes;
    nodesPerStatement = pNodesPerStatement;
  }

  private static <P, T, V> void illegalNode(Node<P, T, V> pNode) {
    throw new IllegalArgumentException("SystemDependenceGraph does not contain node: " + pNode);
  }

  private static <P, T, V> GraphNode<P, T, V> getGraphNode(
      List<GraphNode<P, T, V>> pGraphNodes, Node<P, T, V> pNode) {

    Objects.requireNonNull(pNode, "node must not be null");

    if (pNode.getId() >= pGraphNodes.size()) {
      illegalNode(pNode);
    }

    GraphNode<P, T, V> graphNode = pGraphNodes.get(pNode.getId());

    if (!graphNode.getNode().equals(pNode)) {
      illegalNode(pNode);
    }

    return graphNode;
  }

  private static <P, T, V> void traverse(
      List<GraphNode<P, T, V>> pGraphNodes,
      Collection<Node<P, T, V>> pStartNodes,
      Visitor<P, T, V> pVisitor,
      boolean pForwards) {

    Objects.requireNonNull(pStartNodes, "pStartNodes must not be null");
    Objects.requireNonNull(pVisitor, "pVisitor must not be null");

    Deque<GraphNode<P, T, V>> waitlist = new ArrayDeque<>();

    for (Node<P, T, V> node : pStartNodes) {
      waitlist.add(getGraphNode(pGraphNodes, node));
    }

    while (!waitlist.isEmpty()) {

      GraphNode<P, T, V> graphNode = waitlist.remove();
      VisitResult nodeVisitResult = pVisitor.visitNode(graphNode.getNode());

      if (nodeVisitResult == VisitResult.CONTINUE) {

        List<GraphEdge<P, T, V>> edges =
            pForwards ? graphNode.getLeavingEdges() : graphNode.getEnteringEdges();

        for (GraphEdge<P, T, V> edge : edges) {

          GraphNode<P, T, V> predecessor = edge.getPredecessor();
          GraphNode<P, T, V> successor = edge.getSuccessor();
          VisitResult edgeVisitResult =
              pVisitor.visitEdge(edge.getType(), predecessor.getNode(), successor.getNode());

          if (edgeVisitResult == VisitResult.CONTINUE) {

            GraphNode<P, T, V> next = pForwards ? successor : predecessor;
            waitlist.add(next);

          } else if (nodeVisitResult == VisitResult.TERMINATE) {
            return;
          }
        }

      } else if (nodeVisitResult == VisitResult.TERMINATE) {
        return;
      }
    }
  }

  public static <P, T, V> Builder<P, T, V> builder() {
    return new Builder<>();
  }

  public int getNodeCount() {
    return nodes.size();
  }

  public ImmutableCollection<Node<P, T, V>> getNodes() {
    return nodes;
  }

  public Node<P, T, V> getNodeById(int pId) {
    return nodes.get(pId);
  }

  public ImmutableCollection<Node<P, T, V>> getNodesForStatement(T pStatement) {

    Objects.requireNonNull(pStatement, "pStatement must not be null");

    return nodesPerStatement.get(pStatement);
  }

  public ImmutableSet<V> getDefs(Node<P, T, V> pNode) {
    return ImmutableSet.copyOf(getGraphNode(graphNodes, pNode).getDefs());
  }

  public ImmutableSet<V> getUses(Node<P, T, V> pNode) {
    return ImmutableSet.copyOf(getGraphNode(graphNodes, pNode).getUses());
  }

  public void traverse(Collection<Node<P, T, V>> pStartNodes, ForwardsVisitor<P, T, V> pVisitor) {
    traverse(graphNodes, pStartNodes, pVisitor, true);
  }

  public void traverse(Collection<Node<P, T, V>> pStartNodes, BackwardsVisitor<P, T, V> pVisitor) {
    traverse(graphNodes, pStartNodes, pVisitor, false);
  }

  public ForwardsVisitOnceVisitor<P, T, V> createVisitOnceVisitor(
      ForwardsVisitor<P, T, V> pDelegateVisitor) {
    return new ForwardsVisitOnceVisitor<>(pDelegateVisitor, getNodeCount());
  }

  public BackwardsVisitOnceVisitor<P, T, V> createVisitOnceVisitor(
      BackwardsVisitor<P, T, V> pDelegateVisitor) {
    return new BackwardsVisitOnceVisitor<>(pDelegateVisitor, getNodeCount());
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
    CONTROL_DEPENDENCY,
    PARAMETER_EDGE,
    SUMMARY_EDGE;
  }

  public static final class Node<P, T, V> {

    private final int id;
    private final NodeType type;
    private final Optional<P> procedure;
    private final Optional<T> statement;
    private final Optional<V> variable;

    private final int hash;

    private Node(
        int pId,
        NodeType pType,
        Optional<P> pProcedure,
        Optional<T> pStatement,
        Optional<V> pVariable) {

      id = pId;
      type = pType;
      procedure = pProcedure;
      statement = pStatement;
      variable = pVariable;

      hash = Objects.hash(id, type, procedure, statement, variable);
    }

    public int getId() {
      return id;
    }

    public NodeType getType() {
      return type;
    }

    public Optional<P> getProcedure() {
      return procedure;
    }

    public Optional<T> getStatement() {
      return statement;
    }

    public Optional<V> getVariable() {
      return variable;
    }

    @Override
    public int hashCode() {
      return hash;
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

      Node<?, ?, ?> other = (Node<?, ?, ?>) pObject;

      return id == other.id
          && hash == other.hash
          && type == other.type
          && Objects.equals(procedure, other.procedure)
          && Objects.equals(statement, other.statement)
          && Objects.equals(variable, other.variable);
    }

    @Override
    public String toString() {
      return String.format(
          Locale.ENGLISH,
          "%s[id=%d, type=%s, procedure=%s, statement=%s, variable=%s]",
          getClass().getName(),
          id,
          type,
          procedure,
          statement,
          variable);
    }
  }

  private static final class GraphNode<P, T, V> {

    private final Node<P, T, V> node;

    private List<GraphEdge<P, T, V>> enteringEdges;
    private List<GraphEdge<P, T, V>> leavingEdges;

    private Set<V> defs;
    private Set<V> uses;

    private GraphNode(Node<P, T, V> pNode) {

      node = pNode;

      enteringEdges = new ArrayList<>();
      leavingEdges = new ArrayList<>();

      defs = new HashSet<>();
      uses = new HashSet<>();
    }

    private Node<P, T, V> getNode() {
      return node;
    }

    private List<GraphEdge<P, T, V>> getEnteringEdges() {
      return enteringEdges;
    }

    private void addEnteringEdge(GraphEdge<P, T, V> pEdge) {
      enteringEdges.add(pEdge);
    }

    private List<GraphEdge<P, T, V>> getLeavingEdges() {
      return leavingEdges;
    }

    private void addLeavingEdge(GraphEdge<P, T, V> pEdge) {
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
      return node.hashCode();
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

      GraphNode<?, ?, ?> other = (GraphNode<?, ?, ?>) obj;
      return node.equals(other.node);
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

  private static final class GraphEdge<P, T, V> {

    private final EdgeType type;

    private final GraphNode<P, T, V> predecessor;
    private final GraphNode<P, T, V> successor;

    private GraphEdge(
        EdgeType pType, GraphNode<P, T, V> pPredecessor, GraphNode<P, T, V> pSuccessor) {

      type = pType;

      predecessor = pPredecessor;
      successor = pSuccessor;
    }

    private EdgeType getType() {
      return type;
    }

    private GraphNode<P, T, V> getPredecessor() {
      return predecessor;
    }

    private GraphNode<P, T, V> getSuccessor() {
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

      GraphEdge<?, ?, ?> other = (GraphEdge<?, ?, ?>) pObject;
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

  public static final class Builder<P, T, V> {

    private final List<Node<P, T, V>> nodes;
    private final List<GraphNode<P, T, V>> graphNodes;
    private final Map<NodeMapKey<P, T, V>, GraphNode<P, T, V>> nodeMap;

    private Builder() {

      nodes = new ArrayList<>();
      graphNodes = new ArrayList<>();

      nodeMap = new HashMap<>();
    }

    private GraphNode<P, T, V> graphNode(
        NodeType pType, Optional<P> pProcedure, Optional<T> pStatement, Optional<V> pVariable) {

      NodeMapKey<P, T, V> nodeKey = new NodeMapKey<>(pType, pProcedure, pStatement, pVariable);
      GraphNode<P, T, V> graphNode =
          nodeMap.computeIfAbsent(nodeKey, key -> new GraphNode<>(key.createNode(nodes.size())));
      Node<P, T, V> node = graphNode.getNode();

      if (node.getId() == nodes.size()) {
        nodes.add(node);
        graphNodes.add(graphNode);
      }

      return graphNode;
    }

    private void insertEdge(
        GraphNode<P, T, V> pPredecessor,
        GraphNode<P, T, V> pSuccessor,
        EdgeType pType,
        Optional<V> pCause) {

      GraphEdge<P, T, V> edge = new GraphEdge<>(pType, pPredecessor, pSuccessor);

      pPredecessor.addLeavingEdge(edge);
      pSuccessor.addEnteringEdge(edge);

      if (pCause.isPresent()) {
        V variable = pCause.orElseThrow();
        pPredecessor.addDef(variable);
        pSuccessor.addUse(variable);
      }
    }

    public EdgeChooser node(
        NodeType pType, Optional<P> pProcedure, Optional<T> pStatement, Optional<V> pVariable) {

      Objects.requireNonNull(pType, "pType must not be null");
      Objects.requireNonNull(pProcedure, "pProcedure must not be null");
      Objects.requireNonNull(pStatement, "pStatement must not be null");
      Objects.requireNonNull(pVariable, "pVariable must not be null");

      return new EdgeChooser(graphNode(pType, pProcedure, pStatement, pVariable));
    }

    public SystemDependenceGraph<P, T, V> build() {

      Multimap<T, Node<P, T, V>> nodesPerStatement = ArrayListMultimap.create();

      for (GraphNode<P, T, V> graphNode : graphNodes) {

        graphNode.finish();

        Node<P, T, V> node = graphNode.getNode();
        Optional<T> statement = node.getStatement();

        if (statement.isPresent()) {
          nodesPerStatement.put(node.getStatement().orElseThrow(), node);
        }
      }

      return new SystemDependenceGraph<>(
          ImmutableList.copyOf(nodes),
          ImmutableList.copyOf(graphNodes),
          ImmutableListMultimap.copyOf(nodesPerStatement));
    }

    public final class EdgeChooser {

      private final GraphNode<P, T, V> graphNode;

      private EdgeChooser(GraphNode<P, T, V> pGraphNode) {
        graphNode = pGraphNode;
      }

      public DependencyChooser depends(EdgeType pType, Optional<V> pCause) {

        Objects.requireNonNull(pType, "pType must not be null");
        Objects.requireNonNull(pCause, "pCause must not be null");

        return new DependencyChooser(graphNode, pType, pCause);
      }
    }

    public final class DependencyChooser {

      private final GraphNode<P, T, V> graphNode;
      private final EdgeType edgeType;
      private final Optional<V> cause;

      private DependencyChooser(
          GraphNode<P, T, V> pGraphNode, EdgeType pEdgeType, Optional<V> pCause) {
        graphNode = pGraphNode;
        edgeType = pEdgeType;
        cause = pCause;
      }

      public void on(
          NodeType pType, Optional<P> pProcedure, Optional<T> pStatement, Optional<V> pVariable) {

        Objects.requireNonNull(pType, "pType must not be null");
        Objects.requireNonNull(pProcedure, "pProcedure must not be null");
        Objects.requireNonNull(pStatement, "pStatement must not be null");
        Objects.requireNonNull(pVariable, "pVariable must not be null");

        insertEdge(graphNode(pType, pProcedure, pStatement, pVariable), graphNode, edgeType, cause);
      }
    }

    private int[] getContextIds() {

      Map<P, Integer> contexts = new HashMap<>();
      int[] contextIds = new int[nodes.size()];

      for (int nodeId = 0; nodeId < contextIds.length; nodeId++) {

        Node<P, T, V> node = nodes.get(nodeId);
        Optional<P> procedure = node.getProcedure();

        if (procedure.isPresent()) {
          int contextId = contexts.computeIfAbsent(procedure.orElseThrow(), key -> contexts.size());
          contextIds[nodeId] = contextId;
        } else {
          contextIds[nodeId] = UNDEFINED_ID;
        }
      }

      return contextIds;
    }

    public void insertSummaryEdges(Collection<T> pReachableFrom) {

      int[] contextIds = getContextIds();
      new SummaryEdgeBuilder(contextIds).run(pReachableFrom);
    }

    private final class SummaryEdgeBuilder implements BackwardsVisitor<P, T, V> {

      private final BitSet finishedFormalOutNodes;
      private final int[] contextIds;

      private int currentContextId;
      private boolean currentRecursive;
      private final Set<Node<P, T, V>> currentRelevantFormalInNodes;

      private SummaryEdgeBuilder(int[] pContextIds) {

        finishedFormalOutNodes = new BitSet(nodes.size());
        contextIds = pContextIds;

        currentRelevantFormalInNodes = new HashSet<>();
        currentContextId = -1;
        currentRecursive = false;
      }

      private void setCurrentFormalOutNode(Node<P, T, V> pFormalOutNode) {

        currentContextId = contextIds[pFormalOutNode.getId()];
        currentRecursive = false;
        currentRelevantFormalInNodes.clear();
      }

      @Override
      public VisitResult visitNode(Node<P, T, V> pNode) {

        if (pNode.getType() == NodeType.FORMAL_IN
            && contextIds[pNode.getId()] == currentContextId) {
          currentRelevantFormalInNodes.add(pNode);
        }

        return VisitResult.CONTINUE;
      }

      @Override
      public VisitResult visitEdge(
          EdgeType pType, Node<P, T, V> pPredecessor, Node<P, T, V> pSuccessor) {

        if (pPredecessor.getType() == NodeType.FORMAL_OUT) {

          if (contextIds[pPredecessor.getId()] == currentContextId && !currentRecursive) {
            currentRecursive = true;
            return VisitResult.TERMINATE;
          }

          if (finishedFormalOutNodes.get(pPredecessor.getId())) {
            return VisitResult.SKIP;
          }
        }

        int predecessorContextId = contextIds[pPredecessor.getId()];
        int successorContextId = contextIds[pSuccessor.getId()];

        if (predecessorContextId != successorContextId
            && successorContextId == currentContextId
            && !currentRecursive) {
          return VisitResult.SKIP;
        }

        return VisitResult.CONTINUE;
      }

      private List<Node<P, T, V>> getFormalOutNodes(Collection<T> pReachableFrom) {

        List<Node<P, T, V>> startNodes = new ArrayList<>();
        for (T statement : pReachableFrom) {

          // FIXME: missing procedure
          NodeMapKey<P, T, V> key =
              new NodeMapKey<>(
                  NodeType.STATEMENT, Optional.empty(), Optional.of(statement), Optional.empty());
          GraphNode<P, T, V> graphNode = nodeMap.get(key);

          if (graphNode != null) {
            startNodes.add(graphNode.getNode());
          }
        }

        assert !startNodes.isEmpty();

        List<Node<P, T, V>> formalOutNodes = new ArrayList<>();

        ForwardsVisitor<P, T, V> formalOutNodeCollector =
            new ForwardsVisitor<>() {

              @Override
              public VisitResult visitNode(Node<P, T, V> pNode) {

                if (pNode.getType() == NodeType.FORMAL_OUT) {
                  formalOutNodes.add(pNode);
                }

                return VisitResult.CONTINUE;
              }

              @Override
              public VisitResult visitEdge(
                  EdgeType pType, Node<P, T, V> pPredecessor, Node<P, T, V> pSuccessor) {
                return VisitResult.CONTINUE;
              }
            };

        ForwardsVisitOnceVisitor<P, T, V> visitOnceVisitor =
            new ForwardsVisitOnceVisitor<>(formalOutNodeCollector, nodes.size());

        traverse(graphNodes, startNodes, visitOnceVisitor, true);

        return Lists.reverse(formalOutNodes);
      }

      private void run(Collection<T> pReachableFrom) {

        BackwardsVisitOnceVisitor<P, T, V> visitor =
            new BackwardsVisitOnceVisitor<>(this, nodes.size());

        for (Node<P, T, V> node : getFormalOutNodes(pReachableFrom)) {

          setCurrentFormalOutNode(node);
          traverse(graphNodes, ImmutableList.of(node), visitor, false);
          visitor.reset();

          if (currentRecursive) {
            traverse(graphNodes, ImmutableList.of(node), visitor, false);
            visitor.reset();
          }

          finishedFormalOutNodes.set(node.getId());

          GraphNode<P, T, V> fornalOutGraphNode = graphNodes.get(node.getId());
          for (GraphEdge<P, T, V> outEdge : fornalOutGraphNode.getLeavingEdges()) {
            if (outEdge.getType() == EdgeType.PARAMETER_EDGE) {

              GraphNode<P, T, V> actualOutGraphNode = outEdge.getSuccessor();
              assert actualOutGraphNode.getNode().getType() == NodeType.ACTUAL_OUT;

              for (Node<P, T, V> formalInNode : currentRelevantFormalInNodes) {

                NodeMapKey<P, T, V> actualInNodeKey =
                    new NodeMapKey<>(
                        NodeType.ACTUAL_IN,
                        fornalOutGraphNode.getNode().getProcedure(),
                        actualOutGraphNode.getNode().getStatement(),
                        formalInNode.getVariable());
                GraphNode<P, T, V> actualInGraphNode = nodeMap.get(actualInNodeKey);

                if (actualInGraphNode != null) {
                  insertEdge(
                      actualInGraphNode,
                      actualOutGraphNode,
                      EdgeType.SUMMARY_EDGE,
                      Optional.empty());
                }
              }
            }
          }
        }
      }
    }
  }

  private static final class NodeMapKey<P, T, V> {

    private final NodeType type;
    private final Optional<P> procedure;
    private final Optional<T> statement;
    private final Optional<V> variable;

    private final int hash;

    private NodeMapKey(
        NodeType pType, Optional<P> pProcedure, Optional<T> pStatement, Optional<V> pVariable) {

      type = pType;
      procedure = pProcedure;
      statement = pStatement;
      variable = pVariable;

      hash = Objects.hash(type, procedure, statement, variable);
    }

    private Node<P, T, V> createNode(int pId) {
      return new Node<>(pId, type, procedure, statement, variable);
    }

    @Override
    public int hashCode() {
      return hash;
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

      NodeMapKey<?, ?, ?> other = (NodeMapKey<?, ?, ?>) pObject;

      return hash == other.hash
          && type == other.type
          && Objects.equals(procedure, other.procedure)
          && Objects.equals(statement, other.statement)
          && Objects.equals(variable, other.variable);
    }

    @Override
    public String toString() {
      return String.format(
          Locale.ENGLISH,
          "%s[type=%s, procedure=%s, statement=%s, variable=%s]",
          getClass().getName(),
          type,
          statement,
          variable);
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

  public interface Visitor<P, T, V> {

    public VisitResult visitNode(Node<P, T, V> pNode);

    public VisitResult visitEdge(
        EdgeType pType, Node<P, T, V> pPredecessor, Node<P, T, V> pSuccessor);
  }

  public interface ForwardsVisitor<P, T, V> extends Visitor<P, T, V> {}

  public interface BackwardsVisitor<P, T, V> extends Visitor<P, T, V> {}

  private abstract static class VisitOnceVisitor<P, T, V> implements Visitor<P, T, V> {

    private final boolean forwards;
    private final Visitor<P, T, V> delegateVisitor;

    private final byte[] visited;
    private byte visitedMarker;

    private VisitOnceVisitor(boolean pForwards, Visitor<P, T, V> pDelegateVisitor, int pNodeCount) {

      forwards = pForwards;
      delegateVisitor = pDelegateVisitor;

      visited = new byte[pNodeCount];
      visitedMarker = 1;
    }

    private void reset() {

      visitedMarker++;

      if (visitedMarker == 0) {

        Arrays.fill(visited, (byte) 0);
        visitedMarker = 1;
      }
    }

    private boolean isVisited(Node<P, T, V> pNode) {
      return visited[pNode.getId()] == visitedMarker;
    }

    @Override
    public VisitResult visitNode(Node<P, T, V> pNode) {

      if (!isVisited(pNode)) {

        visited[pNode.getId()] = visitedMarker;

        return delegateVisitor.visitNode(pNode);
      }

      return VisitResult.SKIP;
    }

    @Override
    public VisitResult visitEdge(
        EdgeType pType, Node<P, T, V> pPredecessor, Node<P, T, V> pSuccessor) {

      VisitResult visitResult = delegateVisitor.visitEdge(pType, pPredecessor, pSuccessor);

      if (visitResult == VisitResult.CONTINUE) {

        Node<P, T, V> nextNode = forwards ? pSuccessor : pPredecessor;

        if (isVisited(nextNode)) {
          return VisitResult.SKIP;
        }
      }

      return visitResult;
    }
  }

  public static final class ForwardsVisitOnceVisitor<P, T, V> extends VisitOnceVisitor<P, T, V>
      implements ForwardsVisitor<P, T, V> {

    private ForwardsVisitOnceVisitor(ForwardsVisitor<P, T, V> pDelegateVisitor, int pNodeCount) {
      super(true, pDelegateVisitor, pNodeCount);
    }

    public void reset() {
      super.reset();
    }

    @Override
    public VisitResult visitNode(Node<P, T, V> pNode) {
      return super.visitNode(pNode);
    }

    @Override
    public VisitResult visitEdge(
        EdgeType pType, Node<P, T, V> pPredecessor, Node<P, T, V> pSuccessor) {
      return super.visitEdge(pType, pPredecessor, pSuccessor);
    }
  }

  public static final class BackwardsVisitOnceVisitor<P, T, V> extends VisitOnceVisitor<P, T, V>
      implements BackwardsVisitor<P, T, V> {

    private BackwardsVisitOnceVisitor(BackwardsVisitor<P, T, V> pDelegateVisitor, int pNodeCount) {
      super(false, pDelegateVisitor, pNodeCount);
    }

    public void reset() {
      super.reset();
    }

    @Override
    public VisitResult visitNode(Node<P, T, V> pNode) {
      return super.visitNode(pNode);
    }

    @Override
    public VisitResult visitEdge(
        EdgeType pType, Node<P, T, V> pPredecessor, Node<P, T, V> pSuccessor) {
      return super.visitEdge(pType, pPredecessor, pSuccessor);
    }
  }
}
