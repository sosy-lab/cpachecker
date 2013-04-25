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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class CFAWaitAtMeetOrder {

  public void assignSorting(CFANode rootNode) {
    Set<CFANode> visited = new HashSet<CFANode>(1000);
    Queue<CFANode> forwardAndForkNodes = new ArrayDeque<CFANode>();
    // A deterministic traversal order has to be ensured.
    // Sets/lists have to be implemented in a deterministic fashion if we
    // need to iterate over them e.g. use LinkedHashSet instead of a HashSet.
    Set<CFANode> joinNodes = new LinkedHashSet<CFANode>();

    rootNode.setWaitAtMeetOrderId(-1);
    forwardAndForkNodes.add(rootNode);

    while (forwardAndForkNodes.size() > 0) {
      // Forward-Nodes (Non-Join-Nodes): Assign them the sorting-number of the predecessor.
      // Join-Nodes: Assign them the sorting-number of the predecessor - 1
      while (forwardAndForkNodes.size() > 0) {
        CFANode u = forwardAndForkNodes.remove();
        visited.add(u);

        for(CFAEdge e : CFAUtils.leavingEdges(u)) {
          CFANode v = e.getSuccessor();

          if (!visited.contains(v)) {
            if ((v.getNumEnteringEdges() > 1) && !v.isLoopStart()) {
              // If it is a join-node AND not the start of a loop
              v.setWaitAtMeetOrderId(u.getWaitAtMeetOrderId() - 1);
              joinNodes.add(v);
            } else {
              v.setWaitAtMeetOrderId(u.getWaitAtMeetOrderId());
              forwardAndForkNodes.add(v);
            }
          }
        }
      }

      // Move join-nodes to forward-nodes
      // if all predecessors have been processed.
      Iterator<CFANode> it = joinNodes.iterator();
      while (it.hasNext()) {
        CFANode v = it.next();

        int visitedPredecessors = 0;
        int minPredTopoSortId = 0;
        for (CFAEdge e: CFAUtils.allEnteringEdges(v)) {
            CFANode u = e.getPredecessor();
            if (u.getWaitAtMeetOrderId() != 0) {
              visitedPredecessors++;
            }
            minPredTopoSortId = Math.min(minPredTopoSortId, u.getWaitAtMeetOrderId());
        }

        assert(visitedPredecessors > 0);

        if (visitedPredecessors == v.getNumEnteringEdges()) {
          v.setWaitAtMeetOrderId(Math.min(minPredTopoSortId, v.getWaitAtMeetOrderId()));
          it.remove();
          forwardAndForkNodes.add(v);
        }
      }
    }

  }
}

