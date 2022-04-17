// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.graph;

import com.google.common.graph.MutableNetwork;
import com.google.common.graph.NetworkBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.graph.ForwardingMutableNetwork;
import org.sosy_lab.cpachecker.util.graph.Graphs;

/**
 * An {@code OverlayCfaNetwork} is a {@link CfaNetwork} that is layered on top of another {@code
 * CfaNetwork}. Connections between nodes and edges of an overlay can be independently modified
 * without any changes to the underlying network. This is also why the CFA represented by an overlay
 * and the CFA represented by actual CFA nodes and edges may differ.
 */
public final class OverlayCfaNetwork extends ForwardingMutableNetwork<CFANode, CFAEdge>
    implements CfaNetwork {

  private OverlayCfaNetwork(MutableNetwork<CFANode, CFAEdge> pDelegate) {
    super(pDelegate);
  }

  /**
   * Returns a new mutable {@code OverlayCfaNetwork} instance for the specified underlying {@link
   * CfaNetwork}.
   *
   * @param pCfaNetwork the {@code CfaNetwork} to use as underlay (without modifications, the
   *     overlay is equal to the underlay)
   * @return a new mutable {@code OverlayCfaNetwork} instance for the specified underlying {@code
   *     CfaNetwork}
   */
  public static OverlayCfaNetwork of(CfaNetwork pCfaNetwork) {

    MutableNetwork<CFANode, CFAEdge> mutableNetwork =
        NetworkBuilder.directed().allowsSelfLoops(true).build();

    pCfaNetwork.nodes().forEach(mutableNetwork::addNode);
    for (CFAEdge cfaEdge : pCfaNetwork.edges()) {
      CFANode predecessor = pCfaNetwork.predecessor(cfaEdge);
      CFANode successor = pCfaNetwork.successor(cfaEdge);
      mutableNetwork.addEdge(predecessor, successor, cfaEdge);
    }

    return new OverlayCfaNetwork(mutableNetwork);
  }

  /**
   *
   *
   * <pre>{@code
   * Before:
   * --- a ---> [pNode] --- b ---->
   *
   * After:
   * --- a ---> [pNewPredecessor] --- pNewInEdge ---> [pNode] --- b ---->
   *
   * }</pre>
   */
  public void insertPredecessor(CFANode pNewPredecessor, CFANode pNode, CFAEdge pNewInEdge) {
    Graphs.insertPredecessor(this, pNewPredecessor, pNode, pNewInEdge);
  }

  /**
   *
   *
   * <pre>{@code
   * Before:
   * --- a ---> [pNode] --- b ---->
   *
   * After:
   * --- a ---> [pNode] --- pNewOutEdge ---> [pNewSuccessor] --- b ---->
   *
   * }</pre>
   */
  public void insertSuccessor(CFANode pNode, CFANode pNewSuccessor, CFAEdge pNewOutEdge) {
    Graphs.insertSuccessor(this, pNode, pNewSuccessor, pNewOutEdge);
  }

  /**
   * Replaces a CFA node with a different CFA node in this mutable network.
   *
   * <pre>{@code
   * Before:
   * --- a ---> [pNode] --- b ---->
   *
   * After:
   * --- a ---> [pNewNode] --- b ---->
   *
   * }</pre>
   */
  public void replace(CFANode pNode, CFANode pNewNode) {
    Graphs.replaceNode(this, pNode, pNewNode);
  }

  /**
   * Replaces a CFA edge with a different CFA edge in this mutable network.
   *
   * <pre>{@code
   * Before:
   * --- a ---> [X] --- pEdge ---> [Y] --- b ---->
   *
   * After:
   * --- a ---> [X] --- pNewEdge ---> [Y] --- b ---->
   *
   * }</pre>
   */
  @SuppressFBWarnings("UC_USELESS_VOID_METHOD") // false positive by SpotBugs
  public void replace(CFAEdge pEdge, CFAEdge pNewEdge) {
    Graphs.replaceEdge(this, pEdge, pNewEdge);
  }
}
