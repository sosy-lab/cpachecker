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

import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class CFATopologicalSortSparse {

  public void assignSorting(CFANode rootNode) {
    Set<CFANode> visited = new HashSet<CFANode>(1000);
    Queue<CFANode> forwardAndForkNodes = new ArrayDeque<CFANode>();
    List<CFANode> joinNodes = new LinkedList<CFANode>();

    rootNode.setSparseTopoSortId(-1);
    forwardAndForkNodes.add(rootNode);

    while (forwardAndForkNodes.size() > 0) {
      while (forwardAndForkNodes.size() > 0) {
        CFANode u = forwardAndForkNodes.remove();
        assert (u.getSparseTopoSortId() < 0);

        visited.add(u);
        for(CFAEdge e : leavingEdges(u)) {
          CFANode v = e.getSuccessor();

          if (!visited.contains(v)) {
            if ((v.getNumEnteringEdges() > 1) && !v.isLoopStart()) {
              v.setSparseTopoSortId(u.getSparseTopoSortId() - 1);
              joinNodes.add(v);
            } else {
              v.setSparseTopoSortId(u.getSparseTopoSortId());
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
            if (u.getSparseTopoSortId() != 0) {
              visitedPredecessors++;
            }
            minPredTopoSortId = Math.min(minPredTopoSortId, u.getSparseTopoSortId());
        }

        assert(visitedPredecessors > 0);

        if (visitedPredecessors == v.getNumEnteringEdges()) {
          v.setSparseTopoSortId(Math.min(minPredTopoSortId, v.getSparseTopoSortId()));
          it.remove();
          forwardAndForkNodes.add(v);
        }
      }
    }

  }
}

