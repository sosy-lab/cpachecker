// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNodeWithoutGraphInformation;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.linear_decomposition.LinearBlockNodeDecomposition;

public class MergeBlockNodesDecomposition implements BlockSummaryCFADecomposer {

  private final Predicate<CFANode> isBlockEnd;
  private final long targetNumber;
  private final boolean prioritize;
  private int id;

  private record BlockScope(CFANode start, CFANode last) {}

  public MergeBlockNodesDecomposition(
      Predicate<CFANode> pIsBlockEnd, boolean pPrioritize, long pTargetNumber) {
    isBlockEnd = pIsBlockEnd;
    targetNumber = pTargetNumber;
    prioritize = pPrioritize;
  }

  @Override
  public BlockGraph decompose(CFA cfa) throws InterruptedException {
    if (targetNumber <= 1) {
      return new SingleBlockDecomposition().decompose(cfa);
    }
    LinearBlockNodeDecomposition linearBlockNodeDecomposition =
        new LinearBlockNodeDecomposition(isBlockEnd);
    Collection<? extends BlockNodeWithoutGraphInformation> nodes =
        linearBlockNodeDecomposition.decompose(cfa).getNodes();
    while (nodes.size() > targetNumber) {
      int sizeBefore = nodes.size();
      nodes = mergeHorizontally(nodes);
      if (nodes.size() <= targetNumber) {
        break;
      }
      nodes = mergeVertically(nodes);
      if (nodes.size() <= targetNumber) {
        break;
      }
      nodes = mergeSubsumption(nodes);
      if (sizeBefore == nodes.size()) {
        // also quit if no more merges are possible
        break;
      }
    }
    return BlockGraph.fromBlockNodesWithoutGraphInformation(cfa, nodes);
  }

  private Collection<BlockNodeWithoutGraphInformation> mergeSubsumption(
      Collection<? extends BlockNodeWithoutGraphInformation> pNodes) {
    Set<BlockNodeWithoutGraphInformation> available = new LinkedHashSet<>(pNodes);
    for (BlockNodeWithoutGraphInformation node1 : pNodes) {
      for (BlockNodeWithoutGraphInformation node2 : pNodes) {
        if (!available.contains(node1) || !available.contains(node2) || node1 == node2) {
          // already merged
          continue;
        }
        if (node1.getFirst().equals(node2.getFirst()) && node1.getFirst().equals(node2.getLast())) {
          BlockNodeWithoutGraphInformation merged = mergeSubsumedBlocks(node1, node2);
          available.remove(node1);
          available.remove(node2);
          available.add(merged);
          if (available.size() <= targetNumber) {
            return available;
          }
        }
      }
    }
    return available;
  }

  private Collection<BlockNodeWithoutGraphInformation> mergeHorizontally(
      Collection<? extends BlockNodeWithoutGraphInformation> pNodes) {
    Multimap<BlockScope, BlockNodeWithoutGraphInformation> blockScopes = ArrayListMultimap.create();
    pNodes.forEach(n -> blockScopes.put(new BlockScope(n.getFirst(), n.getLast()), n));
    for (BlockScope blockScope : ImmutableSet.copyOf(blockScopes.keySet())) {
      if (blockScopes.get(blockScope).size() <= 1) {
        continue;
      }
      BlockNodeWithoutGraphInformation result =
          mergeBlocksHorizontally(blockScopes.removeAll(blockScope), blockScope);
      blockScopes.put(blockScope, result);
      if (blockScopes.size() <= targetNumber) {
        return blockScopes.values();
      }
    }
    return blockScopes.values();
  }

