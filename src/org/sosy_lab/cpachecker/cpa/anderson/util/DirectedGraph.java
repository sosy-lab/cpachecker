/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.anderson.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cpa.pointerA.util.DirectedGraph;
import org.sosy_lab.cpachecker.cpa.pointerA.util.Node;

public class DirectedGraph {

  public class Node {

    private DirectedGraph.Node replacement = null;

    public int dfs = 0;
    public int lowlink = 0;

    public DirectedGraph.Node mergePts = null;

    public final Set<String> complexConstrMeSub = new HashSet<String>();
    public final Set<String> complexConstrMeSuper = new HashSet<String>();

    private final Set<String> pointsToSet = new HashSet<String>();

    private final Set<DirectedGraph.Node> predecessors = new HashSet<DirectedGraph.Node>();
    private final Set<DirectedGraph.Node> successors = new HashSet<DirectedGraph.Node>();

    public boolean isValid() {

      return replacement == null;
    }

    public DirectedGraph.Node getReplacement() {

      return replacement;
    }

    public boolean isSuccessor(DirectedGraph.Node succ) {

      return successors.contains(succ);
    }

    public Collection<DirectedGraph.Node> getSuccessors() {

      return successors;
    }

    public boolean propagatePointerTargetsTo(DirectedGraph.Node other) {

      return other.pointsToSet.addAll(this.pointsToSet);
    }

    public void addPointerTarget(String var) {

      pointsToSet.add(var);
    }

    public Collection<String> getPointsToSet() {

      return pointsToSet;
    }

    public Collection<DirectedGraph.Node> getPointsToNodesSet() {

      HashSet<DirectedGraph.Node> ptNSet = new HashSet<DirectedGraph.Node>();

      for (String n : getPointsToSet())
        ptNSet.add(getNode(n));

      return ptNSet;
    }
  }

  public class Edge {

    public final DirectedGraph.Node src;
    public final DirectedGraph.Node dest;

    public Edge(DirectedGraph.Node src, DirectedGraph.Node dest) {
      this.src = src;
      this.dest = dest;
    }

    @Override
    public boolean equals(Object other) {

      if (other == this)
        return true;

      if (!other.getClass().equals(this.getClass()))
        return false;

      DirectedGraph.Edge o = (DirectedGraph.Edge) other;

      return this.src.equals(o.src) && this.dest.equals(o.dest);
    }

    @Override
    public int hashCode() {

      int hash = 61;
      hash = 31 * hash + src.hashCode();
      hash = 31 * hash + dest.hashCode();

      return hash;
    }
  }

  private final Map<String, DirectedGraph.Node> nameMapping = new HashMap<String, DirectedGraph.Node>();

  /**
   * Detects a cycle in this graph that contains the edge from <code>src</code> to <code>dest</code>
   * and merges all nodes in it. Returns the new, merged node, if a cycle is found, else
   * <code>null</code>.
   *
   * @param src The source node for the edge.
   * @param dest The destination node for the edge.
   * @return The merged node, if a cylce was found, else <code>null</code>.
   */
  public DirectedGraph.Node detectAndCollapseCycleContainingEdge(DirectedGraph.Edge edge) {

    HashSet<DirectedGraph.Node> reached = new HashSet<DirectedGraph.Node>();
    LinkedList<LinkedList<DirectedGraph.Node>> stack = new LinkedList<LinkedList<DirectedGraph.Node>>();
    LinkedList<DirectedGraph.Node> cycle = null;

    LinkedList<DirectedGraph.Node> path = new LinkedList<DirectedGraph.Node>();
    path.add(edge.src);
    path.add(edge.dest);
    stack.push(path);

    reached.add(edge.src);
    reached.add(edge.dest);

    // use dfs to find cylces containing n
    dfs: while (!stack.isEmpty()) {

      // get new path to check
      path = stack.poll();
      DirectedGraph.Node cur = path.getLast();

      // check successors
      for (DirectedGraph.Node succ : cur.getSuccessors()) {

        if (reached.contains(succ)) {
          // stop checking this path

          // found cycle?
          if (succ.equals(edge.src)) {

            // set current path as cycle path and stop dfs
            cycle = path;
            break dfs;

          } else {

            // no cycle, but already checked... try next
            continue;
          }
        } // if (reached.contains(succ))

        // succ was not reached yet, so add to reached...
        reached.add(succ);

        // ... and push extended path to stack
        @SuppressWarnings("unchecked")
        LinkedList<DirectedGraph.Node> extPath = (LinkedList<Node>) path.clone();
        extPath.add(succ);
        stack.push(extPath);

      } // for (DirectedGraph.Node succ : cur.getSuccessors())

      // all successors of current path checked and no cycle... try next path from stack

    } // dfs: while (!stack.isEmpty())

    // no cycle found?
    if (cycle == null) {

      // no cycle, nothing else to do, no new node to return...
      return null;
    }

    // collapse nodes in cycle and return new one
    return mergeNodes(cycle.poll(), cycle);
  }

  /**
   * Merges all given in one node and updates the graph appropriately.
   *
   * @param nodes
   *        List of nodes that should be merged.
   * @return The node, that replaces all given.
   */
  public DirectedGraph.Node mergeNodes(DirectedGraph.Node merged, Collection<DirectedGraph.Node> nodes) {

    if (nodes != null)
      for (DirectedGraph.Node n : nodes)
        mergeNodes(merged, n);

    return merged;
  }

  /**
   * Merges the node <code>old</code> into <code>merged</code> and returns <code>merged</code>.
   * The graph is also updated appropriately.
   *
   * @param merged
   *        First node.
   * @param old
   *        Second node.
   * @return the first, updated node <code>merged</code>.
   */
  public DirectedGraph.Node mergeNodes(DirectedGraph.Node merged, DirectedGraph.Node old) {

    old.replacement = merged;

    for (DirectedGraph.Node oldPred : old.predecessors) {
      oldPred.successors.remove(old);
      addEdge(oldPred, merged);
    }

    for (DirectedGraph.Node oldSucc : old.successors) {
      oldSucc.predecessors.remove(old);
      addEdge(merged, oldSucc);
    }

    old.propagatePointerTargetsTo(merged);

    return merged;
  }

  public DirectedGraph.Node getNode(String var) {

    Node n = nameMapping.get(var);
    if (n == null)
      nameMapping.put(var, n = new Node());

    else if (!n.isValid()) {

      do
        n = n.getReplacement();
      while (!n.isValid());
      nameMapping.put(var, n);
    }

    return n;
  }

  public Set<Map.Entry<String, Node>> getNameMappings() {

    Set<Map.Entry<String, Node>> entrySet = nameMapping.entrySet();

    for (Map.Entry<String, Node> entry : entrySet) {
      DirectedGraph.Node val = entry.getValue();
      while (!val.isValid())
        entry.setValue(val = val.getReplacement());
    }

    return nameMapping.entrySet();
  }

  public void addEdge(DirectedGraph.Node src, DirectedGraph.Node dest) {

    if (src == dest)
      return;

    src.successors.add(dest);
    dest.predecessors.add(src);
  }

  public void clear() {

    nameMapping.clear();
  }
}
