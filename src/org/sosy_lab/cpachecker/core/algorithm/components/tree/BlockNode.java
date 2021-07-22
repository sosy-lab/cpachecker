// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.tree;

import com.google.common.collect.ImmutableList;
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

  /**
   * Represents a sub graph of the CFA beginning at <code>pStartNode</code> and ending at <code>
   * pLastNode</code>.
   *
   * @param pStartNode the root node of the block
   * @param pLastNode the final node of the block
   * @param pNodesInBlock all nodes that are part of the sub graph including the root node and the
   *     last node.
   */
  public BlockNode(
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
  }

  public void linkSuccessor(BlockNode node) {
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
    return predecessors;
  }

  public Set<BlockNode> getSuccessors() {
    return successors;
  }

  public CFANode getStartNode() {
    return startNode;
  }

  public CFANode getLastNode() {
    return lastNode;
  }

  public Set<CFANode> getNodesInBlock() {
    return nodesInBlock;
  }

  @Override
  public boolean equals(Object pO) {
    if (pO instanceof BlockNode) {
      BlockNode blockNode = (BlockNode) pO;
      return startNode.equals(blockNode.startNode)
          && lastNode.equals(blockNode.lastNode)
          && nodesInBlock.equals(blockNode.nodesInBlock);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(startNode, lastNode, nodesInBlock);
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
}
