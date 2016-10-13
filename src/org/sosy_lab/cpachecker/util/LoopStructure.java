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
package org.sosy_lab.cpachecker.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cfa.model.CFAEdgeType.FunctionReturnEdge;
import static org.sosy_lab.cpachecker.util.CFAUtils.edgeHasType;
import static org.sosy_lab.cpachecker.util.CFAUtils.hasBackWardsEdges;
import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.exceptions.JParserException;
import org.sosy_lab.cpachecker.exceptions.ParserException;

/**
 * Class collecting and containing information about all loops in a CFA.
 */
public final class LoopStructure {

  /**
   * Class representing one loop in a CFA.
   * A loop is a subset of CFA nodes which are strongly connected,
   * i.e., it is possible to find infinite paths within this subset.
   *
   * Note that this definition is based on the structure of the CFA,
   * not on the syntax of the program.
   * This means that loops created with "goto" (without and "while" or "for" keyword)
   * are considered loops, while something like "while (1) { break; }" is not.
   *
   * Loops represented by this class do not span across several functions,
   * i.e., if there is a function call inside a loop,
   * the called function is not considered part of the loop.
   *
   * For finding all loops in a CFA, use {@link LoopStructure#getLoopStructure(MutableCFA)}.
   * However, when you already have a {@link org.sosy_lab.cpachecker.cfa.CFA} object
   * (as you have inside any analysis),
   * use {@link org.sosy_lab.cpachecker.cfa.CFA#getLoopStructure()}.
   *
   * Usual loop-detection algorithms for graphs
   * do not distinguish between nested loops,
   * but as this is a somewhat important concept at source code level,
   * the algorithm we use to find loops tries to do so.
   * A loop is nested in another loop if the outer loop contains all
   * incoming and outgoing edges of the inner loop
   * (this also means that the outer loop has a super-set of inner edges
   * and loop nodes of the inner loop).
   * The loop-head nodes of two nested nodes are usually not related.
   * This definition works quite well for "typical" nested loops,
   * i.e., two nested "while" statements,
   * but may not cover some edge-cases
   * (for example, when there is an edge from the inner loop
   * directly leaving both loops).
   * In such cases, both loops are considered only one loop
   * (which is legal according to the definition above).
   */
  public static class Loop {
    // Technically not immutable, but all modifying methods are private
    // and never called after the LoopStructure information has been collected.

    // loopHeads is a sub-set of nodes such that all infinite paths through
    // the set nodes will pass through at least one node in loopHeads infinitely often
    // i.e. you will have to pass through at least one loop head in every iteration
    private ImmutableSet<CFANode> loopHeads;

    private ImmutableSortedSet<CFANode> nodes;

    // the following sets are computed lazily by calling {@link #computeSets()}
    private ImmutableSet<CFAEdge> innerLoopEdges;
    private ImmutableSet<CFAEdge> incomingEdges;
    private ImmutableSet<CFAEdge> outgoingEdges;

    private Loop(CFANode loopHead, Set<CFANode> pNodes) {
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
        CFAUtils.enteringEdges(n).copyInto(incomingEdges);
        CFAUtils.leavingEdges(n).copyInto(outgoingEdges);
      }

      innerLoopEdges = Sets.intersection(incomingEdges, outgoingEdges).immutableCopy();
      incomingEdges.removeAll(innerLoopEdges);
      incomingEdges.removeIf(e -> e.getEdgeType().equals(CFAEdgeType.FunctionReturnEdge));
      outgoingEdges.removeAll(innerLoopEdges);
      outgoingEdges.removeIf(e -> e.getEdgeType().equals(CFAEdgeType.FunctionCallEdge));

      assert !incomingEdges.isEmpty() : "Unreachable loop?";

      this.incomingEdges = ImmutableSet.copyOf(incomingEdges);
      this.outgoingEdges = ImmutableSet.copyOf(outgoingEdges);
    }

