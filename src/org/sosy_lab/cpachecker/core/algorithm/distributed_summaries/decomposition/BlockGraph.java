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
              ImmutableSet.of(pCFA.getMainFunction()),
              ImmutableSet.of(),
              idToNode);
      root =
          new BlockNode(
              rootMetaData,
              ImmutableSet.of(),
              ImmutableSet.copyOf(startingPoints.get(pCFA.getMainFunction())),
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
      BlockNode curr =
          new BlockNode(
              blockNode,
              predecessors.build(),
              ImmutableSet.copyOf(startingPoints.get(blockNode.getLastNode())),
              pNotifier,
              idToNode);
      nodes.add(curr);
      if (root == null && curr.getStartNode().equals(pCFA.getMainFunction())) {
        root = curr;
      }
    }
    return new BlockGraph(root, nodes.build());
  }

  public BlockGraph prependDummyRoot(CFA pCFA, ShutdownNotifier pShutdownNotifier)
      throws InterruptedException {
    return fromMetaData(
        transformedImmutableSetCopy(allNodes, n -> n.getMetaData()), pCFA, pShutdownNotifier, true);
  }
}