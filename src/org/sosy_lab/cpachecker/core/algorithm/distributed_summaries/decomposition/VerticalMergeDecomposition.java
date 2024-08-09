// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Sara Ruckstuhl <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNodeWithoutGraphInformation;

public class VerticalMergeDecomposition implements DSSCFADecomposer {

  private final DSSCFADecomposer decomposer;
  private final long targetNumber;
  private final Comparator<BlockNodeWithoutGraphInformation> sort;
  private int id;

  public VerticalMergeDecomposition(
      DSSCFADecomposer pDecomposition,
      long pTargetNumber,
      Comparator<BlockNodeWithoutGraphInformation> pSort) {
    decomposer = pDecomposition;
    targetNumber = pTargetNumber;
    sort = pSort;
  }

  @Override
  public BlockGraph decompose(CFA cfa) throws InterruptedException {
    Collection<? extends BlockNodeWithoutGraphInformation> nodes =
        decomposer.decompose(cfa).getNodes();
    while (nodes.size() > targetNumber) {
      int sizeBefore = nodes.size();
      nodes = sorted(mergeVertically(nodes));
      if (nodes.size() <= targetNumber || sizeBefore == nodes.size()) {
        break;
      }
    }

    return BlockGraph.fromBlockNodesWithoutGraphInformation(cfa, nodes);
  }

  private Collection<BlockNodeWithoutGraphInformation> sorted(
      Collection<BlockNodeWithoutGraphInformation> pSort) {
    if (sort == null) {
      return pSort;
    }
    return ImmutableList.sortedCopyOf(sort, pSort);
  }

  public Collection<BlockNodeWithoutGraphInformation> mergeVertically(
      Collection<? extends BlockNodeWithoutGraphInformation> pNodes) {
    Multimap<CFANode, BlockNodeWithoutGraphInformation> startingPoints = ArrayListMultimap.create();
    Multimap<CFANode, BlockNodeWithoutGraphInformation> endingPoints = ArrayListMultimap.create();
    pNodes.forEach(
        n -> {
          startingPoints.put(n.getFirst(), n);
          endingPoints.put(n.getLast(), n);
        });
    Set<BlockNodeWithoutGraphInformation> removed = new LinkedHashSet<>();
    for (BlockNodeWithoutGraphInformation node : pNodes) {
      if (removed.contains(node)) {
        continue;
      }
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
          removed.add(uniquePredecessor);
          removed.add(uniqueSuccessor);
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
