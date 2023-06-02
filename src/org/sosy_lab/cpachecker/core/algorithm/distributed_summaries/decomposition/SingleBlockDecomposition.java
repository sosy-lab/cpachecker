// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNodeWithoutGraphInformation;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class SingleBlockDecomposition implements BlockSummaryCFADecomposer {

  @Override
  public BlockGraph decompose(CFA cfa) throws InterruptedException {
    CFANode lastNode = null;
    ImmutableSet.Builder<CFAEdge> edges = ImmutableSet.builder();
    List<CFANode> waitlist = new ArrayList<>();
    Set<CFANode> seen = new LinkedHashSet<>();
    waitlist.add(cfa.getMainFunction());
    while (!waitlist.isEmpty()) {
      CFANode current = waitlist.remove(0);
      if (seen.contains(current)) {
        continue;
      }
      if (current.getNumLeavingEdges() == 0) {
        assert lastNode == null;
        lastNode = current;
      }
      seen.add(current);
      for (CFAEdge leavingEdge : CFAUtils.allLeavingEdges(current)) {
        edges.add(leavingEdge);
        waitlist.add(leavingEdge.getSuccessor());
      }
    }
    assert seen.containsAll(cfa.nodes());
    assert lastNode != null;
    return BlockGraph.fromBlockNodesWithoutGraphInformation(
        cfa,
        ImmutableSet.of(
            new BlockNodeWithoutGraphInformation(
                "SB1",
                cfa.getMainFunction(),
                lastNode,
                ImmutableSet.copyOf(cfa.nodes()),
                edges.build())));
  }
}
