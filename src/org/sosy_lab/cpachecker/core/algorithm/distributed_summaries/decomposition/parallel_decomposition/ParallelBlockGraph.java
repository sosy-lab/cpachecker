// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.parallel_decomposition;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNodeWithoutGraphInformation;

/*
 * Like BlockGraph but the root has all blocks as successors
 */
public class ParallelBlockGraph extends BlockGraph {

  private final int uniquePaths;
  private final BlockNode entryBlock;
  private final Map<String, BlockNode> functionToBlock;

  public ParallelBlockGraph(
      BlockNode pRoot, ImmutableSet<BlockNode> pNodes, String pEntryFunction) {
    super(pRoot, pNodes);
    functionToBlock =
        pNodes.stream().collect(Collectors.toMap(n -> n.getFirst().getFunctionName(), n -> n));
    Optional<BlockNode> maybeEntryBlock =
        pNodes.stream()
            .filter(n -> n.getFirst().getFunctionName().equals(pEntryFunction))
            .findFirst();
    entryBlock = maybeEntryBlock.isPresent() ? maybeEntryBlock.get() : pRoot;
    uniquePaths = uniquePaths(pNodes);
  }

  public static ParallelBlockGraph fromBlockNodesWithoutGraphInformation(
      CFA pCFA, Collection<? extends BlockNodeWithoutGraphInformation> pNodes) {
    Multimap<CFANode, BlockNodeWithoutGraphInformation> startNodes = ArrayListMultimap.create();
    Multimap<CFANode, BlockNodeWithoutGraphInformation> endNodes = ArrayListMultimap.create();
    for (BlockNodeWithoutGraphInformation blockNode : pNodes) {
      startNodes.put(blockNode.getFirst(), blockNode);
      endNodes.put(blockNode.getLast(), blockNode);
    }
    BlockNode root =
        new BlockNode(
            BlockGraph.ROOT_ID,
            pCFA.getMainFunction(),
            pCFA.getMainFunction(),
            ImmutableSet.of(pCFA.getMainFunction()),
            ImmutableSet.of(),
            ImmutableSet.of(),
            ImmutableSet.of(),
            FluentIterable.from(pNodes)
                .transform(BlockNodeWithoutGraphInformation::getId)
                .filter(id -> !id.equals(ROOT_ID))
                .toSet());

    Multimap<BlockNodeWithoutGraphInformation, BlockNodeWithoutGraphInformation> loopPredecessors =
        findLoopPredecessors(root, pNodes);

    startNodes.put(root.getFirst(), root);
    endNodes.put(root.getLast(), root);
    ImmutableSet<BlockNode> blockNodes =
        transformedImmutableSetCopy(
            pNodes,
            b ->
                new BlockNode(
                    b.getId(),
                    b.getFirst(),
                    b.getLast(),
                    b.getNodes(),
                    b.getEdges(),
                    ImmutableSet.of(ROOT_ID),
                    Sets.intersection(
                            transformedImmutableSetCopy(
                                endNodes.get(b.getFirst()),
                                BlockNodeWithoutGraphInformation::getId),
                            transformedImmutableSetCopy(
                                loopPredecessors.get(b), BlockNodeWithoutGraphInformation::getId))
                        .immutableCopy(),
                    transformedImmutableSetCopy(
                        startNodes.get(b.getLast()), BlockNodeWithoutGraphInformation::getId)));

    String entryFunction = pCFA.getMainFunction().getFunctionName();
    return new ParallelBlockGraph(root, blockNodes, entryFunction);
  }

  public int getUniquePaths() {
    return uniquePaths;
  }

  public BlockNode getEntryBlock() {
    return entryBlock;
  }

  /*Unique paths is the number of paths from the entry to the exit of the program.
   * It is calculated by a topological sort of the program graph.
   * The path count of a node is the sum of the path counts of its predecessors.
   * The path count of the exit nodes is the number of paths from the entry to the exit.
   */
  private int uniquePaths(ImmutableSet<BlockNode> nodes) {
    Map<BlockNode, Integer> pathCount = new HashMap<>(nodes.size());
    nodes.forEach(n -> pathCount.put(n, 0));
    pathCount.put(entryBlock, 1);

    // perform a topological sort
    List<BlockNode> sortedNodes = new ArrayList<>();
    sortedNodes.add(entryBlock);
    sortedNodes = ImmutableList.copyOf(topologicalSort(sortedNodes, entryBlock));

    // iterate over the sorted nodes and update the path count
    for (BlockNode node : sortedNodes) {
      for (BlockNode succ : getSummarySuccessors(node)) {
        pathCount.put(succ, pathCount.get(succ) + pathCount.get(node));
      }
    }

    return nodes.stream().mapToInt(n -> pathCount.get(n)).sum();
  }

  // Recursive bfs for topological sort
  private List<BlockNode> topologicalSort(List<BlockNode> builder, BlockNode root) {
    ImmutableSet<BlockNode> successorBlocks = getSummarySuccessors(root);
    for (BlockNode successor : successorBlocks) {
      if (!builder.contains(successor)) {
        builder.add(successor);
      }
    }

    for (BlockNode successor : successorBlocks) {
      builder = topologicalSort(builder, successor);
    }
    return builder;
  }

  private ImmutableSet<BlockNode> getSummarySuccessors(BlockNode root) {
    return root.getEdges().stream()
        .filter(
            e ->
                e instanceof FunctionSummaryEdge fce
                    && !fce.getFunctionEntry().getFunctionName().equals("reach_error"))
        .map(FunctionSummaryEdge.class::cast)
        .map(e -> e.getFunctionEntry().getFunctionName())
        .distinct()
        .map(name -> functionToBlock.get(name))
        .collect(ImmutableSet.toImmutableSet());
  }
}
