// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;

/** BlockNodes are coherent subgraphs of CFAs with exactly one entry and exit node. */
public class BlockNode {

  private final BlockNodeMetaData metaData;

  private final Set<BlockNodeMetaData> predecessors;
  private final Set<BlockNodeMetaData> successors;

  private final Map<Integer, CFANode> idToNodeMap;

  private final CFANode actualLastNode;

  private final String code;

  /**
   * Represents a coherent subgraph of the CFA with exactly one entry and one exit node, described
   * by its metadata.
   *
   * @param pMetaData all the metadata for this block
   * @param pPredecessors supplier for all predecessors
   * @param pSuccessors supplier for all successors
   */
  public BlockNode(
      @NonNull BlockNodeMetaData pMetaData,
      @NonNull Set<BlockNodeMetaData> pPredecessors,
      @NonNull Set<BlockNodeMetaData> pSuccessors,
      @NonNull ShutdownNotifier pShutdownNotifier,
      @NonNull Map<Integer, CFANode> pIdToNodeMap)
      throws InterruptedException {
    checkArgument(
        CFAUtils.existsPath(
            pMetaData.getStartNode(),
            pMetaData.getLastNode(),
            node -> CFAUtils.allLeavingEdges(node).toSet(),
            pShutdownNotifier),
        "pNodesInBlock (%s) "
            + "must list all nodes but misses either the root node (%s) "
            + "or the last node (%s).",
        pMetaData.getNodesInBlock(),
        pMetaData.getStartNode(),
        pMetaData.getLastNode());
    Preconditions.checkArgument(
        isBlockNodeValid(pMetaData.getStartNode(), pMetaData.getEdgesInBlock()),
        "BlockNodes require to have exactly one exit node.");

    CFANode node = null;
    for (CFAEdge cfaEdge : pMetaData.getEdgesInBlock()) {
      if (cfaEdge.getDescription().equals(BlockGraphBuilder.DESCRIPTION)) {
        if (node == null) {
          node = cfaEdge.getPredecessor();
        } else {
          throw new AssertionError("There can only be one block end edge");
        }
      }
    }
    if (node == null) {
      node = pMetaData.getLastNode();
    }
    actualLastNode = node;

    metaData = pMetaData;
    predecessors = pPredecessors;
    successors = pSuccessors;
    idToNodeMap = pIdToNodeMap;

    code = getCodeRepresentation();
  }

  public CFANode getActualLastNode() {
    return actualLastNode;
  }

  private boolean isBlockNodeValid(CFANode pStartNode, Set<CFAEdge> pEdgesInBlock) {
    ArrayDeque<CFANode> waiting = new ArrayDeque<>();
    waiting.push(pStartNode);
    Set<CFANode> covered = new LinkedHashSet<>();
    int count = 0;
    while (!waiting.isEmpty()) {
      CFANode curr = waiting.pop();
      boolean hasSuccessor = false;
      for (CFAEdge leavingEdge : CFAUtils.allLeavingEdges(curr)) {
        if (pEdgesInBlock.contains(leavingEdge)) {
          if (covered.contains(leavingEdge.getSuccessor())) {
            waiting.push(leavingEdge.getSuccessor());
          }
          hasSuccessor = true;
        }
      }
      if (!hasSuccessor) {
        count++;
      }
      covered.add(curr);
    }
    return count <= 1;
  }

  /**
   * Returns the corresponding CFANode for a given node number
   *
   * @param number id of CFANode
   * @return CFANode with id {@code number}
   */
  public CFANode getNodeWithNumber(int number) {
    return idToNodeMap.get(number);
  }

  /**
   * compute the code that this block contains (for debugging only)
   *
   * @return code represented by this block
   */
  private String getCodeRepresentation() {
    StringBuilder codeLines = new StringBuilder();
    for (CFAEdge leavingEdge : metaData.getEdgesInBlock()) {
      if (BlockGraphBuilder.isBlockEnd(leavingEdge.getSuccessor())) {
        codeLines.append("[END]\n");
        continue;
      }
      if (leavingEdge.getCode().isBlank()) {
        continue;
      }
      if (leavingEdge.getEdgeType().equals(CFAEdgeType.AssumeEdge)) {
        codeLines.append("[").append(leavingEdge.getCode()).append("]\n");
      } else {
        codeLines.append(leavingEdge.getCode()).append("\n");
      }
    }
    return codeLines.toString();
  }

