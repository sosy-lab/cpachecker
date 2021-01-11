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
import com.google.common.collect.Multimap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
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

/**
 *
 *
 * <table>
 *   <tr>
 *     <td></td><td>Procedure (P)</td><td>Statement (T)</td><td>Variable (V)</td>
 *   </tr>
 *   <tr>
 *     <td>NodeType.ENTRY</td><td>required</td><td>empty</td><td>empty</td>
 *   </tr>
 *   <tr>
 *     <td>NodeType.STATEMENT</td><td>optional (*)</td><td>required</td><td>empty</td>
 *   </tr>
 *   <tr>
 *     <td>NodeType.FORMAL_IN</td><td>required</td><td>empty</td><td>required (***)</td>
 *   </tr>
 *   <tr>
 *     <td>NodeType.FORMAL_OUT</td><td>required</td><td>empty</td><td>required (***)</td>
 *   </tr>
 *   <tr>
 *     <td>NodeType.ACTUAL_IN</td><td>required</td><td>required (**)</td><td>required (***)</td>
 *   </tr>
 *   <tr>
 *     <td>NodeType.ACTUAL_OUT</td><td>required</td><td>required (**)</td><td>required (***)</td>
 *   </tr>
 * </table>
 *
 * <p>(*) If the dependence graph only contains statements from a single procedure, the procedure
 * can be omitted. Otherwise, the procedure is required.
 *
 * <p>(**) All actual-in and actual-out statements of a specific call site must be equal to each
 * other.
 *
 * <p>(***) The variable of an actual-in/out node must be equal to the variable of the corresponding
 * formal-in/out node.
 *
 * <p>
 *
 * <table>
 *   <tr>
 *     <td></td><td>Predecessor</td><td>Successor</td><td>Procedure</td>
 *   </tr>
 *   <tr>
 *     <td>EdgeType.FLOW_DEPENDENCY</td><td>any node</td><td>any node</td><td>intra-procedural</td>
 *   </tr>
 *   <tr>
 *     <td>EdgeType.CONTROL_DEPENDENCY</td><td>any node</td><td>any node</td><td>inter/intra-procedural</td>
 *   </tr>
 *   <tr>
 *     <td>EdgeType.PARAMETER_EDGE</td><td>(A) actual-in or (B) formal-out</td><td>(A) formal-in or (B) actual-out</td><td>inter-procedural</td>
 *   </tr>
 *   <tr>
 *     <td>EdgeType.SUMMARY_EDGE</td><td>actual-in</td><td>actual-out</td><td>inter/intra-procedural</td>
 *   </tr>
 * </table>
 */
public class SystemDependenceGraph<P, T, V> {

  private static final int UNDEFINED_ID = -1;

  private final ImmutableList<Node<P, T, V>> nodes;
  private final ImmutableList<GraphNode<P, T, V>> graphNodes;

  private final ImmutableMultimap<T, Node<P, T, V>> nodesPerStatement;

  private final TypeCounter<NodeType> nodeTypeCounter;
  private final TypeCounter<EdgeType> edgeTypeCounter;

