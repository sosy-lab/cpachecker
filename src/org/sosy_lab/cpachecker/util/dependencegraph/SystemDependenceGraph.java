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
import java.util.function.Function;

public class SystemDependenceGraph<T, V> {

  private final ImmutableList<Node<T, V>> nodes;
  private final ImmutableList<GraphNode<T, V>> graphNodes;

  private final ImmutableMultimap<T, Node<T, V>> nodesPerStatement;

  private SystemDependenceGraph(
      ImmutableList<Node<T, V>> pNodes,
      ImmutableList<GraphNode<T, V>> pGraphNodes,
      ImmutableMultimap<T, Node<T, V>> pNodesPerStatement) {

    nodes = pNodes;
    graphNodes = pGraphNodes;
    nodesPerStatement = pNodesPerStatement;
  }

  private static <T, V> void illegalNode(Node<T, V> pNode) {
    throw new IllegalArgumentException("SystemDependenceGraph does not contain node: " + pNode);
  }

  private static <T, V> GraphNode<T, V> getGraphNode(
      List<GraphNode<T, V>> pGraphNodes, Node<T, V> pNode) {

    Objects.requireNonNull(pNode, "node must not be null");

    if (pNode.getId() >= pGraphNodes.size()) {
      illegalNode(pNode);
    }

    GraphNode<T, V> graphNode = pGraphNodes.get(pNode.getId());

    if (!graphNode.getNode().equals(pNode)) {
      illegalNode(pNode);
    }

    return graphNode;
  }

