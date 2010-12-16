/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.util;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.filter;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionReturnEdge;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

public class CFA {

  /**
   * Find all nodes of the CFA that are reachable from the given entry point.
   * 
   * Same as {@link #transitiveSuccessors(CFANode, boolean)}.
   * 
   * @param rootNode  The start node of the search.
   * @param interprocedural Whether interprocedural edges (function call/return) should be followed. 
   * @return A set of nodes.
   */
  public static Set<CFANode> allNodes(CFAFunctionDefinitionNode rootNode, boolean interprocedural) {
    return transitiveSuccessors(rootNode, interprocedural);
  }

  /**
   * Find all nodes of the CFA that are reachable from the given entry point.
   * @param node  The start node of the search.
   * @param interprocedural Whether interprocedural edges (function call/return) should be followed. 
   * @return A set of nodes.
   */
  public static Set<CFANode> transitiveSuccessors(CFANode node, boolean interprocedural) {
    Set<CFANode> allNodes = new HashSet<CFANode>();
    dfs(node, allNodes, false, interprocedural);
    return allNodes;
  }
  
  /**
   * Find all nodes of the CFA from which a given node is reachable.
   * @param node  The start node of the backwards search.
   * @param interprocedural Whether interprocedural edges (function call/return) should be followed. 
   * @return A set of nodes.
   */
  public static Set<CFANode> transitivePredecessors(CFANode node, boolean interprocedural) {
    Set<CFANode> allNodes = new HashSet<CFANode>();
    dfs(node, allNodes, true, interprocedural);
    return allNodes;
  }
  
  /**
   * Perform a DFS search on the CFA. All visited nodes are added to a given set.
   * If this set is non-empty at the beginning, the search does not traverse
   * beyond the nodes of this set (the part of the CFA reachable from these nodes
   * is considered to be know already).
   * 
   * @param start The start node of the search.
   * @param seen A set of nodes that have already been visited.
   * @param reverse Whether to go backwards or forward.
   * @param interprocedural Whether interprocedural edges (function call/return) should be followed. 
   * @return The highest node id encountered.
   */
  public static int dfs(CFANode start, Set<CFANode> seen, boolean reverse, boolean interprocedural) {
    int maxNodeId = -1; 

    Deque<CFANode> toProcess = new ArrayDeque<CFANode>();
    toProcess.push(start);
    while (!toProcess.isEmpty()) {
      CFANode n = toProcess.pop();
      maxNodeId = Math.max(maxNodeId, n.getNodeNumber());
      seen.add(n);
      if (reverse) {
        for (int i = 0; i < n.getNumEnteringEdges(); ++i) {
          CFAEdge e = n.getEnteringEdge(i);
          if (!interprocedural && (e instanceof FunctionCallEdge || e instanceof FunctionReturnEdge)) {
            continue;
          }
          
          CFANode s = e.getPredecessor();
          if (!seen.contains(s)) {
            toProcess.push(s);
          }
        }
        if (n.getEnteringSummaryEdge() != null) {
          CFANode s = n.getEnteringSummaryEdge().getPredecessor();
          if (!seen.contains(s)) {
            toProcess.push(s);
          }
        }
      } else {
        for (int i = 0; i < n.getNumLeavingEdges(); ++i) {
          CFAEdge e = n.getLeavingEdge(i);
          if (!interprocedural && (e instanceof FunctionCallEdge || e instanceof FunctionReturnEdge)) {
            continue;
          }

          CFANode s = e.getSuccessor();
          if (!seen.contains(s)) {
            toProcess.push(s);
          }
        }
        if (n.getLeavingSummaryEdge() != null) {
          CFANode s = n.getLeavingSummaryEdge().getSuccessor();
          if (!seen.contains(s)) {
            toProcess.push(s);
          }
        }
      }
    }
    return maxNodeId;
  }
 
  /**
   * A predicate that can be used to filter out nodes that are marked as loop start nodes.
   */
  public static Predicate<CFANode> FILTER_LOOP_HEADS = new Predicate<CFANode>() {
    @Override
    public boolean apply(CFANode pNode) {
      return pNode.isLoopStart();
    }
  };