    private void addNodes(Loop l) {
      nodes = ImmutableSortedSet.<CFANode>naturalOrder()
                                .addAll(nodes)
                                .addAll(l.nodes)
                                .build();

      innerLoopEdges = null;
      incomingEdges = null;
      outgoingEdges = null;
    }

    private void mergeWith(Loop l) {
      loopHeads = Sets.union(loopHeads, l.loopHeads).immutableCopy();
      addNodes(l);
    }

    private boolean intersectsWith(Loop l) {
      return !Sets.intersection(nodes, l.nodes).isEmpty();
    }

    /**
     * Check if this loop is an outer loop of a given one
     * according to the above definition (c.f. {@link Loop}).
     */
    public boolean isOuterLoopOf(Loop other) {
      this.computeSets();
      other.computeSets();

      return this.innerLoopEdges.containsAll(other.incomingEdges)
          && this.innerLoopEdges.containsAll(other.outgoingEdges);
    }

    /**
     * Get the set of all CFA nodes that are part of this loop.
     * This also contains the nodes of any inner nested loops.
     * @return a non-empty set of CFA nodes (sorted according to the natural ordering of CFANodes)
     */
    public ImmutableSortedSet<CFANode> getLoopNodes() {
      return nodes;
    }

    /**
     * Get the set of all CFA edges that are inside the loop,
     * i.e., which connect two CFA nodes of the loop.
     * This also contains the edges of any inner nested loops.
     * @return a non-empty set of CFA edges
     */
    public ImmutableSet<CFAEdge> getInnerLoopEdges() {
      computeSets();
      return innerLoopEdges;
    }

    /**
     * Get the set of loop-head nodes of this loop.
     *
     * Important: The definition of loop head is not related to the syntax
     * of the program, i.e., a loop head is not necessarily related
     * to the first node after a "which" statement.
     *
     * The set of loop heads is a subset of the set of all loop nodes
     * such that all infinite paths inside the loop
     * visit at least one of the loop-head nodes infinitely often.
     * (In practice, this means there is at least one loop head visited
     * in every loop iteration.)
     *
     * The heuristic that selects loop heads tries to
     * - select as few nodes as possible,
     * - select nodes close to the beginning of the loop,
     * - select nodes that are visited even on paths with zero loop iterations, and
     * - select nodes that are also syntactic loop heads if possible.
     * However, all of this is not guaranteed for strange loops created with gotos.
     *
     * @return non-empty subset of {@link #getLoopNodes()}
     */
    public ImmutableSet<CFANode> getLoopHeads() {
      return loopHeads;
    }

    /**
     * Get the set of all incoming CFA edges,
     * i.e., edges which connect a non-loop CFA node inside the same function with a loop node.
     * Although called functions are not considered loop nodes,
     * this set does not contain any edges from called functions to inside the loop.
     * @return a non-empty set of CFA edges
     */
    public ImmutableSet<CFAEdge> getIncomingEdges() {
      computeSets();
      return incomingEdges;
    }

    /**
     * Get the set of all outgoing CFA edges,
     * i.e., edges which connect a loop node with a non-loop CFA node inside the same function.
     * Although called functions are not considered loop nodes,
     * this set does not contain any edges from inside the loop to called functions.
     * @return a possibly empty (if the loop does never terminate) set of CFA edges
     */
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

  private final ImmutableMultimap<String, Loop> loops;

  private @Nullable ImmutableSet<CFANode> loopHeads = null; // computed lazily

  // computed lazily
  private @Nullable ImmutableSet<String> loopExitConditionVariables;
  private @Nullable ImmutableSet<String> loopIncDecVariables;

  private LoopStructure(ImmutableMultimap<String, Loop> pLoops) {
    loops = pLoops;
  }

  /**
   * Get the total number of loops in the program.
   */
  public int getCount() {
    return loops.size();
  }

  /**
   * Get all {@link Loop}s in one function.
   */
  public ImmutableCollection<Loop> getLoopsForFunction(String function) {
    return loops.get(checkNotNull(function));
  }

  /**
   * Get all {@link Loop}s in the program.
   */
  public ImmutableCollection<Loop> getAllLoops() {
    return loops.values();
  }

