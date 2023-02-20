// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode.BlockNodeMetaData;

/**
 * Represents a partitioning of a CFA. The blocks contain coherent subgraphs of a CFA. The
 * successors of a block are the blocks that contain successive subgraphs of the same CFA.
 */
public class BlockGraph {

  private final BlockNode root;
  private final ImmutableSet<BlockNode> allNodes;
  private final ImmutableMap<BlockNodeMetaData, BlockNode> metaDataToBlockNode;

  /**
   * Represents the CFA but partitioned into multiple connected blocks.
   *
   * @param pRoot The root node of the
   * @param pAllNodes All distinct nodes of the graph
   */
  private BlockGraph(BlockNode pRoot, ImmutableSet<BlockNode> pAllNodes) {
    root = Objects.requireNonNull(pRoot);
    allNodes = pAllNodes;
    metaDataToBlockNode = Maps.uniqueIndex(allNodes, node -> node.getMetaData());
  }

  public BlockNode getRoot() {
    return root;
  }

  public ImmutableSet<BlockNode> getDistinctNodes() {
    return allNodes;
  }

  public ImmutableSet<BlockNode> successorsOf(BlockNode pBlockNode) {
    return transformedImmutableSetCopy(pBlockNode.getSuccessors(), metaDataToBlockNode::get);
  }

  public ImmutableSet<BlockNode> predecessorsOf(BlockNode pBlockNode) {
    return transformedImmutableSetCopy(pBlockNode.getPredecessors(), metaDataToBlockNode::get);
  }

  public static BlockGraph fromMetaData(
      Set<BlockNodeMetaData> pMetaDataSet, CFA pCFA, ShutdownNotifier pNotifier)
      throws InterruptedException {
    return fromMetaData(pMetaDataSet, pCFA, pNotifier, false);
  }

  private static Multimap<BlockNodeMetaData, BlockNodeMetaData> findLoopPredecessors(
      CFA pCFA, Multimap<CFANode, BlockNodeMetaData> pStart) {
    List<List<BlockNodeMetaData>> waitList = new ArrayList<>();
    Multimap<BlockNodeMetaData, BlockNodeMetaData> loopPredecessors = ArrayListMultimap.create();
    for (BlockNodeMetaData blockNodeMetaData : pStart.get(pCFA.getMainFunction())) {
      ArrayList<BlockNodeMetaData> entry = new ArrayList<>();
      entry.add(blockNodeMetaData);
      waitList.add(entry);
    }
    while (!waitList.isEmpty()) {
      List<BlockNodeMetaData> current = waitList.remove(0);
      BlockNodeMetaData last = current.get(current.size() - 1);
      CFANode lastNode = last.getLastNode();
      for (BlockNodeMetaData blockNodeMetaData : pStart.get(lastNode)) {
        if (current.contains(blockNodeMetaData)) {
          loopPredecessors.put(blockNodeMetaData, last);
          continue;
        }
        ArrayList<BlockNodeMetaData> entry = new ArrayList<>(current);
        entry.add(blockNodeMetaData);
        waitList.add(entry);
      }
    }
    return loopPredecessors;
  }

  private static BlockGraph fromMetaData(
      Set<BlockNodeMetaData> pMetaDataSet,
      CFA pCFA,
      ShutdownNotifier pNotifier,
      boolean pPrependRoot)
      throws InterruptedException {

    Preconditions.checkArgument(
        pMetaDataSet.stream().map(m -> m.getId()).distinct().count() == pMetaDataSet.size());
    Multimap<CFANode, BlockNodeMetaData> startingPoints = ArrayListMultimap.create();
    Multimap<CFANode, BlockNodeMetaData> endingPoints = ArrayListMultimap.create();
    Map<Integer, CFANode> idToNode = Maps.uniqueIndex(pCFA.getAllNodes(), CFANode::getNodeNumber);
    pMetaDataSet.forEach(n -> startingPoints.put(n.getStartNode(), n));
    pMetaDataSet.forEach(n -> endingPoints.put(n.getLastNode(), n));
    ImmutableSet.Builder<BlockNode> nodes = ImmutableSet.builder();
    BlockNode root = null;
    if (pPrependRoot) {
      BlockNodeMetaData rootMetaData =
          new BlockNodeMetaData(
              "root",
              pCFA.getMainFunction(),
              pCFA.getMainFunction(),
              pCFA.getMainFunction(),
              ImmutableSet.of(pCFA.getMainFunction()),
              ImmutableSet.of());
      root =
          new BlockNode(
              rootMetaData,
              ImmutableSet.of(),
              ImmutableSet.copyOf(startingPoints.get(pCFA.getMainFunction())),
              ImmutableSet.of(),
              pNotifier,
              idToNode);
      nodes.add(root);
    }
    for (BlockNodeMetaData blockNode : pMetaDataSet) {
      ImmutableSet.Builder<BlockNodeMetaData> predecessors =
          ImmutableSet.<BlockNodeMetaData>builder()
              .addAll(endingPoints.get(blockNode.getStartNode()));
      // check for root != null unnecessary but CI complained.
      if (pPrependRoot && root != null && blockNode.getStartNode().equals(root.getLastNode())) {
        predecessors.add(root.getMetaData());
      }
      Set<BlockNodeMetaData> predecessorSet = predecessors.build();
      Multimap<BlockNodeMetaData, BlockNodeMetaData> loopPredecessors =
          findLoopPredecessors(pCFA, startingPoints);
      BlockNode curr =
          new BlockNode(
              blockNode,
              predecessorSet,
              ImmutableSet.copyOf(startingPoints.get(blockNode.getLastNode())),
              ImmutableSet.copyOf(loopPredecessors.get(blockNode)),
              pNotifier,
              idToNode);
      nodes.add(curr);
      if (root == null && curr.getStartNode().equals(pCFA.getMainFunction())) {
        root = curr;
      }
    }
    return new BlockGraph(root, nodes.build());
  }

  public BlockGraph prependDummyRoot(CFA pCFA, ShutdownNotifier pNotifier)
      throws InterruptedException {
    return fromMetaData(
        transformedImmutableSetCopy(allNodes, n -> n.getMetaData()), pCFA, pNotifier, true);
  }
}
