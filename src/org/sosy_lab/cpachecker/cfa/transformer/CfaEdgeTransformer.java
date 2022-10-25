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
 * A {@link CfaEdgeTransformer} returns for a given CFA edge (the original edge) a new edge that may
 * differ from the original edge.
 *
 * <p>A {@link CfaEdgeTransformer} must always create a new edge every time its {@link
 * CfaEdgeTransformer#transform(CFAEdge, CfaNetwork, CfaNodeProvider, CfaEdgeProvider) transform
 * method} is called.
 */
@FunctionalInterface
public interface CfaEdgeTransformer {

  /**
   * Returns a new transformed CFA edge for the specified CFA edge.
   *
   * <p>Every time this method is called, a new transformed edge is created.
   *
   * @param pEdge the CFA edge to get a new transformed edge for
   * @param pCfaNetwork The {@link CfaNetwork} the specified edge is a part of. The {@link
   *     CfaNetwork} is used to determine connections between nodes and edges (e.g., the endpoints
   *     of an edge).
   * @param pNodeProvider The creation of CFA edges always depends on some nodes (e.g., the
   *     endpoints of an edge). The specified {@link CfaNodeProvider} resolves those dependencies.
   *     Given the nodes the specified edge depends on, return the corresponding transformed nodes.
   * @param pEdgeProvider The creation of some edges depends on other edges (e.g, we need to know
   *     the {@link FunctionSummaryEdge} to create a {@link FunctionCallEdge}). The specified {@link
   *     CfaEdgeProvider} resolves those dependencies. Given the edges the specified edge depends
   *     on, return the corresponding transformed edges. If the construction of a transformed edge
   *     doesn't depend on any other edges, it's guaranteed that the specified provider is not used,
   *     so a dummy provider can be specified.
   * @return a new transformed CFA edge for the specified CFA edge (must not return {@code null})
   * @throws NullPointerException if any parameter is {@code null}
   */
  CFAEdge transform(
      CFAEdge pEdge,
      CfaNetwork pCfaNetwork,
      CfaNodeProvider pNodeProvider,
      CfaEdgeProvider pEdgeProvider);
}