  /**
   * Computes the transitive closure of the reachability relation on a set of
   * nodes.
   * 
   * The result is given as a two-dimensional array, where
   * (result[i][j] == true) iff that the node with id j is reachable from the node with id i.
   * 
   * This analysis does not know about the special meaning of function calls/exits,
   * so if there exist such edges, the analysis may be imprecise because it does
   * not keep track of the callstack.
   * 
   * @param allNodes The set of all nodes to consider.
   * @param max The highest node id of all nodes in allNodes.
   * @return A two-dimensional array with the transitive closure.
   */
  public static boolean[][] transitiveClosure(Set<CFANode> allNodes, int max) {
    boolean[][] transitiveClosure = new boolean[max+1][max+1];
    // all fields are initialized to 'false' by Java

    // transitiveClosure[i][j] means that j is reachable from i (j is a successor of i)
    
    // initialize for all direct edges
    for (CFANode currentNode : allNodes) {
      final int i = currentNode.getNodeNumber();
      final boolean[] transitiveClosureI = transitiveClosure[i];

      for (int j = 0; j < currentNode.getNumLeavingEdges(); ++j) {
        CFAEdge e = currentNode.getLeavingEdge(j);
        transitiveClosureI[e.getSuccessor().getNodeNumber()] = true;
      }
      
      CFAEdge e = currentNode.getLeavingSummaryEdge();
      if (e != null) {
        transitiveClosureI[e.getSuccessor().getNodeNumber()] = true;
      }
    }
    
    // (Floyd-)Warshall algorithm for transitive closure
    for (int k = 0; k <= max; k++) {
      for (int i = 0; i <= max; i++) {
        final boolean[] transitiveClosureI =  transitiveClosure[i];
        
        for (int j = 0; j <= max; j++) {
//        transitiveClosure[i][j] = transitiveClosure[i][j] || (transitiveClosure[i][k] && transitiveClosure[k][j]); 

          // optimization:
          transitiveClosureI[j]   = transitiveClosureI[j]   || (transitiveClosureI[k]   && transitiveClosure[k][j]); 
        }
      }
    }
    return transitiveClosure;
  }
  
  /**
   * Find all nodes that belong to the same loop as a given node.
   * @param node A node of a loop.
   * @return All nodes of the same loop
   */
  public static Sets.SetView<CFANode> findLoopNodes(CFANode node) {
    return Sets.intersection(transitiveSuccessors(node, false), transitivePredecessors(node, false));
  }
  
  /**
   * Creates two mappings from all loop-entry and loop-exit edges respectively
   * to the head of the loop.
   * 
   * The analysis is purely intraprocedural, so function call/return edges
   * that leave or re-enter loops will always be considered as loop entry or exit edges.
   * 
   * Does not work with nested loops!
   * 
   * @param allNodes The set of all nodes of the CFA.
   * @return Two mappings from loop-entry edges to the head of the loop and from loop-exit edges to the head of the loop.
   */
  public static Pair<Map<CFAEdge, CFANode>, Map<CFAEdge, CFANode>> allLoopEntryExitEdges(Set<CFANode> allNodes) {
    Map<CFAEdge, CFANode> loopEntryEdges = new HashMap<CFAEdge, CFANode>();
    Map<CFAEdge, CFANode> loopExitEdges = new HashMap<CFAEdge, CFANode>();

    for (CFANode loopHeadNode : filter(allNodes, FILTER_LOOP_HEADS)) {
      Collection<CFANode> loopNodes = findLoopNodes(loopHeadNode);
      for (CFANode loopNode : loopNodes) {
        
        { // entry edges
          for (int i = 0; i < loopNode.getNumEnteringEdges(); i++) {
            CFAEdge e = loopNode.getEnteringEdge(i);
            
            if (!loopNodes.contains(e.getPredecessor())) {
              CFANode old = loopEntryEdges.put(e, loopHeadNode);
              
              checkState(old == null, "Edge enters two loops!");
            }
          }
          
          CFAEdge e = loopNode.getEnteringSummaryEdge();
          if (e != null && !loopNodes.contains(e.getPredecessor())) {
            CFANode old = loopEntryEdges.put(e, loopHeadNode);
            
            checkState(old == null, "Edge enters two loops!");
          }
        }
        
        { // exit edges
          for (int i = 0; i < loopNode.getNumLeavingEdges(); i++) {
            CFAEdge e = loopNode.getLeavingEdge(i);
            
            if (!loopNodes.contains(e.getSuccessor())) {
              CFANode old = loopExitEdges.put(e, loopHeadNode);
              
              checkState(old == null, "Edge exits two loops!");
            }
          }
          
          CFAEdge e = loopNode.getLeavingSummaryEdge();
          if (e != null && !loopNodes.contains(e.getSuccessor())) {
            CFANode old = loopExitEdges.put(e, loopHeadNode);
            
            checkState(old == null, "Edge exits two loops!");
          }
        }
      }
    }
    return Pair.of(loopEntryEdges, loopExitEdges);
  }
}
