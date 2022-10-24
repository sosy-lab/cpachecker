// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformer;

import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;

/**
 * A {@code CfaEdgeTransformer} instance returns a transformed CFA edge for a specified CFA edge.
 *
 * <p>To implement a {@code CfaEdgeTransformer}, an implementation for {@link
 * CfaEdgeTransformer#transform(CFAEdge, CfaNetwork, CfaNodeProvider, CfaEdgeProvider)} must be
 * provided. The implementation must guarantee that every time the method is called, a new
 * transformed edge instance is created.
 */
@FunctionalInterface
public interface CfaEdgeTransformer {

  /**
   * Returns a transformed CFA edge for the specified CFA edge.
   *
   * <p>Every time this method is called, a new transformed edge instance is created.
   *
   * @param pEdge the CFA edge to get a transformed edge for
   * @param pCfa The CFA the specified edge is a part of. The CFA is used to determine other nodes
   *     and edges that are required for successful edge transformation (e.g., if the {@link
   *     FunctionSummaryEdge} of a {@link FunctionCallEdge} is required, it's determined using the
   *     specified CFA, but also an edge's predecessor and successor are determined using the
   *     specified CFA).
   * @param pNodeProvider The construction of a transformed edge requires transformed nodes, for
   *     which the specified node provider is used (e.g., the construction of a transformed edge
   *     requires transformed predecessor and successor nodes, so the predecessor and successor of
   *     the edge are determined using the specified CFA and the transformed predecessor and
   *     successor nodes are retrieved using the specified node provider).
   * @param pEdgeProvider If the construction of a transformed edge requires other transformed
   *     edges, the specified edge provider is used (e.g., the construction of a transformed {@code
   *     FunctionCallEdge} requires a transformed {@code FunctionSummaryEdge}, so the {@code
   *     FunctionSummaryEdge} of the {@code FunctionCallEdge} is determined using the specified CFA
   *     and the transformed {@code FunctionSummaryEdge} is retrieved using the specified edge
   *     provider). If the construction of a transformed edge does not require other edges, it's
   *     guaranteed that the specified edge provider is not used, so a dummy provider can be used.
   * @return a transformed CFA edge for the specified CFA edge
   * @throws NullPointerException if any parameter is {@code null}
   */
  CFAEdge transform(
      CFAEdge pEdge, CfaNetwork pCfa, CfaNodeProvider pNodeProvider, CfaEdgeProvider pEdgeProvider);
}
