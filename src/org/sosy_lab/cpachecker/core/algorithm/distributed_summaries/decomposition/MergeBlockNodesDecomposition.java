// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Comparator;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNodeWithoutGraphInformation;

public class MergeBlockNodesDecomposition implements DSSCFADecomposer {

  private final DSSCFADecomposer decomposer;
  private final long targetNumber;
  private final Comparator<BlockNodeWithoutGraphInformation> sort;
  private final HorizontalMergeDecomposition horizontalMerger;
  private final VerticalMergeDecomposition verticalMerger;

  private final boolean allowSingleBlockDecomposition;

  public MergeBlockNodesDecomposition(
      DSSCFADecomposer pDecomposition,
      long pTargetNumber,
      Comparator<BlockNodeWithoutGraphInformation> pSort,
      boolean pAllowSingleBlockDecomposition) {
    horizontalMerger = new HorizontalMergeDecomposition(pDecomposition, pTargetNumber, pSort);
    verticalMerger = new VerticalMergeDecomposition(pDecomposition, pTargetNumber, pSort);
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
    Collection<? extends BlockNodeWithoutGraphInformation> nodes =
        decomposer.decompose(cfa).getNodes();

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
    return BlockGraph.fromBlockNodesWithoutGraphInformation(cfa, nodes);
  }

  private Collection<BlockNodeWithoutGraphInformation> sorted(
      Collection<BlockNodeWithoutGraphInformation> pSort) {
    if (sort == null) {
      return pSort;
    }
    return ImmutableList.sortedCopyOf(sort, pSort);
  }
}
