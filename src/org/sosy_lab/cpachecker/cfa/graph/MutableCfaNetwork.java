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

public interface MutableCfaNetwork extends CfaNetwork, MutableNetwork<CFANode, CFAEdge> {

  public static MutableCfaNetwork wrap(MutableCFA pMutableCfa) {
    return new SimpleMutableCfaNetwork(pMutableCfa);
  }

  public static MutableCfaNetwork createOverlay(CfaNetwork pCfaNetwork) {
    return OverlayCfaNetwork.of(pCfaNetwork);
  }

  public static MutableCfaNetwork createOverlay(CFA pCfa) {
    return OverlayCfaNetwork.of(CfaNetwork.wrap(pCfa));
  }

  @Override
  public boolean addEdge(EndpointPair<CFANode> pEndpoints, CFAEdge pEdge);

  /**
   * Adds the specified CFA edge between its predecessor ({@link CFAEdge#getPredecessor()}) and
   * successor ({@link CFAEdge#getSuccessor()}) to this network.
   *
   * <p>Calling this method has the same effect as calling {@code addEdge(edge.getPredecessor(),
   * edge.getSuccessor(), edge)}.
   *
   * @param pEdge the edge to add to this network
   * @return {@code true} if the network was modified as a result of this call
   * @throws IllegalArgumentException if introducing the edge would lead to parallel edges
   * @throws IllegalArgumentException if introducing the edge would lead to more than one summary
   *     edge leaving or entering a node
   */
  default boolean addEdge(CFAEdge pEdge) {
    return addEdge(pEdge.getPredecessor(), pEdge.getSuccessor(), pEdge);
  }
}
