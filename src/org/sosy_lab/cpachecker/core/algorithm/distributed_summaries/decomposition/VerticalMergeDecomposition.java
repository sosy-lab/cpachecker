// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Sara Ruckstuhl <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;

public class VerticalMergeDecomposition implements DssBlockDecomposition {

  private final DssBlockDecomposition decomposer;
  private final long targetNumber;
  private final Comparator<BlockNode> sort;
  private int id;

  public VerticalMergeDecomposition(
      DssBlockDecomposition pDecomposition, long pTargetNumber, Comparator<BlockNode> pSort) {
    decomposer = pDecomposition;
    targetNumber = pTargetNumber;
    sort = pSort;
  }

  @Override
  public BlockGraph decompose(CFA cfa) throws InterruptedException {
    Collection<BlockNode> nodes = decomposer.decompose(cfa).getNodes();
    while (nodes.size() > targetNumber) {
      int sizeBefore = nodes.size();
      nodes = sorted(mergeVertically(nodes));
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

  public Collection<BlockNode> mergeVertically(Collection<BlockNode> pNodes) {

    Map<String, BlockNode> blocks = new HashMap<>();
    pNodes.forEach(
        n -> {
          blocks.put(n.getId(), n);
        });

    MergeIDTracker idTracker = new MergeIDTracker(blocks.keySet());

    SequencedSet<BlockNode> removed = new LinkedHashSet<>();
    for (BlockNode node : pNodes) {
      if (removed.contains(node)) {
        continue;
      }

      if (node.getSuccessorIds().size() == 1) {
        String uniqueSuccessorID =
            idTracker.resolve(Iterables.getOnlyElement(node.getSuccessorIds()));
        BlockNode successor = blocks.get(uniqueSuccessorID);
        if (successor.getPredecessorIds().size() == 1) {
          String uniquePredecessorID = Iterables.getOnlyElement(successor.getPredecessorIds());
          assert uniquePredecessorID.equals(node.getId());

          BlockNode result = mergeBlocksVertically(node, successor);

          blocks.remove(uniqueSuccessorID);
          blocks.remove(uniquePredecessorID);
          blocks.put(result.getId(), result);

          removed.add(node);
          removed.add(successor);

          idTracker.merge(List.of(uniquePredecessorID, uniqueSuccessorID), result.getId());

          if (blocks.size() <= targetNumber) {
            break;
          }
        }
      }
    }

    return idTracker.mapBlockNodes(blocks.values());
  }

  private BlockNode mergeBlocksVertically(BlockNode pBlockNode1, BlockNode pBlockNode2) {
    return new BlockNode(
        "MV" + id++,
        pBlockNode1.getInitialLocation(),
        pBlockNode2.getFinalLocation(),
        ImmutableSet.copyOf(Iterables.concat(pBlockNode1.getNodes(), pBlockNode2.getNodes())),
        ImmutableSet.copyOf(Iterables.concat(pBlockNode1.getEdges(), pBlockNode2.getEdges())),
        pBlockNode1.getPredecessorIds(),
        pBlockNode1.getLoopPredecessorIds(),
        pBlockNode2.getSuccessorIds(),
        pBlockNode2.getLoopSuccessorIds());
  }
}
