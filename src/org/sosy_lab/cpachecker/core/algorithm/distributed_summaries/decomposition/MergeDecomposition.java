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
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode.BlockNodeMetaData;
import org.sosy_lab.cpachecker.util.Pair;

@Options
public class MergeDecomposition implements CFADecomposer {

  @Option(
      name = "distributedSummaries.desiredNumberOfBlocks",
      description = "desired number of BlockNodes")
  private int desiredNumberOfBlocks = 0;

  private final CFADecomposer decomposer;
  private final ShutdownNotifier notifier;

  public MergeDecomposition(
      CFADecomposer pDecomposer, Configuration pConfiguration, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    pConfiguration.inject(this);
    decomposer = pDecomposer;
    notifier = pShutdownNotifier;
  }

  @Override
  public BlockGraph decompose(CFA cfa) throws InterruptedException {
    BlockGraph merged = decomposer.decompose(cfa);
    while (merged.getDistinctNodes().size() > desiredNumberOfBlocks) {
      int sizeBefore = merged.getDistinctNodes().size();
      merged = mergeHorizontally(merged, cfa);
      merged = mergeVertically(merged, cfa);
      if (sizeBefore == merged.getDistinctNodes().size()) {
        break;
      }
    }
    return merged;
  }

  private BlockGraph mergeVertically(BlockGraph pGraph, CFA pCFA) throws InterruptedException {
    if (pGraph.getDistinctNodes().size() <= desiredNumberOfBlocks) {
      return pGraph;
    }

    Set<BlockNodeMetaData> nodesMetaData =
        pGraph.getDistinctNodes().stream()
            .map(BlockNode::getMetaData)
            .collect(Collectors.toCollection(LinkedHashSet::new));

    Predicate<BlockNode> hasOnlyOneSuccessor = n -> n.getSuccessors().size() == 1;
    Predicate<BlockNode> hasOnlyOnePredecessor = n -> n.getPredecessors().size() == 1;
    Map<BlockNodeMetaData, BlockNode> lookup =
        Maps.uniqueIndex(pGraph.getDistinctNodes(), n -> n.getMetaData());

    for (BlockNode distinctNode : pGraph.getDistinctNodes()) {
      if (hasOnlyOneSuccessor.test(distinctNode)) {
        BlockNodeMetaData successor = Iterables.getOnlyElement(distinctNode.getSuccessors());
        if (lookup.containsKey(successor) && hasOnlyOnePredecessor.test(lookup.get(successor))) {
          nodesMetaData.remove(distinctNode.getMetaData());
          nodesMetaData.remove(successor);
          Set<CFANode> allNodes =
              ImmutableSet.<CFANode>builder()
                  .addAll(distinctNode.getNodesInBlock())
                  .addAll(successor.getNodesInBlock())
                  .build();
          Set<CFAEdge> allEdges =
              ImmutableSet.<CFAEdge>builder()
                  .addAll(distinctNode.getEdgesInBlock())
                  .addAll(successor.getEdgesInBlock())
                  .build();
          BlockNodeMetaData mergedMetaData =
              new BlockNodeMetaData(
                  "M" + distinctNode.getId() + successor.getId(),
                  distinctNode.getStartNode(),
                  successor.getLastNode(),
                  successor.getAbstractionEnd(),
                  allNodes,
                  allEdges);
          nodesMetaData.add(mergedMetaData);
          return mergeVertically(BlockGraph.fromMetaData(nodesMetaData, pCFA, notifier), pCFA);
        }
      }
    }
    return pGraph;
  }

  private BlockGraph mergeHorizontally(BlockGraph pGraph, CFA pCFA) throws InterruptedException {
    Set<BlockNodeMetaData> nodesMetaData =
        pGraph.getDistinctNodes().stream()
            .map(BlockNode::getMetaData)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    ImmutableListMultimap.Builder<Pair<CFANode, CFANode>, BlockNodeMetaData> entriesBuilder =
        ImmutableListMultimap.builder();
    for (BlockNodeMetaData node : nodesMetaData) {
      entriesBuilder.put(Pair.of(node.getStartNode(), node.getLastNode()), node);
    }
    ImmutableMultimap<Pair<CFANode, CFANode>, BlockNodeMetaData> entries = entriesBuilder.build();
    for (Pair<CFANode, CFANode> key : entries.keys()) {
      if (nodesMetaData.size() <= desiredNumberOfBlocks) {
        break;
      }
      ImmutableCollection<BlockNodeMetaData> blocks = entries.get(key);
      if (blocks.size() <= 1) {
        continue;
      }
      CFANode startNode = key.getFirstNotNull();
      CFANode lastNode = key.getSecondNotNull();
      Set<CFANode> nodesInBlock =
          FluentIterable.from(blocks)
              .transformAndConcat(b -> b.getNodesInBlock())
              .copyInto(new LinkedHashSet<>());
      Set<CFAEdge> edgesInBlock =
          FluentIterable.from(blocks)
              .transformAndConcat(b -> b.getEdgesInBlock())
              .copyInto(new LinkedHashSet<>());
      for (CFAEdge blockEndBlankEdge :
          FluentIterable.from(edgesInBlock)
              .filter(e -> e.getDescription().equals(BlockEndUtil.UNIQUE_DESCRIPTION))
              .filter(e -> !e.getPredecessor().equals(lastNode))) {
        nodesInBlock.remove(blockEndBlankEdge.getSuccessor());
        edgesInBlock.remove(blockEndBlankEdge);
      }
      String id = "M" + Joiner.on("").join(FluentIterable.from(blocks).transform(b -> b.getId()));
      CFANode abstraction = Iterables.getFirst(blocks, null).getAbstractionEnd();
      if (abstraction == null) {
        throw new AssertionError("Abstraction location cannot be null");
      }
      nodesMetaData.add(
          new BlockNodeMetaData(id, startNode, lastNode, abstraction, nodesInBlock, edgesInBlock));
      blocks.forEach(nodesMetaData::remove);
    }
    return BlockGraph.fromMetaData(nodesMetaData, pCFA, notifier);
  }
}
