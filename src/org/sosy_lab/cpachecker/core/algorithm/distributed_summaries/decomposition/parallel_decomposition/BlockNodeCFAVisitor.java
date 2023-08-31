// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.parallel_decomposition;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNodeWithoutGraphInformation;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

// Visitor which specifically ignores function call and return edges

class BlockNodeCFAVisitor implements CFAVisitor {
  private final ImmutableSet.Builder<BlockNodeWithoutGraphInformation>
      blockNodesWithoutGraphInformation;
  private final BlockNodeTracker tracker;

  BlockNodeCFAVisitor() {
    tracker = new BlockNodeTracker();
    blockNodesWithoutGraphInformation = ImmutableSet.builder();
  }

  @Override
  public TraversalProcess visitEdge(CFAEdge edge) {
    if ((edge instanceof CFunctionCallEdge cfce
            && !cfce.getSuccessor().getFunctionName().equals("reach_error"))
        || edge instanceof CFunctionReturnEdge) {
      return TraversalProcess.SKIP;
    }
    tracker.track(edge);
    return TraversalProcess.CONTINUE;
  }

  @Override
  public TraversalProcess visitNode(CFANode node) {
    return TraversalProcess.CONTINUE;
  }

  public void finish() {
    blockNodesWithoutGraphInformation.add(tracker.finish());
  }

  ImmutableSet<BlockNodeWithoutGraphInformation> getBlockNodes() {
    return blockNodesWithoutGraphInformation.build();
  }
}
