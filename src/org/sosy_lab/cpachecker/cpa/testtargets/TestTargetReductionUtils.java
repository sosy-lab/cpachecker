// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.testtargets;

import com.google.common.collect.Sets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.DummyCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;

public final class TestTargetReductionUtils {

  private TestTargetReductionUtils() {
  }

  public static Pair<CFANode, CFANode> buildTestGoalGraph(
      final Set<CFAEdge> pTestTargets,
      final Map<CFAEdge, CFAEdge> pCopiedEdgeToTestTargetsMap,
      final FunctionEntryNode pEntryNode) {
    // a set of nodes that has already been created to prevent duplicates
    Set<CFANode> successorNodes = Sets.newHashSetWithExpectedSize(pTestTargets.size() + 2);
    Set<CFANode> visited = new HashSet<>();
    Map<CFANode, CFANode> origCFANodeToCopyMap = new HashMap<>();
    CFANode currentNode;
    Set<CFANode> toExplore = Sets.newHashSetWithExpectedSize(pTestTargets.size() + 1);
    Deque<CFANode> waitlist = new ArrayDeque<>();

    origCFANodeToCopyMap.put(pEntryNode, CFANode.newDummyCFANode(""));
    toExplore.add(pEntryNode);
    origCFANodeToCopyMap.put(pEntryNode.getExitNode(), CFANode.newDummyCFANode(""));
    successorNodes.add(pEntryNode.getExitNode());
    for (CFAEdge target : pTestTargets) {
      successorNodes.add(target.getPredecessor());
      toExplore.add(target.getSuccessor());

      if (!origCFANodeToCopyMap.containsKey(target.getPredecessor())) {
        origCFANodeToCopyMap.put(target.getPredecessor(), CFANode.newDummyCFANode(""));
      }
      if (!origCFANodeToCopyMap.containsKey(target.getSuccessor())) {
        origCFANodeToCopyMap.put(target.getSuccessor(), CFANode.newDummyCFANode(""));
      }

      pCopiedEdgeToTestTargetsMap.put(
          copyAsDummyEdge(
              origCFANodeToCopyMap.get(target.getPredecessor()),
              origCFANodeToCopyMap.get(target.getSuccessor())),
          target);
    }

    for (CFANode predecessor : toExplore) {
      if (!successorNodes.contains(predecessor)) {
      // get next node in the queue
      waitlist.add(predecessor);
      visited.clear();
      }

      while (!waitlist.isEmpty()) {
        currentNode = waitlist.poll();


        for (CFAEdge leaving : CFAUtils.leavingEdges(currentNode)) {
          if (successorNodes.contains(leaving.getSuccessor())) {
            if (!origCFANodeToCopyMap.get(predecessor)
                .hasEdgeTo(origCFANodeToCopyMap.get(leaving.getSuccessor()))) {
              copyAsDummyEdge(
                  origCFANodeToCopyMap.get(predecessor),
                  origCFANodeToCopyMap.get(leaving.getSuccessor()));
            }
          } else {
            if (visited.add(leaving.getSuccessor())) {
              waitlist.add(leaving.getSuccessor());
            }
          }
        }
      }
    }
    return Pair.of(
        origCFANodeToCopyMap.get(pEntryNode),
        origCFANodeToCopyMap.get(pEntryNode.getExitNode()));
  }

  public static CFAEdge copyAsDummyEdge(final CFANode pred, final CFANode succ) {
    CFAEdge newEdge = new DummyCFAEdge(pred, succ);
    pred.addLeavingEdge(newEdge);
    succ.addEnteringEdge(newEdge);
    return newEdge;
  }
}
