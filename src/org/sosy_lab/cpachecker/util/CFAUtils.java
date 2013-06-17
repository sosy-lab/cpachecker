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
package org.sosy_lab.cpachecker.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.exceptions.JParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.primitives.Ints;

public class CFAUtils {

  /**
   * Return an {@link Iterable} that contains all entering edges of a given CFANode,
   * including the summary edge if the node as one.
   */
  public static FluentIterable<CFAEdge> allEnteringEdges(final CFANode node) {
    checkNotNull(node);
    return new FluentIterable<CFAEdge>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return new UnmodifiableIterator<CFAEdge>() {

          // the index of the next edge (-1 means the summary edge)
          private int i = (node.getEnteringSummaryEdge() != null) ? -1 : 0;

          @Override
          public boolean hasNext() {
            return i < node.getNumEnteringEdges();
          }

          @Override
          public CFAEdge next() {
            if (i == -1) {
              i = 0;
              return node.getEnteringSummaryEdge();
            }
            return node.getEnteringEdge(i++);
          }
        };
      }
    };
  }

  /**
   * Return an {@link Iterable} that contains the entering edges of a given CFANode,
   * excluding the summary edge.
   */
  public static FluentIterable<CFAEdge> enteringEdges(final CFANode node) {
    checkNotNull(node);
    return new FluentIterable<CFAEdge>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return new UnmodifiableIterator<CFAEdge>() {

          // the index of the next edge
          private int i = 0;

          @Override
          public boolean hasNext() {
            return i < node.getNumEnteringEdges();
          }

          @Override
          public CFAEdge next() {
             return node.getEnteringEdge(i++);
          }
        };
      }
    };
  }

  /**
   * Return an {@link Iterable} that contains all leaving edges of a given CFANode,
   * including the summary edge if the node as one.
   */
  public static FluentIterable<CFAEdge> allLeavingEdges(final CFANode node) {
    checkNotNull(node);
    return new FluentIterable<CFAEdge>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return new UnmodifiableIterator<CFAEdge>() {

          // the index of the next edge (-1 means the summary edge)
          private int i = (node.getLeavingSummaryEdge() != null) ? -1 : 0;

          @Override
          public boolean hasNext() {
            return i < node.getNumLeavingEdges();
          }

          @Override
          public CFAEdge next() {
            if (i == -1) {
              i = 0;
              return node.getLeavingSummaryEdge();
            }
            return node.getLeavingEdge(i++);
          }
        };
      }
    };
  }

  /**
   * Return an {@link Iterable} that contains the leaving edges of a given CFANode,
   * excluding the summary edge.
   */
  public static FluentIterable<CFAEdge> leavingEdges(final CFANode node) {
    checkNotNull(node);
    return new FluentIterable<CFAEdge>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return new UnmodifiableIterator<CFAEdge>() {

          // the index of the next edge
          private int i = 0;

          @Override
          public boolean hasNext() {
            return i < node.getNumLeavingEdges();
          }

          @Override
          public CFAEdge next() {
             return node.getLeavingEdge(i++);
          }
        };
      }
    };
  }

  private static final Function<CFAEdge,  CFANode> TO_PREDECESSOR = new Function<CFAEdge,  CFANode>() {
      @Override
      public CFANode apply(CFAEdge pInput) {
        return pInput.getPredecessor();
      }
    };


  private static final Function<CFAEdge,  CFANode> TO_SUCCESSOR = new Function<CFAEdge,  CFANode>() {
    @Override
    public CFANode apply(CFAEdge pInput) {
      return pInput.getSuccessor();
    }
  };

  /**
   * Return an {@link Iterable} that contains the predecessor nodes of a given CFANode,
   * excluding the one reachable via the summary edge (if there is one).
   */
  public static FluentIterable<CFANode> predecessorsOf(final CFANode node) {
    return enteringEdges(node).transform(TO_PREDECESSOR);
  }

  /**
   * Return an {@link Iterable} that contains all the predecessor nodes of a given CFANode,
   * including the one reachable via the summary edge (if there is one).
   */
  public static FluentIterable<CFANode> allPredecessorsOf(final CFANode node) {
    return allEnteringEdges(node).transform(TO_PREDECESSOR);
  }

  /**
   * Return an {@link Iterable} that contains the successor nodes of a given CFANode,
   * excluding the one reachable via the summary edge (if there is one).
   */
  public static FluentIterable<CFANode> successorsOf(final CFANode node) {
    return leavingEdges(node).transform(TO_SUCCESSOR);
  }

  /**
   * Return an {@link Iterable} that contains all the successor nodes of a given CFANode,
   * including the one reachable via the summary edge (if there is one).
   */
  public static FluentIterable<CFANode> allSuccessorsOf(final CFANode node) {
    return allLeavingEdges(node).transform(TO_SUCCESSOR);
  }

  /**
   * A predicate delegating to {@link CFANode#isLoopStart()}.
   */
  public static final Predicate<CFANode> IS_LOOP_NODE = new Predicate<CFANode>() {
    @Override
    public boolean apply(@Nullable CFANode pInput) {
      return pInput.isLoopStart();
    }
  };

  public static final Function<CFANode, String> GET_FUNCTION = new Function<CFANode, String>() {
    @Override
    public String apply(CFANode pInput) {
      return pInput.getFunctionName();
    }
  };

  /**
   * A comparator for comparing {@link CFANode}s by their line numbers.
   */
  public static final Comparator<CFANode> LINE_NUMBER_COMPARATOR = new Comparator<CFANode>() {
    @Override
    public int compare(CFANode pO1, CFANode pO2) {
      return Ints.compare(pO1.getLineNumber(), pO2.getLineNumber());
    }
  };


  // wrapper class for Set<CFANode> because Java arrays don't like generics
  private static class Edge {
    private final Set<CFANode> nodes = new HashSet<>(1);

    private void add(Edge n) {
      nodes.addAll(n.nodes);
    }

    private void add(CFANode n) {
      nodes.add(n);
    }

    private Set<CFANode> asNodeSet() {
      return nodes;
    }
  }

  public static class Loop {

    // loopHeads is a sub-set of nodes such that all infinite paths through
    // the set nodes will pass through at least one node in loopHeads infinitively often
    // i.e. you will have to pass through at least one loop head in every iteration
    private ImmutableSet<CFANode> loopHeads;

    private ImmutableSortedSet<CFANode> nodes;

    // the following sets are computed lazily by calling {@link #computeSets()}
    private ImmutableSet<CFAEdge> innerLoopEdges;
    private ImmutableSet<CFAEdge> incomingEdges;
    private ImmutableSet<CFAEdge> outgoingEdges;

    public Loop(CFANode loopHead, Set<CFANode> pNodes) {
      loopHeads = ImmutableSet.of(loopHead);
      nodes = ImmutableSortedSet.<CFANode>naturalOrder()
                                .addAll(pNodes)
                                .add(loopHead)
                                .build();
    }

    private void computeSets() {
      if (innerLoopEdges != null) {
        assert incomingEdges != null;
        assert outgoingEdges != null;
      }

      Set<CFAEdge> incomingEdges = new HashSet<>();
      Set<CFAEdge> outgoingEdges = new HashSet<>();

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

    void addNodes(Loop l) {
      nodes = ImmutableSortedSet.<CFANode>naturalOrder()
                                .addAll(nodes)
                                .addAll(l.nodes)
                                .build();

      innerLoopEdges = null;
      incomingEdges = null;
      outgoingEdges = null;
    }

    void mergeWith(Loop l) {
      loopHeads = Sets.union(loopHeads, l.loopHeads).immutableCopy();
      addNodes(l);
    }

    public boolean intersectsWith(Loop l) {
      return !Sets.intersection(nodes, l.nodes).isEmpty();
    }

    /**
     * Check if this loop is an outer loop of another, given one.
     */
    public boolean isOuterLoopOf(Loop other) {
      this.computeSets();
      other.computeSets();

      return this.innerLoopEdges.containsAll(other.incomingEdges)
          && this.innerLoopEdges.containsAll(other.outgoingEdges);
    }

    public ImmutableSortedSet<CFANode> getLoopNodes() {
      return nodes;
    }

    public ImmutableSet<CFANode> getLoopHeads() {
      return loopHeads;
    }

    public ImmutableSet<CFAEdge> getIncomingEdges() {
      computeSets();
      return incomingEdges;
    }

    public ImmutableSet<CFAEdge> getOutgoingEdges() {
      computeSets();
      return outgoingEdges;
    }

    @Override
    public String toString() {
      computeSets();
      return "Loop with heads " + loopHeads + "\n"
           + "  incoming: " + incomingEdges + "\n"
           + "  outgoing: " + outgoingEdges + "\n"
           + "  nodes:    " + nodes;
    }
  }

  public static Collection<Loop> findLoops(SortedSet<CFANode> nodes, Language language) throws ParserException {
    final int min = nodes.first().getNodeNumber();
    final int max = nodes.last().getNodeNumber();
    final int size = max + 1 - min;

    nodes = new TreeSet<>(nodes); // copy nodes because we change it

    // all nodes of the graph
    // Fields may be null, iff there is no node with this number.
    // forall i : nodes[i].getNodeNumber() == i + min
    final CFANode[] nodesArray = new CFANode[size];

    // all edges of the graph
    // Iff there is an edge from nodes[i] to nodes[j], edges[i][j] is not null.
    // The set edges[i][j].nodes contains all nodes that were eliminated and merged into this edge.
    final Edge[][] edges =  new Edge[size][size];

    List<Loop> loops = new ArrayList<>();

    // FIRST step: initialize arrays
    for (CFANode n : nodes) {
      int i = n.getNodeNumber() - min;
      assert nodesArray[i] == null;
      nodesArray[i] = n;

      for (int e = 0; e < n.getNumLeavingEdges(); e++) {
        CFAEdge edge = n.getLeavingEdge(e);
        CFANode succ = edge.getSuccessor();
        int j = succ.getNodeNumber() - min;
        edges[i][j] = new Edge();

        if (i == j) {
          // self-edge
          handleLoop(succ, i, edges, loops);
        }
      }
    }

    // SECOND step: simplify graph and identify loops
    boolean changed;
    do {
      // first try without the "reverse merge" strategy
      // this strategy may eliminate real loop heads too early so that the
      // algorithm would propose another node of the loop has loop head
      // (which is counter-intuitive to the user)
      changed = identifyLoops(false, nodes, min, nodesArray, edges, loops);

      if (!changed && !nodes.isEmpty()) {
        // but if we have to, try and use this strategy
        changed = identifyLoops(true, nodes, min, nodesArray, edges, loops);
      }

      if (!changed && !nodes.isEmpty()) {
        // This is a very complex loop structure.
        // We just pick a node randomly and merge it into others.
        // This is imprecise, but not wrong.

        CFANode currentNode = nodes.last();
        final int current = currentNode.getNodeNumber() - min;
        // Now merge current into all its successors

        mergeNodeIntoSuccessors(currentNode, current, nodesArray, edges, loops);
        nodes.remove(currentNode);
        changed = true;
      }

    } while (changed && !nodes.isEmpty()); // stop if nothing has changed or nodes is empty


    // check that the complete graph has collapsed
    if (!nodes.isEmpty()) {
      switch (language) {
      case C:
        throw new CParserException("Code structure is too complex, could not detect all loops!");
      case JAVA:
        throw new JParserException("Code structure is too complex, could not detect all loops!");
      default:
        throw new AssertionError("unknown language");
      }
    }

    // THIRD step:
    // check all pairs of loops if one is an inner loop of the other
    // the check is symmetric, so we need to check only (i1, i2) with i1 < i2

    NavigableSet<Integer> toRemove = new TreeSet<>();
    for (int i1 = 0; i1 < loops.size(); i1++) {
      Loop l1 = loops.get(i1);

      for (int i2 = i1+1; i2 < loops.size(); i2++) {
        Loop l2 = loops.get(i2);

        if (!l1.intersectsWith(l2)) {
          // loops have nothing in common
          continue;
        }

        if (l1.isOuterLoopOf(l2)) {

          // l2 is an inner loop
          // add it's nodes to l1
          l1.addNodes(l2);

        } else if (l2.isOuterLoopOf(l1)) {

          // l1 is an inner loop
          // add it's nodes to l2
          l2.addNodes(l1);

        } else {
          // strange goto loop, merge the two together

          l1.mergeWith(l2);
          toRemove.add(i2);
        }
      }
    }

    for (int i : toRemove.descendingSet()) { // need to iterate in reverse order!
      loops.remove(i);
    }

    return loops;
  }

  private static boolean identifyLoops(boolean reverseMerge, SortedSet<CFANode> nodes, final int offset,
      final CFANode[] nodesArray, final Edge[][] edges, List<Loop> loops) {

    boolean changed = false;

      // merge nodes with their neighbors, if possible
      Iterator<CFANode> it = nodes.iterator();
      while (it.hasNext()) {
        final CFANode currentNode = it.next();
        final int current = currentNode.getNodeNumber() - offset;

        // find edges of current
        final int predecessor = findSingleIncomingEdgeOfNode(current, edges);
        final int successor   = findSingleOutgoingEdgeOfNode(current, edges);

        if ((predecessor == -1) && (successor == -1)) {
          // no edges, eliminate node
          it.remove(); // delete currentNode

        } else if ((predecessor == -1) && (successor > -1)) {
          // no incoming edges, one outgoing edge
          final int successor2 = findSingleOutgoingEdgeOfNode(successor, edges);
          if (successor2 == -1) {
            // the current node is a source that is only connected with a sink
            // we can remove it
            edges[current][successor] = null;
            it.remove(); // delete currentNode
          }

        } else if ((successor == -1) && (predecessor > -1)) {
          // one incoming edge, no outgoing edges
          final int predecessor2 = findSingleIncomingEdgeOfNode(predecessor, edges);
          if (predecessor2 == -1) {
            // the current node is a sink that is only connected with a source
            // we can remove it
            edges[predecessor][current] =  null;
            it.remove(); // delete currentNode
          }

        } else if ((predecessor > -1) && (successor != -1)) {
          // current has a single incoming edge from predecessor and is no sink, eliminate current
          changed = true;

          // copy all outgoing edges (current,j) to (predecessor,j)
          moveOutgoingEdges(currentNode, current, predecessor, edges);

          // delete from graph
          edges[predecessor][current] = null;
          it.remove(); // delete currentNode

          // now predecessor node might have gained a self-edge
          if (edges[predecessor][predecessor] != null) {
            CFANode pred = nodesArray[predecessor];
            handleLoop(pred, predecessor, edges, loops);
          }


        } else if (reverseMerge && (successor > -1) && (predecessor != -1)) {
          // current has a single outgoing edge to successor and is no source, eliminate current
          changed = true;

          // copy all incoming edges (j,current) to (j,successor)
          moveIncomingEdges(currentNode, current, successor, edges);

          // delete from graph
          edges[current][successor] = null;
          it.remove(); // delete currentNode

          // now successor node might have gained a self-edge
          if (edges[successor][successor] != null) {
            CFANode succ = nodesArray[successor];
            handleLoop(succ, successor, edges, loops);
          }
        }
      }

      return changed;
  }

  private static void moveIncomingEdges(final CFANode fromNode, final int from, final int to,
      final Edge[][] edges) {
    Edge edgeFromTo = edges[from][to];

    for (int j = 0; j < edges.length; j++) {
      if (edges[j][from] != null) {
        // combine three edges (j,current) (current,successor) and (j,successor)
        // into a single edge (j,successor)
        Edge targetEdge = getEdge(j, to, edges);
        targetEdge.add(edges[j][from]);
        if (edgeFromTo != null) {
          targetEdge.add(edgeFromTo);
        }
        targetEdge.add(fromNode);
        edges[j][from] = null;
      }
    }
  }

  /**
   * Copy all outgoing edges of "from" to "to", and delete them from "from" afterwards.
   */
  private static void moveOutgoingEdges(final CFANode fromNode, final int from, final int to,
      final Edge[][] edges) {
    Edge edgeToFrom = edges[to][from];

    for (int j = 0; j < edges.length; j++) {
      if (edges[from][j] != null) {
        // combine three edges (predecessor,current) (current,j) and (predecessor,j)
        // into a single edge (predecessor,j)
        Edge targetEdge = getEdge(to, j, edges);
        targetEdge.add(edges[from][j]);
        if (edgeToFrom != null) {
          targetEdge.add(edgeToFrom);
        }
        targetEdge.add(fromNode);
        edges[from][j] = null;
      }
    }
  }

  private static void mergeNodeIntoSuccessors(CFANode currentNode, final int current,
      final CFANode[] nodesArray, final Edge[][] edges, List<Loop> loops) {
    List<Integer> predecessors = new ArrayList<>();
    List<Integer> successors = new ArrayList<>();
    for (int i = 0; i < edges.length; i++) {
      if (edges[i][current] != null) {
        predecessors.add(i);
      }
      if (edges[current][i] != null) {
        successors.add(i);
      }
    }

    for (int successor : successors) {
      for (int predecessor : predecessors) {
        // create edge (pred, succ) from (pred, current) and (current, succ)
        Edge targetEdge = getEdge(predecessor, successor, edges);
        targetEdge.add(edges[predecessor][current]);
        targetEdge.add(edges[current][successor]);
        targetEdge.add(currentNode);

      }
      if (edges[successor][successor] != null) {
        CFANode succ = nodesArray[successor];
        handleLoop(succ, successor, edges, loops);
      }
    }

    for (int predecessor : predecessors) {
      edges[predecessor][current] = null;
    }
    for (int successor : successors) {
      edges[current][successor] = null;
    }
  }

  // get edge from edges array, ensuring that it is added if it does not exist yet
  private static Edge getEdge(int i, int j, Edge[][] edges) {
    Edge result = edges[i][j];
    if (edges[i][j] == null) {
      result = new Edge();
      edges[i][j] = result;
    }
    return result;
  }

  // create a loop from a node with a self-edge
  private static void handleLoop(final CFANode loopHead, int loopHeadIndex,
      final Edge[][] edges, Collection<Loop> loops) {
    assert loopHead != null;

    // store loop
    Loop loop = new Loop(loopHead, edges[loopHeadIndex][loopHeadIndex].asNodeSet());
    loops.add(loop);

    // remove this loop from the graph
    edges[loopHeadIndex][loopHeadIndex] = null;
  }

  // find index of single predecessor of node i
  // if there is no successor, -1 is returned
  // if there are several successor, -2 is returned
  private static int findSingleIncomingEdgeOfNode(int i, Edge[][] edges) {
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
  private static int findSingleOutgoingEdgeOfNode(int i, Edge[][] edges) {
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
