// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.dependencegraph;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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
 * Represents a system dependence graph (SDG).
 *
 * <p>New SDG instances can be created using a {@link Builder}. New builder are created by calling
 * {@link #builder()}. Types for procedures, statements, and variables should be specified using the
 * respective type parameters.
 *
 * <p>SDGs are traversed by calling the methods {@link #traverse(Collection, ForwardsVisitor)} or
 * {@link #traverse(Collection, BackwardsVisitor)}.
 *
 * @param <P> the procedure type of the SDG
 * @param <T> the statement type of the SDG
 * @param <V> the variable type of the SDG
 */
public final class SystemDependenceGraph<P, T, V> {

  // list of nodes where the node's index is equal to its id
  private final ImmutableList<Node<P, T, V>> nodes;
  // list of nodes where the graph node's index is equal to its id
  private final ImmutableList<GraphNode<P, T, V>> graphNodes;

  // counters for nodes and edges per type
  private final TypeCounter<NodeType> nodeTypeCounter;
  private final TypeCounter<EdgeType> edgeTypeCounter;

  private SystemDependenceGraph(
      ImmutableList<Node<P, T, V>> pNodes,
      ImmutableList<GraphNode<P, T, V>> pGraphNodes,
      TypeCounter<NodeType> pNodeTypeCounter,
      TypeCounter<EdgeType> pEdgeTypeCounter) {

    nodes = pNodes;
    graphNodes = pGraphNodes;

    nodeTypeCounter = pNodeTypeCounter;
    edgeTypeCounter = pEdgeTypeCounter;
  }

  private static <P, T, V> void throwExceptionForUnknownNode(Node<P, T, V> pNode) {
    throw new IllegalArgumentException("SystemDependenceGraph does not contain node: " + pNode);
  }

  /**
   * Gets the corresponding {@link GraphNode} for the specified {@link Node}. Throws runtime
   * exception if the graph node does not exist or the specified node is {@code null}.
   */
  private static <P, T, V> GraphNode<P, T, V> getGraphNode(
      List<GraphNode<P, T, V>> pGraphNodes, Node<P, T, V> pNode) {

    Objects.requireNonNull(pNode, "node must not be null");

    if (pNode.getId() >= pGraphNodes.size()) {
      throwExceptionForUnknownNode(pNode);
    }

    GraphNode<P, T, V> graphNode = pGraphNodes.get(pNode.getId());

    if (!graphNode.getNode().equals(pNode)) {
      throwExceptionForUnknownNode(pNode);
    }

    return graphNode;
  }

  /**
   * Returns a new {@link SystemDependenceGraph} instance that contains no nodes and no edges.
   *
   * @param <P> the procedure type of the SDG
   * @param <T> the statement type of the SDG
   * @param <V> the variable type of the SDG
   * @return a new SDG that contains no nodes and no edges
   */
  public static <P, T, V> SystemDependenceGraph<P, T, V> empty() {
    return new SystemDependenceGraph<>(
        ImmutableList.of(),
        ImmutableList.of(),
        new TypeCounter<>(NodeType.values().length),
        new TypeCounter<>(EdgeType.values().length));
  }

  /**
   * Returns a builder that can be used to create {@link SystemDependenceGraph} instances.
   *
   * <p>The returned builder can be used to create exactly one SDG. Reusing the builder to create
   * multiple SDGs is not possible.
   *
   * @param <P> the procedure type for the SDG
   * @param <T> the statement type for the SDG
   * @param <V> the variable type for the SDG
   * @return a new SDG builder
   */
  public static <P, T, V> Builder<P, T, V> builder() {
    return new Builder<>();
  }

  /**
   * Returns the number of nodes contained in this system dependence graph.
   *
   * @return the number of nodes in this SDG
   */
  public int getNodeCount() {
    return nodes.size();
  }

  /**
   * Returns the number of nodes of the specified {@link NodeType} contained in this system
   * dependence graph.
   *
   * @param pType the type to get the node count for
   * @return the number of nodes of the specified type in this SDG
   * @throws NullPointerException if {@code pType == null}
   */
  public int getNodeCount(NodeType pType) {

    Objects.requireNonNull(pType, "pType must not be null");

    return nodeTypeCounter.getCount(pType);
  }

  /**
   * Returns the number of edges of the specified {@link EdgeType} in this system dependence graph.
   *
   * @param pType the type to get the edge count for
   * @return the number of edges of the specified type in this SDG
   * @throws NullPointerException if {@code pType == null}
   */
  public int getEdgeCount(EdgeType pType) {

    Objects.requireNonNull(pType, "pType must not be null");

    return edgeTypeCounter.getCount(pType);
  }

  /**
   * Returns a collection consisting of all nodes contained in this system dependence graph.
   *
   * @return an immutable collection of all nodes in this SDG
   */
  public ImmutableCollection<Node<P, T, V>> getNodes() {
    return nodes;
  }

  /**
   * Returns the node with the specified id in this system dependence graph.
   *
   * <p>The SDG contains a node for every id, if {@code id >= 0 && pId < getNodeCount()}.
   *
   * @param pId the id to get the node for
   * @return the node with the specified id in this SDG
   * @throws IllegalArgumentException if {@code pId < 0 || pId >= getNodeCount()}
   */
  public Node<P, T, V> getNodeById(int pId) {

    if (pId < 0 || pId >= getNodeCount()) {
      throw new IllegalArgumentException("SDG does not contain node with id: " + pId);
    }

    return nodes.get(pId);
  }

  /**
   * Returns a set containing the variables defined by the specified node.
   *
   * <p>The returned set may only contain variables, where the definition at the specified node is
   * also used (i.e. there's an edge for this dependency in the SDG).
   *
   * @param pNode the node to get the specified variables for
   * @return a set containing the variables defined by the specified node
   * @throws NullPointerException if {@code pNode == null}
   * @throws IllegalArgumentException if the specified node does not belong to this SDG
   */
  public ImmutableSet<V> getDefs(Node<P, T, V> pNode) {
    return ImmutableSet.copyOf(getGraphNode(graphNodes, pNode).getDefs());
  }

  /**
   * Returns a set containing the variables used by the specified node.
   *
   * <p>The returned set may only contain variables, where the use at the specified node also has a
   * corresponding definition (i.e. there's an edge for this dependency in the SDG).
   *
   * @param pNode the node to get the specified variables for
   * @return a set containing the variables used by the specified node
   * @throws NullPointerException if {@code pNode == null}
   * @throws IllegalArgumentException if the specified node does not belong to this SDG
   */
  public ImmutableSet<V> getUses(Node<P, T, V> pNode) {
    return ImmutableSet.copyOf(getGraphNode(graphNodes, pNode).getUses());
  }

  /**
   * Traverses the SDG specified by the graph nodes using the specified start nodes, visitor, and
   * direction.
   */
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
   * Traverses this system dependence graph by following edges from their predecessor to successor
   * (i.e. forward direction).
   *
   * <p>The traversal starts at the specified set of start nodes. The specified visitor is informed
   * about every node and edge visit. The traversal adheres to the visit results returned by the
   * visitor.
   *
   * <p>If the SDG contains cycles, some nodes can be visited an infinite number if times. The
   * visitor must return appropriate visit results to break out of these cycles. Alternatively, the
   * original visitor can be wrapped by a visit-once-visitor ({@link
   * #createVisitOnceVisitor(ForwardsVisitor)}).
   *
   * @param pStartNodes the nodes to start the traversal at
   * @param pVisitor the visitor to inform about node and edge visits and to guide the traversal
   * @throws NullPointerException if {@code pVisitor == null}, if {@code pStartNodes == null}, or if
   *     any node contained in {@code pStartNodes} is {@code null}
   * @throws IllegalArgumentException if any of the nodes contained in {@code pStartNodes} does not
   *     belong to this SDG
   */
  public void traverse(Collection<Node<P, T, V>> pStartNodes, ForwardsVisitor<P, T, V> pVisitor) {
    traverse(graphNodes, pStartNodes, pVisitor, true);
  }

  /**
   * Traverses this system dependence graph by following edges from their successor to predecessor
   * (i.e. backward direction).
   *
   * <p>The traversal starts at the specified set of start nodes. The specified visitor is informed
   * about every node and edge visit. The traversal adheres to the visit results returned by the
   * visitor.
   *
   * <p>If the SDG contains cycles, some nodes can be visited an infinite number if times. The
   * visitor must return appropriate visit results to break out of these cycles. Alternatively, the
   * original visitor can be wrapped by a visit-once-visitor ({@link
   * #createVisitOnceVisitor(BackwardsVisitor)}).
   *
   * @param pStartNodes the nodes to start the traversal at
   * @param pVisitor the visitor to inform about node and edge visits and to guide the traversal
   * @throws NullPointerException if {@code pVisitor == null}, if {@code pStartNodes == null}, or if
   *     any node contained in {@code pStartNodes} is {@code null}
   * @throws IllegalArgumentException if any of the nodes contained in {@code pStartNodes} does not
   *     belong to this SDG
   */
  public void traverse(Collection<Node<P, T, V>> pStartNodes, BackwardsVisitor<P, T, V> pVisitor) {
    traverse(graphNodes, pStartNodes, pVisitor, false);
  }

  /**
   * Creates a new {@code ForwardsVisitOnceVisitor} that wraps the specified visitor.
   *
   * <p>Node and edge visits are only delegated to the wrapped visitor if the visit-once-visitor has
   * not visited the node/edge yet. Visit results of the wrapped visitor are delegated during the
   * traversal.
   *
   * @param pDelegateVisitor the visitor to wrap
   * @return a new visit-once-visitor that wraps the specified visitor
   * @throws NullPointerException if {@code pDelegateVisitor == null}
   */
  public ForwardsVisitOnceVisitor<P, T, V> createVisitOnceVisitor(
      ForwardsVisitor<P, T, V> pDelegateVisitor) {

    Objects.requireNonNull(pDelegateVisitor, "pDelegateVisitor must not be null");

    return new ForwardsVisitOnceVisitor<>(pDelegateVisitor, getNodeCount());
  }

  /**
   * Creates a new {@link BackwardsVisitOnceVisitor} that wraps the specified visitor.
   *
   * <p>Node and edge visits are only delegated to the wrapped visitor if the visit-once-visitor has
   * not visited the node/edge yet. Visit results of the wrapped visitor are delegated during the
   * traversal.
   *
   * @param pDelegateVisitor the visitor to wrap
   * @return a new visit-once-visitor that wraps the specified visitor
   * @throws NullPointerException if {@code pDelegateVisitor == null}
   */
  public BackwardsVisitOnceVisitor<P, T, V> createVisitOnceVisitor(
      BackwardsVisitor<P, T, V> pDelegateVisitor) {

    Objects.requireNonNull(pDelegateVisitor, "pDelegateVisitor must not be null");

    return new BackwardsVisitOnceVisitor<>(pDelegateVisitor, getNodeCount());
  }

  /** Type of a system dependence graph node. */
  public enum NodeType {

    /**
     * Type of procedure entry nodes.
     *
     * <p>Only one node with this type should exist per procedure. Nodes of the procedure should be
     * directly or indirectly control dependent on the entry node.
     *
     * <ul>
     *   <li>Procedure: required
     *   <li>Statement: empty
     *   <li>Variable: empty
     * </ul>
     */
    ENTRY,

    /**
     * Type of regular statement nodes.
     *
     * <ul>
     *   <li>Procedure: optional (*)
     *   <li>Statement: required
     *   <li>Variable: empty
     * </ul>
     *
     * (*) If the system dependence graph only contains statements from a single procedure and no
     * other non-statement nodes, the procedure can be omitted (this can be used to represent a
     * program dependence graph (PDG)). Otherwise, the procedure is required.
     */
    STATEMENT,

    /**
     * Type of node the represents a variable used by a procedure on the procedure side.
     *
     * <ul>
     *   <li>Procedure: required
     *   <li>Statement: empty
     *   <li>Variable: required
     * </ul>
     */
    FORMAL_IN,

    /**
     * Type of node the represents a variable defined by a procedure on the procedure side.
     *
     * <ul>
     *   <li>Procedure: required
     *   <li>Statement: empty
     *   <li>Variable: required
     * </ul>
     */
    FORMAL_OUT,

    /**
     * Type of node that represents a variable used by a procedure on the caller side.
     *
     * <ul>
     *   <li>Procedure: required
     *   <li>Statement: required
     *   <li>Variable: required
     * </ul>
     */
    ACTUAL_IN,

    /**
     * Type of node that represents a variable defined by a procedure on the caller side.
     *
     * <ul>
     *   <li>Procedure: required
     *   <li>Statement: required
     *   <li>Variable: required
     * </ul>
     */
    ACTUAL_OUT;
  }

  /** Type for system dependence graph edges. */
  public enum EdgeType {

    /**
     * Type for flow dependencies.
     *
     * <p>Edges with this type should always be intra-procedural.
     */
    FLOW_DEPENDENCY,

    /**
     * Type for control dependencies.
     *
     * <p>Edges with this type should always be intra-procedural.
     */
    CONTROL_DEPENDENCY,

    /**
     * Type for declaration dependency edges.
     *
     * <p>Edges with this type can be intra-procedural or inter-procedural.
     */
    DECLARATION_EDGE,

    /**
     * Type for procedure call edges.
     *
     * <p>Edges with this type should always be inter-procedural.
     */
    CALL_EDGE,

    /**
     * Type for parameter edges from actual-in to formal-in or from formal-out to actual-out nodes.
     *
     * <p>Edges with this type should always be inter-procedural.
     */
    PARAMETER_EDGE,

    /**
     * Type for summary edges from actual-in to actual-out nodes.
     *
     * <p>Edges with this type should always be intra-procedural.
     */
    SUMMARY_EDGE;
  }

  /**
   * Represents a single node in a system dependence graph.
   *
   * @param <P> the procedure type of the SDG
   * @param <T> the statement type of the SDG
   * @param <V> the variable type of the SDG
   */
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

    /**
     * Returns the id of this node.
     *
     * <p>Node ids are unique inside a system dependence graph.
     *
     * @return the id of this node
     */
    public int getId() {
      return id;
    }

    /**
     * Returns the type of the node.
     *
     * @return the type of the node
     */
    public NodeType getType() {
      return type;
    }

    /**
     * Returns the procedure of the node.
     *
     * <p>Depending on the {@code NodeType} of the node, the returned optional can be empty.
     *
     * @return the procedure of the node
     */
    public Optional<P> getProcedure() {
      return procedure;
    }

    /**
     * Returns the statement of the node.
     *
     * <p>Depending on the {@code NodeType} of the node, the returned optional can be empty.
     *
     * @return the statement of the node.
     */
    public Optional<T> getStatement() {
      return statement;
    }

    /**
     * Returns the variable of the node.
     *
     * <p>Depending on the {@code NodeType} of the node, the returned optional can be empty.
     *
     * @return the variable of the node
     */
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

  /**
   * This class is used to represent a node in an SDG and its connection to other nodes via entering
   * and leaving edges. This class is private to the SDG class, use {@link Visitor} for graph
   * traversals and {@link Node} to refer to SDG nodes outside the SDG class.
   */
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

    private int getEnteringEdgeCount() {
      return enteringEdges.size();
    }

    private List<GraphEdge<P, T, V>> getEnteringEdges() {
      return enteringEdges;
    }

    private boolean hasEnteringEdgeFrom(EdgeType pType, GraphNode<P, T, V> pPredecessor) {

      for (GraphEdge<P, T, V> graphEdge : enteringEdges) {
        // identity comparison between graph nodes is intended here
        // inside a single SDG, equality can be determined by their identity
        if (graphEdge.getType() == pType && graphEdge.getPredecessor() == pPredecessor) {
          return true;
        }
      }

      return false;
    }

    private void addEnteringEdge(GraphEdge<P, T, V> pEdge) {
      enteringEdges.add(pEdge);
    }

    private int getLeavingEdgeCount() {
      return leavingEdges.size();
    }

    private List<GraphEdge<P, T, V>> getLeavingEdges() {
      return leavingEdges;
    }

    private boolean hasLeavingEdgeTo(EdgeType pType, GraphNode<P, T, V> pSuccessor) {

      for (GraphEdge<P, T, V> graphEdge : leavingEdges) {
        // identity comparison between graph nodes is intended here
        // inside a single SDG, equality can be determined by their identity
        if (graphEdge.getType() == pType && graphEdge.getSuccessor() == pSuccessor) {
          return true;
        }
      }

      return false;
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

    /**
     * During SDG construction, new connections between nodes are added. After construction has been
     * completed, the internal mutable lists/sets can be turned into {@link ImmutableList} and
     * {@link ImmutableSet} instances by calling this method. No more changes can be done to this
     * graph node after calling this method.
     */
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

  /**
   * This class is used to represent an edge between two graph nodes ({@link GraphNode}). This class
   * is private, use {@link Visitor} for graph traversals.
   */
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

  /**
   * Builder for system dependence graphs. Instances of a builder can only be used once. It's not
   * possible to build multiple SDGs with one and the same builder. Calling {@link #build()}
   * finished SDG construction.
   *
   * <p>How a single node is inserted: {@code builder.node(...);}
   *
   * <p>How an edge is inserted: {@code builder.node(...).depends(...).on(...);}. Nodes are inserted
   * as needed if they have not already been inserted.
   *
   * @param <P> the procedure type for the SDG
   * @param <T> the statement type for the SDG
   * @param <V> the variable type for the SDG
   */
  public static final class Builder<P, T, V> {

    // list of nodes where the node's index is equal to its id
    private final List<Node<P, T, V>> nodes;
    // list of nodes where the graph node's index is equal to its id
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

    /**
     * Creates and inserts a {@link GraphNode} and {@link Node} for the specified parameters if such
     * a node does not already exist. In all cases it returns a graph node fitting the specified
     * parameters.
     */
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

    /**
     * Inserts an edge between two graph nodes. Only adds the edge if it doesn't already exist.
     * Also, updates the defs and uses of the nodes by using the cause.
     */
    private void insertEdge(
        GraphNode<P, T, V> pPredecessor,
        GraphNode<P, T, V> pSuccessor,
        EdgeType pType,
        Optional<V> pCause) {

      boolean insertEdge = true;
      // typically, only one of the edge counts is large, so this greatly improves performance
      if (pSuccessor.getEnteringEdgeCount() < pPredecessor.getLeavingEdgeCount()) {
        insertEdge = !pSuccessor.hasEnteringEdgeFrom(pType, pPredecessor);
      } else {
        insertEdge = !pPredecessor.hasLeavingEdgeTo(pType, pSuccessor);
      }

      if (insertEdge) {
        GraphEdge<P, T, V> edge = new GraphEdge<>(pType, pPredecessor, pSuccessor);
        pPredecessor.addLeavingEdge(edge);
        pSuccessor.addEnteringEdge(edge);
      }

      if (pCause.isPresent()) {
        V variable = pCause.orElseThrow();
        pPredecessor.addDef(variable);
        pSuccessor.addUse(variable);
      }

      edgeTypeCounter.increment(pType);
    }

    int getNodeCount() {
      return nodes.size();
    }

    ImmutableList<Node<P, T, V>> getNodes() {
      return ImmutableList.copyOf(nodes);
    }

    void traverse(Collection<Node<P, T, V>> pStartNodes, ForwardsVisitor<P, T, V> pVisitor) {
      SystemDependenceGraph.traverse(graphNodes, pStartNodes, pVisitor, true);
    }

    void traverse(Collection<Node<P, T, V>> pStartNodes, BackwardsVisitor<P, T, V> pVisitor) {
      SystemDependenceGraph.traverse(graphNodes, pStartNodes, pVisitor, false);
    }

    /**
     * Returns an array that contains a generated id, determined by the specified function, for
     * every node ({@code index == node.getId()}) contained in this SDG builder.
     *
     * <p>If two nodes return the same (uses {@code equals}) non-empty function result, they get the
     * same generated id in the returned array. If the function result is empty (see {@link
     * Optional#empty}), the generated id is {@code -1}.
     *
     * @param pFunction the function to map nodes to optional values
     * @return an array that contains the generated ids for every node
     * @throws NullPointerException if {@code pFunction == null}
     */
    int[] createIds(Function<Node<P, T, V>, Optional<?>> pFunction) {

      Objects.requireNonNull(pFunction, "pFunction must not be null");

      Map<Object, Integer> resultMap = new HashMap<>();
      int[] ids = new int[nodes.size()];

      for (int nodeId = 0; nodeId < ids.length; nodeId++) {

        Node<P, T, V> node = nodes.get(nodeId);
        Optional<?> result = pFunction.apply(node);

        if (result != null && result.isPresent()) {
          int id = resultMap.computeIfAbsent(result.orElseThrow(), key -> resultMap.size());
          ids[nodeId] = id;
        } else {
          ids[nodeId] = -1;
        }
      }

      return ids;
    }

    /**
     * Inserts summary edges between actual-in/out nodes for the specified formal-in/out nodes.
     *
     * <p>All actual-in/out nodes connected to the specified formal-in/out nodes via parameter edges
     * are considered. Only summary edges between actual-in/out nodes of the same calling context
     * are inserted.
     *
     * @param pFormalInNode the formal-in node that the formal-out node depends on
     * @param pFormalOutNode the formal-out node that depends on the formal-in node
     * @throws NullPointerException if any of the parameters is {@code null}
     * @throws IllegalArgumentException if {@code pFormalInNode.getType() != NodeType.FORMAL_IN}, or
     *     {@code pFormalOutNode.getType() != NodeType.FORMAL_OUT}, or {@code pFormalOutNode} does
     *     not belong to this SDG builder
     */
    void insertActualSummaryEdges(Node<P, T, V> pFormalInNode, Node<P, T, V> pFormalOutNode) {

      Objects.requireNonNull(pFormalInNode, "pFormalInNode must not be null");
      Objects.requireNonNull(pFormalInNode, "pFormalOutNode must not be null");

      Preconditions.checkArgument(
          pFormalInNode.getType() == NodeType.FORMAL_IN,
          "pFormalInNode does not have type FORMAL_IN");
      Preconditions.checkArgument(
          pFormalOutNode.getType() == NodeType.FORMAL_OUT,
          "pFormalOutNode does not have type FORMAL_OUT");

      GraphNode<P, T, V> formalOutGraphNode = graphNodes.get(pFormalOutNode.getId());
      Preconditions.checkArgument(
          formalOutGraphNode.getNode().equals(pFormalOutNode),
          "pFormalOutNode does not belong to this SDG builder");

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

    /**
     * Inserts a new node for the specified parameters if it has not already been inserted.
     *
     * <p>The returned edge chooser can, but doesn't have to, be used to insert an edge with the new
     * (or already existing but equivalent) node as its successor.
     *
     * @param pType the type of the node to insert
     * @param pProcedure the, depending on the {@link NodeType}, optional procedure of the node to
     *     insert
     * @param pStatement the, depending on the {@link NodeType}, optional statement of the node to
     *     insert
     * @param pVariable the, depending on the {@link NodeType}, optional variable of the node to
     *     insert
     * @return an edge chooser that can be used to insert a dependency
     * @throws NullPointerException if any of the parameters is {@code null}
     */
    public EdgeChooser node(
        NodeType pType, Optional<P> pProcedure, Optional<T> pStatement, Optional<V> pVariable) {

      Objects.requireNonNull(pType, "pType must not be null");
      Objects.requireNonNull(pProcedure, "pProcedure must not be null");
      Objects.requireNonNull(pStatement, "pStatement must not be null");
      Objects.requireNonNull(pVariable, "pVariable must not be null");

      return new EdgeChooser(graphNode(pType, pProcedure, pStatement, pVariable));
    }

    /**
     * Returns the finished system dependence graph created by this builder.
     *
     * <p>The returned SDG contains all nodes an edges that were previously inserted into this
     * builder. This builder cannot be used anymore, after calling this method.
     *
     * @return the finished SDG created by this builder
     */
    public SystemDependenceGraph<P, T, V> build() {

      for (GraphNode<P, T, V> graphNode : graphNodes) {
        graphNode.finish();
      }

      return new SystemDependenceGraph<>(
          ImmutableList.copyOf(nodes),
          ImmutableList.copyOf(graphNodes),
          nodeTypeCounter.copy(),
          edgeTypeCounter.copy());
    }

    /**
     * Chooser for an edge to insert into the system dependence graph. The successor of the edge has
     * already been selected and is known to the edge chooser.
     */
    public final class EdgeChooser {

      private final GraphNode<P, T, V> graphNode;

      private EdgeChooser(GraphNode<P, T, V> pGraphNode) {
        graphNode = pGraphNode;
      }

      /**
       * Chooses the specified edge type for dependency insertion.
       *
       * <p>Optionally, a cause variable can be specified. The cause variable is used to determine a
       * node's defs and uses.
       *
       * <p>No edge is added until a predecessor is selected by the returned {@link
       * DependencyChooser}.
       *
       * @param pType the type of the edge to insert
       * @param pCause the optional variable that caused the dependency
       * @return a chooser for the predecessor of the edge
       * @throws NullPointerException if any of the parameters is {@code null}
       */
      public DependencyChooser depends(EdgeType pType, Optional<V> pCause) {

        Objects.requireNonNull(pType, "pType must not be null");
        Objects.requireNonNull(pCause, "pCause must not be null");

        return new DependencyChooser(graphNode, pType, pCause);
      }

      /**
       * Returns the previously chosen node.
       *
       * <p>This node is used as the edge successor.
       *
       * @return the previously chosen node
       */
      public Node<P, T, V> getNode() {
        return graphNode.getNode();
      }
    }

    /**
     * Chooser for edges to insert into the system dependence graph. The edge and its successor have
     * already been selected.
     */
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

      /**
       * Inserts a new node and the previously chosen edge for the specified parameters if they do
       * not not already exist.
       *
       * <p>The node specified by the parameters is used as the edge predecessor.
       *
       * @param pType the type of the node to insert
       * @param pProcedure the, depending on the {@link NodeType}, optional procedure of the node to
       *     insert
       * @param pStatement the, depending on the {@link NodeType}, optional statement of the node to
       *     insert
       * @param pVariable the, depending on the {@link NodeType}, optional variable of the node to
       *     insert
       */
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

  /**
   * Represents a key that can be used to retrieve nodes from maps by specifying the type,
   * procedure, statement, and variable, but not the id.
   */
  private static final class NodeMapKey<P, T, V> {

    private final NodeType type;
    private final Optional<P> procedure;
    private final Optional<T> statement;
    private final Optional<V> variable;

    // instances of this class are typically used as (hash-)map keys, so caching the hash can
    // improve performance
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

  /**
   * Result of a node or edge visit that guides the system dependence graph traversal and is
   * returned by a visitor.
   *
   * @see Visitor
   */
  public enum VisitResult {

    /**
     * Continue traversal.
     *
     * <p>Meaning for a visited node: follow all edges leaving (for forward traversals) or entering
     * (for backwards traversals) this node.
     *
     * <p>Meaning for a visited edge: follow the edge to its successor (for forward traversals) or
     * predecessor (for backwards traversals).
     */
    CONTINUE,

    /**
     * Terminate traversal immediately.
     *
     * <p>No more nodes and edges are visited after returning this result during SDG traversal. The
     * traversal ends immediately after returning this visit result.
     */
    TERMINATE,

    /**
     * Skip this node or edge during traversal.
     *
     * <p>Meaning for a visited node: do not follow any edges leaving (for forward traversals) or
     * entering (for backward traversals) this node. Other nodes connected to the visited nodes may
     * still be visited during the traversal when adjacent nodes are reached.
     *
     * <p>Meaning for a visited edge: do not follow the edge to its successor (for forward
     * traversals) or predecessor (for backward traversals).
     *
     * <p>The traversal still continues for other edges and nodes that were not skipped.
     */
    SKIP;
  }

  /**
   * Represents an object that gets informed about node and edge visits during system dependence
   * graph traversals. Also, guides the SDG traversal by returning specific visit results. Visitors
   * must implement the {@link ForwardsVisitor} or {@link BackwardsVisitor} interface to be used for
   * SDG traversal.
   *
   * @param <P> the procedure type of the SDG
   * @param <T> the statement type of the SDG
   * @param <V> the variable type of the SDG
   * @see VisitResult
   * @see SystemDependenceGraph#traverse(Collection, ForwardsVisitor)
   * @see SystemDependenceGraph#traverse(Collection, BackwardsVisitor)
   */
  public interface Visitor<P, T, V> {

    /**
     * Accepts visited nodes during system dependence graph traversal.
     *
     * @param pNode the visited node
     * @return a {@link VisitResult} to guide the SDG traversal
     */
    public VisitResult visitNode(Node<P, T, V> pNode);

    /**
     * Accepts visited edges during system dependence graph traversal.
     *
     * <p>Called during SDG traversals for every visited edge. Returns a {@link VisitResult} that
     * guides the SDG traversal.
     *
     * @param pType the {@link EdgeType} of the visited edge
     * @param pPredecessor the predecessor of the visited edge
     * @param pSuccessor the successor of the visited edge
     * @return a {@link VisitResult} to guide the SDG traversal
     */
    public VisitResult visitEdge(
        EdgeType pType, Node<P, T, V> pPredecessor, Node<P, T, V> pSuccessor);
  }

  /**
   * Represents a {@link Visitor} that can be used for forward traversals of system dependence
   * graphs.
   *
   * @param <P> the procedure type of the SDG
   * @param <T> the statement type of the SDG
   * @param <V> the variable type of the SDG
   * @see SystemDependenceGraph#traverse(Collection, ForwardsVisitor)
   */
  public interface ForwardsVisitor<P, T, V> extends Visitor<P, T, V> {}

  /**
   * Represents a {@link Visitor} that can be used for backward traversals of system dependence
   * graphs.
   *
   * @param <P> the procedure type of the SDG
   * @param <T> the statement type of the SDG
   * @param <V> the variable type of the SDG
   * @see SystemDependenceGraph#traverse(Collection, BackwardsVisitor)
   */
  public interface BackwardsVisitor<P, T, V> extends Visitor<P, T, V> {}

  /**
   * Implementation of the visit-once-visitor. Extended by {@link ForwardsVisitOnceVisitor} and
   * {@link BackwardsVisitOnceVisitor}.
   */
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

  /**
   * Represents a {@link Visitor} that can can be used for forward system dependence graph
   * traversals where every node should only be visited once. The visitor can forget all visited
   * nodes by calling {@link ForwardsVisitOnceVisitor#reset}. The visitor can be (re)used for
   * multiple traversals of the same SDG. Visit-once-visitors only work for the SDG they were
   * created for.
   *
   * @param <P> the procedure type of the SDG
   * @param <T> the statement type of the SDG
   * @param <V> the variable type of the SDG
   * @see SystemDependenceGraph#createVisitOnceVisitor(ForwardsVisitor)
   * @see SystemDependenceGraph#traverse(Collection, ForwardsVisitor)
   */
  public static final class ForwardsVisitOnceVisitor<P, T, V> extends VisitOnceVisitor<P, T, V>
      implements ForwardsVisitor<P, T, V> {

    ForwardsVisitOnceVisitor(ForwardsVisitor<P, T, V> pDelegateVisitor, int pNodeCount) {
      super(true, pDelegateVisitor, pNodeCount);
    }

    /**
     * Causes this visit-once-visitor to forget all previously visited nodes.
     *
     * <p>This can, but doesn't have to, be called between different SDG traversals of the same SDG.
     */
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

  /**
   * Represents a {@link Visitor} that can can be used for backward system dependence graph
   * traversals where every node should only be visited once. The visitor can forget all visited
   * nodes by calling {@link ForwardsVisitOnceVisitor#reset}. The visitor can be (re)used for
   * multiple traversals of the same SDG. Visit-once-visitors only work for the SDG they were
   * created for.
   *
   * @param <P> the procedure type of the SDG
   * @param <T> the statement type of the SDG
   * @param <V> the variable type of the SDG
   * @see SystemDependenceGraph#createVisitOnceVisitor(BackwardsVisitor)
   * @see SystemDependenceGraph#traverse(Collection, BackwardsVisitor)
   */
  public static final class BackwardsVisitOnceVisitor<P, T, V> extends VisitOnceVisitor<P, T, V>
      implements BackwardsVisitor<P, T, V> {

    BackwardsVisitOnceVisitor(BackwardsVisitor<P, T, V> pDelegateVisitor, int pNodeCount) {
      super(false, pDelegateVisitor, pNodeCount);
    }

    /**
     * Causes this visit-once-visitor to forget all previously visited nodes.
     *
     * <p>This can, but doesn't have to, be called between different SDG traversals of the same SDG.
     */
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
