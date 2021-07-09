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

  private boolean precondition;
  private boolean postcondition;

  public BlockNode(@NonNull CFANode pStartNode, @NonNull CFANode pLastNode, @NonNull Set<CFANode> pNodesInBlock) {
    startNode = pStartNode;
    lastNode = pLastNode;

    predecessors = new HashSet<>();
    successors = new HashSet<>();

    nodesInBlock = new LinkedHashSet<>(pNodesInBlock);

    // make sure that no node is missing, set removes duplicates automatically
    nodesInBlock.add(pStartNode);
    nodesInBlock.add(pLastNode);

    precondition = true;
    postcondition = true;
  }

  public void sendPostconditionToSuccessors() {
    successors.forEach(node -> node.updatePrecondition(postcondition));
  }

  public void sendPreconditionToPredecessors() {
    predecessors.forEach(node -> node.updatePostcondition(precondition));
  }

  public void updatePrecondition(boolean precond) {
    precondition &= precond;
  }

  public void updatePostcondition(boolean postcond) {
    postcondition &= postcond;
  }

  public void linkSuccessor(BlockNode node) {
    addSuccessors(node);
    node.addPredecessors(this);
  }

  public void addPredecessors(BlockNode... pred) {
    predecessors.addAll(ImmutableList.copyOf(pred));
  }

  public void addSuccessors(BlockNode... succ) {
    successors.addAll(ImmutableList.copyOf(succ));
  }

  public Set<BlockNode> getPredecessors() {
    return predecessors;
  }

  public Set<BlockNode> getSuccessors() {
    return successors;
  }

  @Override
  public boolean equals(Object pO) {
    if (pO instanceof  BlockNode) {
      BlockNode blockNode = (BlockNode) pO;
      return startNode.equals(blockNode.startNode) && lastNode.equals(blockNode.lastNode)
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
    return "BlockNode{" +
        "startNode=" + startNode +
        ", lastNode=" + lastNode +
        ", nodesInBlock=" + nodesInBlock +
        '}';
  }
}