  BlockNodeMetaData getMetaData() {
    return metaData;
  }

  /**
   * Check whether this block is self-circular. Self-circular blocks are their own predecessor.
   *
   * @return true if block is its own predecessor/successor, false otherwise
   */
  public boolean isSelfCircular() {
    return metaData.getLastNode().equals(metaData.getStartNode()) && !isEmpty() && !isRoot();
  }

  public boolean isEmpty() {
    return metaData.getEdgesInBlock().isEmpty();
  }

  public Set<BlockNodeMetaData> getPredecessors() {
    return ImmutableSet.copyOf(predecessors);
  }

  public Set<BlockNodeMetaData> getSuccessors() {
    return ImmutableSet.copyOf(successors);
  }

  public CFANode getStartNode() {
    return metaData.getStartNode();
  }

  public CFANode getLastNode() {
    return metaData.getLastNode();
  }

  public Set<CFANode> getNodesInBlock() {
    return ImmutableSet.copyOf(metaData.getNodesInBlock());
  }

  public Set<CFAEdge> getEdgesInBlock() {
    return ImmutableSet.copyOf(metaData.getEdgesInBlock());
  }

  @Override
  public boolean equals(Object pO) {
    if (!(pO instanceof BlockNode)) {
      return false;
    }
    BlockNode blockNode = (BlockNode) pO;
    return getId().equals(blockNode.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(metaData.getId());
  }

  @Override
  public String toString() {
    return "BlockNode{"
        + "id='"
        + getId()
        + '\''
        + ", startNode="
        + getStartNode()
        + ", lastNode="
        + getLastNode()
        + ", size="
        + getNodesInBlock().size()
        + ", code='"
        + code.replaceAll("\n", "")
        + '\''
        + '}';
  }

  public String getCode() {
    return code;
  }

  public String getId() {
    return metaData.getId();
  }

  public boolean isRoot() {
    return predecessors.isEmpty();
  }

  public static class BlockNodeMetaData {

    private final String id;
    private final CFANode startNode;
    private final CFANode lastNode;
    private final Set<CFANode> nodesInBlock;
    private final Set<CFAEdge> edgesInBlock;
    private final Map<Integer, CFANode> idToNodeMap;

    public BlockNodeMetaData(
        String pId,
        CFANode pStartNode,
        CFANode pLastNode,
        Set<CFANode> pNodesInBlock,
        Set<CFAEdge> pEdgesInBlock,
        Map<Integer, CFANode> pIdToNodeMap) {
      id = pId;
      startNode = pStartNode;
      lastNode = pLastNode;
      nodesInBlock = pNodesInBlock;
      edgesInBlock = pEdgesInBlock;
      idToNodeMap = pIdToNodeMap;
    }

    public CFANode getLastNode() {
      return lastNode;
    }

    public CFANode getStartNode() {
      return startNode;
    }

    public Map<Integer, CFANode> getIdToNodeMap() {
      return idToNodeMap;
    }

    public Set<CFAEdge> getEdgesInBlock() {
      return edgesInBlock;
    }

    public Set<CFANode> getNodesInBlock() {
      return nodesInBlock;
    }

    public String getId() {
      return id;
    }

    @Override
    public String toString() {
      return "BlockNodeMetaData{"
          + "id='"
          + id
          + '\''
          + ", startNode="
          + startNode
          + ", lastNode="
          + lastNode
          + '}';
    }

    @Override
    public boolean equals(Object pO) {
      if (!(pO instanceof BlockNodeMetaData)) {
        return false;
      }
      BlockNodeMetaData that = (BlockNodeMetaData) pO;
      return Objects.equals(id, that.id)
          && Objects.equals(startNode, that.startNode)
          && Objects.equals(lastNode, that.lastNode)
          && Objects.equals(nodesInBlock, that.nodesInBlock)
          && Objects.equals(edgesInBlock, that.edgesInBlock)
          && Objects.equals(idToNodeMap, that.idToNodeMap);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, startNode, lastNode, nodesInBlock, edgesInBlock, idToNodeMap);
    }
  }
}