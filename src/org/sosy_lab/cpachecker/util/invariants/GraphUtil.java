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
package org.sosy_lab.cpachecker.util.invariants;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;

public class GraphUtil {

  /**
   * Create edge path from node path.
   * @param nodes Adjacent nodes should be connected by an edge.
   * @return The path of edges that traverses these nodes.
   */
  public static Vector<CFAEdge> makeEdgePath(Vector<CFANode> nodes) {
    Vector<CFAEdge> edges = new Vector<>();
    int len = nodes.size();
    CFANode N, M;
    CFAEdge E;
    for (int i = 0; i < len - 1; i++) {
      N = nodes.get(i);
      M = nodes.get(i+1);
      E = N.getEdgeTo(M);
      edges.add(E);
    }
    return edges;
  }

  /**
   * IF:
   *   (i) There is exactly one node in 'nodes' whose
   *       'isLoopStart' method returns true -- call this
   *       the loop head -- AND
   *  (ii) each node in 'nodes' has exactly one outgoing edge
   *       to any of the other nodes in 'nodes',
   * THEN we will return a list of CFAEdges that forms a loop,
   * starting from and ending at the loop head.
   * If however the above conditions are not satisfied, then the
   * behavior of this method is indeterminate. It might return
   * a list of CFAEdges, or it might return null. In the former
   * case, the list might form a loop, or it might not.
   */
  public static Vector<CFAEdge> makeEdgeLoop(List<CFANode> nodes, LogManager logger) {
    Vector<CFAEdge> edges = new Vector<>();

    writeAllSuccessors(nodes, logger);

    // Identify loop head.
    CFANode loopHead = null;
    for (CFANode N : nodes) {
      if (N.isLoopStart()) {
        loopHead = N;
        break;
      }
    }
    // If didn't find one, then give up.
    if (loopHead == null) {
      return null;
    }
    logger.log(Level.ALL, "Loop head is: ", loopHead);

    CFANode current = loopHead;
    // Compute a loop.
    //while (nodes.size() > 0) {
    do {
      // Try to find an outgoing edge from current to some
      // node in the list.
      for (CFANode N : nodes) {
        if (current.hasEdgeTo(N)) {
          // Found such an edge.
          // Add it to list of edges, set current to N, and remove N from list of nodes.
          logger.log(Level.ALL, "Found next node in loop: ", N);
          CFAEdge e = current.getEdgeTo(N);
          edges.add(e);
          current = N;
          nodes.remove(N);
          break;
        }
      }
    } while (current != loopHead);

    // Finally, look for edge returning to loop head.
    if (current.hasEdgeTo(loopHead)) {
      CFAEdge e = current.getEdgeTo(loopHead);
      edges.add(e);
    }

    return edges;
  }

  /*
   * This method is purely for debugging purposes.
   */
  private static void writeAllSuccessors(List<CFANode> nodes, LogManager logger) {
    logger.log(Level.ALL, "Computing successors of all nodes.");
    for (CFANode n : nodes) {
      logger.log(Level.ALL, "Successors of ",n);
      int e = n.getNumLeavingEdges();
      List<CFANode> list = new Vector<>(e);
      for (int i = 0; i < e; i++) {
        CFAEdge edge = n.getLeavingEdge(i);
        CFANode s = edge.getSuccessor();
        list.add(s);
      }
      logger.log(Level.ALL, list);
    }
  }

  /**
   * Create edge loop from node loop.
   * @param nodes Adjacent nodes should be connected by an edge, including
   *              an edge from the last node back to the first.
   * @return The loop of edges that traverses these nodes and returns to
   *         the start.
   */
  /**
  @Deprecated
  public static Vector<CFAEdge> makeEdgeLoopSimple(Vector<CFANode> nodes) {
    Vector<CFAEdge> edges = new Vector<>();
    int len = nodes.size();
    CFANode N, M;
    CFAEdge E;
    for (int i = 0; i < len; i++) {
      N = nodes.get(i);
      M = nodes.get((i+1) % len);
      //diag:
      System.err.println(N);
      System.err.println(M);
      //
      E = N.getEdgeTo(M);
      edges.add(E);
    }
    return edges;
  }
  */

  /**
   * Perform a DFS search on the CFA, searching for a path to the target.
   * All visited nodes are added to a given set.
   *
   * @param start The start node of the search.
   * @param target The node to which we are searching for a path.
   * @param reverse Whether to go backwards or forward.
   * @param interprocedural Whether interprocedural edges (function call/return) should be followed.
   * @return A Vector of nodes starting with start and ending with target if we found a path,
   *         otherwise empty.
   */
  public static Vector<CFANode> dfs(CFANode start, CFANode target, boolean reverse, boolean interprocedural) {
    Vector<CFANode> path = new Vector<>();
    Set<CFANode> seen = new HashSet<>();
    HashMap<CFANode, CFANode> links = new HashMap<>();
    Deque<CFANode> toProcess = new ArrayDeque<>();
    toProcess.push(start);
    CFANode n;
    while (!toProcess.isEmpty()) {
      n = toProcess.pop();
      if (n == target) {
        Vector<CFANode> back = new Vector<>();
        back.add(n);
        while (n != start && links.containsKey(n)) {
          n = links.get(n);
          back.add(n);
        }
        for (int i = back.size() - 1; i >= 0; i--) {
          path.add(back.get(i));
        }
        break;
      }
      seen.add(n);
      if (reverse) {
        for (int i = 0; i < n.getNumEnteringEdges(); ++i) {
          CFAEdge e = n.getEnteringEdge(i);
          if (!interprocedural && (e instanceof CFunctionCallEdge || e instanceof CFunctionReturnEdge)) {
            continue;
          }

          CFANode s = e.getPredecessor();
          if (!seen.contains(s)) {
            toProcess.push(s);
            links.put(s, n);
          }
        }
        if (n.getEnteringSummaryEdge() != null) {
          CFANode s = n.getEnteringSummaryEdge().getPredecessor();
          if (!seen.contains(s)) {
            toProcess.push(s);
            links.put(s, n);
          }
        }
      } else {
        for (int i = 0; i < n.getNumLeavingEdges(); ++i) {
          CFAEdge e = n.getLeavingEdge(i);
          if (!interprocedural && (e instanceof CFunctionCallEdge || e instanceof CFunctionReturnEdge)) {
            continue;
          }

          CFANode s = e.getSuccessor();
          if (!seen.contains(s)) {
            toProcess.push(s);
            links.put(s, n);
          }
        }
        if (n.getLeavingSummaryEdge() != null) {
          CFANode s = n.getLeavingSummaryEdge().getSuccessor();
          if (!seen.contains(s)) {
            toProcess.push(s);
            links.put(s, n);
          }
        }
      }
    }
    return path;
  }

}
