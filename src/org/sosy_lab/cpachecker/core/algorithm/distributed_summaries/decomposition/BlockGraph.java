// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode.BlockGraphBuilder;

/**
 * Represents a partitioning of a CFA. The blocks contain coherent subgraphs of a CFA. The
 * successors of a block are the blocks that contain successive subgraphs of the same CFA.
 */
public class BlockGraph {

  private final BlockNode root;
  private final BlockGraphBuilder factory;

  /**
   * Represents the CFA but partitioned into multiple connected blocks.
   *
   * @param pRoot the root node of the
   * @param pFactory
   */
  public BlockGraph(BlockNode pRoot, BlockGraphBuilder pFactory) {
    root = pRoot;
    factory = pFactory;
  }

  public BlockNode getRoot() {
    return root;
  }

  public ImmutableSet<BlockNode> getDistinctNodes() {
    Set<BlockNode> nodes = new HashSet<>();
    ArrayDeque<BlockNode> waiting = new ArrayDeque<>();
    waiting.add(root);
    while (!waiting.isEmpty()) {
      BlockNode top = waiting.pop();
      if (nodes.add(top)) {
        waiting.addAll(top.getSuccessors());
      }
    }
    return ImmutableSet.copyOf(nodes);
  }

  public BlockGraph merge(int pDesiredNumberOfBlocks) {
    return factory.merge(pDesiredNumberOfBlocks);
  }
}
