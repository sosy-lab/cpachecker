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
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.Iterables.any;
import static com.google.common.collect.Iterables.filter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionReturnEdge;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
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
  
  // wrapper class for Set<CFANode> because Java arrays don't like generics
  private static class NodeSet {
    private final Set<CFANode> nodes = new HashSet<CFANode>(1);
  }
  
  private static class Loop {
    
    private SortedSet<CFANode> nodes;
    private Set<CFAEdge> innerLoopEdges;
    private Set<CFAEdge> incomingEdges;
    private Set<CFAEdge> outgoingEdges;
    
    public Loop(Set<CFANode> pNodes) {
      nodes = ImmutableSortedSet.copyOf(pNodes);
      
      Set<CFAEdge> incomingEdges = new HashSet<CFAEdge>();
      Set<CFAEdge> outgoingEdges = new HashSet<CFAEdge>();
      
      for (CFANode n : nodes) {
        for (int i = 0; i < n.getNumEnteringEdges(); i++) {
          incomingEdges.add(n.getEnteringEdge(i));
        }
        for (int i = 0; i < n.getNumLeavingEdges(); i++) {
          outgoingEdges.add(n.getLeavingEdge(i));
        }
      }
      
      innerLoopEdges = Sets.intersection(incomingEdges, outgoingEdges).immutableCopy();
      incomingEdges.removeAll(innerLoopEdges);
      outgoingEdges.removeAll(innerLoopEdges);
      
      assert !incomingEdges.isEmpty() : "Unreachable loop?";
      
      this.incomingEdges = ImmutableSet.copyOf(incomingEdges);
      this.outgoingEdges = ImmutableSet.copyOf(outgoingEdges);
    }
    
    public void addNodes(Iterable<CFANode> pNodes) {
      nodes = ImmutableSortedSet.<CFANode>naturalOrder().addAll(nodes).addAll(pNodes).build();
      
      Set<CFAEdge> incomingEdges = new HashSet<CFAEdge>();
      Set<CFAEdge> outgoingEdges = new HashSet<CFAEdge>();
      
      for (CFANode n : nodes) {
        for (int i = 0; i < n.getNumEnteringEdges(); i++) {
          incomingEdges.add(n.getEnteringEdge(i));
        }
        for (int i = 0; i < n.getNumLeavingEdges(); i++) {
          outgoingEdges.add(n.getLeavingEdge(i));
        }
      }
      
      innerLoopEdges = Sets.intersection(incomingEdges, outgoingEdges).immutableCopy();
      incomingEdges.removeAll(innerLoopEdges);
      outgoingEdges.removeAll(innerLoopEdges);
      
      assert !incomingEdges.isEmpty() : "Unreachable loop?";
      
      this.incomingEdges = ImmutableSet.copyOf(incomingEdges);
      this.outgoingEdges = ImmutableSet.copyOf(outgoingEdges);
    }
   
    @Override
    public String toString() {
      return nodes.toString() + "\n" + incomingEdges + "\n" + outgoingEdges + "\n";
    }
  }
  
  public static Collection<Loop> findLoops(SortedSet<CFANode> pNodes) {
    final int min = pNodes.first().getNodeNumber();
    final int max = pNodes.last().getNodeNumber();
    final int size = max + 1 - min;
    
    // all nodes of the graph
    // Fields may be null, iff there is no node with this number.
    // forall i : nodes[i].getNodeNumber() == i + min
    final CFANode[] nodes = new CFANode[size];
    
    // all edges of the graph
    // Iff there is an edge from nodes[i] to nodes[j], edges[i][j] is not null.
    // The set edges[i][j].nodes contains all nodes that were eliminated and merged into this edge. 
    final NodeSet[][] edges =  new NodeSet[size][size];
    
    // initialize arrays
    for (CFANode n : pNodes) {
      int i = n.getNodeNumber() - min;
      assert nodes[i] == null;
      if (n.getNumEnteringEdges() > 0 && n.getNumEnteringEdges() > 0) {
        // only add nodes that aren't sources nor sinks
        nodes[i] = n;
  
        for (int e = 0; e < n.getNumLeavingEdges(); e++) {
          CFAEdge edge = n.getLeavingEdge(e);
          CFANode succ = edge.getSuccessor();
          int j = succ.getNodeNumber() - min;
          edges[i][j] = new NodeSet();
        }
      }
    }

    List<Loop> loops = new ArrayList<Loop>();

    boolean changed;
    do {
      changed = false;
      
      // merge nodes with their neighbors, if possible
      for (int i = 0; i < size; i++) {
        if (nodes[i] == null) {
          continue;
        }

        // find edges of i
        final int predecessor = findSingleIncomingEdgeOfNode(i, edges);
        final int successor   = findSingleOutgoingEdgeOfNode(i, edges);
        
        if (predecessor == -1 && successor == -1) {
          // no edges, eliminate node
          nodes[i] = null;
          continue;
        
          
        } else if (predecessor > -1) {
          // i has a single incoming edge from predecessor, eliminate i
          changed = true;
          
          // copy all outgoing edges (i,j) to (predecessor,j)
          for (int j = 0; j < size; j++) {
            if (edges[i][j] != null) {
              // combine three edges (i,j) (predecessor,i) and (predecessor,j)
              // into a single edge (predecessor,j)
              if (edges[predecessor][j] == null) {
                edges[predecessor][j] = new NodeSet();
              }
              edges[predecessor][j].nodes.addAll(edges[i][j].nodes);
              edges[predecessor][j].nodes.addAll(edges[predecessor][i].nodes);
              edges[predecessor][j].nodes.add(nodes[i]);
              edges[i][j] = null;
            }
          }
          
          // delete from graph
          edges[predecessor][i] = null;
          nodes[i] = null;

          // now predecessor node might have gained a self-edge
          if (edges[predecessor][predecessor] != null) {
            handleLoop(nodes, edges, loops, predecessor);
          }
        
          
        } else if (successor > -1) {
          // i has a single outgoing edge to successor, eliminate i
          changed = true;
          
          // copy all incoming edges (j,i) to (j,successor)
          for (int j = 0; j < size; j++) {
            if (edges[j][i] != null) {
              // combine three edges (j,i) (i,successor) and (j,successor)
              // into a single edge (j,successor)
              if (edges[j][successor] == null) {
                edges[j][successor] = new NodeSet();
              }
              edges[j][successor].nodes.addAll(edges[j][i].nodes);
              edges[j][successor].nodes.addAll(edges[i][successor].nodes);
              edges[j][successor].nodes.add(nodes[i]);
              edges[j][i] = null;
            }
          }
                    
          // delete from graph
          edges[i][successor] = null;
          nodes[i] = null;

          // now successor node might have gained a self-edge
          if (edges[successor][successor] != null) {
            handleLoop(nodes, edges, loops, successor);
          }
        }
      }
      
    } while (changed);

    
    // check that the complete graph has collapsed
    if (any(Arrays.asList(nodes), notNull())) {
      throw new RuntimeException("Code structure is too complex, could not detect all loops!");
    }
   
    // check all pairs of loops if one is an inner loop of the other
    // the check is symmetric, so we need to check only (i1, i2) with i1 < i2
    
    NavigableSet<Integer> toRemove = new TreeSet<Integer>();
    for (int i1 = 0; i1 < loops.size(); i1++) {
      Loop l1 = loops.get(i1);
      
      for (int i2 = i1+1; i2 < loops.size(); i2++) {
        Loop l2 = loops.get(i2);
        
        if (Sets.intersection(l1.nodes, l2.nodes).isEmpty()) {
          // loops have nothing in common
          continue;
        }
        
        if (l1.innerLoopEdges.containsAll(l2.incomingEdges)
         && l1.innerLoopEdges.containsAll(l2.outgoingEdges)) {
          
          // l2 is an inner loop
          // add it's nodes to l1
//          System.out.println(l2 + " is inner loop of " + l1);
          l1.addNodes(l2.nodes);
          
        } else if (l2.innerLoopEdges.containsAll(l1.incomingEdges)
                && l2.innerLoopEdges.containsAll(l1.outgoingEdges)) {

          // l1 is an inner loop
          // add it's nodes to l2
//          System.out.println(l1 + " is inner loop of " + l2);
          l2.addNodes(l1.nodes);
          
        } else {
          // strange goto loop, merge the two together
//          System.out.println(l2 + " is merged into " + l1);

          l1.addNodes(l2.nodes);
          toRemove.add(i2);
        }
      }
    }

    for (int i : toRemove.descendingSet()) { // need to iterate in reverse order!
      loops.remove(i);
    }
 
    return loops;
  }

  private static void handleLoop(final CFANode[] nodes,
      final NodeSet[][] edges, Collection<Loop> loops, int loopHeadIndex) {
    CFANode loopHead = nodes[loopHeadIndex];

    // collect all nodes that belong to this loop
    Set<CFANode> loopNodes = edges[loopHeadIndex][loopHeadIndex].nodes;
    loopNodes.add(loopHead);

    Loop loop = new Loop(loopNodes);
    loops.add(loop);
//    System.out.println("Found a loop: " + loop);
    
    // remove this loop
    edges[loopHeadIndex][loopHeadIndex] = null;
  }

  // find index of single predecessor of node i
  // if there is no successor, -1 is returned
  // if there are several successor, -2 is returned
  private static int findSingleIncomingEdgeOfNode(int i, NodeSet[][] edges) {
    final int size = edges.length;
    
    int predecessor = -1;
    for (int j = 0; j < size; j++) {
      if (edges[j][i] != null) {
        // i has incoming edge from j
        
        if (predecessor > -1) {
          // not the only incoming edge
          return -2;
        } else {
          predecessor = j;
        }
      }
    }
    return predecessor;
  }
  
  // find index of single successor of node i
  // if there is no successor, -1 is returned
  // if there are several successors, -2 is returned
  private static int findSingleOutgoingEdgeOfNode(int i, NodeSet[][] edges) {
    final int size = edges.length;
    
    int successor = -1;
    for (int j = 0; j < size; j++) {
      if (edges[i][j] != null) {
        // i has outgoing edge to j
        
        if (successor > -1) {
          // not the only outgoing edge
          return -2;
        } else {
          successor = j;
        }
      }
    }
    return successor;
  }
}
