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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.SequencedSet;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;

public class VerticalMergeDecomposition implements DssBlockDecomposition {

  private final DssBlockDecomposition decomposer;
  private final long targetNumber;
  private final Comparator<BlockNode> sort;
  private int id;

  private final boolean mergeFunctionCalls;

  public VerticalMergeDecomposition(
      DssBlockDecomposition pDecomposition,
      long pTargetNumber,
      Comparator<BlockNode> pSort,
      boolean pMergeFunctionCalls) {
    decomposer = pDecomposition;
    targetNumber = pTargetNumber;
    sort = pSort;
    mergeFunctionCalls = pMergeFunctionCalls;
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

    // Insertion order, because blocks.values() below becomes the input of the next merge round
    Map<String, BlockNode> blocks = new LinkedHashMap<>();
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

          if (!mergeFunctionCalls
              && MergeBlockNodesDecomposition.containCallsOrReturnsOfSameFunction(
                  ImmutableList.of(successor, node))) {
            continue;
          }

          if (mergeReachesFinalLocationEarly(node, successor)) {
            continue;
          }

          BlockNode result = mergeBlocksVertically(node, successor);

          blocks.remove(uniqueSuccessorID);
          blocks.remove(uniquePredecessorID);
          blocks.put(result.getId(), result);

          removed.add(node);
          removed.add(successor);

          idTracker.merge(ImmutableList.of(uniquePredecessorID, uniqueSuccessorID), result.getId());

          if (blocks.size() <= targetNumber) {
            break;
          }
        }
      }
    }

    return idTracker.mapBlockNodeEdges(blocks.values());
  }

  /**
   * The block analysis stops at the first arrival at the final location of a block. Merging must
   * therefore not create a block whose final location is passed before its end, or the edges behind
   * that first arrival become unreachable. In a CFA, this cannot happen because every block end is
   * a block boundary, but the copies of the inlining decomposition share their CFA nodes, so the
   * end of one copy can lie inside the merged block as a node of an earlier copy.
   */
  private static boolean mergeReachesFinalLocationEarly(BlockNode pFirst, BlockNode pSecond) {
    CFANode initial = pFirst.getInitialLocation();
    CFANode end = pSecond.getFinalLocation();
    if (initial.equals(end)) {
      // the merged block starts at its end, so it must not return to it before its last edge
      return pFirst.getEdges().stream().anyMatch(e -> e.getSuccessor().equals(initial));
    }
    return Iterables.any(pFirst.getEdges(), e -> e.getPredecessor().equals(end))
        || Iterables.any(pSecond.getEdges(), e -> e.getPredecessor().equals(end));
  }

  private BlockNode mergeBlocksVertically(BlockNode pBlockNode1, BlockNode pBlockNode2) {
    return new BlockNode(
        "MV" + id++,
        pBlockNode1.getInitialLocation(),
        pBlockNode2.getFinalLocation(),
        ImmutableSet.copyOf(Iterables.concat(pBlockNode1.getNodes(), pBlockNode2.getNodes())),
        ImmutableSet.copyOf(Iterables.concat(pBlockNode1.getEdges(), pBlockNode2.getEdges())),
        pBlockNode1.getPredecessorIds(),
        pBlockNode2.getSuccessorIds());
  }
}
