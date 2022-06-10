// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.sdg;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import org.sosy_lab.cpachecker.util.sdg.traversal.BackwardsSdgVisitor;
import org.sosy_lab.cpachecker.util.sdg.traversal.ForwardsSdgVisitor;

/**
 * Builder for system dependence graphs. Instances of a builder can only be used once. It's not
 * possible to build multiple SDGs with one and the same builder. Calling {@link #build()} finishes
 * SDG construction.
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
public interface SdgBuilder<P, T, V, N extends SdgNode<P, T, V>, E extends SdgEdge<V>> {

  int getNodeCount();

  ImmutableList<N> getNodes();

  void traverse(Collection<N> pStartNodes, ForwardsSdgVisitor<V, N, E> pVisitor);

  void traverse(Collection<N> pStartNodes, BackwardsSdgVisitor<V, N, E> pVisitor);

  /**
   * Returns an array that contains a generated id, determined by the specified function, for every
   * node ({@code index == node.getId()}) contained in this SDG builder.
   *
   * <p>If two nodes return the same (uses {@code equals}) non-empty function result, they get the
   * same generated id in the returned array. If the function result is empty (see {@link
   * Optional#empty}), the generated id is {@code -1}.
   *
   * @param pFunction the function to map nodes to optional values
   * @return an array that contains the generated ids for every node
   * @throws NullPointerException if {@code pFunction == null}
   */
  int[] createIds(Function<SdgNode<P, T, V>, Optional<?>> pFunction);

  /**
   * Inserts summary edges between actual-in/out nodes for the specified formal-in/out nodes.
   *
   * <p>All actual-in/out nodes connected to the specified formal-in/out nodes via parameter edges
   * are considered. Only summary edges between actual-in/out nodes of the same calling context are
   * inserted.
   *
   * @param pFormalInNode the formal-in node that the formal-out node depends on
   * @param pFormalOutNode the formal-out node that depends on the formal-in node
   * @throws NullPointerException if any of the parameters is {@code null}
   * @throws IllegalArgumentException if {@code pFormalInNode.getType() != NodeType.FORMAL_IN}, or
   *     {@code pFormalOutNode.getType() != NodeType.FORMAL_OUT}, or {@code pFormalOutNode} does not
   *     belong to this SDG builder
   */
  void insertActualSummaryEdges(N pFormalInNode, N pFormalOutNode);

  /**
   * Returns the finished system dependence graph created by this builder.
   *
   * <p>The returned SDG contains all nodes an edges that were previously inserted into this
   * builder. This builder cannot be used anymore, after calling this method.
   *
   * @return the finished SDG created by this builder
   */
  AbstractSystemDependenceGraph<V, N, E> build();
}
