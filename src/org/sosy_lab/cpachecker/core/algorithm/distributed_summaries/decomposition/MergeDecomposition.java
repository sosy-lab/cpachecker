// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode.BlockNodeMetaData;
import org.sosy_lab.cpachecker.util.Pair;

public class MergeDecomposition implements CFADecomposer {

  private final int numberOfBlocks;
  private final CFADecomposer decomposer;
  private final ShutdownNotifier notifier;

  public MergeDecomposition(CFADecomposer pDecomposer, int pDesiredNumberOfBlocks, ShutdownNotifier pShutdownNotifier) {
    decomposer = pDecomposer;
    numberOfBlocks = pDesiredNumberOfBlocks;
    notifier = pShutdownNotifier;
  }

  @Override
  public BlockGraph decompose(CFA cfa) throws InterruptedException {
    Map<Integer, CFANode> idToNode = Maps.uniqueIndex(cfa.getAllNodes(), CFANode::getNodeNumber);
    BlockGraph merged = decomposer.decompose(cfa);
    while (merged.getDistinctNodes().size() > numberOfBlocks) {
      int sizeBefore = merged.getDistinctNodes().size();
      merged = mergeHorizontally(merged, cfa, idToNode);
      merged = mergeVertically(merged, cfa, idToNode);
      if (sizeBefore == merged.getDistinctNodes().size()) {
        break;
      }
    }
    return merged;
  }

  private BlockGraph mergeVertically(BlockGraph pGraph, CFA pCFA, Map<Integer, CFANode> pIntegerCFANodeMap)
      throws InterruptedException {
    if (pGraph.getDistinctNodes().size() <= numberOfBlocks) {
      return pGraph;
    }

    Set<BlockNodeMetaData> nodesMetaData = pGraph.getDistinctNodes().stream().map(BlockNode::getMetaData)
        .collect(Collectors.toCollection(LinkedHashSet::new));

    Predicate<BlockNode> hasOnlyOneSuccessor = n -> n.getSuccessors().size() == 1;
    Predicate<BlockNode> hasOnlyOnePredecessor = n -> n.getPredecessors().size() == 1;
    Map<BlockNodeMetaData, BlockNode> lookup = Maps.uniqueIndex(pGraph.getDistinctNodes(), n -> n.getMetaData());

    for (BlockNode distinctNode : pGraph.getDistinctNodes()) {
      if (hasOnlyOneSuccessor.test(distinctNode)) {
        BlockNodeMetaData successor = Iterables.getOnlyElement(distinctNode.getSuccessors());
        if (lookup.containsKey(successor) && hasOnlyOnePredecessor.test(lookup.get(successor))) {
          nodesMetaData.remove(distinctNode.getMetaData());
          nodesMetaData.remove(successor);
          Set<CFANode> allNodes = ImmutableSet.<CFANode>builder().addAll(distinctNode.getNodesInBlock()).addAll(successor.getNodesInBlock()).build();
          Set<CFAEdge> allEdges = ImmutableSet.<CFAEdge>builder().addAll(distinctNode.getEdgesInBlock()).addAll(successor.getEdgesInBlock()).build();
          BlockNodeMetaData mergedMetaData = new BlockNodeMetaData("M" + distinctNode.getId() + successor.getId(), distinctNode.getStartNode(), successor.getLastNode(), allNodes, allEdges, pIntegerCFANodeMap);
          nodesMetaData.add(mergedMetaData);
          return mergeVertically(BlockGraph.fromMetaData(nodesMetaData, pCFA, notifier), pCFA, pIntegerCFANodeMap);
        }
      }
    }
    return pGraph;
  }

  private BlockGraph mergeHorizontally(BlockGraph pGraph, CFA pCFA, Map<Integer, CFANode> pIntegerCFANodeMap)
      throws InterruptedException {
    Set<BlockNodeMetaData> nodesMetaData = pGraph.getDistinctNodes().stream().map(BlockNode::getMetaData)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    ImmutableListMultimap.Builder<Pair<CFANode, CFANode>, BlockNodeMetaData> entriesBuilder = ImmutableListMultimap.builder();
    for (BlockNodeMetaData node : nodesMetaData) {
      entriesBuilder.put(Pair.of(node.getStartNode(), node.getLastNode()), node);
    }
    ImmutableMultimap<Pair<CFANode, CFANode>, BlockNodeMetaData> entries = entriesBuilder.build();
    for (Pair<CFANode, CFANode> key : entries.keys()) {
      if (nodesMetaData.size() <= numberOfBlocks) {
        break;
      }
      ImmutableCollection<BlockNodeMetaData> blocks = entries.get(key);
      if (blocks.size() <= 1) {
        continue;
      }
      CFANode startNode = key.getFirstNotNull();
      CFANode lastNode = key.getSecondNotNull();
      ImmutableSet<CFANode>
          nodesInBlock = FluentIterable.from(blocks).transformAndConcat(b -> b.getNodesInBlock()).toSet();
      ImmutableSet<CFAEdge>
          edgesInBlock = FluentIterable.from(blocks).transformAndConcat(b -> b.getEdgesInBlock()).toSet();
      String id = "M" + Joiner.on("").join(FluentIterable.from(blocks).transform(b -> b.getId()));
      nodesMetaData.add(new BlockNodeMetaData(id, startNode, lastNode, nodesInBlock, edgesInBlock, pIntegerCFANodeMap));
      nodesMetaData.removeAll(blocks);
    }
    return BlockGraph.fromMetaData(nodesMetaData, pCFA, notifier);
  }
}
