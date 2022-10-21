// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.graph;

import com.google.common.graph.MutableNetwork;
import com.google.common.graph.NetworkBuilder;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * A flexible {@link MutableCfaNetwork} that provides more advanced operations for modifying a CFA
 * that wouldn't be possible for all {@link MutableCfaNetwork} implementations.
 *
 * <p>In order to provide more advanced operations, a {@link FlexCfaNetwork} always operated on its
 * own copy of a CFA or {@link CfaNetwork}. The original CFA or {@link CfaNetwork} is never modified
 * by any operations on the {@link FlexCfaNetwork}.
 *
 * <p>All connections between elements of a CFA (i.e., nodes and edges) are defined by a {@link
 * FlexCfaNetwork}. Depending on the implementation, the CFA represented by a {@link FlexCfaNetwork}
 * may differ from the CFA represented by its elements (e.g., {@link CFAEdge#getSuccessor()} and
 * {@link FlexCfaNetwork#successor(CFAEdge)} may not return the same value). It's important to only
 * use methods provided by a {@link FlexCfaNetwork} if more than a single CFA node and/or edge is
 * involved.
 *
 * <p>All returned sets are unmodifiable views, so modifications attempts throw an exception.
 * However, modifications to a {@link FlexCfaNetwork} will be reflected in its returned set views. A
 * {@link FlexCfaNetwork} must not be modified while any of its set view are iterated, as this might
 * lead to incorrect iterations.
 */
public interface FlexCfaNetwork extends MutableCfaNetwork {

  /**
   * Returns a new {@link FlexCfaNetwork} that is a copy of the specified {@link CfaNetwork}.
   *
   * <p>The specified {@link CfaNetwork} is never modified by any operations on the {@link
   * FlexCfaNetwork}.
   *
   * @param pCfaNetwork the {@link CfaNetwork} to create a {@link FlexCfaNetwork} for
   * @return a new {@link FlexCfaNetwork} that is a copy of the specified {@link CfaNetwork}
   * @throws NullPointerException if {@code pCfaNetwork == null}
   */
  public static FlexCfaNetwork copy(CfaNetwork pCfaNetwork) {

    MutableNetwork<CFANode, CFAEdge> mutableNetwork =
        NetworkBuilder.directed().allowsSelfLoops(true).build();

    pCfaNetwork.nodes().forEach(mutableNetwork::addNode);

    for (CFAEdge edge : pCfaNetwork.edges()) {
      CFANode predecessor = pCfaNetwork.predecessor(edge);
      CFANode successor = pCfaNetwork.successor(edge);
      mutableNetwork.addEdge(predecessor, successor, edge);
    }

    return ForwardingFlexCfaNetwork.of(mutableNetwork);
  }

  /**
   * Returns a new {@link FlexCfaNetwork} that represents a copy of the specified CFA.
   *
   * <p>The specified CFA is never modified by any operations on the {@link FlexCfaNetwork}.
   *
   * @param pCfa the CFA to create a {@link FlexCfaNetwork} for
   * @return a new {@link FlexCfaNetwork} that represents a copy of the specified CFA
   * @throws NullPointerException if {@code pCfa == null}
   */
  public static FlexCfaNetwork copy(CFA pCfa) {
    return copy(CfaNetwork.wrap(pCfa));
  }

  /**
   * Inserts a new predecessor node, a new edge connecting it to the specified node, and reconnects
   * existing in-edges accordingly.
   *
   * <p>The operation is best described by the following diagram:
   *
   * <pre>{@code
   * Before:
   * --- a ---> [pNode] --- b ---->
   *
   * After:
   * --- a ---> [pNewPredecessor] --- pNewInEdge ---> [pNode] --- b ---->
   *
   * }</pre>
   *
   * @throws NullPointerException if any parameter is {@code null}
   * @throws IllegalArgumentException if {@code pNode} isn't part the the CFA represented by this
   *     {@link FlexCfaNetwork}
   * @throws IllegalArgumentException if {@code pNewPredecessor} or {@code pNewInEdge} already exist
   *     in this {@link FlexCfaNetwork}
   */
  void insertPredecessor(CFANode pNewPredecessor, CFAEdge pNewInEdge, CFANode pNode);

  /**
   * Inserts a new successor node, a new edge connecting it to the specified node, and reconnects
   * existing out-edges accordingly.
   *
   * <p>The operation is best described by the following diagram:
   *
   * <pre>{@code
   * Before:
   * --- a ---> [pNode] --- b ---->
   *
   * After:
   * --- a ---> [pNode] --- pNewOutEdge ---> [pNewSuccessor] --- b ---->
   *
   * }</pre>
   *
   * @throws NullPointerException if any parameter is {@code null}
   * @throws IllegalArgumentException if {@code pEdge} isn't part the the CFA represented by this
   *     {@link FlexCfaNetwork}
   * @throws IllegalArgumentException if {@code pNewSuccessor} or {@code pNewOutEdge} already exist
   *     in this {@link FlexCfaNetwork}
   */
  void insertSuccessor(CFANode pNode, CFAEdge pNewOutEdge, CFANode pNewSuccessor);

  /**
   * Replaces the specified node with a new node.
   *
   * <p>The operation is best described by the following diagram:
   *
   * <pre>{@code
   * Before:
   * --- a ---> [pNode] --- b ---->
   *
   * After:
   * --- a ---> [pNewNode] --- b ---->
   *
   * }</pre>
   *
   * @throws NullPointerException if any parameter is {@code null}
   * @throws IllegalArgumentException if {@code pEdge} isn't part the the CFA represented by this
   *     {@link FlexCfaNetwork}
   * @throws IllegalArgumentException if {@code pNewNode} already exists in this {@link
   *     FlexCfaNetwork}
   */
  void replaceNode(CFANode pNode, CFANode pNewNode);

  /**
   * Replaces the specified edge with a new edge.
   *
   * <p>The operation is best described by the following diagram:
   *
   * <pre>{@code
   * Before:
   * --- a ---> [X] --- pEdge ---> [Y] --- b ---->
   *
   * After:
   * --- a ---> [X] --- pNewEdge ---> [Y] --- b ---->
   *
   * }</pre>
   *
   * @throws NullPointerException if any parameter is {@code null}
   * @throws IllegalArgumentException if {@code pEdge} isn't part the the CFA represented by this
   *     {@link FlexCfaNetwork}
   * @throws IllegalArgumentException if {@code pNewEdge} already exists in this {@link
   *     FlexCfaNetwork}
   */
  void replaceEdge(CFAEdge pEdge, CFAEdge pNewEdge);
}