  /**
   * Get all loop head nodes (as returned by {@link Loop#getLoopHeads()})
   * for all loops in the program.
   */
  public ImmutableSet<CFANode> getAllLoopHeads() {
    if (loopHeads == null) {
      loopHeads = from(loops.values()).transformAndConcat(Loop::getLoopHeads).toSet();
    }
    return loopHeads;
  }

  public ImmutableSet<Loop> getLoopsForLoopHead(final CFANode loopHead) {
    return from(loops.values()).filter(loop -> loop.getLoopHeads().contains(loopHead)).toSet();
  }

  /**
   * Return all variables appearing in loop exit conditions.
   * The variable names are scoped in the same way as {@link VariableClassification}
   * does.
   */
  public Set<String> getLoopExitConditionVariables() {
    if (loopExitConditionVariables == null) {
      loopExitConditionVariables = collectLoopCondVars();
    }
    return loopExitConditionVariables;
  }

  private ImmutableSet<String> collectLoopCondVars() {
    // Get all variables that are used in exit-conditions
    return from(loops.values())
        .transform(Loop::getOutgoingEdges)
        .filter(CAssumeEdge.class)
        .transform(CAssumeEdge::getExpression)
        .transformAndConcat(CFAUtils::getVariableNamesOfExpression)
        .toSet();
  }

  /**
   * Return all variables that are incremented or decremented by a fixed constant
   * inside loops.
   * The variable names are scoped in the same way as {@link VariableClassification}
   * does.
   */
  public Set<String> getLoopIncDecVariables() {
    if (loopIncDecVariables == null) {
      loopIncDecVariables = collectLoopIncDecVariables();
    }
    return loopIncDecVariables;
  }

  private ImmutableSet<String> collectLoopIncDecVariables() {
    ImmutableSet.Builder<String> result = ImmutableSet.builder();
    for (Loop l : loops.values()) {
     // Get all variables that are incremented or decrement by literal values
      for (CFAEdge e : l.getInnerLoopEdges()) {
        String var = obtainIncDecVariable(e);
        if (var != null) {
          result.add(var);
        }
      }
    }
    return result.build();
  }

  /**
   * This method obtains a variable referenced in this edge that are incremented or decremented by a constant
   * (if there is one such variable).
   * @param e the edge from which to obtain variables
   * @return a variable name or null
   */
  @Nullable
  private static String obtainIncDecVariable(CFAEdge e) {
    if (e instanceof CStatementEdge) {
      CStatementEdge stmtEdge = (CStatementEdge) e;
      if (stmtEdge.getStatement() instanceof CAssignment) {
        CAssignment assign = (CAssignment) stmtEdge.getStatement();

        if (assign.getLeftHandSide() instanceof CIdExpression) {
          CIdExpression assignementToId = (CIdExpression) assign.getLeftHandSide();
          String assignToVar = assignementToId.getDeclaration().getQualifiedName();

          if (assign.getRightHandSide() instanceof CBinaryExpression) {
            CBinaryExpression binExpr = (CBinaryExpression) assign.getRightHandSide();
            BinaryOperator op = binExpr.getOperator();

            if (op == BinaryOperator.PLUS || op == BinaryOperator.MINUS) {

              if (binExpr.getOperand1() instanceof CLiteralExpression
                  || binExpr.getOperand2() instanceof CLiteralExpression) {
                CIdExpression operandId = null;

                if (binExpr.getOperand1() instanceof CIdExpression) {
                  operandId = (CIdExpression) binExpr.getOperand1();
                }
                if (binExpr.getOperand2() instanceof CIdExpression) {
                  operandId = (CIdExpression) binExpr.getOperand2();
                }

                if (operandId != null) {
                  String operandVar = operandId.getDeclaration().getQualifiedName();
                  if (assignToVar.equals(operandVar)) {
                    return assignToVar;
                  }
                }
              }
            }
          }
        }
      }
    }
    return null;
  }


