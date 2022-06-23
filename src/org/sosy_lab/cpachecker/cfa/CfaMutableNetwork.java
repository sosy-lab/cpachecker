// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.graph.MutableNetwork;
import com.google.common.graph.NetworkBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.CFAUtils;

public interface CfaMutableNetwork extends MutableNetwork<CFANode, CFAEdge> {

  /**
   * Returns a new {@code CfaMutableNetwork} instance representing the specified CFA.
   *
   * <p>The returned {@code CfaMutableNetwork} contains all nodes, regular edges, and summary edges
   * that occur in the specified CFA.
   *
   * <p>Modifying the returned mutable network does not change the original CFA, so calling methods
   * on the returned {@code CfaMutableNetwork} instance is safe. However, mutating existing {@code
   * CFANode} and {@code CFAEdge} objects may change (and break) the original CFA.
   *
   * @param pCfa the CFA to create the {@code CfaMutableNetwork} for
   * @return the {@code CfaMutableNetwork} for the specified CFA
   * @throws IllegalArgumentException if the specified CFA contains parallel edges (i.e., edges
   *     connected to the same nodes in the same order)
   * @throws NullPointerException if {@code pCfa == null}
   */
  static CfaMutableNetwork of(CFA pCfa) {

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

    return new ChangeRecordingCfaMutableNetwork(mutableNetwork);
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
  void insertPredecessor(CFANode pNewPredecessor, CFANode pNode, CFAEdge pNewInEdge);

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
  void insertSuccessor(CFANode pNode, CFANode pNewSuccessor, CFAEdge pNewOutEdge);

  /**
   * Replaces a CFA node with a different CFA node in this mutable network.
   *
   * <p>Only the mutable network is changed by this method. The actual CFA nodes and edges are not
   * modified. Connections between nodes and edges represented by the CFA nodes and edges themselves
   * (i.e., defined by {@code CFAEdge#getSuccessor()}, {@code CFANode#getLeavingEdge(int)}, etc.)
   * and connections represented by the mutable network are going to differ after invoking this
   * method.
   *
   * <p>The mutable network is changed in the following way:
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
  void replace(CFANode pNode, CFANode pNewNode);

  /**
   * Replaces a CFA edge with a different CFA edge in this mutable network.
   *
   * <p>Only the mutable network is changed by this method. The actual CFA nodes and edges are not
   * modified. Connections between nodes and edges represented by the CFA nodes and edges themselves
   * (i.e., defined by {@code CFAEdge#getSuccessor()}, {@code CFANode#getLeavingEdge(int)}, etc.)
   * and connections represented by the mutable network are going to differ after invoking this
   * method.
   *
   * <p>The mutable network is changed in the following way:
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
  void replace(CFAEdge pEdge, CFAEdge pNewEdge);
}
