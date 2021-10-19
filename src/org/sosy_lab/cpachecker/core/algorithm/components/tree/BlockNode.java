// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.tree;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class BlockNode {

  private final CFANode startNode;
  private final CFANode lastNode;
  private final Set<CFANode> nodesInBlock;

  private final Set<BlockNode> predecessors;
  private final Set<BlockNode> successors;

  private final String id;

  /**
   * Represents a sub graph of the CFA beginning at <code>pStartNode</code> and ending at <code>
   * pLastNode</code>.
   *
   * @param pStartNode the root node of the block
   * @param pLastNode the final node of the block
   * @param pNodesInBlock all nodes that are part of the sub graph including the root node and the
   *     last node.
   */
  private BlockNode(
      @NonNull CFANode pStartNode,
      @NonNull CFANode pLastNode,
      @NonNull Set<CFANode> pNodesInBlock) {
    // pNodesInBlock is a set allowing to represent branches.
    if (!pNodesInBlock.contains(pStartNode) || !pNodesInBlock.contains(pLastNode)) {
      throw new AssertionError(
          "pNodesInBlock ("
              + pNodesInBlock
              + ") must list all nodes but misses either the root node ("
              + pStartNode
              + ") or the last node ("
              + pLastNode
              + ").");
    }
    startNode = pStartNode;
    lastNode = pLastNode;

    predecessors = new HashSet<>();
    successors = new HashSet<>();

    nodesInBlock = new LinkedHashSet<>(pNodesInBlock);
    id = generateUniqueId(startNode, lastNode, nodesInBlock);
  }

  private static String generateUniqueId(CFANode pStartNode, CFANode pEndNode, Set<CFANode> pNodesInBlock) {
    StringBuilder id = new StringBuilder("N" + pStartNode.getNodeNumber());
    for(CFANode n: pNodesInBlock) {
      id.append("N").append(n.getNodeNumber());
    }
    return id.append("N").append(pEndNode.getNodeNumber()).toString();
  }

  private void linkSuccessor(BlockNode node) {
    addSuccessors(node);
    node.addPredecessors(this);
  }

  private void addPredecessors(BlockNode... pred) {
    predecessors.addAll(ImmutableList.copyOf(pred));
  }

  private void addSuccessors(BlockNode... succ) {
    successors.addAll(ImmutableList.copyOf(succ));
  }

  public Set<BlockNode> getPredecessors() {
    return ImmutableSet.copyOf(predecessors);
  }

  public Set<BlockNode> getSuccessors() {
    return ImmutableSet.copyOf(successors);
  }

  public CFANode getStartNode() {
    return startNode;
  }

  public CFANode getLastNode() {
    return lastNode;
  }

  public Set<CFANode> getNodesInBlock() {
    return ImmutableSet.copyOf(nodesInBlock);
  }

  @Override
  public boolean equals(Object pO) {
    if (!(pO instanceof BlockNode)) {
      return false;
    }
    BlockNode blockNode = (BlockNode) pO;
    return id.equals(blockNode.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "BlockNode{"
        + "startNode="
        + startNode
        + ", lastNode="
        + lastNode
        + ", nodesInBlock="
        + nodesInBlock
        + '}';
  }

  public String getId() {
    return id;
  }

  public static class BlockNodeFactory {

    public BlockNode makeBlock(CFANode pStartNode, CFANode pEndNode, Set<CFANode> pNodesInBlock) {
      return new BlockNode(pStartNode, pEndNode, pNodesInBlock);
    }

    public void linkSuccessor(BlockNode pNode, BlockNode pNodeSuccessor) {
      pNode.linkSuccessor(pNodeSuccessor);
    }

  }
}
