// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.NetworkBuilder;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.graph.ForwardingMutableNetwork;

public class MutableCfaNetwork extends ForwardingMutableNetwork<CFANode, CFAEdge> {

  private MutableCfaNetwork(MutableNetwork<CFANode, CFAEdge> pDelegate) {
    super(pDelegate);
  }

  /**
   * Returns a new {@code MutableCfaNetwork} instance representing the specified CFA.
   *
   * <p>The returned {@code MutableCfaNetwork} contains all nodes, regular edges, and summary edges
   * that occur in the specified CFA.
   *
   * <p>Modifying the returned mutable network does not change the original CFA, so calling methods
   * on the returned {@code MutableCfaNetwork} instance is safe. However, mutating existing {@code
   * CFANode} and {@code CFAEdge} objects may change (and break) the original CFA.
   *
   * @param pCfa the CFA to create the {@code MutableCfaNetwork} for
   * @return the {@code MutableCfaNetwork} for the specified CFA
   * @throws IllegalArgumentException if the specified CFA contains parallel edges (i.e., edges
   *     connected to the same nodes in the same order)
   * @throws NullPointerException if {@code pCfa == null}
   */
  public static MutableCfaNetwork of(CFA pCfa) {

    MutableNetwork<CFANode, CFAEdge> mutableNetwork =
        NetworkBuilder.directed().allowsSelfLoops(true).build();

    for (CFANode cfaNode : pCfa.getAllNodes()) {
      mutableNetwork.addNode(cfaNode);
      if (cfaNode instanceof FunctionEntryNode) {
        // unreachable FunctionExitNodes are not in the set returned by getAllNodes
        FunctionExitNode functionExitNode = ((FunctionEntryNode) cfaNode).getExitNode();
        mutableNetwork.addNode(functionExitNode);
      }
    }

    for (CFANode predecessor : pCfa.getAllNodes()) {
      for (CFAEdge cfaEdge : CFAUtils.allLeavingEdges(predecessor)) {
        CFANode successor = cfaEdge.getSuccessor();
        boolean edgeAdded = mutableNetwork.addEdge(predecessor, successor, cfaEdge);
        checkArgument(edgeAdded, "CFA must not contain parallel edges");
      }
    }

    return new MutableCfaNetwork(mutableNetwork);
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

    List<CFAEdge> nodeInEdges = ImmutableList.copyOf(inEdges(pNode));
    List<CFANode> nodeUs = new ArrayList<>(nodeInEdges.size());

    for (CFAEdge nodeInEdge : nodeInEdges) {
      nodeUs.add(incidentNodes(nodeInEdge).nodeU());
      removeEdge(nodeInEdge);
    }

    addNode(pNewPredecessor);

    for (int index = 0; index < nodeInEdges.size(); index++) {
      addEdge(nodeUs.get(index), pNewPredecessor, nodeInEdges.get(index));
    }

    addEdge(pNewPredecessor, pNode, pNewInEdge);
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

    List<CFAEdge> nodeOutEdges = ImmutableList.copyOf(outEdges(pNode));
    List<CFANode> nodeVs = new ArrayList<>(nodeOutEdges.size());

    for (CFAEdge nodeOutEdge : nodeOutEdges) {
      nodeVs.add(incidentNodes(nodeOutEdge).nodeV());
      removeEdge(nodeOutEdge);
    }

    addNode(pNewSuccessor);

    for (int index = 0; index < nodeOutEdges.size(); index++) {
      addEdge(pNewSuccessor, nodeVs.get(index), nodeOutEdges.get(index));
    }

    addEdge(pNode, pNewSuccessor, pNewOutEdge);
  }
}
