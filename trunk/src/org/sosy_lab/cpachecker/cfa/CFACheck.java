/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa;

import static com.google.common.base.Verify.verify;
import static org.sosy_lab.cpachecker.util.CFAUtils.enteringEdges;
import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import com.google.common.base.VerifyException;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

public class CFACheck {

  /**
   * Traverse the CFA and run a series of checks at each node
   * @param cfa Node to start traversal from
   * @param nodes Optional set of all nodes in the CFA (may be null)
   * @return true if all checks succeed
   * @throws VerifyException if not all checks succeed
   */
  public static boolean check(FunctionEntryNode cfa, @Nullable Set<CFANode> nodes)
      throws VerifyException {

    Set<CFANode> visitedNodes = new HashSet<>();
    Deque<CFANode> waitingNodeList = new ArrayDeque<>();

    waitingNodeList.add(cfa);
    while (!waitingNodeList.isEmpty()) {
      CFANode node = waitingNodeList.poll();

      if (visitedNodes.add(node)) {
        Iterables.addAll(waitingNodeList, CFAUtils.successorsOf(node));
        Iterables.addAll(waitingNodeList, CFAUtils.predecessorsOf(node)); // just to be sure to get ALL nodes.

        // The actual checks
        isConsistent(node);
        checkEdgeCount(node);
      }
    }

    if (nodes != null) {
      verify(
          visitedNodes.equals(nodes),
          "\nNodes in CFA but not reachable through traversal: %s\nNodes reached that are not in CFA: %s",
          Iterables.transform(Sets.difference(nodes, visitedNodes), CFACheck::debugFormat),
          Iterables.transform(Sets.difference(visitedNodes, nodes), CFACheck::debugFormat));
    }
    return true;
  }

  private static String debugFormat(CFANode node) {
    // try to get some information about location from node
    FileLocation location = FileLocation.DUMMY;
    if (node.getNumEnteringEdges() > 0) {
      location = node.getEnteringEdge(0).getFileLocation();
    } else if (node.getNumLeavingEdges() > 0) {
      location = node.getLeavingEdge(0).getFileLocation();
    }
    return node.getFunctionName() + ":" + node + " (" + location + ")";
  }

  /**
   * Verify that the number of edges and their types match.
   * @param pNode Node to be checked
   */
  private static void checkEdgeCount(CFANode pNode) {

    // check entering edges
    int entering = pNode.getNumEnteringEdges();
    if (entering == 0) {
      verify(
          pNode instanceof FunctionEntryNode,
          "Dead code: node %s has no incoming edges (successors are %s)",
          debugFormat(pNode),
          CFAUtils.successorsOf(pNode).transform(CFACheck::debugFormat));
    }

    // check leaving edges
    if (!(pNode instanceof FunctionExitNode)) {
      switch (pNode.getNumLeavingEdges()) {
        case 0:
          verify(pNode instanceof CFATerminationNode, "Dead end at node %s", debugFormat(pNode));
          break;

        case 1:
          CFAEdge edge = pNode.getLeavingEdge(0);
          verify(
              !(edge instanceof AssumeEdge),
              "AssumeEdge does not appear in pair at node %s",
              debugFormat(pNode));
          verify(
              !(edge instanceof CFunctionSummaryStatementEdge),
              "CFunctionSummaryStatementEdge is not paired with CFunctionCallEdge at node %s",
              debugFormat(pNode));
          break;

        case 2:
          CFAEdge edge1 = pNode.getLeavingEdge(0);
          CFAEdge edge2 = pNode.getLeavingEdge(1);
          //relax this assumption for summary edges
          if (edge1 instanceof CFunctionSummaryStatementEdge) {
            verify(
                edge2 instanceof CFunctionCallEdge,
                "CFunctionSummaryStatementEdge is not paired with CFunctionCallEdge at node %s",
                debugFormat(pNode));
          } else if (edge2 instanceof CFunctionSummaryStatementEdge) {
            verify(
                edge1 instanceof CFunctionCallEdge,
                "CFunctionSummaryStatementEdge is not paired with CFunctionCallEdge at node %s",
                debugFormat(pNode));
          } else {
            verify(
                (edge1 instanceof AssumeEdge) && (edge2 instanceof AssumeEdge),
                "Branching without conditions at node %s",
                debugFormat(pNode));

          AssumeEdge ae1 = (AssumeEdge)edge1;
          AssumeEdge ae2 = (AssumeEdge)edge2;
            verify(
                ae1.getTruthAssumption() != ae2.getTruthAssumption(),
                "Inconsistent branching at node %s",
                debugFormat(pNode));
          }
          break;

        default:
          throw new VerifyException("Too much branching at node " + debugFormat(pNode));
      }
    }
  }

  /**
   * Check all entering and leaving edges for corresponding leaving/entering edges
   * at predecessor/successor nodes, and that there are no duplicates
   * @param pNode Node to be checked
   */
  private static void isConsistent(CFANode pNode) {
    Set<CFAEdge> seenEdges = new HashSet<>();
    Set<CFANode> seenNodes = new HashSet<>();

    for (CFAEdge edge : leavingEdges(pNode)) {
      verify(seenEdges.add(edge), "Duplicate leaving edge %s on node %s", edge, debugFormat(pNode));

      CFANode successor = edge.getSuccessor();
      verify(
          seenNodes.add(successor),
          "Duplicate successor %s for node %s",
          successor,
          debugFormat(pNode));

      verify(
          enteringEdges(successor).contains(edge),
          "Node %s has leaving edge %s, but node %s does not have this edge as entering edge!",
          debugFormat(pNode),
          edge,
          debugFormat(successor));
    }

    seenEdges.clear();
    seenNodes.clear();

    for (CFAEdge edge : enteringEdges(pNode)) {
      verify(
          seenEdges.add(edge), "Duplicate entering edge %s on node %s", edge, debugFormat(pNode));

      CFANode predecessor = edge.getPredecessor();
      verify(
          seenNodes.add(predecessor),
          "Duplicate predecessor %s for node %s",
          predecessor,
          debugFormat(pNode));

      verify(
          leavingEdges(predecessor).contains(edge),
          "Node %s has entering edge %s, but node %s does not have this edge as leaving edge!",
          debugFormat(pNode),
          edge,
          debugFormat(predecessor));
    }
  }
}
