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
import org.sosy_lab.cpachecker.util.sdg.traversal.BackwardsSdgVisitor;
import org.sosy_lab.cpachecker.util.sdg.traversal.ForwardsSdgVisitor;
import org.sosy_lab.cpachecker.util.sdg.traversal.SdgVisitResult;
import org.sosy_lab.cpachecker.util.sdg.traversal.SdgVisitor;
import org.sosy_lab.cpachecker.util.sdg.traversal.VisitOnceBackwardsSdgVisitor;
import org.sosy_lab.cpachecker.util.sdg.traversal.VisitOnceForwardsSdgVisitor;

/**
 * Represents a system dependence graph (SDG).
 *
 * <p>New SDG instances can be created using a {@link Builder}. New builder are created by calling
 * {@link #builder()}. Types for procedures, statements, and variables should be specified using the
 * respective type parameters.
 *
 * <p>SDGs are traversed by calling the methods {@link #traverse(Collection, ForwardsSdgVisitor)} or
 * {@link #traverse(Collection, BackwardsSdgVisitor)}.
 *
 * @param <V> The type of variables in this SDG. Variables are defined and used. Dependencies exist
 *     between defs and subsequent uses. Furthermore, formal-in/out and actual-in/out nodes exist
 *     for specific variables.
 * @param <N> The SDG node type of this SDG. The node type must be a subclass of {@link SdgNode} or
 *     {@link SdgNode} itself.
 * @param <E> The SDG edge type of this SDG. The edge type must be a subclass of {@link SdgEdge} or
 *     {@link SdgEdge} itself.
 */
public class SystemDependenceGraph<V, N extends SdgNode<?, ?, V>, E extends SdgEdge<V>> {

  // list of nodes where the node's index is equal to its id
  private final ImmutableList<N> nodes;
  // list of nodes where the graph node's index is equal to its id
  private final ImmutableList<GraphNode.ImmutableGraphNode<V, N, E>> graphNodes;

  // counters for nodes and edges per type
  private final TypeCounter<SdgNodeType> nodeTypeCounter;
  private final TypeCounter<SdgEdgeType> edgeTypeCounter;

