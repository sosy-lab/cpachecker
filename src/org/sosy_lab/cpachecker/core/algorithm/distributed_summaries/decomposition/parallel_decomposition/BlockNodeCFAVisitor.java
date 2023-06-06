// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.parallel_decomposition;

import com.google.common.collect.ImmutableSet;
import java.util.function.Predicate;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNodeWithoutGraphInformation;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

// Visitor which specifically ignores function call and return edges

class BlockNodeCFAVisitor implements CFAVisitor {
  private final Predicate<CFANode> isBlockEnd;
  private final ImmutableSet.Builder<BlockNodeWithoutGraphInformation>
      blockNodesWithoutGraphInformation;
  private final BlockNodeTracker tracker;

  BlockNodeCFAVisitor(Predicate<CFANode> pIsBlockEnd) {
    isBlockEnd = pIsBlockEnd;
    tracker = new BlockNodeTracker();
    blockNodesWithoutGraphInformation = ImmutableSet.builder();
  }

  @Override
  public TraversalProcess visitEdge(CFAEdge edge) {
    if (edge instanceof CFunctionCallEdge || edge instanceof
                                                 CFunctionReturnEdge)
      return TraversalProcess.SKIP;
    tracker.track(edge);
    if (isBlockEnd.test(edge.getSuccessor())) {
      blockNodesWithoutGraphInformation.add(tracker.finish());
    }
    return TraversalProcess.CONTINUE;
  }

  @Override
  public TraversalProcess visitNode(CFANode node) {
    return TraversalProcess.CONTINUE;
  }

  ImmutableSet<BlockNodeWithoutGraphInformation> getBlockNodes() {
    return blockNodesWithoutGraphInformation.build();
  }
}
