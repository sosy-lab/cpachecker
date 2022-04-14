// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.graph;

import com.google.common.collect.TreeMultimap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.graph.ForwardingMutableNetwork;
import org.sosy_lab.cpachecker.util.graph.Graphs;

// TODO: merge CfaMutableNetwork and MutableCfaNetwork
public class CfaMutableNetwork extends ForwardingMutableNetwork<CFANode, CFAEdge> {

  private final CfaNetwork delegate;

  private CfaMutableNetwork(MutableCfaNetwork pDelegate) {
    super(pDelegate);

    delegate = pDelegate;
  }

  /** Returns a new {@code CfaMutableNetwork} instance representing the specified CFA. */
  public static CfaMutableNetwork of(CFA pCfa) {

    NavigableMap<String, FunctionEntryNode> functionEntryNodes = new TreeMap<>();
    TreeMultimap<String, CFANode> allNodes = TreeMultimap.create();

    for (CFANode node : pCfa.getAllNodes()) {

      String functionName = node.getFunction().getQualifiedName();
      allNodes.put(functionName, node);

      if (node instanceof FunctionEntryNode) {
        functionEntryNodes.put(functionName, (FunctionEntryNode) node);
      }
    }

    MutableCFA mutableCfa = new MutableCFA(functionEntryNodes, allNodes, pCfa.getMetadata());

    return new CfaMutableNetwork(MutableCfaNetwork.of(mutableCfa));
  }

  public CfaNetwork getCfaNetwork() {
    return delegate;
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