  /**
   * Gets the loop structure of a control flow automaton with one single loop.
   * Do not call this method outside of the frontend,
   * use {@link org.sosy_lab.cpachecker.cfa.CFA#getLoopStructure()} instead.
   * @param pSingleLoopHead the loop head of the single loop.
   * @return the loop structure of the control flow automaton.
   */
  public static LoopStructure getLoopStructureForSingleLoop(CFANode pSingleLoopHead) {
    Predicate<CFAEdge> noFunctionReturnEdge = not(edgeHasType(FunctionReturnEdge));

    // First, find all nodes reachable via the loop head
    Deque<CFANode> waitlist = new ArrayDeque<>();
    Set<CFANode> reachableSuccessors = new HashSet<>();
    Set<CFANode> visited = new HashSet<>();
    waitlist.push(pSingleLoopHead);
    boolean firstIteration = true;
    while (!waitlist.isEmpty()) {
      CFANode current = waitlist.pop();
      for (CFAEdge leavingEdge : CFAUtils.allLeavingEdges(current).filter(noFunctionReturnEdge)) {
        CFANode successor = leavingEdge.getSuccessor();
        if (visited.add(successor)) {
          waitlist.push(successor);
        }
      }
      if (firstIteration) {
        firstIteration = false;
      } else {
        reachableSuccessors.add(current);
      }
    }

    // If the loop head cannot reach itself, there is no loop
    if (!reachableSuccessors.contains(pSingleLoopHead)) {
      return new LoopStructure(ImmutableMultimap.<String, Loop>of());
    }

    /*
     * Now, Find all loop nodes by checking which of the nodes reachable via
     * the loop head, the loop head itself is reachable from.
     */
    visited.clear();
    waitlist.offer(pSingleLoopHead);
    Set<CFANode> loopNodes = new HashSet<>();
    while (!waitlist.isEmpty()) {
      CFANode current = waitlist.poll();
      if (reachableSuccessors.contains(current)) {
        loopNodes.add(current);
        for (CFAEdge enteringEdge : CFAUtils.allEnteringEdges(current)) {
          CFANode predecessor = enteringEdge.getPredecessor();
          if (visited.add(predecessor)) {
            waitlist.offer(predecessor);
          }
        }
      }
    }
    String loopFunction = pSingleLoopHead.getFunctionName();
    // A size of one means only the loop head is contained
    if (loopNodes.isEmpty()
        || (loopNodes.size() == 1 && !pSingleLoopHead.hasEdgeTo(pSingleLoopHead))) {
      return new LoopStructure(ImmutableMultimap.<String, Loop>of());
    }

    return new LoopStructure(ImmutableMultimap.of(loopFunction, new Loop(pSingleLoopHead, loopNodes)));
  }


  // -------- Code related to retrieving LoopStructure information in gneral case --------

  // wrapper class for Set<CFANode> because Java arrays don't like generics
  private static class Edge {
    private final Set<CFANode> nodes = Sets.newHashSetWithExpectedSize(1);

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

  /**
   * Build loop-structure information for a CFA.
   * Do not call this method outside of the frontend,
   * use {@link org.sosy_lab.cpachecker.cfa.CFA#getLoopStructure()} instead.
   * @throws ParserException If the structure of the CFA is too complex for determining loops.
   */
  public static LoopStructure getLoopStructure(MutableCFA cfa) throws ParserException {
    ImmutableMultimap.Builder<String, Loop> loops = ImmutableMultimap.builder();
    for (String functionName : cfa.getAllFunctionNames()) {
      SortedSet<CFANode> nodes = cfa.getFunctionNodes(functionName);
      loops.putAll(functionName, findLoops(nodes, cfa.getLanguage()));
    }
    return new LoopStructure(loops.build());
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
   */
  private static Collection<Loop> findLoops(SortedSet<CFANode> nodes, Language language) throws ParserException {

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
    final Function<CFANode, Integer> arrayIndexForNode = CFANode::getReversePostorderId;
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
    do {
      toRemove.clear();
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
    } while (!toRemove.isEmpty());

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
}
