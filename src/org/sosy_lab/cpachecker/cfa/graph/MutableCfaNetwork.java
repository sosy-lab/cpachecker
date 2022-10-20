// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.graph;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableNetwork;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * Represents a {@link CFA} as a {@link MutableNetwork}.
 *
 * <p>All connections between elements of a CFA (i.e., nodes and edges) are defined by a {@code
 * MutableCfaNetwork}. Depending on the implementation, the CFA represented by a {@link
 * MutableCfaNetwork} may differ from the CFA represented by its elements (e.g., {@link
 * CFAEdge#getSuccessor()} and {@link FlexCfaNetwork#successor(CFAEdge)} may not return the same
 * value). It's important to only use methods provided by a {@link MutableCfaNetwork}, if more than
 * a single CFA node and/or edge is involved.
 *
 * <p>For performance reasons, some expensive checks are only performed if Java assertions are
 * enabled. Even though this is bad practice in general, this is also the case for some
 * preconditions. E.g., for some implementations, checking whether a CFA node or edge given as
 * method argument belongs to the CFA represented by a {@link MutableCfaNetwork} can be quite
 * expensive.
 *
 * <p>All returned sets are unmodifiable views, so attempts to modify such a set will throw an
 * exception, but modifications to the CFA represented by a {@code MutableNetwork} will be reflected
 * in the set. Don't try to modify the CFA represented by a {@code MutableNetwork} while iterating
 * though such a view as correctness of the iteration cannot be guaranteed anymore.
 */
public interface MutableCfaNetwork extends CfaNetwork, MutableNetwork<CFANode, CFAEdge> {

  /**
   * Creates a new {@link MutableCfaNetwork} that forwards all method calls the to specified {@link
   * MutableCFA}.
   *
   * <p>All modifying operations on the returned {@link MutableCfaNetwork} change the specified
   * {@link MutableCFA}.
   *
   * <p>The CFA represented by the returned {@link MutableCfaNetwork} always matches the CFA
   * represented by its elements (e.g., {@link CFAEdge#getSuccessor()} and {@link
   * FlexCfaNetwork#successor(CFAEdge)} always return the same value). Endpoints of a CFA edge and
   * endpoints given as arguments to an {@code addEdge} method must match.
   *
   * <p>IMPORTANT: The specified CFA must not contain any parallel edges (i.e., edges that connect
   * the same nodes in the same order) and never add them in the future. Additionally, the set
   * returned by {@link CFA#getAllNodes()} must not contain any duplicates and never add them in the
   * future (if the CFA is mutable). Be aware that these requirements are not enforced, if Java
   * assertions are disabled.
   *
   * @param pMutableCfa the {@link MutableCFA} all method calls are forwarded to and represented by
   *     the returned {@link MutableCfaNetwork}
   * @return a new {@link MutableCfaNetwork} that forwards all calls the specified {@link
   *     MutableCFA}
   * @throws NullPointerException if {@code pMutableCfa == null}
   */
  public static MutableCfaNetwork wrap(MutableCFA pMutableCfa) {
    return WrappingMutableCfaNetwork.wrap(pMutableCfa);
  }

  /**
   * Adds the specified CFA edge between {@code pPredecessor} and {@code pSuccessor}.
   *
   * <p>{@code pNewEdge} must be unique to this network.
   *
   * <p>If either or both endpoints are not already present in this graph, this method will silently
   * {@link #addNode(CFANode) add} each missing endpoint to the graph.
   *
   * <p>Depending on the implementation, the specified endpoints must match the edge's {@link
   * CFAEdge#getPredecessor() predecessor} and {@link CFAEdge#getSuccessor() successor}.
   *
   * @param pPredecessor the edge's predecessor
   * @param pSuccessor the edge's successor
   * @param pNewEdge the edge to add to this network
   * @return {@code true} if the network was modified as a result of this call
   * @throws NullPointerException if any parameter is {@code null}
   * @throws IllegalArgumentException if introducing the edge would lead to parallel edges
   * @throws IllegalArgumentException if introducing the edge would lead to more than one summary
   *     edge leaving or entering a node
   */
  @Override
  boolean addEdge(CFANode pPredecessor, CFANode pSuccessor, CFAEdge pNewEdge);

  /**
   * Adds the specified CFA edge between the {@code pEndpoints}.
   *
   * <p>{@code pNewEdge} must be unique to this network.
   *
   * <p>If either or both endpoints are not already present in this graph, this method will silently
   * {@link #addNode(CFANode) add} each missing endpoint to the graph.
   *
   * <p>Depending on the implementation, the specified endpoints must match the edge's {@link
   * CFAEdge#getPredecessor() predecessor} and {@link CFAEdge#getSuccessor() successor}.
   *
   * @param pEndpoints the edge's endpoints
   * @param pNewEdge the edge to add to this network
   * @return {@code true} if the network was modified as a result of this call
   * @throws NullPointerException if any parameter is {@code null}
   * @throws IllegalArgumentException if the specified endpoints are unordered
   * @throws IllegalArgumentException if introducing the edge would lead to parallel edges
   * @throws IllegalArgumentException if introducing the edge would lead to more than one summary
   *     edge leaving or entering a node
   */
  @Override
  default boolean addEdge(EndpointPair<CFANode> pEndpoints, CFAEdge pNewEdge) {
    checkArgument(pEndpoints.isOrdered(), "endpoints must be ordered");
    return addEdge(pEndpoints.nodeU(), pEndpoints.nodeV(), pNewEdge);
  }

  /**
   * Adds the specified CFA edge between {@code pPredecessor} and {@code pSuccessor}.
   *
   * <p>{@code pNewEdge} must be unique to this network.
   *
   * <p>If either or both endpoints are not already present in this graph, this method will silently
   * {@link #addNode(CFANode) add} each missing endpoint to the graph.
   *
   * <p>Depending on the implementation, the specified endpoints must match the edge's {@link
   * CFAEdge#getPredecessor() predecessor} and {@link CFAEdge#getSuccessor() successor}.
   *
   * @param pPredecessor the edge's predecessor
   * @param pNewEdge the edge to add to this network
   * @param pSuccessor the edge's successor
   * @return {@code true} if the network was modified as a result of this call
   * @throws NullPointerException if any parameter is {@code null}
   * @throws IllegalArgumentException if introducing the edge would lead to parallel edges
   * @throws IllegalArgumentException if introducing the edge would lead to more than one summary
   *     edge leaving or entering a node
   */
  default boolean addEdge(CFANode pPredecessor, CFAEdge pNewEdge, CFANode pSuccessor) {
    return addEdge(pPredecessor, pSuccessor, pNewEdge);
  }

  /**
   * Adds the specified CFA edge between its {@link CFAEdge#getPredecessor() predecessor} and {@link
   * CFAEdge#getSuccessor() successor}.
   *
   * <p>{@code pNewEdge} must be unique to this network.
   *
   * <p>If either or both endpoints are not already present in this graph, this method will silently
   * {@link #addNode(CFANode) add} each missing endpoint to the graph.
   *
   * <p>Calling this method has the same effect as calling {@code addEdge(edge.getPredecessor(),
   * edge.getSuccessor(), edge)}.
   *
   * @param pNewEdge the edge to add to this network
   * @return {@code true} if the network was modified as a result of this call
   * @throws NullPointerException if {@code pNewEdge == null}
   * @throws IllegalArgumentException if introducing the edge would lead to parallel edges
   * @throws IllegalArgumentException if introducing the edge would lead to more than one summary
   *     edge leaving or entering a node
   */
  default boolean addEdge(CFAEdge pNewEdge) {
    return addEdge(pNewEdge.getPredecessor(), pNewEdge.getSuccessor(), pNewEdge);
  }
}
