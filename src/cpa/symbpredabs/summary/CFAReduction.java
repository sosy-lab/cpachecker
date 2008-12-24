/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.symbpredabs.summary;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import logging.LazyLogger;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAErrorNode;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.CFANode;


/**
 * Perform a (very) simple cone-of-influence reduction on the given CFA.
 * That is, get rid of all the nodes/edges that are not reachable from the
 * error location(s).
 *
 * In fact, this should probably *not* be called ConeOfInfluenceCFAReduction,
 * since it is *much* more trivial (and less powerful) than that.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class CFAReduction {

  public CFAReduction() {}

  public CFAFunctionDefinitionNode removeIrrelevantForErrorLocations(
                                                                     CFAFunctionDefinitionNode cfa) {
    Map<CFANode, Integer> dfsMap = new HashMap<CFANode, Integer>();
    Map<CFANode, Integer> dfsMapFromError = new HashMap<CFANode, Integer>();
    dfs(cfa, dfsMap, false);
    for (CFANode n : dfsMap.keySet()) {
      if (n instanceof CFAErrorNode) {
        dfs(n, dfsMapFromError, true);
      }
    }
    // now detach all the nodes not visited
    for (CFANode n : dfsMap.keySet()) {
      if (!dfsMapFromError.containsKey(n)) {
        while (n.getNumEnteringEdges() > 0) {
          CFAEdge removedEdge = n.getEnteringEdge(0);
          n.removeEnteringEdge(removedEdge);
          CFANode prevNode = removedEdge.getPredecessor();
          prevNode.removeLeavingEdge(removedEdge);
        }
        while (n.getNumLeavingEdges() > 0) {
          CFAEdge removedEdge = n.getLeavingEdge(0);
          n.removeLeavingEdge(removedEdge);
          CFANode succNode = removedEdge.getSuccessor();
          succNode.removeEnteringEdge(removedEdge);
        }
        n.addEnteringSummaryEdge(null);
        n.addLeavingSummaryEdge(null);
      }
    }
    return cfa;
  }

  private void dfs(CFANode start, Map<CFANode, Integer> dfsMarked,
                   boolean reverse) {
    Stack<CFANode> toProcess = new Stack<CFANode>();

    toProcess.push(start);
    while (!toProcess.empty()) {
      CFANode n = toProcess.peek();
      if (dfsMarked.containsKey(n) && dfsMarked.get(n) == 1) {
        toProcess.pop();
        continue;
      }
      boolean finished = true;
      dfsMarked.put(n, -1);
      if (reverse) {
        for (int i = 0; i < n.getNumEnteringEdges(); ++i) {
          CFAEdge e = n.getEnteringEdge(i);
          CFANode s = e.getPredecessor();
          if (!dfsMarked.containsKey(s) || dfsMarked.get(s) == 0) {
            toProcess.push(s);
            finished = false;
          }
        }
        if (n.getEnteringSummaryEdge() != null) {
          CFANode s = n.getEnteringSummaryEdge().getPredecessor();
          if (!dfsMarked.containsKey(s) || dfsMarked.get(s) == 0) {
            toProcess.push(s);
            finished = false;
          }
        }
      } else {
        for (int i = 0; i < n.getNumLeavingEdges(); ++i) {
          CFAEdge e = n.getLeavingEdge(i);
          CFANode s = e.getSuccessor();
          if (!dfsMarked.containsKey(s) || dfsMarked.get(s) == 0) {
            toProcess.push(s);
            finished = false;
          }
        }
        if (n.getLeavingSummaryEdge() != null) {
          CFANode s = n.getLeavingSummaryEdge().getSuccessor();
          if (!dfsMarked.containsKey(s) || dfsMarked.get(s) == 0) {
            toProcess.push(s);
            finished = false;
          }
        }
      }
      if (finished) {
        toProcess.pop();
        LazyLogger.log(LazyLogger.DEBUG_3,
            "FINISHED: ", n.getNodeNumber());
        dfsMarked.put(n, 1);
      }
    }
  }
}
