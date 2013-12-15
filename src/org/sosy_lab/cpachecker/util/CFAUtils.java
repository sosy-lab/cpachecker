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
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatementVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.java.CFAGenerationRuntimeException;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.exceptions.JParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Queues;
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

  static final Function<CFAEdge,  CFANode> TO_PREDECESSOR = new Function<CFAEdge,  CFANode>() {
      @Override
      public CFANode apply(CFAEdge pInput) {
        return pInput.getPredecessor();
      }
    };


  static final Function<CFAEdge,  CFANode> TO_SUCCESSOR = new Function<CFAEdge,  CFANode>() {
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
        enteringEdges(n).copyInto(incomingEdges);
        leavingEdges(n).copyInto(outgoingEdges);
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

    public ImmutableSet<CFAEdge> getInnerLoopEdges() {
      computeSets();
      return innerLoopEdges;
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

  /**
   * This Visitor searches for backwards edges in the CFA, if some backwards edges
   * were found can be obtained by calling the method hasBackwardsEdges()
   */
  static class FindBackwardsEdgesVisitor extends DefaultCFAVisitor {

    private boolean hasBackwardsEdges = false;

    @Override
    public TraversalProcess visitNode(CFANode pNode) {

      if (pNode.getNumLeavingEdges() == 0) {
        return TraversalProcess.CONTINUE;
      } else if (pNode.getNumLeavingEdges() == 1
                 && pNode.getLeavingEdge(0).getSuccessor().getReversePostorderId() >= pNode.getReversePostorderId()) {

        hasBackwardsEdges = true;
        return TraversalProcess.ABORT;
      } else if (pNode.getNumLeavingEdges() == 2
                 && (pNode.getLeavingEdge(0).getSuccessor().getReversePostorderId() >= pNode.getReversePostorderId() ||
                 pNode.getLeavingEdge(1).getSuccessor().getReversePostorderId() >= pNode.getReversePostorderId())) {
        hasBackwardsEdges = true;
        return TraversalProcess.ABORT;
      } else if (pNode.getNumLeavingEdges() > 2) {
        throw new CFAGenerationRuntimeException("forgotten case in traversing cfa with more than 2 leaving edges");
      } else {
        return TraversalProcess.CONTINUE;
      }
    }

    public boolean hasBackwardsEdges() {
      return hasBackwardsEdges;
    }
  }

  /**
   * Searches for backwards edges from a given starting node
   * @param actNode The node where the search is started
   * @return indicates if a backwards edge was found
   */
  private static boolean hasBackWardsEdges(CFANode rootNode) {
    FindBackwardsEdgesVisitor visitor = new FindBackwardsEdgesVisitor();

    CFATraversal.dfs().ignoreSummaryEdges().traverseOnce(rootNode, visitor);

    return visitor.hasBackwardsEdges();
  }

  /**
   * Find all loops inside a given set of CFA nodes.
   * The nodes in the given set may not be connected
   * with any nodes outside of this set.
   * This method tries to differentiate nested loops.
   *
   * @param nodes The set of nodes to look for loops in.
   * @param language The source language.
   * @return A collection of found loops.
   * @throws ParserException
   */
  public static Collection<Loop> findLoops(SortedSet<CFANode> nodes, Language language) throws ParserException {

    // if there are no backwards directed edges, there are no loops, so we do
    // not need to search for them
    {
      CFANode functionExitNode = nodes.first(); // The function exit node is always the first
      if (functionExitNode instanceof FunctionExitNode) {
        CFANode functionEntryNode = ((FunctionExitNode)functionExitNode).getEntryNode();

        if (!hasBackWardsEdges(functionEntryNode)) {
          return ImmutableList.of();
        }
      }
    }

    nodes = new TreeSet<>(nodes); // copy nodes because we change it

    // We need to store some information per pair of CFANodes.
    // We could use Map<Pair<CFANode, CFANode>> but it would be very memory
    // inefficient. Instead we use some arrays.
    // We use the reverse post-order id of each node as the array index for that node,
    // because this id is unique, without gaps, and its minimum is 0.
    // It's important to not use the node number because it has large gaps.
    final Function<CFANode, Integer> arrayIndexForNode = new Function<CFANode, Integer>() {
        @Override
        public Integer apply(CFANode n) {
          return n.getReversePostorderId();
        }
      };
    // this is the size of the arrays
    int size = nodes.size();

    // all nodes of the graph
    // forall i : arrayIndexForNode.apply(nodes[i]) == i
    final CFANode[] nodesArray = new CFANode[size];

    // all edges of the graph
    // Iff there is an edge from nodes[i] to nodes[j], edges[i][j] is not null.
    // The set edges[i][j].nodes contains all nodes that were eliminated and merged into this edge.
    final Edge[][] edges =  new Edge[size][size];

    List<Loop> loops = new ArrayList<>();

    // FIRST step: initialize arrays
    for (CFANode n : nodes) {
      int i = arrayIndexForNode.apply(n);
      assert nodesArray[i] == null : "reverse post-order id is not unique, "
          + i + " occurs twice in function " + n.getFunctionName()
          + " at " + n + " and " + nodesArray[i];
      nodesArray[i] = n;

      for (CFAEdge edge : leavingEdges(n)) {
        CFANode succ = edge.getSuccessor();
        int j = arrayIndexForNode.apply(succ);
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
      changed = identifyLoops(false, nodes, arrayIndexForNode, nodesArray, edges, loops);

      if (!changed && !nodes.isEmpty()) {
        // but if we have to, try and use this strategy
        changed = identifyLoops(true, nodes, arrayIndexForNode, nodesArray, edges, loops);
      }

      if (!changed && !nodes.isEmpty()) {
        // This is a very complex loop structure.
        // We just pick a node randomly and merge it into others.
        // This is imprecise, but not wrong.

        CFANode currentNode = nodes.last();
        final int current = arrayIndexForNode.apply(currentNode);

        // Mark this node as a loop head
        if (edges[current][current] == null) {
          edges[current][current] = new Edge();
        }
        handleLoop(currentNode, current, edges, loops);

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

  private static boolean identifyLoops(boolean reverseMerge, SortedSet<CFANode> nodes,
      final Function<CFANode, Integer> arrayIndexForNode,
      final CFANode[] nodesArray, final Edge[][] edges, List<Loop> loops) {

    boolean changed = false;

      // merge nodes with their neighbors, if possible
      Iterator<CFANode> it = nodes.iterator();
      while (it.hasNext()) {
        final CFANode currentNode = it.next();
        final int current = arrayIndexForNode.apply(currentNode);

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

  public static Set<Integer> collectTokensFromStatement(CRightHandSide pStmt) {
    final TreeSet<Integer> result = Sets.newTreeSet();

    pStmt.accept(new CRightHandSideVisitor<Void, RuntimeException>() {

      @Override
      public Void visit(CBinaryExpression pIastBinaryExpression) throws RuntimeException {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Void visit(CCastExpression pIastCastExpression) throws RuntimeException {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Void visit(CCharLiteralExpression pIastCharLiteralExpression) throws RuntimeException {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Void visit(CFloatLiteralExpression pIastFloatLiteralExpression) throws RuntimeException {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Void visit(CIntegerLiteralExpression pIastIntegerLiteralExpression) throws RuntimeException {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Void visit(CStringLiteralExpression pIastStringLiteralExpression) throws RuntimeException {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Void visit(CTypeIdExpression pIastTypeIdExpression) throws RuntimeException {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Void visit(CTypeIdInitializerExpression pCTypeIdInitializerExpression) throws RuntimeException {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Void visit(CUnaryExpression pIastUnaryExpression) throws RuntimeException {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Void visit(CImaginaryLiteralExpression PIastLiteralExpression) throws RuntimeException {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Void visit(CArraySubscriptExpression pIastArraySubscriptExpression) throws RuntimeException {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Void visit(CFieldReference pIastFieldReference) throws RuntimeException {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Void visit(CIdExpression pIastIdExpression) throws RuntimeException {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Void visit(CPointerExpression pPointerExpression) throws RuntimeException {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Void visit(CComplexCastExpression pComplexCastExpression) throws RuntimeException {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Void visit(CFunctionCallExpression pIastFunctionCallExpression) throws RuntimeException {
        // TODO Auto-generated method stub
        return null;
      }

    });

    return result;
  }

  public static Set<Integer> collectTokensFromStatement(CStatement pStmt) {
    final TreeSet<Integer> result = Sets.newTreeSet();

    pStmt.accept(new CStatementVisitor<Void, RuntimeException>() {
      @Override
      public Void visit(CExpressionAssignmentStatement pS) throws RuntimeException {
        result.add(pS.getFileLocation().getStartingLineNumber());
        result.addAll(collectTokensFromExpression(pS.getLeftHandSide()));
        result.addAll(collectTokensFromExpression(pS.getRightHandSide()));
        return null;
      }
      @Override
      public Void visit(CExpressionStatement pS) throws RuntimeException {
        result.add(pS.getFileLocation().getStartingLineNumber());
        result.addAll(collectTokensFromExpression(pS.getExpression()));
        return null;
      }
      @Override
      public Void visit(CFunctionCallAssignmentStatement pS) throws RuntimeException {
        result.add(pS.getFileLocation().getStartingLineNumber());
        result.addAll(collectTokensFromExpression(pS.getLeftHandSide()));
        result.addAll(collectTokensFromExpression(pS.getRightHandSide().getFunctionNameExpression()));
        for (CExpression expr : pS.getRightHandSide().getParameterExpressions()) {
          result.addAll(collectTokensFromExpression(expr));
        }
        return null;
      }
      @Override
      public Void visit(CFunctionCallStatement pS) throws RuntimeException {
        result.add(pS.getFileLocation().getStartingLineNumber());
        return null;
      }
    });

    return result;
  }

  public static Set<Integer> collectTokensFromExpression(CExpression pExpr) {
    final TreeSet<Integer> result = Sets.newTreeSet();
    result.add(pExpr.getFileLocation().getStartingLineNumber());

    DefaultCExpressionVisitor<Void, RuntimeException> visitor = new DefaultCExpressionVisitor<Void, RuntimeException>() {

      @Override
        protected Void visitDefault(CExpression pE) throws RuntimeException {
          if (pE != null) {
            if (pE.getFileLocation() != null) {
              result.add(pE.getFileLocation().getStartingLineNumber());
            }
          }
          return null;
        }

      @Override
        public Void visit(CArraySubscriptExpression pE) throws RuntimeException {
          result.add(pE.getFileLocation().getStartingLineNumber());
          pE.getArrayExpression().accept(this);
          pE.getSubscriptExpression().accept(this);
          return super.visit(pE);
        }

      @Override
        public Void visit(CBinaryExpression pE) throws RuntimeException {
          result.add(pE.getFileLocation().getStartingLineNumber());
          pE.getOperand1().accept(this);
          pE.getOperand2().accept(this);
          return super.visit(pE);
        }

      @Override
        public Void visit(CCastExpression pE) throws RuntimeException {
          result.add(pE.getFileLocation().getStartingLineNumber());
          pE.getOperand().accept(this);
          return super.visit(pE);
        }

      @Override
        public Void visit(CComplexCastExpression pE) throws RuntimeException {
          result.add(pE.getFileLocation().getStartingLineNumber());
          pE.getOperand().accept(this);
          return super.visit(pE);
        }

      @Override
        public Void visit(CFieldReference pE) throws RuntimeException {
          result.add(pE.getFileLocation().getStartingLineNumber());
          pE.getFieldOwner().accept(this);
          return super.visit(pE);
        }

      @Override
        public Void visit(CPointerExpression pE) throws RuntimeException {
          result.add(pE.getFileLocation().getStartingLineNumber());
          pE.getOperand().accept(this);
          return super.visit(pE);
        }

      @Override
        public Void visit(CUnaryExpression pE) throws RuntimeException {
          result.add(pE.getFileLocation().getStartingLineNumber());
          pE.getOperand().accept(this);
          return super.visit(pE);
        }

      };

    pExpr.accept(visitor);

    return result;
  }

  public static Set<Integer> getTokensFromCFAEdge(CFAEdge pEdge) {
    final TreeSet<Integer> result = Sets.newTreeSet();
    final Deque<CFAEdge> edges = Queues.newArrayDeque();
    final Deque<CExpression> expressions = Queues.newArrayDeque();

    edges.add(pEdge);

    while (!edges.isEmpty()) {
      CFAEdge edge = edges.pop();
      switch (edge.getEdgeType()) {
      case MultiEdge: edges.addAll(((MultiEdge) edge).getEdges()); break;
      case AssumeEdge: expressions.add(((CAssumeEdge) edge).getExpression()); break;
      case CallToReturnEdge:
        CFunctionSummaryEdge fnSumEdge = (CFunctionSummaryEdge) edge;
        result.add(fnSumEdge.getLineNumber());
        result.addAll(collectTokensFromStatement(fnSumEdge.getExpression()));
        result.addAll(collectTokensFromExpression(fnSumEdge.getExpression().getFunctionCallExpression().getFunctionNameExpression()));
        result.add(fnSumEdge.getExpression().getFileLocation().getStartingLineNumber());
        result.add(fnSumEdge.getExpression().getFunctionCallExpression().getFileLocation().getStartingLineNumber());
        expressions.addAll(fnSumEdge.getExpression().getFunctionCallExpression().getParameterExpressions());
      break;
      case DeclarationEdge: result.add(((CDeclarationEdge) edge).getDeclaration().getFileLocation().getStartingLineNumber()); break;
      case FunctionCallEdge:
        result.add(((CFunctionCallEdge) edge).getLineNumber());
        expressions.addAll(((CFunctionCallEdge) edge).getArguments());
      break ;
      case FunctionReturnEdge: result.add(((CFunctionReturnEdge) edge).getLineNumber()); break;
      case ReturnStatementEdge: result.add(((CReturnStatementEdge) edge).getLineNumber()); break;
      case StatementEdge: result.addAll(collectTokensFromStatement(((CStatementEdge) edge).getStatement())); break;
      }

      while(!expressions.isEmpty()) {
        CExpression expr = expressions.pop();
        result.addAll(collectTokensFromExpression(expr));
      }
    }

    return result;
  }
}
