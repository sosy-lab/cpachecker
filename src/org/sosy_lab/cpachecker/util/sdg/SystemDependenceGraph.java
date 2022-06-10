// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.sdg;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import org.sosy_lab.cpachecker.util.sdg.traversal.BackwardsSdgVisitor;
import org.sosy_lab.cpachecker.util.sdg.traversal.ForwardsSdgVisitor;
import org.sosy_lab.cpachecker.util.sdg.traversal.VisitOnceBackwardsSdgVisitor;
import org.sosy_lab.cpachecker.util.sdg.traversal.VisitOnceForwardsSdgVisitor;

/**
 * Represents a system dependence graph (SDG).
 *
 * <p>New SDG instances can be created using a {@link AbstractSystemDependenceGraph.Builder}. New
 * builder are created by calling {@link AbstractSystemDependenceGraph#builder()}. Types for
 * procedures, statements, and variables should be specified using the respective type parameters.
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
public interface SystemDependenceGraph<V, N extends SdgNode<?, ?, V>, E extends SdgEdge<V>> {

  /**
   * Returns the number of nodes contained in this system dependence graph.
   *
   * @return the number of nodes in this SDG
   */
  int getNodeCount();

  /**
   * Returns the number of nodes of the specified {@link SdgNodeType} contained in this system
   * dependence graph.
   *
   * @param pType the type to get the node count for
   * @return the number of nodes of the specified type in this SDG
   * @throws NullPointerException if {@code pType == null}
   */
  int getNodeCount(SdgNodeType pType);

  /**
   * Returns the number of edges of the specified {@link SdgEdgeType} in this system dependence
   * graph.
   *
   * @param pType the type to get the edge count for
   * @return the number of edges of the specified type in this SDG
   * @throws NullPointerException if {@code pType == null}
   */
  int getEdgeCount(SdgEdgeType pType);

  /**
   * Returns a collection consisting of all nodes contained in this system dependence graph.
   *
   * @return an immutable collection of all nodes in this SDG
   */
  ImmutableCollection<N> getNodes();

  /**
   * Returns the node with the specified id in this system dependence graph.
   *
   * <p>The SDG contains a node for every id, if {@code id >= 0 && pId < getNodeCount()}.
   *
   * @param pId the id to get the node for
   * @return the node with the specified id in this SDG
   * @throws IllegalArgumentException if {@code pId < 0 || pId >= getNodeCount()}
   */
  N getNodeById(int pId);

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
  ImmutableSet<V> getDefs(N pNode);

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
  ImmutableSet<V> getUses(N pNode);

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
  void traverse(Collection<N> pStartNodes, ForwardsSdgVisitor<V, N, E> pVisitor);

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
  void traverse(Collection<N> pStartNodes, BackwardsSdgVisitor<V, N, E> pVisitor);

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
  VisitOnceForwardsSdgVisitor<V, N, E> createVisitOnceVisitor(
      ForwardsSdgVisitor<V, N, E> pDelegateVisitor);

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
  VisitOnceBackwardsSdgVisitor<V, N, E> createVisitOnceVisitor(
      BackwardsSdgVisitor<V, N, E> pDelegateVisitor);
}
