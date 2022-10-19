// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.graph;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.Network;
import com.google.common.graph.NetworkBuilder;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.util.graph.ForwardingMutableNetwork;

/**
 * An {@code OverlayCfaNetwork} is a {@link CfaNetwork} that is layered on top of another {@code
 * CfaNetwork}.
 *
 * <p>Connections between nodes and edges of an overlay can be independently modified without any
 * changes to the underlying network. Note that the CFA represented by an overlay and the CFA
 * represented by actual CFA nodes and edges may differ.
 */
final class OverlayCfaNetwork extends ForwardingMutableNetwork<CFANode, CFAEdge>
    implements MutableCfaNetwork {

  private final CfaNetwork delegate;

  private OverlayCfaNetwork(MutableNetwork<CFANode, CFAEdge> pDelegate) {
    super(pDelegate);
    delegate = new DelegateCfaNetwork(pDelegate);
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
  static OverlayCfaNetwork of(CfaNetwork pCfaNetwork) {

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

  @Override
  public CFANode predecessor(CFAEdge pEdge) {
    return delegate.predecessor(pEdge);
  }

  @Override
  public CFANode successor(CFAEdge pEdge) {
    return delegate.successor(pEdge);
  }

  @Override
  public Optional<FunctionExitNode> functionExitNode(FunctionEntryNode pFunctionEntryNode) {
    return delegate.functionExitNode(pFunctionEntryNode);
  }

  @Override
  public FunctionSummaryEdge functionSummaryEdge(FunctionCallEdge pFunctionCallEdge) {
    return delegate.functionSummaryEdge(pFunctionCallEdge);
  }

  @Override
  public FunctionSummaryEdge functionSummaryEdge(FunctionReturnEdge pFunctionReturnEdge) {
    return delegate.functionSummaryEdge(pFunctionReturnEdge);
  }

  @Override
  public FunctionEntryNode functionEntryNode(FunctionSummaryEdge pFunctionSummaryEdge) {
    return delegate.functionEntryNode(pFunctionSummaryEdge);
  }

  private static final class DelegateCfaNetwork extends AbstractCfaNetwork {

    private Network<CFANode, CFAEdge> delegate;

    private DelegateCfaNetwork(Network<CFANode, CFAEdge> pDelegate) {
      delegate = pDelegate;
    }

    @Override
    public Set<CFAEdge> inEdges(CFANode pNode) {
      return delegate.inEdges(pNode);
    }

    @Override
    public Set<CFAEdge> outEdges(CFANode pNode) {
      return delegate.outEdges(pNode);
    }

    @Override
    public EndpointPair<CFANode> incidentNodes(CFAEdge pEdge) {
      return delegate.incidentNodes(pEdge);
    }

    @Override
    public Set<CFANode> nodes() {
      return delegate.nodes();
    }
  }
}
