// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode.BlockNodeMetaData;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class KnownBlockEndsDecomposition implements CFADecomposer {

  private final ShutdownNotifier shutdownNotifier;
  private final BiPredicate<CFANode, Integer> isBlockEnd;

  /**
   * @param pShutdownNotifier shutdown notifier for block node preconditions
   * @param pIsBlockEnd oracle that tells whether a node with current path length is a block end.
   */
  public KnownBlockEndsDecomposition(
      ShutdownNotifier pShutdownNotifier, BiPredicate<CFANode, Integer> pIsBlockEnd) {
    shutdownNotifier = pShutdownNotifier;
    isBlockEnd = pIsBlockEnd;
  }

  @Override
  public BlockGraph decompose(CFA cfa) throws InterruptedException {
    Set<CFANode> meetNodes = new LinkedHashSet<>();
    int idCounter = 0;
    meetNodes.add(cfa.getMainFunction());
    Set<CFANode> coveredMeetNodes = new LinkedHashSet<>();
    Set<BlockNodeMetaData> blockNodes = new LinkedHashSet<>();
    while (!meetNodes.isEmpty()) {
      meetNodes.removeAll(coveredMeetNodes);
      coveredMeetNodes.addAll(meetNodes);
      Set<CFANode> toAdd = new LinkedHashSet<>();
      for (CFANode meetNode : meetNodes) {
        for (ImmutableList<CFANode> cfaNodes : findPathsToNextMergeNode(meetNode)) {
          BlockNodeMetaData currentNode =
              new BlockNodeMetaData(
                  "LB" + idCounter,
                  meetNode,
                  getLastNodeFromPath(cfaNodes),
                  ImmutableSet.copyOf(cfaNodes),
                  findEdgesOnPath(cfaNodes),
                  ImmutableMap.of());
          blockNodes.add(currentNode);
          toAdd.add(getLastNodeFromPath(cfaNodes));
          idCounter++;
        }
      }
      meetNodes.addAll(toAdd);
    }
    return BlockGraph.fromMetaData(blockNodes, cfa, shutdownNotifier);
  }

  private List<ImmutableList<CFANode>> findPathsToNextMergeNode(CFANode pNode) {
    List<List<CFANode>> nodes = new ArrayList<>();
    List<ImmutableList<CFANode>> finished = new ArrayList<>();
    nodes.add(new ArrayList<>(ImmutableList.of(pNode)));
    while (!nodes.isEmpty()) {
      List<CFANode> currentPath = nodes.remove(0);
      CFANode last = getLastNodeFromPath(currentPath);
      for (CFANode cfaNode : CFAUtils.allSuccessorsOf(last)) {
        ImmutableList<CFANode> extendedPath =
            ImmutableList.<CFANode>builder().addAll(currentPath).add(cfaNode).build();
        if (isBlockEnd.test(cfaNode, currentPath.size() - 1)) {
          finished.add(extendedPath);
        } else {
          nodes.add(new ArrayList<>(extendedPath));
        }
      }
    }
    return finished;
  }

  private CFANode getLastNodeFromPath(List<CFANode> pNodes) {
    return pNodes.get(pNodes.size() - 1);
  }

  private ImmutableSet<CFAEdge> findEdgesOnPath(List<CFANode> pPath) {
    if (pPath.size() < 2) {
      return ImmutableSet.of();
    }
    ImmutableSet.Builder<CFAEdge> edges = ImmutableSet.builder();
    for (int i = 1; i < pPath.size(); i++) {
      edges.add(findEdge(pPath.get(i - 1), pPath.get(i)));
    }
    return edges.build();
  }

  private CFAEdge findEdge(CFANode from, CFANode to) {
    return CFAUtils.allLeavingEdges(from)
        .firstMatch(edge -> edge.getSuccessor().equals(to))
        .toJavaUtil()
        .orElseThrow();
  }
}
