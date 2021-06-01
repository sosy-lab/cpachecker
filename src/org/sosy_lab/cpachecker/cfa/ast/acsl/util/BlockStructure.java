// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.util;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class BlockStructure {

  private final CFA cfa;
  private final ImmutableSet<Block> blocks;

  public BlockStructure(CFA pCfa, Set<Block> pBlocks) {
    cfa = pCfa;
    blocks = ImmutableSet.copyOf(pBlocks);
    addEdgesToBlocks();
    computeNodes();
  }

  private void addEdgesToBlocks() {
    for (CFANode node : cfa.getAllNodes()) {
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        CFAEdge edge = node.getLeavingEdge(i);
        if (ignoreEdge(edge)) {
          continue;
        }
        for (Block block : blocks) {
          if (block.isFunction() || block.isLoop()) {
            continue;
          }
          if (isEnteringEdge(edge, block)) {
            block.addEnteringEdge(edge);
          }
          if (isLeavingEdge(edge, block)) {
            block.addLeavingEdge(edge);
          }
        }
      }
    }
  }

  private void computeNodes() {
    for (Block block : blocks) {
      if (block instanceof StatementBlock) {
        ((StatementBlock) block).computeContainedNodes();
      }
    }
  }

  private boolean isEnteringEdge(CFAEdge edge, Block block) {
    if (isInBlock(edge, block)) {
      return false;
    }
    boolean allInside = true;
    boolean atLeastOneInside = false;
    Queue<CFANode> toVisit = new ArrayDeque<>();
    Set<CFANode> visited = new HashSet<>();
    toVisit.add(edge.getSuccessor());
    while (!toVisit.isEmpty()) {
      CFANode currentNode = toVisit.poll();
      if (visited.contains(currentNode)
          || !currentNode.getFunctionName().equals(edge.getPredecessor().getFunctionName())) {
        continue;
      }
      visited.add(currentNode);
      // TODO: Maybe use CFATraversal to get all relevant edges
      for (int i = 0; i < currentNode.getNumLeavingEdges(); i++) {
        CFAEdge currentEdge = currentNode.getLeavingEdge(i);
        if (ignoreEdge(currentEdge)) {
          toVisit.add(currentEdge.getSuccessor());
        } else if (!isInBlock(currentEdge, block)) {
          allInside = false;
        } else {
          atLeastOneInside = true;
        }
      }
    }
    return allInside && atLeastOneInside;
  }

  private boolean isLeavingEdge(CFAEdge edge, Block block) {
    if (!isInBlock(edge, block)) {
      return false;
    }
    boolean allOutside = true;
    boolean atLeastOneOutside = false;
    Queue<CFANode> toVisit = new ArrayDeque<>();
    Set<CFANode> visited = new HashSet<>();
    toVisit.add(edge.getSuccessor());
    while (!toVisit.isEmpty()) {
      CFANode currentNode = toVisit.poll();
      if (visited.contains(currentNode)
          || !currentNode.getFunctionName().equals(edge.getSuccessor().getFunctionName())) {
        continue;
      }
      visited.add(currentNode);
      // TODO: Maybe use CFATraversal to get all relevant edges
      for (int i = 0; i < currentNode.getNumLeavingEdges(); i++) {
        CFAEdge currentEdge = currentNode.getLeavingEdge(i);
        if (ignoreEdge(currentEdge)) {
          toVisit.add(currentEdge.getSuccessor());
        } else if (isInBlock(currentEdge, block)) {
          allOutside = false;
        } else {
          atLeastOneOutside = true;
        }
      }
    }
    return allOutside && atLeastOneOutside;
  }

  private boolean isInBlock(CFAEdge edge, Block block) {
    FileLocation location = edge.getFileLocation();
    return block.getStartOffset() < location.getNodeOffset()
        && location.getNodeOffset() < block.getEndOffset();
  }

  private boolean ignoreEdge(CFAEdge edge) {
    return edge.getFileLocation().equals(FileLocation.DUMMY)
        || edge.getDescription().contains("__CPAchecker_TMP");
  }

  public Set<Block> getBlocks() {
    return blocks;
  }

  public Block getInnermostBlockOf(CFANode node) {
    Block innermostBlock = null;
    for (Block block : blocks) {
      if (block.getContainedNodes().contains(node)
          && (innermostBlock == null || innermostBlock.contains(block))) {
        innermostBlock = block;
      }
    }
    return innermostBlock;
  }

  public Block getInnermostBlockOf(FileLocation location) {
    Block innermostBlock = null;
    for (Block block : blocks) {
      if (block.getStartOffset() < location.getNodeOffset()
          && location.getNodeOffset() < block.getEndOffset()
          && (innermostBlock == null || innermostBlock.contains(block))) {
        innermostBlock = block;
      }
    }
    return innermostBlock;
  }

  public Set<CFANode> getNodesInInnermostBlockOf(CFANode node) {
    return getInnermostBlockOf(node).getContainedNodes();
  }
}
