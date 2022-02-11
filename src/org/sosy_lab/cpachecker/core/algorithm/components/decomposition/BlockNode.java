// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.decomposition;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.builder.ReferencedVariablesCollector;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class BlockNode {

  private final CFANode startNode;
  private final CFANode lastNode;
  private final Set<CFANode> nodesInBlock;
  private final Block block;

  private final Set<BlockNode> predecessors;
  private final Set<BlockNode> successors;

  private final Map<Integer, CFANode> idToNodeMap;

  private final String id;
  private final String code;

  /**
   * Represents a sub graph of the CFA beginning at <code>pStartNode</code> and ending at <code>
   * pLastNode</code>.
   *
   * @param pStartNode    the root node of the block
   * @param pLastNode     the final node of the block
   * @param pNodesInBlock all nodes that are part of the sub graph including the root node and the
   *                      last node.
   */
  private BlockNode(
      @NonNull String pId,
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

    block = new Block(new ReferencedVariablesCollector(pNodesInBlock).getVars(),
        ImmutableSet.of(pStartNode), ImmutableSet.of(pLastNode), pNodesInBlock);
    startNode = pStartNode;
    lastNode = pLastNode;

    predecessors = new HashSet<>();
    successors = new HashSet<>();

    nodesInBlock = new LinkedHashSet<>(pNodesInBlock);
    idToNodeMap = generateIdToNodeMap(nodesInBlock);
    id = pId;

    code = computeCode(pNodesInBlock);
  }

  private ImmutableMap<Integer, CFANode> generateIdToNodeMap(Set<CFANode> nodes) {
    Map<Integer, CFANode> nodeMap = new HashMap<>();
    nodes.forEach(n -> nodeMap.put(n.getNodeNumber(), n));
    return ImmutableMap.copyOf(nodeMap);
  }

  public CFANode getNodeWithNumber(int number) {
    return idToNodeMap.get(number);
  }

  private String computeCode(Set<CFANode> pNodes) {
    StringBuilder codeLines = new StringBuilder();
    for (CFANode node : pNodes) {
      for (CFAEdge leavingEdge : CFAUtils.leavingEdges(node)) {
        if (pNodes.contains(leavingEdge.getSuccessor())) {
          if (leavingEdge.getCode().isBlank()) {
            continue;
          }
          if (leavingEdge.getEdgeType().equals(CFAEdgeType.AssumeEdge)) {
            codeLines.append("[").append(leavingEdge.getCode()).append("]\n");
          } else {
            codeLines.append(leavingEdge.getCode()).append("\n");
          }
          break;
        }
      }
    }
    return codeLines.toString();
  }

  public boolean isSelfCircular() {
    return lastNode.equals(startNode) && !isEmpty() && !isRoot();
  }

  public boolean isEmpty() {
    return nodesInBlock.size() == 1;
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

  public Block getBlock() {
    return block;
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
    return "BlockNode{" +
        "id='" + id + '\'' +
        ", startNode=" + startNode +
        ", lastNode=" + lastNode +
        ", size=" + nodesInBlock.size() +
        ", code='" + code.replaceAll("\n", "") + '\'' +
        '}';
  }

  public String getCode() {
    return code;
  }

  public String getId() {
    return id;
  }

  public boolean isRoot() {
    return predecessors.isEmpty();
  }

  public static class BlockNodeFactory {

    private int blockCount;

    public BlockNode makeBlock(CFANode pStartNode, CFANode pEndNode, Set<CFANode> pNodesInBlock) {
      return new BlockNode("B" + blockCount++, pStartNode, pEndNode, pNodesInBlock);
    }

    public void linkSuccessor(BlockNode pNode, BlockNode pNodeSuccessor) {
      pNode.linkSuccessor(pNodeSuccessor);
    }

    public void removeNode(BlockNode pNode) {
      pNode.predecessors.forEach(p -> p.successors.remove(pNode));
      pNode.successors.forEach(p -> p.predecessors.remove(pNode));
    }

  }
}
