// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.sdg;

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
 * @param <V> The type of variables in this SDG. Variables are defined and used. Dependencies exist
 *     between defs and subsequent uses. Furthermore, formal-in/out and actual-in/out nodes exist
 *     for specific variables.
 * @param <N> The node type of this SDG. The node type must be a subclass of {@link
 *     SystemDependenceGraph.Node} or {@link SystemDependenceGraph.Node} itself.
 */
public class SystemDependenceGraph<V, N extends SystemDependenceGraph.Node<?, ?, V>> {

  // list of nodes where the node's index is equal to its id
  private final ImmutableList<N> nodes;
  // list of nodes where the graph node's index is equal to its id
  private final ImmutableList<GraphNode.ImmutableGraphNode<V, N>> graphNodes;

  // counters for nodes and edges per type
  private final TypeCounter<NodeType> nodeTypeCounter;
  private final TypeCounter<EdgeType> edgeTypeCounter;

  private SystemDependenceGraph(
      ImmutableList<N> pNodes,
      ImmutableList<GraphNode.ImmutableGraphNode<V, N>> pGraphNodes,
      TypeCounter<NodeType> pNodeTypeCounter,
      TypeCounter<EdgeType> pEdgeTypeCounter) {

    nodes = pNodes;
    graphNodes = pGraphNodes;

    nodeTypeCounter = pNodeTypeCounter;
    edgeTypeCounter = pEdgeTypeCounter;
  }

  /**
   * Creates a new {@link SystemDependenceGraph} instance from the specified SDG.
   *
   * <p>The constructed SDG is a copy of the specified SDG. This non-private constructor is required
   * for subclasses of {@link SystemDependenceGraph}.
   *
   * @param pSdg a SDG to create a copy of
   */
  protected SystemDependenceGraph(SystemDependenceGraph<V, N> pSdg) {
    this(pSdg.nodes, pSdg.graphNodes, pSdg.nodeTypeCounter, pSdg.edgeTypeCounter);
  }

  private static <N extends Node<?, ?, ?>> void throwExceptionForUnknownNode(N pNode) {
    throw new IllegalArgumentException("SystemDependenceGraph does not contain node: " + pNode);
  }

  /**
   * Gets the corresponding {@link GraphNode} for the specified {@link Node}. Throws runtime
   * exception if the graph node does not exist or the specified node is {@code null}.
   */
  private static <V, N extends Node<?, ?, V>> GraphNode<V, N> getGraphNode(
      List<? extends GraphNode<V, N>> pGraphNodes, N pNode) {

    Objects.requireNonNull(pNode, "node must not be null");

    if (pNode.getId() >= pGraphNodes.size()) {
      throwExceptionForUnknownNode(pNode);
    }

    GraphNode<V, N> graphNode = pGraphNodes.get(pNode.getId());

    if (!graphNode.getNode().equals(pNode)) {
      throwExceptionForUnknownNode(pNode);
    }

    return graphNode;
  }

