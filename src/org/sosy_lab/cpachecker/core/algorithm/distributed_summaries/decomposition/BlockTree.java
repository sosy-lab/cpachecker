// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode.BlockNodeFactory;

public class BlockTree {

  private final BlockNode root;
  private final BlockNodeFactory factory;

  public BlockTree(BlockNode pRoot, BlockNodeFactory pFactory) {
    root = pRoot;
    factory = pFactory;
  }

  public BlockNode getRoot() {
    return root;
  }

  public ImmutableSet<BlockNode> getDistinctNodes() {
    Set<BlockNode> nodes = new HashSet<>();
    ArrayDeque<BlockNode> waiting = new ArrayDeque<>();
    waiting.add(root);
    while (!waiting.isEmpty()) {
      BlockNode top = waiting.pop();
      if (nodes.add(top)) {
        waiting.addAll(top.getSuccessors());
      }
    }
    return ImmutableSet.copyOf(nodes);
  }

  public BlockNode mergeSameStartAndEnd(BlockNode pNode1, BlockNode pNode2) {
    if (!(pNode1.getStartNode().equals(pNode2.getStartNode())
        && pNode1.getLastNode().equals(pNode2.getLastNode()))) {
      throw new AssertionError(
          "Nodes must start and end on the same CFANode: " + pNode1 + " " + pNode2);
    }
    Set<CFANode> nodesInBlock = new LinkedHashSet<>(pNode1.getNodesInBlock());
    nodesInBlock.addAll(pNode2.getNodesInBlock());
    Set<CFAEdge> edgesInBlock = new LinkedHashSet<>(pNode1.getEdgesInBlock());
    edgesInBlock.addAll(pNode2.getEdgesInBlock());
    BlockNode merged =
        factory.makeBlock(pNode1.getStartNode(), pNode2.getLastNode(), nodesInBlock, edgesInBlock);
    pNode1.getPredecessors().forEach(n -> factory.linkSuccessor(n, merged));
    pNode2.getPredecessors().forEach(n -> factory.linkSuccessor(n, merged));
    pNode1.getSuccessors().forEach(n -> factory.linkSuccessor(merged, n));
    pNode2.getSuccessors().forEach(n -> factory.linkSuccessor(merged, n));
    factory.removeNode(pNode1);
    factory.removeNode(pNode2);
    return merged;
  }

  public BlockNode mergeSingleSuccessors(BlockNode pNode1, BlockNode pNode2) {
    if (pNode1.getSuccessors().size() == 1 && pNode2.getPredecessors().size() == 1) {
      if (pNode2.getPredecessors().contains(pNode1)) {
        Set<CFANode> nodesInBlock = new LinkedHashSet<>(pNode1.getNodesInBlock());
        nodesInBlock.addAll(pNode2.getNodesInBlock());
        Set<CFAEdge> edgesInBlock = new LinkedHashSet<>(pNode1.getEdgesInBlock());
        edgesInBlock.addAll(pNode2.getEdgesInBlock());
        BlockNode merged =
            factory.makeBlock(
                pNode1.getStartNode(), pNode2.getLastNode(), nodesInBlock, edgesInBlock);
        pNode1.getPredecessors().forEach(n -> factory.linkSuccessor(n, merged));
        pNode2.getSuccessors().forEach(n -> factory.linkSuccessor(merged, n));
        factory.removeNode(pNode1);
        factory.removeNode(pNode2);
        return merged;
      }
    }
    throw new AssertionError("Blocks must be in one line to be merged");
  }

  public ImmutableSet<BlockNode> removeEmptyBlocks() {
    Set<BlockNode> removed = new HashSet<>();
    ImmutableSet<BlockNode> nodes = getDistinctNodes();
    for (BlockNode node : nodes) {
      if (node.isRoot() || !node.isEmpty()) {
        continue;
      }
      Set<BlockNode> predecessors = node.getPredecessors();
      Set<BlockNode> successors = node.getSuccessors();
      for (BlockNode predecessor : predecessors) {
        for (BlockNode successor : successors) {
          factory.linkSuccessor(predecessor, successor);
        }
      }
      factory.removeNode(node);
      removed.add(node);
    }
    return ImmutableSet.copyOf(removed);
  }
}
