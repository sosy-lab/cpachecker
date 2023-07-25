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
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNodeWithoutGraphInformation;

/*
 * Like BlockGraph but the root has all blocks as successors
 */
public class ParallelBlockGraph extends BlockGraph {

  private final int uniquePaths;
  private final BlockNode entryBlock;

  public ParallelBlockGraph(BlockNode pRoot, ImmutableSet<BlockNode> pNodes, String pEntryFunction) {
    super(pRoot, pNodes);
    uniquePaths = uniquePaths(pNodes, pEntryFunction);
    ImmutableList<BlockNode> entryNodes =
        pNodes.stream()
            .filter(n -> n.getFirst().getFunctionName().equals(pEntryFunction))
            .collect(ImmutableList.toImmutableList());
    entryBlock = entryNodes.get(0);
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

  public BlockNode getEntryBlock(){
    return entryBlock;
  }


  /*Unique paths is the number of paths from the entry to the exit of the program.
   * It is calculated by a topological sort of the program graph.
   * The path count of a node is the sum of the path counts of its predecessors.
   * The path count of the exit nodes is the number of paths from the entry to the exit.
   */
  private static int uniquePaths(ImmutableSet<BlockNode> nodes, String entryFunction) {
    ImmutableList<BlockNode> entryNodes =
        nodes.stream()
            .filter(n -> n.getFirst().getFunctionName().equals(entryFunction))
            .collect(ImmutableList.toImmutableList());
    BlockNode root = entryNodes.get(0);

    // initialize path count and function to block map
    Map<String, BlockNode> functionToBlock =
        nodes.stream()
            .collect(Collectors.toMap(n -> n.getFirst().getFunctionName(), n -> n));
    Map<BlockNode, Integer> pathCount = new HashMap<>(nodes.size());
    nodes.forEach(n -> pathCount.put(n, 0));
    pathCount.put(root, 1);

    // perform a topological sort
    List<BlockNode> sortedNodes = new ArrayList<>();
    sortedNodes.add(root);
    sortedNodes = ImmutableList.copyOf(topologicalSort(sortedNodes, root, functionToBlock));

    // iterate over the sorted nodes and update the path count
    for (BlockNode node : sortedNodes) {
      for (BlockNode succ : getSummarySuccessors(node, functionToBlock)) {
        pathCount.put(succ, pathCount.get(succ) + pathCount.get(node));
      }
    }

    ImmutableList<BlockNode> exitNodes =
        nodes.stream()
            .filter(n -> getSummarySuccessors(n, functionToBlock).isEmpty())
            .collect(ImmutableList.toImmutableList());

    return exitNodes.stream().mapToInt(n -> pathCount.get(n)).sum();
  }

  // Recursive bfs for topological sort
  private static ImmutableList<BlockNode> topologicalSort(
      List<BlockNode> builder, BlockNode root, Map<String, BlockNode> functionToBlock) {
    List<BlockNode> successorBlocks = getSummarySuccessors(root, functionToBlock);

    for (BlockNode successor : successorBlocks) {
      if (!builder.contains(successor)) {
        builder.add(successor);
      }
    }

    for (BlockNode successor : successorBlocks) {
      builder = topologicalSort(builder, successor, functionToBlock);
    }
    return ImmutableList.copyOf(builder);
  }

  private static ImmutableList<BlockNode> getSummarySuccessors(
      BlockNode root, Map<String, BlockNode> functionToBlock) {
    return root.getEdges().stream()
        .filter(e -> e instanceof FunctionSummaryEdge)
        .map(e -> ((FunctionSummaryEdge) e).getFunctionEntry().getFunctionName())
        .map(name -> functionToBlock.get(name))
        .collect(ImmutableList.toImmutableList());
  }
}