  private SystemDependenceGraph(
      ImmutableList<N> pNodes,
      ImmutableList<GraphNode.ImmutableGraphNode<V, N, E>> pGraphNodes,
      TypeCounter<SdgNodeType> pNodeTypeCounter,
      TypeCounter<SdgEdgeType> pEdgeTypeCounter) {

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
  protected SystemDependenceGraph(SystemDependenceGraph<V, N, E> pSdg) {
    this(pSdg.nodes, pSdg.graphNodes, pSdg.nodeTypeCounter, pSdg.edgeTypeCounter);
  }

  private static <N extends SdgNode<?, ?, ?>> void throwExceptionForUnknownNode(N pNode) {
    throw new IllegalArgumentException("SystemDependenceGraph does not contain node: " + pNode);
  }

  /**
   * Gets the corresponding {@link GraphNode} for the specified {@link SdgNode}. Throws runtime
   * exception if the graph node does not exist or the specified node is {@code null}.
   */
  private static <V, N extends SdgNode<?, ?, V>, E extends SdgEdge<V>>
      GraphNode<V, N, E> getGraphNode(List<? extends GraphNode<V, N, E>> pGraphNodes, N pNode) {

    Objects.requireNonNull(pNode, "node must not be null");

    if (pNode.getId() >= pGraphNodes.size()) {
      throwExceptionForUnknownNode(pNode);
    }

    GraphNode<V, N, E> graphNode = pGraphNodes.get(pNode.getId());

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
  public static <V, N extends SdgNode<?, ?, V>, E extends SdgEdge<V>>
      SystemDependenceGraph<V, N, E> empty() {
    return new SystemDependenceGraph<>(
        ImmutableList.of(),
        ImmutableList.of(),
        new TypeCounter<>(SdgNodeType.values().length),
        new TypeCounter<>(SdgEdgeType.values().length));
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
  public static <P, T, V> Builder<P, T, V, SdgNode<P, T, V>, SdgEdge<V>> builder() {
    return new Builder<>(Function.identity(), Function.identity());
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
   * @param <N> the SDG node type for the SDG
   * @param <E> the SDG edge type for the SDG
   * @param pNodeCreationFunction function that transforms {@link SdgNode} instances to instances of
   *     {@code N}
   * @param pEdgeCreationFunction function that transforms {@link SdgEdge} instances to instances of
   *     {@code E}
   * @return a new SDG builder
   */
  public static <P, T, V, N extends SdgNode<P, T, V>, E extends SdgEdge<V>>
      Builder<P, T, V, N, E> builder(
          Function<SdgNode<P, T, V>, N> pNodeCreationFunction,
          Function<SdgEdge<V>, E> pEdgeCreationFunction) {
    return new Builder<>(pNodeCreationFunction, pEdgeCreationFunction);
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
   * Returns the number of nodes of the specified {@link SdgNodeType} contained in this system
   * dependence graph.
   *
   * @param pType the type to get the node count for
   * @return the number of nodes of the specified type in this SDG
   * @throws NullPointerException if {@code pType == null}
   */
  public final int getNodeCount(SdgNodeType pType) {

    Objects.requireNonNull(pType, "pType must not be null");

    return nodeTypeCounter.getCount(pType);
  }

  /**
   * Returns the number of edges of the specified {@link SdgEdgeType} in this system dependence
   * graph.
   *
   * @param pType the type to get the edge count for
   * @return the number of edges of the specified type in this SDG
   * @throws NullPointerException if {@code pType == null}
   */
  public final int getEdgeCount(SdgEdgeType pType) {

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
  private static <V, N extends SdgNode<?, ?, V>, E extends SdgEdge<V>> void traverse(
      List<? extends GraphNode<V, N, E>> pGraphNodes,
      Collection<N> pStartNodes,
      SdgVisitor<V, N, E> pVisitor,
      boolean pForwards) {

    Objects.requireNonNull(pStartNodes, "pStartNodes must not be null");
    Objects.requireNonNull(pVisitor, "pVisitor must not be null");

    Deque<GraphNode<V, N, E>> waitlist = new ArrayDeque<>();

    for (N node : pStartNodes) {
      waitlist.add(getGraphNode(pGraphNodes, node));
    }

    while (!waitlist.isEmpty()) {

      GraphNode<V, N, E> graphNode = waitlist.remove();
      SdgVisitResult nodeVisitResult = pVisitor.visitNode(graphNode.getNode());

      if (nodeVisitResult == SdgVisitResult.CONTINUE) {

        List<GraphEdge<V, N, E>> edges =
            pForwards ? graphNode.getLeavingEdges() : graphNode.getEnteringEdges();

        for (GraphEdge<V, N, E> edge : edges) {

          GraphNode<V, N, E> predecessor = edge.getPredecessor();
          GraphNode<V, N, E> successor = edge.getSuccessor();
          SdgVisitResult edgeVisitResult =
              pVisitor.visitEdge(edge.getEdge(), predecessor.getNode(), successor.getNode());

          if (edgeVisitResult == SdgVisitResult.CONTINUE) {

            GraphNode<V, N, E> next = pForwards ? successor : predecessor;
            waitlist.add(next);

          } else if (nodeVisitResult == SdgVisitResult.TERMINATE) {
            return;
          }
        }

      } else if (nodeVisitResult == SdgVisitResult.TERMINATE) {
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
   * #createVisitOnceVisitor(ForwardsSdgVisitor)}).
   *
   * @param pStartNodes the nodes to start the traversal at
   * @param pVisitor the visitor to inform about node and edge visits and to guide the traversal
   * @throws NullPointerException if {@code pVisitor == null}, if {@code pStartNodes == null}, or if
   *     any node contained in {@code pStartNodes} is {@code null}
   * @throws IllegalArgumentException if any of the nodes contained in {@code pStartNodes} does not
   *     belong to this SDG
   */
  public final void traverse(Collection<N> pStartNodes, ForwardsSdgVisitor<V, N, E> pVisitor) {
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
   * #createVisitOnceVisitor(BackwardsSdgVisitor)}).
   *
   * @param pStartNodes the nodes to start the traversal at
   * @param pVisitor the visitor to inform about node and edge visits and to guide the traversal
   * @throws NullPointerException if {@code pVisitor == null}, if {@code pStartNodes == null}, or if
   *     any node contained in {@code pStartNodes} is {@code null}
   * @throws IllegalArgumentException if any of the nodes contained in {@code pStartNodes} does not
   *     belong to this SDG
   */
  public final void traverse(Collection<N> pStartNodes, BackwardsSdgVisitor<V, N, E> pVisitor) {
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
  public final VisitOnceForwardsSdgVisitor<V, N, E> createVisitOnceVisitor(
      ForwardsSdgVisitor<V, N, E> pDelegateVisitor) {

    Objects.requireNonNull(pDelegateVisitor, "pDelegateVisitor must not be null");

    return new VisitOnceForwardsSdgVisitor<>(pDelegateVisitor, getNodeCount());
  }

  /**
   * Creates a new {@link VisitOnceBackwardsSdgVisitor} that wraps the specified visitor.
   *
   * <p>Node and edge visits are only delegated to the wrapped visitor if the visit-once-visitor has
   * not visited the node/edge yet. Visit results of the wrapped visitor are delegated during the
   * traversal.
   *
   * @param pDelegateVisitor the visitor to wrap
   * @return a new visit-once-visitor that wraps the specified visitor
   * @throws NullPointerException if {@code pDelegateVisitor == null}
   */
  public final VisitOnceBackwardsSdgVisitor<V, N, E> createVisitOnceVisitor(
      BackwardsSdgVisitor<V, N, E> pDelegateVisitor) {

    Objects.requireNonNull(pDelegateVisitor, "pDelegateVisitor must not be null");

    return new VisitOnceBackwardsSdgVisitor<>(pDelegateVisitor, getNodeCount());
  }

  /**
   * This class is used to represent a node in an SDG and its connection to other nodes via entering
   * and leaving edges. This class is private to the SDG class, use {@link SdgVisitor} for graph
   * traversals and {@link SdgNode} to refer to SDG nodes outside the SDG class.
   */
  private abstract static class GraphNode<V, N extends SdgNode<?, ?, V>, E extends SdgEdge<V>> {

    private final N node;

    private GraphNode(N pNode) {
      node = pNode;
    }

    final N getNode() {
      return node;
    }

    abstract List<GraphEdge<V, N, E>> getEnteringEdges();

    abstract List<GraphEdge<V, N, E>> getLeavingEdges();

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

      GraphNode<?, ?, ?> other = (GraphNode<?, ?, ?>) pObject;
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

    private static class MutableGraphNode<V, N extends SdgNode<?, ?, V>, E extends SdgEdge<V>>
        extends GraphNode<V, N, E> {

      private final List<GraphEdge<V, N, E>> enteringEdges;
      private final List<GraphEdge<V, N, E>> leavingEdges;

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
      List<GraphEdge<V, N, E>> getEnteringEdges() {
        return enteringEdges;
      }

      private boolean hasEnteringEdgeFrom(SdgEdgeType pType, GraphNode<V, N, E> pPredecessor) {

        for (GraphEdge<V, N, E> graphEdge : enteringEdges) {
          // identity comparison between graph nodes is intended here
          // inside a single SDG, equality can be determined by their identity
          if (graphEdge.getEdge().getType() == pType
              && graphEdge.getPredecessor() == pPredecessor) {
            return true;
          }
        }

        return false;
      }

      private void addEnteringEdge(GraphEdge<V, N, E> pEdge) {
        enteringEdges.add(pEdge);
      }

      private int getLeavingEdgeCount() {
        return leavingEdges.size();
      }

      @Override
      List<GraphEdge<V, N, E>> getLeavingEdges() {
        return leavingEdges;
      }

      private boolean hasLeavingEdgeTo(SdgEdgeType pType, GraphNode<V, N, E> pSuccessor) {

        for (GraphEdge<V, N, E> graphEdge : leavingEdges) {
          // identity comparison between graph nodes is intended here
          // inside a single SDG, equality can be determined by their identity
          if (graphEdge.getEdge().getType() == pType && graphEdge.getSuccessor() == pSuccessor) {
            return true;
          }
        }

        return false;
      }

      private void addLeavingEdge(GraphEdge<V, N, E> pEdge) {
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

    private static class ImmutableGraphNode<V, N extends SdgNode<?, ?, V>, E extends SdgEdge<V>>
        extends GraphNode<V, N, E> {

      private ImmutableList<GraphEdge<V, N, E>> enteringEdges;
      private ImmutableList<GraphEdge<V, N, E>> leavingEdges;

      private ImmutableSet<V> defs;
      private ImmutableSet<V> uses;

      private ImmutableGraphNode(N pNode, ImmutableSet<V> pDefs, ImmutableSet<V> pUses) {
        super(pNode);

        defs = pDefs;
        uses = pUses;
      }

      @Override
      ImmutableList<GraphEdge<V, N, E>> getEnteringEdges() {
        return enteringEdges;
      }

      @Override
      ImmutableList<GraphEdge<V, N, E>> getLeavingEdges() {
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
   * is private, use {@link SdgVisitor} for graph traversals.
   */
  private static final class GraphEdge<V, N extends SdgNode<?, ?, V>, E extends SdgEdge<V>> {

    private final E edge;

    private final GraphNode<V, N, E> predecessor;
    private final GraphNode<V, N, E> successor;

    private GraphEdge(E pEdge, GraphNode<V, N, E> pPredecessor, GraphNode<V, N, E> pSuccessor) {

      edge = pEdge;

      predecessor = pPredecessor;
      successor = pSuccessor;
    }

    private E getEdge() {
      return edge;
    }

    private GraphNode<V, N, E> getPredecessor() {
      return predecessor;
    }

    private GraphNode<V, N, E> getSuccessor() {
      return successor;
    }

    @Override
    public int hashCode() {
      return Objects.hash(edge, predecessor, successor);
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
      return Objects.equals(edge, other.edge)
          && Objects.equals(predecessor, other.predecessor)
          && Objects.equals(successor, other.successor);
    }

    @Override
    public String toString() {
      return String.format(
          Locale.ENGLISH,
          "%s[edge=%s, predecessor=%s, successor=%s]",
          getClass().getName(),
          edge,
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
  public static final class Builder<P, T, V, N extends SdgNode<P, T, V>, E extends SdgEdge<V>> {

    private final Function<SdgNode<P, T, V>, N> nodeCreationFunction;
    private final Function<SdgEdge<V>, E> edgeCreationFunction;

    // list of nodes where the node's index is equal to its id
    private final List<N> nodes;
    // list of nodes where the graph node's index is equal to its id
    private final List<GraphNode.MutableGraphNode<V, N, E>> graphNodes;
    private final Map<NodeMapKey<P, T, V>, GraphNode.MutableGraphNode<V, N, E>> nodeMap;

    private final TypeCounter<SdgNodeType> nodeTypeCounter;
    private final TypeCounter<SdgEdgeType> edgeTypeCounter;

    private Builder(
        Function<SdgNode<P, T, V>, N> pNodeCreationFunction,
        Function<SdgEdge<V>, E> pEdgeCreationFunction) {

      nodeCreationFunction = pNodeCreationFunction;
      edgeCreationFunction = pEdgeCreationFunction;

      nodes = new ArrayList<>();
      graphNodes = new ArrayList<>();
      nodeMap = new HashMap<>();

      nodeTypeCounter = new TypeCounter<>(SdgNodeType.values().length);
      edgeTypeCounter = new TypeCounter<>(SdgEdgeType.values().length);
    }

    private GraphNode.MutableGraphNode<V, N, E> newGraphNode(NodeMapKey<P, T, V> pNodeKey) {
      SdgNode<P, T, V> node = pNodeKey.createNode(nodes.size());
      return new GraphNode.MutableGraphNode<>(nodeCreationFunction.apply(node));
    }

    /**
     * Creates and inserts a {@link GraphNode} and {@link SdgNode} for the specified parameters if
     * such a node does not already exist. In all cases it returns a graph node fitting the
     * specified parameters.
     */
    private GraphNode.MutableGraphNode<V, N, E> graphNode(
        SdgNodeType pType, P pProcedure, Optional<T> pStatement, Optional<V> pVariable) {

      NodeMapKey<P, T, V> nodeKey = new NodeMapKey<>(pType, pProcedure, pStatement, pVariable);
      GraphNode.MutableGraphNode<V, N, E> graphNode =
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
        GraphNode.MutableGraphNode<V, N, E> pPredecessor,
        GraphNode.MutableGraphNode<V, N, E> pSuccessor,
        SdgEdgeType pType,
        Optional<V> pCause) {

      // FIXME using the type is not enough to distinguish edges, the cause must also be compared
      boolean insertEdge = true;
      // typically, only one of the edge counts is large, so this greatly improves performance
      if (pSuccessor.getEnteringEdgeCount() < pPredecessor.getLeavingEdgeCount()) {
        insertEdge = !pSuccessor.hasEnteringEdgeFrom(pType, pPredecessor);
      } else {
        insertEdge = !pPredecessor.hasLeavingEdgeTo(pType, pSuccessor);
      }

      if (insertEdge) {
        E edge = edgeCreationFunction.apply(SdgEdge.of(pType, pCause.orElse(null)));
        GraphEdge<V, N, E> graphEdge = new GraphEdge<>(edge, pPredecessor, pSuccessor);
        pPredecessor.addLeavingEdge(graphEdge);
        pSuccessor.addEnteringEdge(graphEdge);
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

    void traverse(Collection<N> pStartNodes, ForwardsSdgVisitor<V, N, E> pVisitor) {
      SystemDependenceGraph.traverse(graphNodes, pStartNodes, pVisitor, true);
    }

    void traverse(Collection<N> pStartNodes, BackwardsSdgVisitor<V, N, E> pVisitor) {
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
    int[] createIds(Function<SdgNode<P, T, V>, Optional<?>> pFunction) {

      Objects.requireNonNull(pFunction, "pFunction must not be null");

      Map<Object, Integer> resultMap = new HashMap<>();
      int[] ids = new int[nodes.size()];

      for (int nodeId = 0; nodeId < ids.length; nodeId++) {

        SdgNode<P, T, V> node = nodes.get(nodeId);
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
          pFormalInNode.getType() == SdgNodeType.FORMAL_IN,
          "pFormalInNode does not have type FORMAL_IN");
      Preconditions.checkArgument(
          pFormalOutNode.getType() == SdgNodeType.FORMAL_OUT,
          "pFormalOutNode does not have type FORMAL_OUT");

      GraphNode<V, N, E> formalOutGraphNode = graphNodes.get(pFormalOutNode.getId());
      Preconditions.checkArgument(
          formalOutGraphNode.getNode().equals(pFormalOutNode),
          "pFormalOutNode does not belong to this SDG builder");

      for (GraphEdge<V, N, E> outEdge : formalOutGraphNode.getLeavingEdges()) {
        if (outEdge.getEdge().hasType(SdgEdgeType.PARAMETER_EDGE)) {

          GraphNode.MutableGraphNode<V, N, E> actualOutGraphNode =
              (GraphNode.MutableGraphNode<V, N, E>) outEdge.getSuccessor();
          assert actualOutGraphNode.getNode().getType() == SdgNodeType.ACTUAL_OUT;

          NodeMapKey<P, T, V> actualInNodeKey =
              new NodeMapKey<>(
                  SdgNodeType.ACTUAL_IN,
                  actualOutGraphNode.getNode().getProcedure(),
                  actualOutGraphNode.getNode().getStatement(),
                  pFormalInNode.getVariable());
          GraphNode.MutableGraphNode<V, N, E> actualInGraphNode = nodeMap.get(actualInNodeKey);

          if (actualInGraphNode != null) {
            insertEdge(
                actualInGraphNode, actualOutGraphNode, SdgEdgeType.SUMMARY_EDGE, Optional.empty());
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
     * @param pProcedure the procedure of the node to insert
     * @param pStatement the, depending on the {@link SdgNodeType}, optional statement of the node
     *     to insert
     * @param pVariable the, depending on the {@link SdgNodeType}, optional variable of the node to
     *     insert
     * @return an edge chooser that can be used to insert a dependency
     * @throws NullPointerException if any of the parameters is {@code null}
     */
    public EdgeChooser node(
        SdgNodeType pType, P pProcedure, Optional<T> pStatement, Optional<V> pVariable) {

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
    public SystemDependenceGraph<V, N, E> build() {

      ImmutableList.Builder<GraphNode.ImmutableGraphNode<V, N, E>> immutableGraphNodesBuilder =
          ImmutableList.builderWithExpectedSize(graphNodes.size());
      List<ImmutableList.Builder<GraphEdge<V, N, E>>> immutableEnteringEdges =
          new ArrayList<>(graphNodes.size());
      List<ImmutableList.Builder<GraphEdge<V, N, E>>> immutableLeavingEdges =
          new ArrayList<>(graphNodes.size());

      for (GraphNode.MutableGraphNode<V, N, E> mutableGraphNode : graphNodes) {
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

      ImmutableList<GraphNode.ImmutableGraphNode<V, N, E>> immutableGraphNodes =
          immutableGraphNodesBuilder.build();

      for (GraphNode.MutableGraphNode<V, N, E> mutableGraphNode : graphNodes) {
        int predecessorId = mutableGraphNode.getNode().getId();
        for (GraphEdge<V, N, E> graphEdge : mutableGraphNode.getLeavingEdges()) {
          int successorId = graphEdge.getSuccessor().getNode().getId();
          GraphEdge<V, N, E> immutableGraphEdge =
              new GraphEdge<>(
                  graphEdge.getEdge(),
                  immutableGraphNodes.get(predecessorId),
                  immutableGraphNodes.get(successorId));
          immutableLeavingEdges.get(predecessorId).add(immutableGraphEdge);
          immutableEnteringEdges.get(successorId).add(immutableGraphEdge);
        }
      }

      for (int index = 0; index < immutableGraphNodes.size(); index++) {
        GraphNode.ImmutableGraphNode<V, N, E> immutableGraphNode = immutableGraphNodes.get(index);
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

      private final GraphNode.MutableGraphNode<V, N, E> graphNode;

      private EdgeChooser(GraphNode.MutableGraphNode<V, N, E> pGraphNode) {
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
      public DependencyChooser depends(SdgEdgeType pType, Optional<V> pCause) {

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

      private final GraphNode.MutableGraphNode<V, N, E> graphNode;
      private final SdgEdgeType edgeType;
      private final Optional<V> cause;

      private DependencyChooser(
          GraphNode.MutableGraphNode<V, N, E> pGraphNode,
          SdgEdgeType pEdgeType,
          Optional<V> pCause) {
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
       * @param pProcedure the procedure of the node to insert
       * @param pStatement the, depending on the {@link SdgNodeType}, optional statement of the node
       *     to insert
       * @param pVariable the, depending on the {@link SdgNodeType}, optional variable of the node
       *     to insert
       */
      public void on(
          SdgNodeType pType, P pProcedure, Optional<T> pStatement, Optional<V> pVariable) {

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

    private final SdgNodeType type;
    private final P procedure;
    private final Optional<T> statement;
    private final Optional<V> variable;

    // instances of this class are typically used as (hash-)map keys, so caching the hash can
    // improve performance
    private final int hash;

    private NodeMapKey(
        SdgNodeType pType, P pProcedure, Optional<T> pStatement, Optional<V> pVariable) {

      type = pType;
      procedure = pProcedure;
      statement = pStatement;
      variable = pVariable;

      hash = Objects.hash(type, procedure, statement, variable);
    }

    private SdgNode<P, T, V> createNode(int pId) {
      return new SdgNode<>(pId, type, procedure, statement, variable);
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

  /**
   * Used to count objects of a specific type. Used for {@link SdgNodeType} and {@link SdgEdgeType}.
   */
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
}