  /**
   * Returns a new {@link SystemDependenceGraph} instance that contains no nodes and no edges.
   *
   * @param <V> the variable type of the SDG
   * @param <N> the node type of the SDG
   * @return a new SDG that contains no nodes and no edges
   */
  public static <V, N extends Node<?, ?, V>> SystemDependenceGraph<V, N> empty() {
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
  public static <P, T, V> Builder<P, T, V, Node<P, T, V>> builder() {
    return new Builder<>(Function.identity());
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
   * @param <N> the node type for the SDG
   * @param pNodeCreationFunction function that transforms {@link SystemDependenceGraph.Node}
   *     instances to instances of {@code N}
   * @return a new SDG builder
   */
  public static <P, T, V, N extends Node<P, T, V>> Builder<P, T, V, N> builder(
      Function<Node<P, T, V>, N> pNodeCreationFunction) {
    return new Builder<>(pNodeCreationFunction);
  }

  /**
   * Returns the number of nodes contained in this system dependence graph.
   *
   * @return the number of nodes in this SDG
   */
  public final int getNodeCount() {
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
  public final int getNodeCount(NodeType pType) {

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
  public final int getEdgeCount(EdgeType pType) {

    Objects.requireNonNull(pType, "pType must not be null");

    return edgeTypeCounter.getCount(pType);
  }

  /**
   * Returns a collection consisting of all nodes contained in this system dependence graph.
   *
   * @return an immutable collection of all nodes in this SDG
   */
  public final ImmutableCollection<N> getNodes() {
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
  public final N getNodeById(int pId) {

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
  public final ImmutableSet<V> getDefs(N pNode) {
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
  public final ImmutableSet<V> getUses(N pNode) {
    return ImmutableSet.copyOf(getGraphNode(graphNodes, pNode).getUses());
  }

  /**
   * Traverses the SDG specified by the graph nodes using the specified start nodes, visitor, and
   * direction.
   */
  private static <V, N extends Node<?, ?, V>> void traverse(
      List<? extends GraphNode<V, N>> pGraphNodes,
      Collection<N> pStartNodes,
      Visitor<N> pVisitor,
      boolean pForwards) {

    Objects.requireNonNull(pStartNodes, "pStartNodes must not be null");
    Objects.requireNonNull(pVisitor, "pVisitor must not be null");

    Deque<GraphNode<V, N>> waitlist = new ArrayDeque<>();

    for (N node : pStartNodes) {
      waitlist.add(getGraphNode(pGraphNodes, node));
    }

    while (!waitlist.isEmpty()) {

      GraphNode<V, N> graphNode = waitlist.remove();
      VisitResult nodeVisitResult = pVisitor.visitNode(graphNode.getNode());

      if (nodeVisitResult == VisitResult.CONTINUE) {

        List<GraphEdge<V, N>> edges =
            pForwards ? graphNode.getLeavingEdges() : graphNode.getEnteringEdges();

        for (GraphEdge<V, N> edge : edges) {

          GraphNode<V, N> predecessor = edge.getPredecessor();
          GraphNode<V, N> successor = edge.getSuccessor();
          VisitResult edgeVisitResult =
              pVisitor.visitEdge(edge.getType(), predecessor.getNode(), successor.getNode());

          if (edgeVisitResult == VisitResult.CONTINUE) {

            GraphNode<V, N> next = pForwards ? successor : predecessor;
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
  public final void traverse(Collection<N> pStartNodes, ForwardsVisitor<N> pVisitor) {
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
  public final void traverse(Collection<N> pStartNodes, BackwardsVisitor<N> pVisitor) {
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
  public final ForwardsVisitOnceVisitor<N> createVisitOnceVisitor(
      ForwardsVisitor<N> pDelegateVisitor) {

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
  public final BackwardsVisitOnceVisitor<N> createVisitOnceVisitor(
      BackwardsVisitor<N> pDelegateVisitor) {

    Objects.requireNonNull(pDelegateVisitor, "pDelegateVisitor must not be null");

    return new BackwardsVisitOnceVisitor<>(pDelegateVisitor, getNodeCount());
  }

  /**
   * Type of a system dependence graph node.
   *
   * <p>C program example to show the different node types (the resulting nodes depend on the used
   * construction method, so real result may differ):
   *
   * <pre>
   * int global = 1;
   *
   * int foo(int p) {
   *   global += p;
   *   return global;
   * }
   *
   * int main() {
   *   int x = 2;
   *   int y = foo(x);
   * }
   * </pre>
   *
   * <table>
   *   <tr>
   *     <th>Type</th>
   *     <th>Procedure</th>
   *     <th>Statement</th>
   *     <th>Variable</th>
   *   </tr>
   *   <tr>
   *   <tr>
   *     <th>{@code STATEMENT}</th>
   *     <th>-</th>
   *     <th>{@code int global = 0;}</th>
   *     <th>-</th>
   *   </tr>
   *   <tr>
   *     <th>{@code ENTRY}</th>
   *     <th>{@code int foo(int)}</th>
   *     <th>-</th>
   *     <th>-</th>
   *   </tr>
   *   <tr>
   *     <th>{@code FORMAL_IN}</th>
   *     <th>{@code int foo(int)}</th>
   *     <th>-</th>
   *     <th>{@code foo::p}</th>
   *   </tr>
   *    <tr>
   *     <th>{@code FORMAL_IN}</th>
   *     <th>{@code int foo(int)}</th>
   *     <th>-</th>
   *     <th>{@code global}</th>
   *   </tr>
   *    <tr>
   *     <th>{@code STATEMENT}</th>
   *     <th>{@code int foo(int)}</th>
   *     <th>{@code global += p;}</th>
   *     <th>-</th>
   *   </tr>
   *    <tr>
   *     <th>{@code STATEMENT}</th>
   *     <th>{@code int foo(int)}</th>
   *     <th>{@code return global;}</th>
   *     <th>-</th>
   *   </tr>
   *   <tr>
   *     <th>{@code FORMAL_OUT}</th>
   *     <th>{@code int foo(int)}</th>
   *     <th>-</th>
   *     <th>{@code foo::__retval__}</th>
   *   </tr>
   *    <tr>
   *     <th>{@code FORMAL_OUT}</th>
   *     <th>{@code int foo(int)}</th>
   *     <th>-</th>
   *     <th>{@code global}</th>
   *   </tr>
   *   <tr>
   *     <th>{@code ENTRY}</th>
   *     <th>{@code int main()}</th>
   *     <th>-</th>
   *     <th>-</th>
   *   </tr>
   *   <tr>
   *     <th>{@code STATEMENT}</th>
   *     <th>{@code int main()}</th>
   *     <th>{@code int x = 5;}</th>
   *     <th>-</th>
   *   </tr>
   *   <tr>
   *     <th>{@code STATEMENT}</th>
   *     <th>{@code int main()}</th>
   *     <th>{@code int y = foo(x);}</th>
   *     <th>-</th>
   *   </tr>
   *   <tr>
   *     <th>{@code ACTUAL_IN}</th>
   *     <th>{@code int main()}</th>
   *     <th>{@code int y = foo(x);}</th>
   *     <th>{@code foo::p}</th>
   *   </tr>
   *   <tr>
   *     <th>{@code ACTUAL_IN}</th>
   *     <th>{@code int main()}</th>
   *     <th>{@code int y = foo(x);}</th>
   *     <th>{@code global}</th>
   *   </tr>
   *   <tr>
   *     <th>{@code ACTUAL_OUT}</th>
   *     <th>{@code int main()}</th>
   *     <th>{@code int y = foo(x);}</th>
   *     <th>{@code foo::__retval__}</th>
   *   </tr>
   *   <tr>
   *     <th>{@code ACTUAL_OUT}</th>
   *     <th>{@code int main()}</th>
   *     <th>{@code int y = foo(x);}</th>
   *     <th>{@code global}</th>
   *   </tr>
   *   <tr>
   *     <th>{@code FORMAL_OUT}</th>
   *     <th>{@code int main()}</th>
   *     <th>-</th>
   *     <th>{@code main::__retval__}</th>
   *   </tr>
   * </table>
   */
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
     * Type of regular statement, expression, and declaration nodes.
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
     * Type of nodes that represent variables that are visible to or defined by some procedure
     * caller and used inside the procedure.
     *
     * <p>This is the case for e.g. parameters and used global variables.
     *
     * <ul>
     *   <li>Procedure: required
     *   <li>Statement: empty
     *   <li>Variable: required
     * </ul>
     */
    FORMAL_IN,

    /**
     * Type of nodes that represent variables that are visible to some procedure caller and defined
     * inside the procedure.
     *
     * <p>This is the case for e.g. return values and modified global variables.
     *
     * <ul>
     *   <li>Procedure: required
     *   <li>Statement: empty
     *   <li>Variable: required
     * </ul>
     */
    FORMAL_OUT,

    /**
     * Type of nodes that represent variables at a specific call sites and are connected to {@code
     * FORMAL_IN} nodes.
     *
     * <ul>
     *   <li>Procedure: required
     *   <li>Statement: required
     *   <li>Variable: required
     * </ul>
     */
    ACTUAL_IN,

    /**
     * Type of nodes that represent variables at a specific call sites and are connected to {@code
     * FORMAL_OUT} nodes.
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
   * @param <P> The type of procedures in the SDG. Typically, programs are organized into
   *     procedures, functions, or similar constructs that consist of statements, expressions, and
   *     declarations. In an SDG, all these compound constructs are refered to as procedures and are
   *     of the type specified by this type parameter.
   * @param <T> The statement type of the SDG. Typically, programs consist of statements,
   *     expressions, and declarations. In an SDG, all these parts are refered to as statements and
   *     are of the type specified by this type parameter.
   * @param <V> The type of variables in the SDG. Variables are defined and used. Dependencies exist
   *     between defs and subsequent uses. Furthermore, formal-in/out and actual-in/out nodes exist
   *     for specific variables.
   */
  public static class Node<P, T, V> {

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
     * Creates a new {@link SystemDependenceGraph.Node} instance from the specified node.
     *
     * <p>The constructed node is a copy of the specified node. This non-private constructor is
     * required for subclasses of {@link SystemDependenceGraph.Node}.
     *
     * @param pNode a node to create a copy of
     */
    protected Node(Node<P, T, V> pNode) {
      this(pNode.id, pNode.type, pNode.procedure, pNode.statement, pNode.variable);
    }

    /**
     * Returns the id of this node.
     *
     * <p>Node ids are unique inside a system dependence graph.
     *
     * @return the id of this node
     */
    public final int getId() {
      return id;
    }

    /**
     * Returns the type of the node.
     *
     * @return the type of the node
     */
    public final NodeType getType() {
      return type;
    }

    /**
     * Returns the procedure of the node.
     *
     * <p>Depending on the {@code NodeType} of the node, the returned optional can be empty.
     *
     * @return the procedure of the node
     */
    public final Optional<P> getProcedure() {
      return procedure;
    }

    /**
     * Returns the statement of the node.
     *
     * <p>Depending on the {@code NodeType} of the node, the returned optional can be empty.
     *
     * @return the statement of the node.
     */
    public final Optional<T> getStatement() {
      return statement;
    }

    /**
     * Returns the variable of the node.
     *
     * <p>Depending on the {@code NodeType} of the node, the returned optional can be empty.
     *
     * @return the variable of the node
     */
    public final Optional<V> getVariable() {
      return variable;
    }

    @Override
    public final int hashCode() {
      return hash;
    }

    @Override
    public final boolean equals(Object pObject) {

      if (this == pObject) {
        return true;
      }

      if (!(pObject instanceof Node)) {
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
    public final String toString() {
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
  private abstract static class GraphNode<V, N extends Node<?, ?, V>> {

    private final N node;

    private GraphNode(N pNode) {
      node = pNode;
    }

    final N getNode() {
      return node;
    }

    abstract List<GraphEdge<V, N>> getEnteringEdges();

    abstract List<GraphEdge<V, N>> getLeavingEdges();

    abstract Set<V> getDefs();

    abstract Set<V> getUses();

    @Override
    public final int hashCode() {
      return node.hashCode();
    }

    @Override
    public final boolean equals(Object pObject) {

      if (this == pObject) {
        return true;
      }

      if (!(pObject instanceof GraphNode)) {
        return false;
      }

      GraphNode<?, ?> other = (GraphNode<?, ?>) pObject;
      return node.equals(other.node);
    }

    @Override
    public final String toString() {
      return String.format(
          Locale.ENGLISH,
          "%s[node=%s, enteringEdges=%s, leavingEdges=%s, defs=%s, uses=%s]",
          getClass().getName(),
          node,
          getEnteringEdges(),
          getLeavingEdges(),
          getDefs(),
          getUses());
    }

    private static class MutableGraphNode<V, N extends Node<?, ?, V>> extends GraphNode<V, N> {

      private final List<GraphEdge<V, N>> enteringEdges;
      private final List<GraphEdge<V, N>> leavingEdges;

      private final Set<V> defs;
      private final Set<V> uses;

      private MutableGraphNode(N pNode) {

        super(pNode);

        enteringEdges = new ArrayList<>();
        leavingEdges = new ArrayList<>();

        defs = new HashSet<>();
        uses = new HashSet<>();
      }

      private int getEnteringEdgeCount() {
        return enteringEdges.size();
      }

      @Override
      List<GraphEdge<V, N>> getEnteringEdges() {
        return enteringEdges;
      }

      private boolean hasEnteringEdgeFrom(EdgeType pType, GraphNode<V, N> pPredecessor) {

        for (GraphEdge<V, N> graphEdge : enteringEdges) {
          // identity comparison between graph nodes is intended here
          // inside a single SDG, equality can be determined by their identity
          if (graphEdge.getType() == pType && graphEdge.getPredecessor() == pPredecessor) {
            return true;
          }
        }

        return false;
      }

      private void addEnteringEdge(GraphEdge<V, N> pEdge) {
        enteringEdges.add(pEdge);
      }

      private int getLeavingEdgeCount() {
        return leavingEdges.size();
      }

      @Override
      List<GraphEdge<V, N>> getLeavingEdges() {
        return leavingEdges;
      }

      private boolean hasLeavingEdgeTo(EdgeType pType, GraphNode<V, N> pSuccessor) {

        for (GraphEdge<V, N> graphEdge : leavingEdges) {
          // identity comparison between graph nodes is intended here
          // inside a single SDG, equality can be determined by their identity
          if (graphEdge.getType() == pType && graphEdge.getSuccessor() == pSuccessor) {
            return true;
          }
        }

        return false;
      }

      private void addLeavingEdge(GraphEdge<V, N> pEdge) {
        leavingEdges.add(pEdge);
      }

      @Override
      Set<V> getDefs() {
        return defs;
      }

      private void addDef(V pVariable) {
        defs.add(pVariable);
      }

      @Override
      Set<V> getUses() {
        return uses;
      }

      private void addUse(V pVariable) {
        uses.add(pVariable);
      }
    }

    private static class ImmutableGraphNode<V, N extends Node<?, ?, V>> extends GraphNode<V, N> {

      private ImmutableList<GraphEdge<V, N>> enteringEdges;
      private ImmutableList<GraphEdge<V, N>> leavingEdges;

      private ImmutableSet<V> defs;
      private ImmutableSet<V> uses;

      private ImmutableGraphNode(N pNode, ImmutableSet<V> pDefs, ImmutableSet<V> pUses) {
        super(pNode);

        defs = pDefs;
        uses = pUses;
      }

      @Override
      ImmutableList<GraphEdge<V, N>> getEnteringEdges() {
        return enteringEdges;
      }

      @Override
      ImmutableList<GraphEdge<V, N>> getLeavingEdges() {
        return leavingEdges;
      }

      @Override
      ImmutableSet<V> getDefs() {
        return defs;
      }

      @Override
      ImmutableSet<V> getUses() {
        return uses;
      }
    }
  }

  /**
   * This class is used to represent an edge between two graph nodes ({@link GraphNode}). This class
   * is private, use {@link Visitor} for graph traversals.
   */
  private static final class GraphEdge<V, N extends Node<?, ?, V>> {

    private final EdgeType type;

    private final GraphNode<V, N> predecessor;
    private final GraphNode<V, N> successor;

    private GraphEdge(EdgeType pType, GraphNode<V, N> pPredecessor, GraphNode<V, N> pSuccessor) {

      type = pType;

      predecessor = pPredecessor;
      successor = pSuccessor;
    }

    private EdgeType getType() {
      return type;
    }

    private GraphNode<V, N> getPredecessor() {
      return predecessor;
    }

    private GraphNode<V, N> getSuccessor() {
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

  /**
   * Builder for system dependence graphs. Instances of a builder can only be used once. It's not
   * possible to build multiple SDGs with one and the same builder. Calling {@link #build()}
   * finishes SDG construction.
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
  public static final class Builder<P, T, V, N extends Node<P, T, V>> {

    private final Function<Node<P, T, V>, N> nodeCreationFunction;

    // list of nodes where the node's index is equal to its id
    private final List<N> nodes;
    // list of nodes where the graph node's index is equal to its id
    private final List<GraphNode.MutableGraphNode<V, N>> graphNodes;
    private final Map<NodeMapKey<P, T, V>, GraphNode.MutableGraphNode<V, N>> nodeMap;

    private final TypeCounter<NodeType> nodeTypeCounter;
    private final TypeCounter<EdgeType> edgeTypeCounter;

    private Builder(Function<Node<P, T, V>, N> pNodeCreationFunction) {

      nodeCreationFunction = pNodeCreationFunction;

      nodes = new ArrayList<>();
      graphNodes = new ArrayList<>();
      nodeMap = new HashMap<>();

      nodeTypeCounter = new TypeCounter<>(NodeType.values().length);
      edgeTypeCounter = new TypeCounter<>(EdgeType.values().length);
    }

    private GraphNode.MutableGraphNode<V, N> newGraphNode(NodeMapKey<P, T, V> pNodeKey) {
      Node<P, T, V> node = pNodeKey.createNode(nodes.size());
      return new GraphNode.MutableGraphNode<>(nodeCreationFunction.apply(node));
    }

    /**
     * Creates and inserts a {@link GraphNode} and {@link Node} for the specified parameters if such
     * a node does not already exist. In all cases it returns a graph node fitting the specified
     * parameters.
     */
    private GraphNode.MutableGraphNode<V, N> graphNode(
        NodeType pType, Optional<P> pProcedure, Optional<T> pStatement, Optional<V> pVariable) {

      NodeMapKey<P, T, V> nodeKey = new NodeMapKey<>(pType, pProcedure, pStatement, pVariable);
      GraphNode.MutableGraphNode<V, N> graphNode =
          nodeMap.computeIfAbsent(nodeKey, this::newGraphNode);
      N node = graphNode.getNode();

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
        GraphNode.MutableGraphNode<V, N> pPredecessor,
        GraphNode.MutableGraphNode<V, N> pSuccessor,
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
        GraphEdge<V, N> edge = new GraphEdge<>(pType, pPredecessor, pSuccessor);
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

    ImmutableList<N> getNodes() {
      return ImmutableList.copyOf(nodes);
    }

    void traverse(Collection<N> pStartNodes, ForwardsVisitor<N> pVisitor) {
      SystemDependenceGraph.traverse(graphNodes, pStartNodes, pVisitor, true);
    }

    void traverse(Collection<N> pStartNodes, BackwardsVisitor<N> pVisitor) {
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
    void insertActualSummaryEdges(N pFormalInNode, N pFormalOutNode) {

      Objects.requireNonNull(pFormalInNode, "pFormalInNode must not be null");
      Objects.requireNonNull(pFormalInNode, "pFormalOutNode must not be null");

      Preconditions.checkArgument(
          pFormalInNode.getType() == NodeType.FORMAL_IN,
          "pFormalInNode does not have type FORMAL_IN");
      Preconditions.checkArgument(
          pFormalOutNode.getType() == NodeType.FORMAL_OUT,
          "pFormalOutNode does not have type FORMAL_OUT");

      GraphNode<V, N> formalOutGraphNode = graphNodes.get(pFormalOutNode.getId());
      Preconditions.checkArgument(
          formalOutGraphNode.getNode().equals(pFormalOutNode),
          "pFormalOutNode does not belong to this SDG builder");

      for (GraphEdge<V, N> outEdge : formalOutGraphNode.getLeavingEdges()) {
        if (outEdge.getType() == EdgeType.PARAMETER_EDGE) {

          GraphNode.MutableGraphNode<V, N> actualOutGraphNode =
              (GraphNode.MutableGraphNode<V, N>) outEdge.getSuccessor();
          assert actualOutGraphNode.getNode().getType() == NodeType.ACTUAL_OUT;

          NodeMapKey<P, T, V> actualInNodeKey =
              new NodeMapKey<>(
                  NodeType.ACTUAL_IN,
                  actualOutGraphNode.getNode().getProcedure(),
                  actualOutGraphNode.getNode().getStatement(),
                  pFormalInNode.getVariable());
          GraphNode.MutableGraphNode<V, N> actualInGraphNode = nodeMap.get(actualInNodeKey);

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
    public SystemDependenceGraph<V, N> build() {

      ImmutableList.Builder<GraphNode.ImmutableGraphNode<V, N>> immutableGraphNodesBuilder =
          ImmutableList.builderWithExpectedSize(graphNodes.size());
      List<ImmutableList.Builder<GraphEdge<V, N>>> immutableEnteringEdges =
          new ArrayList<>(graphNodes.size());
      List<ImmutableList.Builder<GraphEdge<V, N>>> immutableLeavingEdges =
          new ArrayList<>(graphNodes.size());

      for (GraphNode.MutableGraphNode<V, N> mutableGraphNode : graphNodes) {
        immutableGraphNodesBuilder.add(
            new GraphNode.ImmutableGraphNode<>(
                mutableGraphNode.getNode(),
                ImmutableSet.copyOf(mutableGraphNode.getDefs()),
                ImmutableSet.copyOf(mutableGraphNode.getUses())));
        immutableEnteringEdges.add(
            ImmutableList.builderWithExpectedSize(mutableGraphNode.getEnteringEdgeCount()));
        immutableLeavingEdges.add(
            ImmutableList.builderWithExpectedSize(mutableGraphNode.getLeavingEdgeCount()));
      }

      ImmutableList<GraphNode.ImmutableGraphNode<V, N>> immutableGraphNodes =
          immutableGraphNodesBuilder.build();

      for (GraphNode.MutableGraphNode<V, N> mutableGraphNode : graphNodes) {
        int predecessorId = mutableGraphNode.getNode().getId();
        for (GraphEdge<V, N> graphEdge : mutableGraphNode.getLeavingEdges()) {
          int successorId = graphEdge.getSuccessor().getNode().getId();
          GraphEdge<V, N> immutableGraphEdge =
              new GraphEdge<>(
                  graphEdge.getType(),
                  immutableGraphNodes.get(predecessorId),
                  immutableGraphNodes.get(successorId));
          immutableLeavingEdges.get(predecessorId).add(immutableGraphEdge);
          immutableEnteringEdges.get(successorId).add(immutableGraphEdge);
        }
      }

      for (int index = 0; index < immutableGraphNodes.size(); index++) {
        GraphNode.ImmutableGraphNode<V, N> immutableGraphNode = immutableGraphNodes.get(index);
        immutableGraphNode.enteringEdges = immutableEnteringEdges.get(index).build();
        immutableGraphNode.leavingEdges = immutableLeavingEdges.get(index).build();
      }

      return new SystemDependenceGraph<>(
          ImmutableList.copyOf(nodes),
          immutableGraphNodes,
          nodeTypeCounter.copy(),
          edgeTypeCounter.copy());
    }

    /**
     * Chooser for an edge to insert into the system dependence graph. The successor of the edge has
     * already been selected and is known to the edge chooser.
     */
    public final class EdgeChooser {

      private final GraphNode.MutableGraphNode<V, N> graphNode;

      private EdgeChooser(GraphNode.MutableGraphNode<V, N> pGraphNode) {
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
      public N getNode() {
        return graphNode.getNode();
      }
    }

    /**
     * Chooser for edges to insert into the system dependence graph. The edge and its successor have
     * already been selected.
     */
    public final class DependencyChooser {

      private final GraphNode.MutableGraphNode<V, N> graphNode;
      private final EdgeType edgeType;
      private final Optional<V> cause;

      private DependencyChooser(
          GraphNode.MutableGraphNode<V, N> pGraphNode, EdgeType pEdgeType, Optional<V> pCause) {
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
   * @param <N> the node type of the SDG
   * @see VisitResult
   * @see SystemDependenceGraph#traverse(Collection, ForwardsVisitor)
   * @see SystemDependenceGraph#traverse(Collection, BackwardsVisitor)
   */
  public interface Visitor<N extends Node<?, ?, ?>> {

    /**
     * Accepts visited nodes during system dependence graph traversal.
     *
     * @param pNode the visited node
     * @return a {@link VisitResult} to guide the SDG traversal
     */
    VisitResult visitNode(N pNode);

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
    VisitResult visitEdge(EdgeType pType, N pPredecessor, N pSuccessor);
  }

  /**
   * Represents a {@link Visitor} that can be used for forward traversals of system dependence
   * graphs.
   *
   * @param <N> the node type of the SDG
   * @see SystemDependenceGraph#traverse(Collection, ForwardsVisitor)
   */
  public interface ForwardsVisitor<N extends Node<?, ?, ?>> extends Visitor<N> {}

  /**
   * Represents a {@link Visitor} that can be used for backward traversals of system dependence
   * graphs.
   *
   * @param <N> the node type of the SDG
   * @see SystemDependenceGraph#traverse(Collection, BackwardsVisitor)
   */
  public interface BackwardsVisitor<N extends Node<?, ?, ?>> extends Visitor<N> {}

  /**
   * Implementation of the visit-once-visitor. Extended by {@link ForwardsVisitOnceVisitor} and
   * {@link BackwardsVisitOnceVisitor}.
   */
  private abstract static class VisitOnceVisitor<N extends Node<?, ?, ?>> implements Visitor<N> {

    private final boolean forwards;
    private final Visitor<N> delegateVisitor;

    private final byte[] visited;
    private byte visitedMarker;

    private VisitOnceVisitor(boolean pForwards, Visitor<N> pDelegateVisitor, int pNodeCount) {

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

    private boolean isVisited(N pNode) {
      return visited[pNode.getId()] == visitedMarker;
    }

    @Override
    public VisitResult visitNode(N pNode) {

      if (!isVisited(pNode)) {

        visited[pNode.getId()] = visitedMarker;

        return delegateVisitor.visitNode(pNode);
      }

      return VisitResult.SKIP;
    }

    @Override
    public VisitResult visitEdge(EdgeType pType, N pPredecessor, N pSuccessor) {

      VisitResult visitResult = delegateVisitor.visitEdge(pType, pPredecessor, pSuccessor);

      if (visitResult == VisitResult.CONTINUE) {

        N nextNode = forwards ? pSuccessor : pPredecessor;

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
   * @param <N> the node type of the SDG
   * @see SystemDependenceGraph#createVisitOnceVisitor(ForwardsVisitor)
   * @see SystemDependenceGraph#traverse(Collection, ForwardsVisitor)
   */
  public static final class ForwardsVisitOnceVisitor<N extends Node<?, ?, ?>>
      extends VisitOnceVisitor<N> implements ForwardsVisitor<N> {

    ForwardsVisitOnceVisitor(ForwardsVisitor<N> pDelegateVisitor, int pNodeCount) {
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
    public VisitResult visitNode(N pNode) {
      return super.visitNode(pNode);
    }

    @Override
    public VisitResult visitEdge(EdgeType pType, N pPredecessor, N pSuccessor) {
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
   * @param <N> the node type of the SDG
   * @see SystemDependenceGraph#createVisitOnceVisitor(BackwardsVisitor)
   * @see SystemDependenceGraph#traverse(Collection, BackwardsVisitor)
   */
  public static final class BackwardsVisitOnceVisitor<N extends Node<?, ?, ?>>
      extends VisitOnceVisitor<N> implements BackwardsVisitor<N> {

    BackwardsVisitOnceVisitor(BackwardsVisitor<N> pDelegateVisitor, int pNodeCount) {
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
    public VisitResult visitNode(N pNode) {
      return super.visitNode(pNode);
    }

    @Override
    public VisitResult visitEdge(EdgeType pType, N pPredecessor, N pSuccessor) {
      return super.visitEdge(pType, pPredecessor, pSuccessor);
    }
  }
}