  private SystemDependenceGraph(
      ImmutableList<Node<P, T, V>> pNodes,
      ImmutableList<GraphNode<P, T, V>> pGraphNodes,
      ImmutableMultimap<T, Node<P, T, V>> pNodesPerStatement,
      TypeCounter<NodeType> pNodeTypeCounter,
      TypeCounter<EdgeType> pEdgeTypeCounter) {

    nodes = pNodes;
    graphNodes = pGraphNodes;
    nodesPerStatement = pNodesPerStatement;

    nodeTypeCounter = pNodeTypeCounter;
    edgeTypeCounter = pEdgeTypeCounter;
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

  /**
   * Returns a new system dependence graph that does not contain any nodes and edges.
   *
   * @param <P> the procedure type.
   * @param <T> the statement type.
   * @param <V> the variable type.
   * @return a new system dependence graph that does not contain any nodes and edges.
   */
  public static <P, T, V> SystemDependenceGraph<P, T, V> empty() {
    return new SystemDependenceGraph<>(
        ImmutableList.of(),
        ImmutableList.of(),
        ImmutableMultimap.of(),
        new TypeCounter<>(NodeType.values().length),
        new TypeCounter<>(EdgeType.values().length));
  }

  public static <P, T, V> Builder<P, T, V> builder() {
    return new Builder<>();
  }

  public int getNodeCount() {
    return nodes.size();
  }

  /**
   * Returns the number of nodes of the specified {@link NodeType} in this system dependence graph.
   *
   * @param pType the type to get the node count for.
   * @return the number of nodes of the specified type in this system dependence graph.
   * @throws NullPointerException if {@code pType} is {@code null}.
   */
  public int getNodeCount(NodeType pType) {

    Objects.requireNonNull(pType, "pType must not be null");

    return nodeTypeCounter.getCount(pType);
  }

  /**
   * Returns the number of edges of the specified {@link EdgeType} in this system dependence graph.
   *
   * @param pType the type to get the edge count for.
   * @return the number of edges of the specified type in this system dependence graph.
   * @throws NullPointerException if {@code pType} is {@code null}.
   */
  public int getEdgeCount(EdgeType pType) {

    Objects.requireNonNull(pType, "pType must not be null");

    return edgeTypeCounter.getCount(pType);
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
    ENTRY,
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

    private final TypeCounter<NodeType> nodeTypeCounter;
    private final TypeCounter<EdgeType> edgeTypeCounter;

    private Builder() {

      nodes = new ArrayList<>();
      graphNodes = new ArrayList<>();
      nodeMap = new HashMap<>();

      nodeTypeCounter = new TypeCounter<>(NodeType.values().length);
      edgeTypeCounter = new TypeCounter<>(EdgeType.values().length);
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

        nodeTypeCounter.increment(pType);
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

      edgeTypeCounter.increment(pType);
    }

    public int getNodeCount() {
      return nodes.size();
    }

    public ImmutableList<Node<P, T, V>> getNodes() {
      return ImmutableList.copyOf(nodes);
    }

    public void traverse(Collection<Node<P, T, V>> pStartNodes, ForwardsVisitor<P, T, V> pVisitor) {
      SystemDependenceGraph.traverse(graphNodes, pStartNodes, pVisitor, true);
    }

    public void traverse(
        Collection<Node<P, T, V>> pStartNodes, BackwardsVisitor<P, T, V> pVisitor) {
      SystemDependenceGraph.traverse(graphNodes, pStartNodes, pVisitor, false);
    }

    public int[] createIds(Function<Node<P, T, V>, Optional<?>> pFunction) {

      Map<Object, Integer> resultMap = new HashMap<>();
      int[] ids = new int[nodes.size()];

      for (int nodeId = 0; nodeId < ids.length; nodeId++) {

        Node<P, T, V> node = nodes.get(nodeId);
        Optional<?> result = pFunction.apply(node);

        if (result.isPresent()) {
          int id = resultMap.computeIfAbsent(result.orElseThrow(), key -> resultMap.size());
          ids[nodeId] = id;
        } else {
          ids[nodeId] = UNDEFINED_ID;
        }
      }

      return ids;
    }

    public void insertActualSummaryEdges(
        Node<P, T, V> pFormalInNode, Node<P, T, V> pFormalOutNode) {

      Objects.requireNonNull(pFormalInNode, "pFormalInNode must not be null");
      Objects.requireNonNull(pFormalInNode, "pFormalOutNode must not be null");

      GraphNode<P, T, V> formalOutGraphNode = graphNodes.get(pFormalOutNode.getId());
      assert formalOutGraphNode.getNode().equals(pFormalOutNode);

      for (GraphEdge<P, T, V> outEdge : formalOutGraphNode.getLeavingEdges()) {
        if (outEdge.getType() == EdgeType.PARAMETER_EDGE) {

          GraphNode<P, T, V> actualOutGraphNode = outEdge.getSuccessor();
          assert actualOutGraphNode.getNode().getType() == NodeType.ACTUAL_OUT;

          NodeMapKey<P, T, V> actualInNodeKey =
              new NodeMapKey<>(
                  NodeType.ACTUAL_IN,
                  actualOutGraphNode.getNode().getProcedure(),
                  actualOutGraphNode.getNode().getStatement(),
                  pFormalInNode.getVariable());
          GraphNode<P, T, V> actualInGraphNode = nodeMap.get(actualInNodeKey);

          if (actualInGraphNode != null) {
            insertEdge(
                actualInGraphNode, actualOutGraphNode, EdgeType.SUMMARY_EDGE, Optional.empty());
          }
        }
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
          ImmutableListMultimap.copyOf(nodesPerStatement),
          nodeTypeCounter.copy(),
          edgeTypeCounter.copy());
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
          procedure,
          statement,
          variable);
    }
  }

  /** Used to count objects of a specific type. Used for {@link NodeType} and {@link EdgeType}. */
  private static final class TypeCounter<T extends Enum<T>> {

    private final int[] counters;

    private TypeCounter(int[] pCounters) {
      counters = pCounters;
    }

    private TypeCounter(int pTypeCount) {
      this(new int[pTypeCount]);
    }

    private int getCount(T pType) {
      return counters[pType.ordinal()];
    }

    private void increment(T pType) {
      counters[pType.ordinal()]++;
    }

    private TypeCounter<T> copy() {
      return new TypeCounter<>(Arrays.copyOf(counters, counters.length));
    }
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

    public ForwardsVisitOnceVisitor(ForwardsVisitor<P, T, V> pDelegateVisitor, int pNodeCount) {
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

    public BackwardsVisitOnceVisitor(BackwardsVisitor<P, T, V> pDelegateVisitor, int pNodeCount) {
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
