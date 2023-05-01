// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNodeWithoutGraphInformation;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.dependencegraph.MergePoint;

public class BridgeDecomposition implements BlockSummaryCFADecomposer {

  private int id = 1;

  @Override
  public BlockGraph decompose(CFA cfa) throws InterruptedException {
    Set<BlockNodeWithoutGraphInformation> blocks = new LinkedHashSet<>();
    MergePoint<CFANode> merge =
        new MergePoint<>(
            cfa.getMainFunction().getExitNode().orElseThrow(),
            CFAUtils::successorsOf,
            CFAUtils::predecessorsOf);

    LinkedList<LinkedList<CFAEdge>> paths = new LinkedList<>();
    for (CFAEdge leavingEdge : CFAUtils.leavingEdges(cfa.getMainFunction())) {
      LinkedList<CFAEdge> initialPath = new LinkedList<>();
      initialPath.add(leavingEdge);
      paths.add(initialPath);
    }
    while (!paths.isEmpty()) {
      LinkedList<CFAEdge> current = paths.removeFirst();
      LinkedList<CFAEdge> next = new LinkedList<>(current);
      CFAEdge lastEdge = current.getLast();
      CFANode lastNode = lastEdge.getSuccessor();
      if (lastNode.getNumLeavingEdges() > 1) {
        blocks.add(listToBlockNode(current));
        next = new LinkedList<>();
        while (lastNode.getNumLeavingEdges() > 1) {
          CFANode mergePoint = merge.findMergePoint(lastEdge.getSuccessor());
          blocks.add(findAllEdges(lastNode, mergePoint));
          lastNode = mergePoint;
        }
      }
      if (lastNode.getNumLeavingEdges() == 1) {
        paths.add(CFAUtils.leavingEdges(lastNode).copyInto(next));
      } else if (!next.isEmpty()) {
        blocks.add(listToBlockNode(next));
      }
    }
    return BlockGraph.fromBlockNodesWithoutGraphInformation(cfa, blocks);
  }

  private BlockNodeWithoutGraphInformation listToBlockNode(LinkedList<CFAEdge> pList) {
    CFANode first = pList.getFirst().getPredecessor();
    CFANode last = pList.getLast().getSuccessor();
    ImmutableSet<CFAEdge> edges = ImmutableSet.copyOf(pList);
    ImmutableSet<CFANode> nodes =
        FluentIterable.from(edges)
            .transformAndConcat(edge -> ImmutableSet.of(edge.getSuccessor(), edge.getPredecessor()))
            .toSet();
    return new BlockNodeWithoutGraphInformation("B" + id++, first, last, nodes, edges);
  }

  private BlockNodeWithoutGraphInformation findAllEdges(CFANode initial, CFANode last) {
    ImmutableSet.Builder<CFANode> nodes = ImmutableSet.builder();
    ImmutableSet.Builder<CFAEdge> edges = ImmutableSet.builder();
    LinkedList<CFANode> toExplore = new LinkedList<>();
    toExplore.add(initial);
    while (!toExplore.isEmpty()) {
      CFANode cfaNode = toExplore.removeFirst();
      nodes.add(cfaNode);
      if (cfaNode.equals(last)) {
        continue;
      }
      for (CFAEdge leavingEdge : CFAUtils.leavingEdges(cfaNode)) {
        edges.add(leavingEdge);
        toExplore.add(leavingEdge.getSuccessor());
      }
    }
    return new BlockNodeWithoutGraphInformation(
        "B" + id++, initial, last, nodes.build(), edges.build());
  }
}
