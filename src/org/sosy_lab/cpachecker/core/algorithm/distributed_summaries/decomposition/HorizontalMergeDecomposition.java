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
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNodeWithoutGraphInformation;

public class HorizontalMergeDecomposition implements DSSCFADecomposer {

  private final DSSCFADecomposer decomposer;
  private final long targetNumber;
  private final Comparator<BlockNodeWithoutGraphInformation> sort;
  private int id;

  private record BlockScope(CFANode start, CFANode last) {}

  public HorizontalMergeDecomposition(
      DSSCFADecomposer pDecomposition,
      long pTargetNumber,
      Comparator<BlockNodeWithoutGraphInformation> pSort) {
    decomposer = pDecomposition;
    targetNumber = pTargetNumber;
    sort = pSort;
  }

  @Override
  public BlockGraph decompose(CFA pCfa) throws InterruptedException {
    Collection<? extends BlockNodeWithoutGraphInformation> nodes =
        decomposer.decompose(pCfa).getNodes();
    while (nodes.size() > targetNumber) {
      int sizeBefore = nodes.size();
      nodes = sorted(mergeHorizontally(nodes));
      if (nodes.size() <= targetNumber || sizeBefore == nodes.size()) {
        break;
      }
    }
    return BlockGraph.fromBlockNodesWithoutGraphInformation(pCfa, nodes);
  }

  private Collection<BlockNodeWithoutGraphInformation> sorted(
      Collection<BlockNodeWithoutGraphInformation> pSort) {
    if (sort == null) {
      return pSort;
    }
    return ImmutableList.sortedCopyOf(sort, pSort);
  }

  Collection<BlockNodeWithoutGraphInformation> mergeHorizontally(
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
}
