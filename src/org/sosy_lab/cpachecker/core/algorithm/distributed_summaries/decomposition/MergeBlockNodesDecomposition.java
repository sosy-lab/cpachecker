// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Comparator;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;

public class MergeBlockNodesDecomposition implements DssBlockDecomposition {

  private final DssBlockDecomposition decomposer;
  private final long targetNumber;
  private final Comparator<BlockNode> sort;
  private final HorizontalMergeDecomposition horizontalMerger;
  private final VerticalMergeDecomposition verticalMerger;

  private final boolean allowSingleBlockDecomposition;

  public MergeBlockNodesDecomposition(
      DssBlockDecomposition pDecomposition,
      long pTargetNumber,
      int pHorizontalMergeLimit,
      Comparator<BlockNode> pSort,
      boolean pAllowSingleBlockDecomposition,
      boolean mergeFunctionCalls) {
    horizontalMerger =
        new HorizontalMergeDecomposition(
            pDecomposition, pTargetNumber, pHorizontalMergeLimit, pSort, mergeFunctionCalls);
    verticalMerger =
        new VerticalMergeDecomposition(pDecomposition, pTargetNumber, pSort, mergeFunctionCalls);
    decomposer = pDecomposition;
    targetNumber = pTargetNumber;
    sort = pSort;
    allowSingleBlockDecomposition = pAllowSingleBlockDecomposition;
  }

  @Override
  public BlockGraph decompose(CFA cfa) throws InterruptedException {
    if (targetNumber <= 1 && allowSingleBlockDecomposition) {
      return new SingleBlockDecomposition().decompose(cfa);
    }
    Collection<BlockNode> nodes = decomposer.decompose(cfa).getNodes();

    while (nodes.size() > targetNumber) {
      int sizeBefore = nodes.size();
      nodes = sorted(horizontalMerger.mergeHorizontally(nodes));
      if (nodes.size() <= targetNumber) {
        break;
      }
      nodes = sorted(verticalMerger.mergeVertically(nodes));
      if (sizeBefore == nodes.size()) {
        break;
      }
    }
    return new BlockGraph(ImmutableSet.copyOf(nodes));
  }

  public static boolean containCallsOrReturnsOfSameFunction(Iterable<BlockNode> toMerge) {

    return FluentIterable.from(toMerge)
        .transformAndConcat(b -> b.getEdges())
        .filter(e -> e instanceof FunctionCallEdge || e instanceof FunctionReturnEdge)
        .transform(e -> (e instanceof FunctionCallEdge) ? e.getSuccessor() : e.getPredecessor())
        .toMultiset()
        .entrySet()
        .stream()
        .anyMatch(entry -> entry.getCount() > 1);
  }

  /**
   * Whether a block made of the given blocks would pass its final location before its end. The
   * analysis of a block stops at the final location, so such a block would never explore the paths
   * that continue from there, e.g., the second call of a function whose entry is the final location
   * and whose first call lies in the block, too. A block whose initial and final location coincide
   * passes the final location only at its start.
   */
  public static boolean passesFinalLocationEarly(
      CFANode pInitialLocation, CFANode pFinalLocation, Iterable<BlockNode> pBlocks) {
    return !pInitialLocation.equals(pFinalLocation)
        && FluentIterable.from(pBlocks)
            .transformAndConcat(BlockNode::getEdges)
            .anyMatch(e -> e.getPredecessor().equals(pFinalLocation));
  }

  private Collection<BlockNode> sorted(Collection<BlockNode> pSort) {
    if (sort == null) {
      return pSort;
    }
    return ImmutableList.sortedCopyOf(sort, pSort);
  }
}
