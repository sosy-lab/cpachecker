// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/** BlockNodes are coherent subgraphs of CFAs with exactly one entry and exit node. */
public class BlockNode {

  private final BlockNodeMetaData metaData;

  private final Supplier<Set<BlockNode>> predecessors;
  private final Supplier<Set<BlockNode>> successors;

  private final Map<Integer, CFANode> idToNodeMap;

  private final String code;

  /**
   * Represents a subgraph of the CFA beginning at {@code pStartNode} and ending at {@code
   * pLastNode}
   *
   * @param pMetaData all the metadata for this block
   * @param pPredecessors supplier for all predecessors
   * @param pSuccessors supplier for all successors
   */
  private BlockNode(
      @NonNull BlockNodeMetaData pMetaData,
      @NonNull Supplier<Set<BlockNode>> pPredecessors,
      @NonNull Supplier<Set<BlockNode>> pSuccessors,
      @NonNull Map<Integer, CFANode> pIdToNodeMap) {
    checkState(
        pMetaData.getNodesInBlock().contains(pMetaData.getStartNode())
            && pMetaData.getNodesInBlock().contains(pMetaData.getLastNode()), /* TODO make lazy */
        "pNodesInBlock ("
            + pMetaData.getNodesInBlock()
            + ") must list all nodes but misses either the root node ("
            + pMetaData.getStartNode()
            + ") or the last node ("
            + pMetaData.getLastNode()
            + ").");

    metaData = pMetaData;
    predecessors = pPredecessors;
    successors = pSuccessors;

    idToNodeMap = pIdToNodeMap;

    code = blockToPseudoCode();
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
  private String blockToPseudoCode() {
    StringBuilder codeLines = new StringBuilder();
    for (CFAEdge leavingEdge : metaData.getEdgesInBlock()) {
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

  public Set<BlockNode> getPredecessors() {
    return ImmutableSet.copyOf(predecessors.get());
  }

  public Set<BlockNode> getSuccessors() {
    return ImmutableSet.copyOf(successors.get());
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
    return predecessors.get().isEmpty();
  }

  /** Builder for {@link BlockGraph}. */
  public static class BlockGraphBuilder {

    private int blockCount;
    private final Map<Integer, CFANode> idToNodeMap;
    private final Multimap<BlockNodeMetaData, BlockNodeMetaData> successors;
    private final Multimap<BlockNodeMetaData, BlockNodeMetaData> predecessors;
    private final Set<BlockNodeMetaData> blocks;

    private BlockNodeMetaData root;

    /**
     * Build a block graph for a given CFA
     *
     * @param pCfa CFA that will be partitioned into a graph of {@link BlockNode}s
     */
    public BlockGraphBuilder(CFA pCfa) {
      idToNodeMap = Maps.uniqueIndex(pCfa.getAllNodes(), CFANode::getNodeNumber);
      successors = ArrayListMultimap.create();
      predecessors = ArrayListMultimap.create();
      blocks = new LinkedHashSet<>();
    }

    public void setRoot(BlockNodeMetaData pRoot) {
      root = pRoot;
    }

    public BlockNodeMetaData makeBlock(
        CFANode pStartNode, CFANode pEndNode, Set<CFANode> pNodesInBlock, Set<CFAEdge> pEdges) {
      BlockNodeMetaData blockNodeMetaData =
          new BlockNodeMetaData(
              "B" + blockCount++,
              pStartNode,
              pEndNode,
              pNodesInBlock,
              pEdges,
              ImmutableMap.copyOf(idToNodeMap));
      blocks.add(blockNodeMetaData);
      return blockNodeMetaData;
    }

    public void linkSuccessor(BlockNodeMetaData pNode, BlockNodeMetaData pNodeSuccessor) {
      successors.put(pNode, pNodeSuccessor);
      predecessors.put(pNodeSuccessor, pNode);
    }

    public void unlinkSuccessor(BlockNodeMetaData pNode, BlockNodeMetaData pNodeSuccessor) {
      successors.remove(pNode, pNodeSuccessor);
      predecessors.remove(pNodeSuccessor, pNode);
    }

    public void removeNode(BlockNodeMetaData pNode) {
      removeFromMultimap(successors, pNode);
      removeFromMultimap(predecessors, pNode);
      blocks.remove(pNode);
    }

    private void removeFromMultimap(
        Multimap<BlockNodeMetaData, BlockNodeMetaData> pMultimap, BlockNodeMetaData pNode) {
      pMultimap.removeAll(pNode);
      Set<BlockNodeMetaData> keys = pMultimap.keySet();
      for (BlockNodeMetaData key : keys) {
        pMultimap.remove(key, pNode);
      }
    }

    public BlockNodeMetaData mergeSameStartAndEnd(
        BlockNodeMetaData pNode1, BlockNodeMetaData pNode2) {
      if (!(pNode1.getStartNode().equals(pNode2.getStartNode())
          && pNode1.getLastNode().equals(pNode2.getLastNode()))) {
        throw new AssertionError(
            "Nodes must start and end on the same CFANode: " + pNode1 + " " + pNode2);
      }
      Set<CFANode> nodesInBlock = new LinkedHashSet<>(pNode1.getNodesInBlock());
      nodesInBlock.addAll(pNode2.getNodesInBlock());
      Set<CFAEdge> edgesInBlock = new LinkedHashSet<>(pNode1.getEdgesInBlock());
      edgesInBlock.addAll(pNode2.getEdgesInBlock());
      BlockNodeMetaData merged =
          makeBlock(pNode1.getStartNode(), pNode2.getLastNode(), nodesInBlock, edgesInBlock);
      predecessors.get(pNode1).forEach(n -> linkSuccessor(n, merged));
      predecessors.get(pNode2).forEach(n -> linkSuccessor(n, merged));
      successors.get(pNode1).forEach(n -> linkSuccessor(merged, n));
      successors.get(pNode2).forEach(n -> linkSuccessor(merged, n));
      removeNode(pNode1);
      removeNode(pNode2);
      return merged;
    }

    public BlockNodeMetaData mergeSingleSuccessors(
        BlockNodeMetaData pNode1, BlockNodeMetaData pNode2) {
      if (successors.get(pNode1).size() == 1 && predecessors.get(pNode2).size() == 1) {
        if (predecessors.get(pNode2).contains(pNode1)) {
          Set<CFANode> nodesInBlock = new LinkedHashSet<>(pNode1.getNodesInBlock());
          nodesInBlock.addAll(pNode2.getNodesInBlock());
          Set<CFAEdge> edgesInBlock = new LinkedHashSet<>(pNode1.getEdgesInBlock());
          edgesInBlock.addAll(pNode2.getEdgesInBlock());
          BlockNodeMetaData merged =
              makeBlock(pNode1.getStartNode(), pNode2.getLastNode(), nodesInBlock, edgesInBlock);
          predecessors.get(pNode1).forEach(n -> linkSuccessor(n, merged));
          predecessors.get(pNode2).forEach(n -> linkSuccessor(merged, n));
          removeNode(pNode1);
          removeNode(pNode2);
          return merged;
        }
      }
      throw new AssertionError("Blocks must be in one line to be merged");
    }

    public void removeEmptyBlocks() {
      for (BlockNodeMetaData node : blocks) {
        if (predecessors.get(node).isEmpty() || !node.getEdgesInBlock().isEmpty()) {
          continue;
        }
        Set<BlockNodeMetaData> pred = ImmutableSet.copyOf(predecessors.get(node));
        Set<BlockNodeMetaData> succ = ImmutableSet.copyOf(successors.get(node));
        for (BlockNodeMetaData predecessor : pred) {
          for (BlockNodeMetaData successor : succ) {
            linkSuccessor(predecessor, successor);
          }
        }
        removeNode(node);
      }
    }

    public BlockGraph build() {
      Objects.requireNonNull(root, "Root has to be set manually in advance");
      removeEmptyBlocks();
      Map<BlockNodeMetaData, BlockNode> nodes = new HashMap<>();
      for (BlockNodeMetaData data : blocks) {
        BlockNode blockNode =
            new BlockNode(
                data,
                () -> transformedImmutableSetCopy(predecessors.get(data), nodes::get),
                () -> transformedImmutableSetCopy(successors.get(data), nodes::get),
                idToNodeMap);
        nodes.put(data, blockNode);
      }
      return new BlockGraph(nodes.get(root), this);
    }

    public BlockGraph merge(int desiredNumberOfBlocks) {
      Set<BlockNodeMetaData> nodes = new LinkedHashSet<>(blocks);
      nodes.remove(root);
      Multimap<String, BlockNodeMetaData> compatibleBlocks = ArrayListMultimap.create();
      nodes.forEach(
          n ->
              compatibleBlocks.put(
                  "N" + n.getStartNode().getNodeNumber() + "N" + n.getLastNode().getNodeNumber(),
                  n));
      for (String key : ImmutableSet.copyOf(compatibleBlocks.keySet())) {
        List<BlockNodeMetaData> mergeNodes = new ArrayList<>(compatibleBlocks.removeAll(key));
        if (nodes.size() <= desiredNumberOfBlocks) {
          break;
        }
        if (mergeNodes.size() > 1) {
          BlockNodeMetaData current = mergeNodes.remove(0);
          nodes.remove(current);
          for (int i = mergeNodes.size() - 1; i >= 0; i--) {
            BlockNodeMetaData remove = mergeNodes.remove(i);
            nodes.remove(remove);
            current = mergeSameStartAndEnd(current, remove);
          }
          nodes.add(current);
          compatibleBlocks.put(key, current);
        }
      }
      Set<BlockNodeMetaData> alreadyFound = new HashSet<>();
      while (desiredNumberOfBlocks < nodes.size()) {
        Optional<BlockNodeMetaData> potentialNode =
            nodes.stream()
                .filter(n -> successors.get(n).size() == 1 && !alreadyFound.contains(n))
                .findAny();
        if (potentialNode.isEmpty()) {
          break;
        }
        BlockNodeMetaData node = potentialNode.orElseThrow();
        alreadyFound.add(node);
        if (node.equals(root)) {
          continue;
        }
        BlockNodeMetaData singleSuccessor = Iterables.getOnlyElement(successors.get(node));
        if (predecessors.get(singleSuccessor).size() == 1) {
          BlockNodeMetaData merged = mergeSingleSuccessors(node, singleSuccessor);
          nodes.remove(node);
          nodes.remove(singleSuccessor);
          nodes.add(merged);
        }
      }
      return build();
    }
  }

  static class BlockNodeMetaData {

    private final String id;
    private final CFANode startNode;
    private final CFANode lastNode;
    private final Set<CFANode> nodesInBlock;
    private final Set<CFAEdge> edgesInBlock;
    private final Map<Integer, CFANode> idToNodeMap;

    private BlockNodeMetaData(
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
