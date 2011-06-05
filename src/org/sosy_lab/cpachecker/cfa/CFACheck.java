/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionExitNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;

public class CFACheck {

  /**
   * Traverse the CFA and run a series of checks at each node
   * @param cfa Node to start traversal from
   * @param nodes Optional set of all nodes in the CFA (may be null)
   */
  public static boolean check(CFAFunctionDefinitionNode cfa, Set<CFANode> nodes) {

    Set<CFANode> visitedNodes = new HashSet<CFANode>();
    Deque<CFANode> waitingNodeList = new ArrayDeque<CFANode>();

    waitingNodeList.add(cfa);
    while (!waitingNodeList.isEmpty()) {
      CFANode node = waitingNodeList.poll();

      if (visitedNodes.add(node)) {
        for (int edgeIdx = 0; edgeIdx < node.getNumLeavingEdges(); edgeIdx++) {
          CFAEdge edge = node.getLeavingEdge(edgeIdx);
          waitingNodeList.add(edge.getSuccessor());
        }

        // The actual checks
        isConsistent(node);
        checkEdgeCount(node);
      }
    }

    if (nodes != null) {
      assert visitedNodes.equals(nodes);
    }
    return true;
  }

  /**
   * Verify that the number of edges and their types match.
   * @param pNode Node to be checked
   */
  private static void checkEdgeCount(CFANode pNode) {

    // check entering edges
    int entering = pNode.getNumEnteringEdges();
    if (entering == 0) {
      assert (pNode instanceof CFAFunctionDefinitionNode) : "Dead code: node " + pNode + " has no incoming edges";

    } else if (entering > 2) {
      assert (pNode instanceof CFAFunctionDefinitionNode)
          || (pNode instanceof CFAFunctionExitNode)
          || (pNode instanceof CFALabelNode)
          || (pNode.isLoopStart())
          : "Too many incoming edges at node " + pNode.getLineNumber();
    }

    // check leaving edges
    if (!(pNode instanceof CFAFunctionExitNode)) {
      switch (pNode.getNumLeavingEdges()) {
      case 0:
        // not possible to check this, this case occurs when CFA pruning is enabled
//        assert false : "Dead end at node " + pNode;
        break;

      case 1: break;

      case 2:
        CFAEdge edge1 = pNode.getLeavingEdge(0);
        CFAEdge edge2 = pNode.getLeavingEdge(1);
        assert (edge1 instanceof AssumeEdge) && (edge2 instanceof AssumeEdge) : "Branching without conditions at node " + pNode;

        AssumeEdge ae1 = (AssumeEdge)edge1;
        AssumeEdge ae2 = (AssumeEdge)edge2;
        assert ae1.getTruthAssumption() != ae2.getTruthAssumption() : "Inconsistent branching at node " + pNode;
        break;

      default:
        assert false : "Too much branching at node " + pNode;
      }
    }
  }

  /**
   * Check all entering and leaving edges for corresponding leaving/entering edges
   * at predecessor/successor nodes, and that there are no duplicates
   * @param pNode Node to be checked
   */
  private static void isConsistent(CFANode pNode) {
    Set<CFAEdge> seenEdges = new HashSet<CFAEdge>();
    Set<CFANode> seenNodes = new HashSet<CFANode>();

    for (int edgeIdx = 0; edgeIdx < pNode.getNumLeavingEdges(); ++edgeIdx) {
      CFAEdge edge = pNode.getLeavingEdge(edgeIdx);
      if (!seenEdges.add(edge)) {
        assert false : "Duplicate leaving edge " + edge + " on node " + pNode;
      }

      CFANode successor = edge.getSuccessor();
      if (!seenNodes.add(successor)) {
        assert false : "Duplicate successor " + successor + " for node " + pNode;
      }

      boolean hasEdge = false;
      for (int succEdgeIdx = 0; succEdgeIdx < successor.getNumEnteringEdges(); ++succEdgeIdx) {
        if (successor.getEnteringEdge(succEdgeIdx) == edge) {
          hasEdge = true;
          break;
        }
      }
      assert hasEdge : "Node " + pNode + " has leaving edge " + edge
          + ", but pNode " + successor + " does not have this edge as entering edge!";
    }

    seenEdges.clear();
    seenNodes.clear();

    for (int edgeIdx = 0; edgeIdx < pNode.getNumEnteringEdges(); ++edgeIdx) {
      CFAEdge edge = pNode.getEnteringEdge(edgeIdx);
      if (!seenEdges.add(edge)) {
        assert false : "Duplicate entering edge " + edge + " on node " + pNode;
      }

      CFANode predecessor = edge.getPredecessor();
      if (!seenNodes.add(predecessor)) {
        assert false : "Duplicate predecessor " + predecessor + " for node " + pNode;
      }

      boolean hasEdge = false;
      for (int predEdgeIdx = 0; predEdgeIdx < predecessor.getNumLeavingEdges(); ++predEdgeIdx) {
        if (predecessor.getLeavingEdge(predEdgeIdx) == edge) {
          hasEdge = true;
          break;
        }
      }
      assert hasEdge : "Node " + pNode + " has entering edge " + edge
          + ", but pNode " + predecessor + " does not have this edge as leaving edge!";
    }
  }
}
