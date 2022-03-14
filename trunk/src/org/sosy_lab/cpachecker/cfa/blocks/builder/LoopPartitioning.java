// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.blocks.builder;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

/** <code>PartitioningHeuristic</code> that creates blocks for each loop-body. */
public class LoopPartitioning extends PartitioningHeuristic {

  private Map<CFANode, Set<CFANode>> loopHeaderToLoopBody = null;

  public LoopPartitioning(LogManager pLogger, CFA pCfa, Configuration pConfig) {
    super(pLogger, pCfa, pConfig);
  }

  private void initLoopMap() {
    loopHeaderToLoopBody = new HashMap<>();
    if (cfa.getLoopStructure().isPresent()) {
      for (Loop loop : cfa.getLoopStructure().orElseThrow().getAllLoops()) {
        if (loop.getLoopHeads().size() == 1) {
          // currently only loops with single loop heads supported
          loopHeaderToLoopBody.put(
              Iterables.getOnlyElement(loop.getLoopHeads()), loop.getLoopNodes());
        }
      }
    }
  }

  private static boolean isMainFunction(CFANode pNode) {
    return pNode instanceof FunctionEntryNode && pNode.getNumEnteringEdges() == 0;
  }

  private boolean isLoopHead(CFANode pNode) {
    return cfa.getAllLoopHeads().orElseThrow().contains(pNode);
  }

  private boolean hasBlankEdgeFromLoop(CFANode pNode) {
    for (CFAEdge edge : CFAUtils.enteringEdges(pNode)) {
      if (edge instanceof BlankEdge && isLoopHead(edge.getPredecessor())) {
        return true;
      }
    }
    return false;
  }

  private static boolean selfLoop(CFANode pNode) {
    return pNode.getNumLeavingEdges() == 1 && pNode.getLeavingEdge(0).getSuccessor().equals(pNode);
  }

  @Override
  protected Set<CFANode> getBlockForNode(CFANode pBlockHead) {
    if (isMainFunction(pBlockHead)) {
      Preconditions.checkArgument(
          cfa.getMainFunction().getFunctionName().equals(pBlockHead.getFunctionName()));
      return CFATraversal.dfs().ignoreFunctionCalls().collectNodesReachableFrom(pBlockHead);
    }

    if (loopHeaderToLoopBody == null) {
      initLoopMap();
    }

    if (!loopHeaderToLoopBody.containsKey(pBlockHead)
        || !isLoopHead(pBlockHead)
        || hasBlankEdgeFromLoop(pBlockHead)
        || selfLoop(pBlockHead)) {
      // loopStructure is missing in CFA or loop with multiple headers or self loop
      return null;
    }

    Set<CFANode> loopBody = new HashSet<>(loopHeaderToLoopBody.get(pBlockHead));
    insertLoopStartState(loopBody, pBlockHead);
    insertLoopReturnStates(loopBody);
    return loopBody;
  }

  private void insertLoopStartState(Set<CFANode> pLoopBody, CFANode pLoopHeader) {
    for (CFAEdge edge : CFAUtils.enteringEdges(pLoopHeader)) {
      if (edge instanceof BlankEdge && !pLoopBody.contains(edge.getPredecessor())) {
        pLoopBody.add(edge.getPredecessor());
      }
    }
  }

  private void insertLoopReturnStates(Set<CFANode> pLoopBody) {
    List<CFANode> addNodes = new ArrayList<>();
    for (CFANode node : pLoopBody) {
      for (CFAEdge edge : CFAUtils.leavingEdges(node)) {
        if (!pLoopBody.contains(edge.getSuccessor())
            && !(edge.getEdgeType() == CFAEdgeType.FunctionCallEdge)) {
          addNodes.add(edge.getSuccessor());
        }
      }
    }

    // Normally pLoopBody.addAll(addNodes) would be enough. In special cases we have to add more
    // nodes,
    // because they are reachable from the loop and the loopReturnNodes are reachable from them.
    // This happens with break-statement-branches, that do not only skip the loop, but do some
    // calculations.
    // Then all calculation-Nodes are outside the block, but the loopReturnNode is after them and in
    // the block.
    // Example: for(..) { if (..) { calc ..; break; } .. }
    // So we also add their predecessors to the block, so that the loop-block has only one
    // entry-node.
    // We assume, that the node direct after the loop is _only_ reachable
    // either through the loopstart or with a break-statement.
    final List<CFANode> waitlist = new ArrayList<>(addNodes);
    while (!waitlist.isEmpty()) {
      final CFANode node = waitlist.remove(0);
      if (pLoopBody.add(node)) {
        for (CFAEdge edge : CFAUtils.enteringEdges(node)) {
          if (edge.getEdgeType() != CFAEdgeType.FunctionReturnEdge) {
            waitlist.add(edge.getPredecessor());
          }
        }
      }
    }
  }
}
