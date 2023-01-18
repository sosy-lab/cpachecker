// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.graph;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableNetwork;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * Represents a {@link CFA} as a {@link MutableNetwork}.
 *
 * <p>A {@link MutableCfaNetwork} is a {@link CfaNetwork} that also provides basic operations for
 * modifying CFAs, like adding and removing nodes/edges.
 *
 * <p>All connections between elements of a CFA (i.e., nodes and edges) are defined by a {@link
 * MutableCfaNetwork}. Depending on the implementation, the CFA represented by a {@link
 * MutableCfaNetwork} may differ from the CFA represented by its elements (e.g., {@link
 * CFAEdge#getSuccessor()} and {@link MutableCfaNetwork#successor(CFAEdge)} may not return the same
 * value). It's important to only use methods provided by {@link MutableCfaNetwork} if more than a
 * single CFA node and/or edge is involved.
 *
 * <p>For performance reasons, some expensive checks are only performed if Java assertions are
 * enabled. Even though this is bad practice in general, this is also the case for some
 * preconditions. E.g., for some implementations, checking whether a CFA node or edge actually
 * belongs to a {@link MutableCfaNetwork} can be quite expensive, so this isn't necessarily checked
 * for all method arguments if Java assertions are disabled.
 *
 * <p>All returned sets are unmodifiable views, so modification attempts throw an exception.
 * However, modifications to a {@link MutableCfaNetwork} will be reflected in its returned set
 * views. A {@link MutableCfaNetwork} must not be modified while any of its set view are iterated,
 * as this might lead to incorrect iterations.
 */
public interface MutableCfaNetwork extends CfaNetwork, MutableNetwork<CFANode, CFAEdge> {

  /**
   * Creates a new {@link MutableCfaNetwork} that represents the specified {@link MutableCFA}.
   *
   * <p>All modifying operations on the returned {@link MutableCfaNetwork} change the specified
   * {@link MutableCFA}.
   *
   * <p>All changes to the specified CFA are reflected in the returned {@link MutableCfaNetwork}.
   * The CFA represented by the returned {@link MutableCfaNetwork} always matches the CFA
   * represented by its elements (e.g., {@link CFAEdge#getSuccessor()} and {@link
   * MutableCfaNetwork#successor(CFAEdge)} always return the same value). Endpoints of a CFA edge
   * and endpoints given as arguments to an {@code addEdge} method must match.
   *
   * <p>IMPORTANT: The specified CFA must not contain any parallel edges (i.e., edges that connect
   * the same nodes in the same order) and never add them in the future. Additionally, the
   * collections returned by {@link CFA#getAllNodes()} and {@link CFA#getAllFunctionHeads()} must
   * not contain any duplicates and never add them in the future. Be aware that these requirements
   * are not enforced if Java assertions are disabled.
   *
   * @param pMutableCfa the {@link MutableCFA} to create a {@link MutableCfaNetwork} for
   * @return a new {@link MutableCfaNetwork} that represents the specified {@link MutableCFA}
   * @throws NullPointerException if {@code pMutableCfa == null}
   */
  public static MutableCfaNetwork wrap(MutableCFA pMutableCfa) {
    return WrappingMutableCfaNetwork.wrap(pMutableCfa);
  }

  /**
   * Adds the specified CFA edge between {@code pPredecessor} and {@code pSuccessor}.
   *
   * <p>{@code pNewEdge} must be unique to this {@link MutableCfaNetwork}.
   *
   * <p>If either or both specified endpoints are not already present in this {@link
   * MutableCfaNetwork}, this method will silently {@link MutableCfaNetwork#addNode(CFANode) add}
   * each missing endpoint to the {@link MutableCfaNetwork}.
   *
   * <p>Depending on the implementation, the specified endpoints must match the edge's {@link
   * CFAEdge#getPredecessor() predecessor} and {@link CFAEdge#getSuccessor() successor}.
   *
   * @param pPredecessor the edge's predecessor in this {@link MutableCfaNetwork}
   * @param pSuccessor the edge's successor in this {@link MutableCfaNetwork}
   * @param pNewEdge the edge to add to this {@link MutableCfaNetwork}
   * @return {@code true} if this {@link MutableCfaNetwork} was modified as a result of this call
   * @throws NullPointerException if any parameter is {@code null}
   * @throws IllegalArgumentException if introducing the edge would lead to parallel edges
   * @throws IllegalArgumentException if introducing the edge would lead to more than one summary
   *     edge leaving or entering a node
   */
  @Override
  boolean addEdge(CFANode pPredecessor, CFANode pSuccessor, CFAEdge pNewEdge);

  /**
   * Adds the specified CFA edge between the specified endpoints.
   *
   * <p>{@code pNewEdge} must be unique to this {@link MutableCfaNetwork}.
   *
   * <p>If either or both specified endpoints are not already present in this {@link
   * MutableCfaNetwork}, this method will silently {@link MutableCfaNetwork#addNode(CFANode) add}
   * each missing endpoint to the {@link MutableCfaNetwork}.
   *
   * <p>Depending on the implementation, the specified endpoints must match the edge's {@link
   * CFAEdge#getPredecessor() predecessor} and {@link CFAEdge#getSuccessor() successor}.
   *
   * @param pEndpoints the edge's endpoints in this {@link MutableCfaNetwork}
   * @param pNewEdge the edge to add to this {@link MutableCfaNetwork}
   * @return {@code true} if this {@link MutableCfaNetwork} was modified as a result of this call
   * @throws NullPointerException if any parameter is {@code null}
   * @throws IllegalArgumentException if the specified endpoints are unordered
   * @throws IllegalArgumentException if introducing the edge would lead to parallel edges
   * @throws IllegalArgumentException if introducing the edge would lead to more than one summary
   *     edge leaving or entering a node
   */
  @Override
  boolean addEdge(EndpointPair<CFANode> pEndpoints, CFAEdge pNewEdge);

  /**
   * Adds the specified CFA edge between {@code pPredecessor} and {@code pSuccessor}.
   *
   * <p>{@code pNewEdge} must be unique to this {@link MutableCfaNetwork}.
   *
   * <p>If either or both specified endpoints are not already present in this {@link
   * MutableCfaNetwork}, this method will silently {@link MutableCfaNetwork#addNode(CFANode) add}
   * each missing endpoint to the {@link MutableCfaNetwork}.
   *
   * <p>Depending on the implementation, the specified endpoints must match the edge's {@link
   * CFAEdge#getPredecessor() predecessor} and {@link CFAEdge#getSuccessor() successor}.
   *
   * @param pPredecessor the edge's predecessor in this {@link MutableCfaNetwork}
   * @param pNewEdge the edge to add to this {@link MutableCfaNetwork}
   * @param pSuccessor the edge's successor in this {@link MutableCfaNetwork}
   * @return {@code true} if this {@link MutableCfaNetwork} was modified as a result of this call
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
   * <p>{@code pNewEdge} must be unique to this {@link MutableCfaNetwork}.
   *
   * <p>If either or both endpoints are not already present in this {@link MutableCfaNetwork}, this
   * method will silently {@link MutableCfaNetwork#addNode(CFANode) add} each missing endpoint to
   * the {@link MutableCfaNetwork}.
   *
   * <p>Calling this method has the same effect as calling {@code addEdge(edge.getPredecessor(),
   * edge.getSuccessor(), edge)}.
   *
   * @param pNewEdge the edge to add to this {@link MutableCfaNetwork}
   * @return {@code true} if this {@link MutableCfaNetwork} was modified as a result of this call
   * @throws NullPointerException if {@code pNewEdge == null}
   * @throws IllegalArgumentException if introducing the edge would lead to parallel edges
   * @throws IllegalArgumentException if introducing the edge would lead to more than one summary
   *     edge leaving or entering a node
   */
  default boolean addEdge(CFAEdge pNewEdge) {
    return addEdge(pNewEdge.getPredecessor(), pNewEdge.getSuccessor(), pNewEdge);
  }
}
