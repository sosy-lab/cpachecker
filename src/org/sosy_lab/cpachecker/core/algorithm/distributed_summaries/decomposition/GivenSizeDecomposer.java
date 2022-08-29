// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;

public class GivenSizeDecomposer implements CFADecomposer {

  private final CFADecomposer decomposer;
  private final int desiredNumberOfBlocks;

  /**
   * A decomposer that merges as many parts as possible to maybe reach the desired number of blocks
   *
   * @param pDecomposer parent decomposer
   * @param pDesiredNumber desired number of blocks
   * @throws InvalidConfigurationException thrown if configuration is invalid
   */
  public GivenSizeDecomposer(CFADecomposer pDecomposer, int pDesiredNumber)
      throws InvalidConfigurationException {
    decomposer = pDecomposer;
    desiredNumberOfBlocks = pDesiredNumber;
  }

  @Override
  public BlockGraph cut(CFA cfa) throws InterruptedException {
    BlockGraph blockGraph = decomposer.cut(cfa);
    int oldSize = blockGraph.getDistinctNodes().size();
    while (oldSize >= desiredNumberOfBlocks) {
      blockGraph = BlockGraph.merge(blockGraph, desiredNumberOfBlocks);
      int newSize = blockGraph.getDistinctNodes().size();
      if (newSize == oldSize) {
        break;
      }
      oldSize = newSize;
    }
    return blockGraph;
  }
}
