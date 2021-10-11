// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Equivalence;
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

public class MutableCfaNetwork
    extends ForwardingMutableNetwork<CFANode, Equivalence.Wrapper<CFAEdge>> {

  private MutableCfaNetwork(MutableNetwork<CFANode, Equivalence.Wrapper<CFAEdge>> pDelegate) {
    super(pDelegate);
  }

  public static Equivalence.Wrapper<CFAEdge> wrap(CFAEdge pCfaEdge) {
    return Equivalence.identity().wrap(checkNotNull(pCfaEdge));
  }

  public static MutableCfaNetwork of(CFA pCfa) {

    MutableNetwork<CFANode, Equivalence.Wrapper<CFAEdge>> mutableNetwork =
        NetworkBuilder.directed().allowsParallelEdges(true).allowsSelfLoops(true).build();

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
        Equivalence.Wrapper<CFAEdge> wrappedCfaEdge = wrap(cfaEdge);
        mutableNetwork.addEdge(predecessor, successor, wrappedCfaEdge);
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
  public void insertPredecessor(
      CFANode pNewPredecessor, CFANode pNode, Equivalence.Wrapper<CFAEdge> pNewInEdge) {

    List<Equivalence.Wrapper<CFAEdge>> nodeInEdges = new ArrayList<>(inEdges(pNode));
    List<CFANode> nodeUs = new ArrayList<>(nodeInEdges.size());

    for (var nodeInEdge : nodeInEdges) {
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
  public void insertSuccessor(
      CFANode pNode, CFANode pNewSuccessor, Equivalence.Wrapper<CFAEdge> pNewOutEdge) {

    List<Equivalence.Wrapper<CFAEdge>> nodeOutEdges = new ArrayList<>(outEdges(pNode));
    List<CFANode> nodeVs = new ArrayList<>(nodeOutEdges.size());

    for (var nodeOutEdge : nodeOutEdges) {
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
