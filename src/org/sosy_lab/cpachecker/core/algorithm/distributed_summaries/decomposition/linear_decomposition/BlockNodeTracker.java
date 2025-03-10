// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.linear_decomposition;

import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNodeWithoutGraphInformation;

class BlockNodeTracker {
  private final Set<CFAEdge> edgesInCurrentBlock;
  private final Set<CFANode> nodesInCurrentBlock;
  private CFANode first;
  private CFANode last;

  private int count;

  BlockNodeTracker() {
    edgesInCurrentBlock = new LinkedHashSet<>();
    nodesInCurrentBlock = new LinkedHashSet<>();
  }

  void track(CFAEdge pEdge) {
    edgesInCurrentBlock.add(pEdge);
    if (first == null) {
      first = pEdge.getPredecessor();
    }
    last = pEdge.getSuccessor();
    nodesInCurrentBlock.add(pEdge.getPredecessor());
    nodesInCurrentBlock.add(pEdge.getSuccessor());
  }

  BlockNodeWithoutGraphInformation finish() {
    assert nodesInCurrentBlock.contains(first) && nodesInCurrentBlock.contains(last);
    BlockNodeWithoutGraphInformation blockNodeWithoutGraphInformation =
        new BlockNodeWithoutGraphInformation(
            "L" + count++,
            first,
            last,
            ImmutableSet.copyOf(nodesInCurrentBlock),
            ImmutableSet.copyOf(edgesInCurrentBlock));
    first = null;
    last = null;
    nodesInCurrentBlock.clear();
    edgesInCurrentBlock.clear();
    return blockNodeWithoutGraphInformation;
  }
}
