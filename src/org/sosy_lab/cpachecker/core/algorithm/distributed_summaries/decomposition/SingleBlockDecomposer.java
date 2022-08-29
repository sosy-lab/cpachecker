// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraph.BlockGraphFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode.BlockNodeMetaData;
import org.sosy_lab.cpachecker.util.CFAUtils;

/** Decompose a CFA into a single block containing the complete CFA */
public class SingleBlockDecomposer implements CFADecomposer {

  private final ShutdownNotifier shutdownNotifier;

  public SingleBlockDecomposer(ShutdownNotifier pShutdownNotifier) {
    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  public BlockGraph cut(CFA cfa) throws InterruptedException {
    BlockGraphFactory builder = new BlockGraphFactory(cfa, shutdownNotifier);
    CFANode startNode = cfa.getMainFunction();
    // we do not get error conditions
    CFANode lastNode = CFANode.newDummyCFANode();
    Set<CFAEdge> edges = new LinkedHashSet<>();
    for (CFANode allNode : cfa.getAllNodes()) {
      CFAUtils.leavingEdges(allNode).copyInto(edges);
      CFAUtils.enteringEdges(allNode).copyInto(edges);
    }
    Set<CFANode> nodes = new LinkedHashSet<>(cfa.getAllNodes());
    nodes.add(lastNode);
    BlockNodeMetaData root =
        builder.makeBlock(startNode, startNode, ImmutableSet.of(startNode), ImmutableSet.of());
    BlockNodeMetaData workerBlock = builder.makeBlock(startNode, lastNode, nodes, edges);
    builder.setRoot(root);
    builder.linkSuccessor(root, workerBlock);
    return builder.build();
  }
}
