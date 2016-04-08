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
package org.sosy_lab.cpachecker.cpa.andersen.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * This class represents a directed graph.<br>
 * A new {@link DirectedGraph.Node} for the variable <code>var</code> is inserted by calling
 * {@link DirectedGraph#getNode(String var)} if <code>var</code> is not already mapped to another
 * node.
 */
public class DirectedGraph {

  /**
   * This class represents a <code>Node</code> in a {@link DirectedGraph}.
   */
  public class Node {

    /** The Node this one was merged into, or <code>null</code> if this node is still valid. */
    private DirectedGraph.Node replacement = null;

    public int dfs = 0;
    public int lowlink = 0;

    /** The {@link DirectedGraph.Node} this points-to variables can safely be merged with. */
    public DirectedGraph.Node mergePts = null;

    /**
     * {var | "*n \subseteq var" \in complex-constraints}, where n is (one of) the variable(s) this
     * {@link DirectedGraph.Node} stands for.
     */
    public final Set<String> complexConstrMeSub = new HashSet<>();

    /**
     * {var | "var \subseteq *n" \in complex-constraints}, where n is (one of) the variable(s) this
     * {@link DirectedGraph.Node} stands for.
     */
    public final Set<String> complexConstrMeSuper = new HashSet<>();

    /** This {@link DirectedGraph.Node}s points-to set. */
    private final Set<String> pointsToSet = new HashSet<>();

    /** This {@link DirectedGraph.Node}s predecessors. */
    private final Set<DirectedGraph.Node> predecessors = new HashSet<>();

    /** This {@link DirectedGraph.Node}s successors. */
    private final Set<DirectedGraph.Node> successors = new HashSet<>();


    private Node() {}

    /**
     * Test if this {@link DirectedGraph.Node} is valid.<br>
     * <i>Note:</i> A {@link DirectedGraph.Node} gets invalid, if it is merged into another one.
     *
     * @return <code>true</code> if this {@link DirectedGraph.Node} is still valid.
     *
     * @see DirectedGraph.Node#getReplacement()
     */
    public boolean isValid() {

      return replacement == null;
    }

    /**
     * Returns the {@link DirectedGraph.Node}, this one was merged into, or <code>null</code> if
     * this {@link DirectedGraph.Node} is still vaild.
     *
     * @return the {@link DirectedGraph.Node} this one was merged into.
     *
     * @see DirectedGraph.Node#isValid()
     */
    public DirectedGraph.Node getReplacement() {

      return replacement;
    }

    /**
     * Tests if the given {@link DirectedGraph.Node} is a successor of this one. In other words,
     * tests if there is an edge from this {@link DirectedGraph.Node} to the passed one.
     *
     * @param succ
     *        The other {@link DirectedGraph.Node}.
     * @return <code>true</code> if the given {@link DirectedGraph.Node} is a successor of this one.
     */
    public boolean isSuccessor(DirectedGraph.Node succ) {

      return successors.contains(succ);
    }

    /**
     * Returns a {@link Collection} of this {@link DirectedGraph.Node}s successors.
     *
     * @return a {@link Collection} of this {@link DirectedGraph.Node}s successors.
     */
    public Collection<DirectedGraph.Node> getSuccessors() {

      return successors;
    }

    /**
     * Propagates all points-to information from this {@link DirectedGraph.Node} to the given one.
     *
     * @return <code>true</code> if the points-to set of the given {@link DirectedGraph.Node}
     *         changed as a result of this call.
     */
    public boolean propagatePointerTargetsTo(DirectedGraph.Node other) {

      return other.pointsToSet.addAll(this.pointsToSet);
    }

    /**
     * Adds a variable to this {@link DirectedGraph.Node}s points-to set.
     *
     * @param var
     *        Name of the variable that should be added to this {@link DirectedGraph.Node}s
     *        points-to set.
     */
    public void addPointerTarget(String var) {

      pointsToSet.add(var);
    }

    /**
     * Returns a {@link Collection} of this {@link DirectedGraph.Node}s points-to set.
     *
     * @return this {@link DirectedGraph.Node}'s points-to set.
     */
    public Collection<String> getPointsToSet() {

      return pointsToSet;
    }

    /**
     * Returns a {@link Set} of {@link DirectedGraph.Node}s of this ones points-to set.
     *
     * @return the {@link DirectedGraph.Node}s the variables of this ones points-to sets represent.
     */
    public Set<DirectedGraph.Node> getPointsToNodesSet() {

      HashSet<DirectedGraph.Node> ptNSet = new HashSet<>();

      for (String n : getPointsToSet()) {
        ptNSet.add(getNode(n));
      }

      return ptNSet;
    }
  }

  /**
   * The <code>Edge</code> represents an edge in the corresponding graph. This class only
   * encapsulates two nodes and is note directly used in the {@link DirectedGraph}.
   */
  public static class Edge {

    /** The source {@link DirectedGraph.Node} of this {@link DirectedGraph.Edge}. */
    public final DirectedGraph.Node src;

    /** The destination {@link DirectedGraph.Node} of this {@link DirectedGraph.Edge}. */
    public final DirectedGraph.Node dest;

    /**
     * Creates a new {@link DirectedGraph.Edge} with the given {@link DirectedGraph.Node}s as source
     * and destination.
     *
     * @param src
     *        The source {@link DirectedGraph.Node} of this {@link DirectedGraph.Edge}.
     * @param dest
     *        The destination {@link DirectedGraph.Node} of this {@link DirectedGraph.Edge}.
     */
    public Edge(DirectedGraph.Node src, DirectedGraph.Node dest) {
      this.src = src;
      this.dest = dest;
    }

    @Override
    public boolean equals(Object other) {

      if (other == this) {
        return true;
      }
      if (other == null) {
        return false;
      }
      if (!other.getClass().equals(this.getClass())) {
        return false;
      }

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

  /** This graphs name mapping. */
  private final Map<String, DirectedGraph.Node> nameMapping = new HashMap<>();

  /**
   * Detects a cycle in this graph that contains the edge from <code>src</code> to <code>dest</code>
   * and merges all nodes in it. Returns the new, merged node, if a cycle is found, else
   * <code>null</code>.
   * @return The merged node, if a cylce was found, else <code>null</code>.
   */
  public DirectedGraph.Node detectAndCollapseCycleContainingEdge(DirectedGraph.Edge edge) {

    HashSet<DirectedGraph.Node> reached = new HashSet<>();
    LinkedList<LinkedList<DirectedGraph.Node>> stack = new LinkedList<>();
    LinkedList<DirectedGraph.Node> cycle = null;

    LinkedList<DirectedGraph.Node> path = new LinkedList<>();
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

    if (nodes != null) {
      for (DirectedGraph.Node n : nodes) {
        mergeNodes(merged, n);
      }
    }

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

  /**
   * Returns the {@link DirectedGraph.Node} of this {@link DirectedGraph} representing the given
   * variable. If there is no node associated with the given variable, a new one is created and
   * returned.
   *
   * @param var
   *        Variable name for the requested {@link DirectedGraph.Node}.
   *
   * @return the {@link DirectedGraph.Node} associated witht he given variable.
   */
  public DirectedGraph.Node getNode(String var) {

    Node n = nameMapping.get(var);
    if (n == null) {
      nameMapping.put(var, n = new Node());
    } else if (!n.isValid()) {

      do {
        n = n.getReplacement();
      } while (!n.isValid());
      nameMapping.put(var, n);
    }

    return n;
  }

  /**
   * Returns the name mappings for this graph. Modifications of the elements of the returned
   * {@link Collection} are reflected in this graphs name mapping.
   *
   * @return a {@link Collection} of name mappings.
   */
  public Set<Map.Entry<String, Node>> getNameMappings() {

    Set<Map.Entry<String, Node>> entrySet = nameMapping.entrySet();

    for (Map.Entry<String, Node> entry : entrySet) {
      DirectedGraph.Node val = entry.getValue();
      while (!val.isValid()) {
        entry.setValue(val = val.getReplacement());
      }
    }

    return nameMapping.entrySet();
  }

  /**
   * Adds an edge to this graph.
   *
   * @param src
   *        Source node of the new edge.
   * @param dest
   *        Destination node of the new edge.
   */
  public void addEdge(DirectedGraph.Node src, DirectedGraph.Node dest) {

    if (src == dest) {
      return;
    }

    src.successors.add(dest);
    dest.predecessors.add(src);
  }
}