  private Collection<BlockNodeWithoutGraphInformation> mergeVertically(
      Collection<? extends BlockNodeWithoutGraphInformation> pNodes) {
    Multimap<CFANode, BlockNodeWithoutGraphInformation> startingPoints = ArrayListMultimap.create();
    Multimap<CFANode, BlockNodeWithoutGraphInformation> endingPoints = ArrayListMultimap.create();

    // prioritization
    List<BlockNodeWithoutGraphInformation> prioritized = new ArrayList<>(pNodes.size());
    pNodes.forEach(
        n -> {
          startingPoints.put(n.getFirst(), n);
          endingPoints.put(n.getLast(), n);
          // merge where we cant append an abstraction edge and where functions start
          if (prioritize
              && (n.getLast().getLeavingSummaryEdge() != null
                  || n.getFirst() instanceof FunctionEntryNode)) {
            prioritized.add(0, n);
          } else {
            prioritized.add(n);
          }
        });
    for (BlockNodeWithoutGraphInformation node : prioritized) {
      Collection<BlockNodeWithoutGraphInformation> successors = startingPoints.get(node.getLast());
      if (successors.size() == 1) {
        BlockNodeWithoutGraphInformation uniqueSuccessor = Iterables.getOnlyElement(successors);
        Collection<BlockNodeWithoutGraphInformation> predecessors =
            endingPoints.get(uniqueSuccessor.getFirst());
        if (predecessors.size() == 1) {
          BlockNodeWithoutGraphInformation uniquePredecessor =
              Iterables.getOnlyElement(predecessors);
          assert uniquePredecessor == node; // same reference is correct here
          BlockNodeWithoutGraphInformation result =
              mergeBlocksVertically(uniquePredecessor, uniqueSuccessor);
          startingPoints.remove(uniquePredecessor.getFirst(), uniquePredecessor);
          startingPoints.remove(uniqueSuccessor.getFirst(), uniqueSuccessor);
          endingPoints.remove(uniquePredecessor.getLast(), uniquePredecessor);
          endingPoints.remove(uniqueSuccessor.getLast(), uniqueSuccessor);
          startingPoints.put(result.getFirst(), result);
          endingPoints.put(result.getLast(), result);
          if (startingPoints.size() <= targetNumber) {
            assert startingPoints.values().containsAll(endingPoints.values());
            return startingPoints.values();
          }
        }
      }
    }
    assert startingPoints.values().containsAll(endingPoints.values());
    return startingPoints.values();
  }

  private BlockNodeWithoutGraphInformation mergeSubsumedBlocks(
      BlockNodeWithoutGraphInformation pOuter, BlockNodeWithoutGraphInformation pInner) {
    Preconditions.checkArgument(
        pOuter.getNodes().contains(pInner.getFirst())
            && pOuter.getNodes().contains(pInner.getLast()));
    return new BlockNodeWithoutGraphInformation(
        "MS" + id++,
        pOuter.getFirst(),
        pOuter.getLast(),
        ImmutableSet.<CFANode>builder().addAll(pOuter.getNodes()).addAll(pInner.getNodes()).build(),
        ImmutableSet.<CFAEdge>builder()
            .addAll(pOuter.getEdges())
            .addAll(pInner.getEdges())
            .build());
  }

  private BlockNodeWithoutGraphInformation mergeBlocksHorizontally(
      Collection<BlockNodeWithoutGraphInformation> pNodes, BlockScope pScope) {
    Preconditions.checkArgument(
        pNodes.stream()
            .allMatch(
                b -> b.getFirst().equals(pScope.start()) && b.getLast().equals(pScope.last())),
        "Some of the given nodes do not have the same scope.");
    return new BlockNodeWithoutGraphInformation(
        "MH" + id++,
        pScope.start(),
        pScope.last(),
        FluentIterable.from(pNodes)
            .transformAndConcat(BlockNodeWithoutGraphInformation::getNodes)
            .toSet(),
        FluentIterable.from(pNodes)
            .transformAndConcat(BlockNodeWithoutGraphInformation::getEdges)
            .toSet());
  }

  private BlockNodeWithoutGraphInformation mergeBlocksVertically(
      BlockNodeWithoutGraphInformation pBlockNode1, BlockNodeWithoutGraphInformation pBlockNode2) {
    return new BlockNodeWithoutGraphInformation(
        "MV" + id++,
        pBlockNode1.getFirst(),
        pBlockNode2.getLast(),
        ImmutableSet.copyOf(Iterables.concat(pBlockNode1.getNodes(), pBlockNode2.getNodes())),
        ImmutableSet.copyOf(Iterables.concat(pBlockNode1.getEdges(), pBlockNode2.getEdges())));
  }
}