  private static <T, V> void traverse(
      List<GraphNode<T, V>> pGraphNodes,
      Collection<Node<T, V>> pStartNodes,
      Visitor<T, V> pVisitor,
      boolean pForwards) {

    Objects.requireNonNull(pStartNodes, "pStartNodes must not be null");
    Objects.requireNonNull(pVisitor, "pVisitor must not be null");

    Deque<GraphNode<T, V>> waitlist = new ArrayDeque<>();

    for (Node<T, V> node : pStartNodes) {
      waitlist.add(getGraphNode(pGraphNodes, node));
    }

    while (!waitlist.isEmpty()) {

      GraphNode<T, V> graphNode = waitlist.remove();
      VisitResult nodeVisitResult = pVisitor.visitNode(graphNode.getNode());

      if (nodeVisitResult == VisitResult.CONTINUE) {

        List<GraphEdge<T, V>> edges =
            pForwards ? graphNode.getLeavingEdges() : graphNode.getEnteringEdges();

        for (GraphEdge<T, V> edge : edges) {

          GraphNode<T, V> predecessor = edge.getPredecessor();
          GraphNode<T, V> successor = edge.getSuccessor();
          VisitResult edgeVisitResult =
              pVisitor.visitEdge(edge.getType(), predecessor.getNode(), successor.getNode());

          if (edgeVisitResult == VisitResult.CONTINUE) {

            GraphNode<T, V> next = pForwards ? successor : predecessor;
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

  public static <T, V> Builder<T, V> builder() {
    return new Builder<>();
  }

  public int getNodeCount() {
    return nodes.size();
  }

  public ImmutableCollection<Node<T, V>> getNodes() {
    return nodes;
  }

  public Node<T, V> getNodeById(int pId) {
    return nodes.get(pId);
  }

  public ImmutableCollection<Node<T, V>> getNodesForStatement(T pStatement) {

    Objects.requireNonNull(pStatement, "pStatement must not be null");

    return nodesPerStatement.get(pStatement);
  }

  public ImmutableSet<V> getDefs(Node<T, V> pNode) {
    return ImmutableSet.copyOf(getGraphNode(graphNodes, pNode).getDefs());
  }

  public ImmutableSet<V> getUses(Node<T, V> pNode) {
    return ImmutableSet.copyOf(getGraphNode(graphNodes, pNode).getUses());
  }

  public void traverse(Collection<Node<T, V>> pStartNodes, ForwardsVisitor<T, V> pVisitor) {
    traverse(graphNodes, pStartNodes, pVisitor, true);
  }

  public void traverse(Collection<Node<T, V>> pStartNodes, BackwardsVisitor<T, V> pVisitor) {
    traverse(graphNodes, pStartNodes, pVisitor, false);
  }

  public ForwardsVisitOnceVisitor<T, V> createVisitOnceVisitor(
      ForwardsVisitor<T, V> pDelegateVisitor) {
    return new ForwardsVisitOnceVisitor<>(pDelegateVisitor, getNodeCount());
  }

  public BackwardsVisitOnceVisitor<T, V> createVisitOnceVisitor(
      BackwardsVisitor<T, V> pDelegateVisitor) {
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

  public static final class Node<T, V> {

    private final int id;
    private final NodeType type;
    private final T statement;
    private final Optional<V> variable;

    private final int hash;

    private Node(int pId, NodeType pType, T pStatement, Optional<V> pVariable) {

      id = pId;
      type = pType;
      statement = pStatement;
      variable = pVariable;

      hash = Objects.hash(id, type, statement, variable);
    }

    public int getId() {
      return id;
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

      Node<?, ?> other = (Node<?, ?>) pObject;

      return id == other.id
          && hash == other.hash
          && type == other.type
          && Objects.equals(statement, other.statement)
          && Objects.equals(variable, other.variable);
    }

    @Override
    public String toString() {
      return String.format(
          Locale.ENGLISH,
          "%s[id=%d, type=%s, statement=%s, variable=%s]",
          getClass().getName(),
          id,
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

      GraphNode<?, ?> other = (GraphNode<?, ?>) obj;
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

    private final List<Node<T, V>> nodes;
    private final List<GraphNode<T, V>> graphNodes;
    private final Map<NodeMapKey<T, V>, GraphNode<T, V>> nodeMap;

    private Builder() {

      nodes = new ArrayList<>();
      graphNodes = new ArrayList<>();

      nodeMap = new HashMap<>();
    }

    private GraphNode<T, V> graphNode(NodeType pType, T pStatement, Optional<V> pVariable) {

      NodeMapKey<T, V> nodeKey = new NodeMapKey<>(pType, pStatement, pVariable);
      GraphNode<T, V> graphNode =
          nodeMap.computeIfAbsent(nodeKey, key -> new GraphNode<>(key.createNode(nodes.size())));
      Node<T, V> node = graphNode.getNode();

      if (node.getId() == nodes.size()) {
        nodes.add(node);
        graphNodes.add(graphNode);
      }

      return graphNode;
    }

    private void insertEdge(
        GraphNode<T, V> pPredecessor,
        GraphNode<T, V> pSuccessor,
        EdgeType pType,
        Optional<V> pCause) {

      GraphEdge<T, V> edge = new GraphEdge<>(pType, pPredecessor, pSuccessor);

      pPredecessor.addLeavingEdge(edge);
      pSuccessor.addEnteringEdge(edge);

      if (pCause.isPresent()) {
        V variable = pCause.orElseThrow();
        pPredecessor.addDef(variable);
        pSuccessor.addUse(variable);
      }
    }

    public EdgeChooser node(NodeType pType, T pStatement, Optional<V> pVariable) {

      Objects.requireNonNull(pType, "pType must not be null");
      Objects.requireNonNull(pStatement, "pStatement must not be null");
      Objects.requireNonNull(pVariable, "pVariable must not be null");

      return new EdgeChooser(graphNode(pType, pStatement, pVariable));
    }

    public SystemDependenceGraph<T, V> build() {

      Multimap<T, Node<T, V>> nodesPerStatement = ArrayListMultimap.create();

      for (GraphNode<T, V> graphNode : graphNodes) {

        graphNode.finish();

        Node<T, V> node = graphNode.getNode();
        nodesPerStatement.put(node.getStatement(), node);
      }

      return new SystemDependenceGraph<>(
          ImmutableList.copyOf(nodes),
          ImmutableList.copyOf(graphNodes),
          ImmutableListMultimap.copyOf(nodesPerStatement));
    }

    public final class EdgeChooser {

      private final GraphNode<T, V> graphNode;

      private EdgeChooser(GraphNode<T, V> pGraphNode) {
        graphNode = pGraphNode;
      }

      public DependencyChooser depends(EdgeType pType, Optional<V> pCause) {

        Objects.requireNonNull(pType, "pType must not be null");
        Objects.requireNonNull(pCause, "pCause must not be null");

        return new DependencyChooser(graphNode, pType, pCause);
      }
    }

    public final class DependencyChooser {

      private final GraphNode<T, V> graphNode;
      private final EdgeType edgeType;
      private final Optional<V> cause;

      private DependencyChooser(
          GraphNode<T, V> pGraphNode, EdgeType pEdgeType, Optional<V> pCause) {
        graphNode = pGraphNode;
        edgeType = pEdgeType;
        cause = pCause;
      }

      public void on(NodeType pType, T pStatement, Optional<V> pVariable) {

        Objects.requireNonNull(pType, "pType must not be null");
        Objects.requireNonNull(pStatement, "pStatement must not be null");
        Objects.requireNonNull(pVariable, "pVariable must not be null");

        insertEdge(graphNode(pType, pStatement, pVariable), graphNode, edgeType, cause);
      }
    }

    private <C> int[] getContextIds(Function<T, C> pContextFunction) {

      Map<C, Integer> contexts = new HashMap<>();
      int[] contextIds = new int[nodes.size()];

      for (int nodeId = 0; nodeId < contextIds.length; nodeId++) {

        Node<T, V> node = nodes.get(nodeId);
        C context = pContextFunction.apply(node.getStatement());

        int contextId = contexts.computeIfAbsent(context, key -> contexts.size());
        contextIds[nodeId] = contextId;
      }

      return contextIds;
    }

    public <C> void insertSummaryEdges(
        Function<T, C> pContextFunction, Collection<T> pReachableFrom) {

      int[] contextIds = getContextIds(pContextFunction);
      new SummaryEdgeBuilder(contextIds).run(pReachableFrom);
    }

    private final class SummaryEdgeBuilder implements BackwardsVisitor<T, V> {

      private final BitSet finishedFormalOutNodes;
      private final int[] contextIds;

      private int currentContextId;
      private boolean currentRecursive;
      private final Set<Node<T, V>> currentRelevantFormalInNodes;

      private SummaryEdgeBuilder(int[] pContextIds) {

        finishedFormalOutNodes = new BitSet(nodes.size());
        contextIds = pContextIds;

        currentRelevantFormalInNodes = new HashSet<>();
        currentContextId = -1;
        currentRecursive = false;
      }

      private void setCurrentFormalOutNode(Node<T, V> pFormalOutNode) {

        currentContextId = contextIds[pFormalOutNode.getId()];
        currentRecursive = false;
        currentRelevantFormalInNodes.clear();
      }

      @Override
      public VisitResult visitNode(Node<T, V> pNode) {

        if (pNode.getType() == NodeType.FORMAL_IN
            && contextIds[pNode.getId()] == currentContextId) {
          currentRelevantFormalInNodes.add(pNode);
        }

        return VisitResult.CONTINUE;
      }

      @Override
      public VisitResult visitEdge(EdgeType pType, Node<T, V> pPredecessor, Node<T, V> pSuccessor) {

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

      private List<Node<T, V>> getFormalOutNodes(Collection<T> pReachableFrom) {

        List<Node<T, V>> startNodes = new ArrayList<>();
        for (T statement : pReachableFrom) {

          NodeMapKey<T, V> key = new NodeMapKey<>(NodeType.STATEMENT, statement, Optional.empty());
          GraphNode<T, V> graphNode = nodeMap.get(key);

          if (graphNode != null) {
            startNodes.add(graphNode.getNode());
          }
        }

        assert !startNodes.isEmpty();

        List<Node<T, V>> formalOutNodes = new ArrayList<>();

        BackwardsVisitor<T, V> formalOutNodeCollector =
            new BackwardsVisitor<>() {

              @Override
              public VisitResult visitNode(Node<T, V> pNode) {

                if (pNode.getType() == NodeType.FORMAL_OUT) {
                  formalOutNodes.add(pNode);
                }

                return VisitResult.CONTINUE;
              }

              @Override
              public VisitResult visitEdge(
                  EdgeType pType, Node<T, V> pPredecessor, Node<T, V> pSuccessor) {
                return VisitResult.CONTINUE;
              }
            };

        BackwardsVisitOnceVisitor<T, V> visitOnceVisitor =
            new BackwardsVisitOnceVisitor<>(formalOutNodeCollector, nodes.size());

        traverse(graphNodes, startNodes, visitOnceVisitor, false);

        return Lists.reverse(formalOutNodes);
      }

      private void run(Collection<T> pReachableFrom) {

        BackwardsVisitOnceVisitor<T, V> visitor =
            new BackwardsVisitOnceVisitor<>(this, nodes.size());

       for (Node<T, V> node : getFormalOutNodes(pReachableFrom)) {

          setCurrentFormalOutNode(node);
          traverse(graphNodes, ImmutableList.of(node), visitor, false);
          visitor.reset();

          if (currentRecursive) {
            traverse(graphNodes, ImmutableList.of(node), visitor, false);
            visitor.reset();
          }

          finishedFormalOutNodes.set(node.getId());

          GraphNode<T, V> fornalOutGraphNode = graphNodes.get(node.getId());
          for (GraphEdge<T, V> outEdge : fornalOutGraphNode.getLeavingEdges()) {
            if (outEdge.getType() == EdgeType.PARAMETER_EDGE) {

              GraphNode<T, V> actualOutGraphNode = outEdge.getSuccessor();
              assert actualOutGraphNode.getNode().getType() == NodeType.ACTUAL_OUT;

              for (Node<T, V> formalInNode : currentRelevantFormalInNodes) {

                NodeMapKey<T, V> actualInNodeKey =
                    new NodeMapKey<>(
                        NodeType.ACTUAL_IN,
                        actualOutGraphNode.getNode().getStatement(),
                        formalInNode.getVariable());
                GraphNode<T, V> actualInGraphNode = nodeMap.get(actualInNodeKey);

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

  private static final class NodeMapKey<T, V> {

    private final NodeType type;
    private final T statement;
    private final Optional<V> variable;

    private final int hash;

    private NodeMapKey(NodeType pType, T pStatement, Optional<V> pVariable) {

      type = pType;
      statement = pStatement;
      variable = pVariable;

      hash = Objects.hash(type, statement, variable);
    }

    private Node<T, V> createNode(int pId) {
      return new Node<>(pId, type, statement, variable);
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

      NodeMapKey<?, ?> other = (NodeMapKey<?, ?>) pObject;

      return hash == other.hash
          && type == other.type
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

  public interface ForwardsVisitor<T, V> extends Visitor<T, V> {}

  public interface BackwardsVisitor<T, V> extends Visitor<T, V> {}

  private abstract static class VisitOnceVisitor<T, V> implements Visitor<T, V> {

    private final boolean forwards;
    private final Visitor<T, V> delegateVisitor;

    private final byte[] visited;
    private byte visitedMarker;

    private VisitOnceVisitor(boolean pForwards, Visitor<T, V> pDelegateVisitor, int pNodeCount) {

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

    private boolean isVisited(Node<T, V> pNode) {
      return visited[pNode.getId()] == visitedMarker;
    }

    @Override
    public VisitResult visitNode(Node<T, V> pNode) {

      if (!isVisited(pNode)) {

        visited[pNode.getId()] = visitedMarker;

        return delegateVisitor.visitNode(pNode);
      }

      return VisitResult.SKIP;
    }

    @Override
    public VisitResult visitEdge(EdgeType pType, Node<T, V> pPredecessor, Node<T, V> pSuccessor) {

      VisitResult visitResult = delegateVisitor.visitEdge(pType, pPredecessor, pSuccessor);

      if (visitResult == VisitResult.CONTINUE) {

        Node<T, V> nextNode = forwards ? pSuccessor : pPredecessor;

        if (isVisited(nextNode)) {
          return VisitResult.SKIP;
        }
      }

      return visitResult;
    }
  }

  public static final class ForwardsVisitOnceVisitor<T, V> extends VisitOnceVisitor<T, V>
      implements ForwardsVisitor<T, V> {

    private ForwardsVisitOnceVisitor(ForwardsVisitor<T, V> pDelegateVisitor, int pNodeCount) {
      super(true, pDelegateVisitor, pNodeCount);
    }

    public void reset() {
      super.reset();
    }

    @Override
    public VisitResult visitNode(Node<T, V> pNode) {
      return super.visitNode(pNode);
    }

    @Override
    public VisitResult visitEdge(EdgeType pType, Node<T, V> pPredecessor, Node<T, V> pSuccessor) {
      return super.visitEdge(pType, pPredecessor, pSuccessor);
    }
  }

  public static final class BackwardsVisitOnceVisitor<T, V> extends VisitOnceVisitor<T, V>
      implements BackwardsVisitor<T, V> {

    private BackwardsVisitOnceVisitor(BackwardsVisitor<T, V> pDelegateVisitor, int pNodeCount) {
      super(false, pDelegateVisitor, pNodeCount);
    }

    public void reset() {
      super.reset();
    }

    @Override
    public VisitResult visitNode(Node<T, V> pNode) {
      return super.visitNode(pNode);
    }

    @Override
    public VisitResult visitEdge(EdgeType pType, Node<T, V> pPredecessor, Node<T, V> pSuccessor) {
      return super.visitEdge(pType, pPredecessor, pSuccessor);
    }
  }
}
