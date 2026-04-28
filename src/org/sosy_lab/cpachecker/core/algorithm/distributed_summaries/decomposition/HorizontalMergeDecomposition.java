// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Sara Ruckstuhl <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Comparator;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;

public class HorizontalMergeDecomposition implements DssBlockDecomposition {

  private final DssBlockDecomposition decomposer;
  private final long targetNumber;
  private final Comparator<BlockNode> sort;
  private int id;

  private record BlockScope(ImmutableSet<String> predecessors, ImmutableSet<String> successors) {}

  public HorizontalMergeDecomposition(
      DssBlockDecomposition pDecomposition, long pTargetNumber, Comparator<BlockNode> pSort) {
    decomposer = pDecomposition;
    targetNumber = pTargetNumber;
    sort = pSort;
  }

  @Override
  public BlockGraph decompose(CFA pCfa) throws InterruptedException {
    Collection<BlockNode> nodes = decomposer.decompose(pCfa).getNodes();
    while (nodes.size() > targetNumber) {
      int sizeBefore = nodes.size();
      nodes = sorted(mergeHorizontally(nodes));
      if (nodes.size() <= targetNumber || sizeBefore == nodes.size()) {
        break;
      }
    }
    return new BlockGraph(ImmutableSet.copyOf(nodes));
  }

  private Collection<BlockNode> sorted(Collection<BlockNode> pSort) {
    if (sort == null) {
      return pSort;
    }
    return ImmutableList.sortedCopyOf(sort, pSort);
  }

  Collection<BlockNode> mergeHorizontally(Collection<BlockNode> pNodes) {
    Multimap<BlockScope, BlockNode> blockScopes = ArrayListMultimap.create();
    pNodes.forEach(
        n -> blockScopes.put(new BlockScope(n.getPredecessorIds(), n.getSuccessorIds()), n));

    MergeIDTracker idTracker =
        new MergeIDTracker(FluentIterable.from(pNodes).transform(n -> n.getId()));

    for (BlockScope blockScope : ImmutableSet.copyOf(blockScopes.keySet())) {
      if (blockScopes.get(blockScope).size() <= 1) {
        continue;
      }

      Collection<BlockNode> toMerge = blockScopes.removeAll(blockScope);
      BlockNode result = mergeBlocksHorizontally(toMerge, blockScope);

      idTracker.merge(FluentIterable.from(toMerge).transform(n -> n.getId()), result.getId());

      blockScopes.put(blockScope, result);
      if (blockScopes.size() <= targetNumber) {
        break;
      }
    }
    return idTracker.mapBlockNodes(blockScopes.values());
  }

  private BlockNode mergeBlocksHorizontally(Collection<BlockNode> pCollection, BlockScope pScope) {
    Preconditions.checkArgument(
        pCollection.stream()
            .allMatch(
                b ->
                    b.getSuccessorIds().equals(pScope.successors())
                        && b.getPredecessorIds().equals(pScope.predecessors())),
        "Some of the given nodes do not have the same scope.");

    BlockNode first = pCollection.iterator().next();

    return new BlockNode(
        "MH" + id++,
        first.getInitialLocation(),
        first.getFinalLocation(),
        FluentIterable.from(pCollection).transformAndConcat(BlockNode::getNodes).toSet(),
        FluentIterable.from(pCollection).transformAndConcat(BlockNode::getEdges).toSet(),
        first.getPredecessorIds(),
        first.getLoopPredecessorIds(),
        first.getSuccessorIds(),
        first.getLoopSuccessorIds());
  }
}
