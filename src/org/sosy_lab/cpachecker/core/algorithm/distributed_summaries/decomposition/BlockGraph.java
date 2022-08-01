// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode.BlockNodeMetaData;
import org.sosy_lab.cpachecker.util.Pair;

/**
 * Represents a partitioning of a CFA. The blocks contain coherent subgraphs of a CFA. The
 * successors of a block are the blocks that contain successive subgraphs of the same CFA.
 */
public class BlockGraph {

  private final BlockNode root;
  private final BlockGraphFactory factory;

  /**
   * Represents the CFA but partitioned into multiple connected blocks.
   *
   * @param pRoot The root node of the
   * @param pFactory The factory that created this block graph.
   */
  public BlockGraph(BlockNode pRoot, BlockGraphFactory pFactory) {
    root = pRoot;
    factory = pFactory;
  }

  public BlockNode getRoot() {
    return root;
  }

  public ImmutableSet<BlockNode> getDistinctNodes() {
    Set<BlockNode> nodes = new LinkedHashSet<>();
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

  public static BlockGraph merge(BlockGraph pBlockGraph, int pDesiredNumberOfBlocks)
      throws InterruptedException {
    return pBlockGraph.factory.merge(pDesiredNumberOfBlocks);
  }

  /** Builder for {@link BlockGraph}. */
  public static class BlockGraphFactory {

    private int blockCount;
    private final Map<Integer, CFANode> idToNodeMap;
    private final Multimap<BlockNodeMetaData, BlockNodeMetaData> successors;
    private final Multimap<BlockNodeMetaData, BlockNodeMetaData> predecessors;
    private final Set<BlockNodeMetaData> blocks;
    private final ShutdownNotifier shutdownNotifier;

    private BlockNodeMetaData root;

    /**
     * Build a block graph for a given CFA
     *
     * @param pCfa CFA that will be partitioned into a graph of {@link BlockNode}s
     */
    public BlockGraphFactory(CFA pCfa, ShutdownNotifier pShutdownNotifier) {
      idToNodeMap = Maps.uniqueIndex(pCfa.getAllNodes(), CFANode::getNodeNumber);
      successors = ArrayListMultimap.create();
      predecessors = ArrayListMultimap.create();
      blocks = new LinkedHashSet<>();
      shutdownNotifier = pShutdownNotifier;
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

    public BlockGraph build() throws InterruptedException {
      Objects.requireNonNull(root, "Root has to be set manually in advance");
      removeEmptyBlocks();
      Map<BlockNodeMetaData, BlockNode> nodes = new HashMap<>();
      for (BlockNodeMetaData data : blocks) {
        BlockNode blockNode =
            new BlockNode(
                data,
                () -> transformedImmutableSetCopy(predecessors.get(data), nodes::get),
                () -> transformedImmutableSetCopy(successors.get(data), nodes::get),
                idToNodeMap,
                shutdownNotifier);
        nodes.put(data, blockNode);
      }
      return new BlockGraph(nodes.get(root), this);
    }

    public BlockGraph merge(int desiredNumberOfBlocks) throws InterruptedException {
      Set<BlockNodeMetaData> nodes = new LinkedHashSet<>(blocks);
      nodes.remove(root);
      Multimap<Pair<CFANode, CFANode>, BlockNodeMetaData> compatibleBlocks =
          ArrayListMultimap.create();
      nodes.forEach(n -> compatibleBlocks.put(Pair.of(n.getStartNode(), n.getLastNode()), n));
      for (Pair<CFANode, CFANode> key : ImmutableSet.copyOf(compatibleBlocks.keySet())) {
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
      Set<BlockNodeMetaData> alreadyFound = new LinkedHashSet<>();
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
}
