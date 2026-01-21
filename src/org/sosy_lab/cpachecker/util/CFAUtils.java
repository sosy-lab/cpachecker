// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.common.collect.Collections3.elementAndList;
import static org.sosy_lab.common.collect.Collections3.listAndElement;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.graph.Traverser;
import com.google.errorprone.annotations.DoNotCall;
import com.google.errorprone.annotations.InlineMe;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import org.sosy_lab.common.Optionals;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.AAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.ACastExpression;
import org.sosy_lab.cpachecker.cfa.ast.ACharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.AStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AbstractSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayRangeDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CEnumerator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayCreationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayInitializer;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayLengthExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JAstNode;
import org.sosy_lab.cpachecker.cfa.ast.java.JBooleanLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JClassInstanceCreation;
import org.sosy_lab.cpachecker.cfa.ast.java.JClassLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JEnumConstantExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JNullLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JRunTimeTypeEqualsType;
import org.sosy_lab.cpachecker.cfa.ast.java.JThisExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableRunTimeType;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBooleanConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTermTuple;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIntegerConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibRealConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSymbolApplicationTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTermAssignmentCfaStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibVariableDeclarationTuple;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibAtTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibCheckTrueTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibEnsuresTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibInvariantTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibRequiresTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibSymbolApplicationRelationalTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.ast.ASTElement;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.ast.IfElement;
import org.sosy_lab.cpachecker.util.ast.IterationElement;

public class CFAUtils {

  public static final Pattern CFA_NODE_NAME_PATTERN = Pattern.compile("N([0-9][0-9]*)");

  /**
   * Return an {@link Iterable} that contains all entering edges of a given CFANode, including the
   * summary edge if the node has one.
   *
   * <p>WARNING: Summary edges are included, so the returned {@link FluentIterable} may contain
   * parallel edges (i.e., multiple directed edges from some node {@code u} to some node {@code v}).
   * These edges are equal, so a set would only contain one of the parallel edges.
   */
  @InlineMe(replacement = "node.getAllEnteringEdges()")
  @Deprecated
  public static FluentIterable<CFAEdge> allEnteringEdges(final CFANode node) {
    return node.getAllEnteringEdges();
  }

  /**
   * Return an {@link Iterable} that contains the predecessor and successor of a given {@link
   * CFAEdge}
   *
   * @param pEdge the edge for which the predecessor and successor should be returned
   * @return an {@link Iterable} containing the predecessor and successor of the given edge
   */
  public static ImmutableList<CFANode> nodes(CFAEdge pEdge) {
    return ImmutableList.of(pEdge.getPredecessor(), pEdge.getSuccessor());
  }

  /**
   * Return an {@link Iterable} that contains the entering edges of a given CFANode, excluding the
   * summary edge.
   */
  @InlineMe(replacement = "node.getEnteringEdges()")
  @Deprecated
  public static FluentIterable<CFAEdge> enteringEdges(final CFANode node) {
    return node.getEnteringEdges();
  }

  /**
   * Return an {@link Iterable} that contains all leaving edges of a given CFANode, including the
   * summary edge if the node as one.
   *
   * <p>WARNING: Summary edges are included, so the returned {@link FluentIterable} may contain
   * parallel edges (i.e., multiple directed edges from some node {@code u} to some node {@code v}).
   * These edges are equal, so a set would only contain one of the parallel edges.
   */
  @InlineMe(replacement = "node.getAllLeavingEdges()")
  @Deprecated
  public static FluentIterable<CFAEdge> allLeavingEdges(final CFANode node) {
    return node.getAllLeavingEdges();
  }

  /**
   * Returns all edges which are reachable in the forward direction without any branchings from the
   * current edge.
   *
   * @param edge The edge where to start the reachability analysis from
   * @return All the edges which can be reached from the current edge in the forward direction
   */
  public static Set<CFAEdge> forwardLinearReach(CFAEdge edge) {
    CFAEdge current = edge;
    ImmutableSet.Builder<CFAEdge> builder = ImmutableSet.builder();
    while (current.getSuccessor().getNumLeavingEdges() == 1) {
      current = current.getSuccessor().getLeavingEdge(0);
      builder.add(current);
    }
    return builder.build();
  }

  /**
   * Return an {@link Iterable} that contains the leaving edges of a given CFANode, excluding the
   * summary edge.
   */
  @InlineMe(replacement = "node.getLeavingEdges()")
  @Deprecated
  public static FluentIterable<CFAEdge> leavingEdges(final CFANode node) {
    return node.getLeavingEdges();
  }

  /**
   * Returns a {@link FluentIterable} that contains all edges of the specified CFA, including all
   * summary edges.
   *
   * <p>WARNING: Summary edges are included, so the returned {@link FluentIterable} may contain
   * parallel edges (i.e., multiple directed edges from some node {@code u} to some node {@code v}).
   * These edges are equal, so a set would only contain one of the parallel edges.
   *
   * @return a {@link FluentIterable} that contains all edges of the specified CFA, including all
   *     summary edges.
   * @throws NullPointerException if {@code pCfa == null}
   */
  public static FluentIterable<CFAEdge> allEdges(CFA pCfa) {
    return FluentIterable.from(pCfa.nodes()).transformAndConcat(CFAUtils::allLeavingEdges);
  }

  /**
   * Return an {@link Iterable} that contains the predecessor nodes of a given CFANode, excluding
   * the one reachable via the summary edge (if there is one).
   */
  public static FluentIterable<CFANode> predecessorsOf(final CFANode node) {
    return node.getEnteringEdges().transform(CFAEdge::getPredecessor);
  }

  /**
   * Return an {@link Iterable} that contains all the predecessor nodes of a given CFANode,
   * including the one reachable via the summary edge (if there is one).
   */
  public static FluentIterable<CFANode> allPredecessorsOf(final CFANode node) {
    return node.getAllEnteringEdges().transform(CFAEdge::getPredecessor);
  }

  /**
   * Return an {@link Iterable} that contains the successor nodes of a given CFANode, excluding the
   * one reachable via the summary edge (if there is one).
   */
  public static FluentIterable<CFANode> successorsOf(final CFANode node) {
    return node.getLeavingEdges().transform(CFAEdge::getSuccessor);
  }

  /**
   * Return an {@link Iterable} that contains all the successor nodes of a given CFANode, including
   * the one reachable via the summary edge (if there is one).
   */
  public static FluentIterable<CFANode> allSuccessorsOf(final CFANode node) {
    return node.getAllLeavingEdges().transform(CFAEdge::getSuccessor);
  }

  @Deprecated // entry nodes do not have summary edges
  @InlineMe(
      replacement = "CFAUtils.predecessorsOf(node)",
      imports = "org.sosy_lab.cpachecker.util.CFAUtils")
  public static FluentIterable<CFANode> allPredecessorsOf(final FunctionEntryNode node) {
    return predecessorsOf(node);
  }

  @Deprecated // exit nodes do not have summary edges
  @InlineMe(
      replacement = "CFAUtils.predecessorsOf(node)",
      imports = "org.sosy_lab.cpachecker.util.CFAUtils")
  public static FluentIterable<CFANode> allPredecessorsOf(final FunctionExitNode node) {
    return predecessorsOf(node);
  }

  @Deprecated // entry nodes do not have summary edges
  @InlineMe(
      replacement = "CFAUtils.successorsOf(node)",
      imports = "org.sosy_lab.cpachecker.util.CFAUtils")
  public static FluentIterable<CFANode> allSuccessorsOf(final FunctionEntryNode node) {
    return successorsOf(node);
  }

  @Deprecated // exit nodes do not have summary edges
  @InlineMe(
      replacement = "CFAUtils.successorsOf(node)",
      imports = "org.sosy_lab.cpachecker.util.CFAUtils")
  public static FluentIterable<CFANode> allSuccessorsOf(final FunctionExitNode node) {
    return successorsOf(node);
  }

  @Deprecated // termination nodes do not have successors
  @DoNotCall
  public static FluentIterable<CFAEdge> successorsOf(
      @SuppressWarnings("unused") final CFATerminationNode node) {
    throw new AssertionError("useless method");
  }

  @Deprecated // termination nodes do not have successors
  @DoNotCall
  public static FluentIterable<CFAEdge> allSuccessorsOf(
      @SuppressWarnings("unused") final CFATerminationNode node) {
    throw new AssertionError("useless method");
  }

  /** Returns the other AssumeEdge (with the negated condition) of a given AssumeEdge. */
  public static AssumeEdge getComplimentaryAssumeEdge(AssumeEdge edge) {
    return Iterables.getOnlyElement(
        edge.getPredecessor()
            .getLeavingEdges()
            .filter(e -> !e.equals(edge))
            .filter(AssumeEdge.class));
  }

  /**
   * Checks if a path from the source to the target exists, using the given function to obtain the
   * edges leaving a node.
   *
   * @param pSource the search start node.
   * @param pTarget the target.
   * @param pGetLeavingEdges the function used to obtain leaving edges and thus the successors of a
   *     node.
   * @param pShutdownNotifier the shutdown notifier to be checked.
   * @return {@code true} if a path from the source to the target exists, {@code false} otherwise.
   * @throws InterruptedException if a shutdown has been requested by the given shutdown notifier.
   */
  public static boolean existsPath(
      CFANode pSource,
      CFANode pTarget,
      Function<CFANode, Iterable<CFAEdge>> pGetLeavingEdges,
      ShutdownNotifier pShutdownNotifier)
      throws InterruptedException {
    Set<CFANode> visited = new HashSet<>();
    Queue<CFANode> waitlist = new ArrayDeque<>();
    waitlist.offer(pSource);
    while (!waitlist.isEmpty()) {
      pShutdownNotifier.shutdownIfNecessary();
      CFANode current = waitlist.poll();
      if (current.equals(pTarget)) {
        return true;
      }
      if (visited.add(current)) {
        for (CFAEdge leavingEdge : pGetLeavingEdges.apply(current)) {
          CFANode succ = leavingEdge.getSuccessor();
          waitlist.offer(succ);
        }
      }
    }
    return false;
  }

  public static Collection<CFANode> getEndlessLoopHeads(final LoopStructure pLoopStructure) {
    ImmutableCollection<Loop> loops = pLoopStructure.getAllLoops();
    Set<CFANode> loopHeads = new HashSet<>();

    for (Loop l : loops) {
      if (l.getOutgoingEdges().isEmpty()
          || l.getOutgoingEdges().stream()
              .allMatch(x -> x.getSuccessor() instanceof CFATerminationNode)) {
        // one loopHead per loop should be enough for finding all locations
        loopHeads.addAll(l.getLoopHeads());
      }
    }
    return loopHeads;
  }

  public static Collection<CFANode> getProgramSinks(
      final LoopStructure pLoopStructure, final FunctionEntryNode pCfaEntryNode) {
    Set<CFANode> sinks = new HashSet<>();
    pCfaEntryNode.getExitNode().ifPresent(sinks::add);

    sinks.addAll(getEndlessLoopHeads(pLoopStructure));
    return sinks;
  }

  public static Map<Integer, CFANode> getMappingFromNodeIDsToCFANodes(CFA pCfa) {
    return Maps.uniqueIndex(pCfa.nodes(), CFANode::getNodeNumber);
  }

  /**
   * This method returns true if the set of nodes is connected, i.e., there is a path between every
   * pair of nodes in the set.
   *
   * <p>Currently, this is quite inefficient, so use with caution and only for small sets of nodes.
   *
   * @param pCfaNodes the set of nodes
   * @return true if the set of nodes is connected i.e. there is a path between every pair of nodes
   *     in the set
   */
  public static boolean isConnected(Set<CFANode> pCfaNodes) {
    if (pCfaNodes.isEmpty()) {
      return true;
    }

    Multimap<Integer, CFANode> idsToNode = HashMultimap.create();
    Integer currentId = 0;
    for (CFANode node : pCfaNodes) {
      Multimap<Integer, CFANode> newIdsToNode = HashMultimap.create(idsToNode);
      newIdsToNode.put(currentId, node);
      for (CFANode connectedNode :
          FluentIterable.concat(CFAUtils.allPredecessorsOf(node), CFAUtils.allSuccessorsOf(node))) {
        for (Integer id : idsToNode.keySet()) {
          if (newIdsToNode.get(id).contains(connectedNode)) {
            newIdsToNode.putAll(currentId, newIdsToNode.removeAll(id));
          }
        }
      }
      idsToNode = newIdsToNode;
      currentId++;
    }

    return idsToNode.keySet().size() == 1;
  }

  /**
   * Computes all s-t bridges in a CFA between a source and target node.
   *
   * <p>An s-t bridge is an edge that must be traversed by every path from the source node to the
   * target node. This implementation is based on the algorithm described in "A simplified algorithm
   * computing all bridges and articulation points" by Cairo et al. (2021).
   *
   * <p>The algorithm works by:
   *
   * <ol>
   *   <li>Finding any path P from source to target
   *   <li>Iteratively identifying bridge edges by performing BFS on G \ P (graph with P edges
   *       reversed)
   *   <li>Computing connected components between bridges
   * </ol>
   *
   * @param pSource the source node
   * @param pTarget the target node
   * @param pNodes all nodes to consider in the search (usually all reachable nodes)
   * @return a set of edges that are s-t bridges between source and target
   */
  public static ImmutableSet<CFAEdge> computeSTBridges(
      CFANode pSource, CFANode pTarget, Set<CFANode> pNodes) {

    // Find an arbitrary path from source to target
    List<CFAEdge> path = findPath(pSource, pTarget, pNodes);
    if (path.isEmpty()) {
      // No path exists, no bridges
      return ImmutableSet.of();
    }

    // Initialize component assignment for each node (0 = unvisited)
    Map<CFANode, Integer> componentAssignment = new HashMap<>();
    for (CFANode node : pNodes) {
      componentAssignment.put(node, 0);
    }

    Set<CFAEdge> pathEdges = new HashSet<>(path);
    List<CFAEdge> bridges = new ArrayList<>();
    Queue<CFANode> nodeQueue = new ArrayDeque<>();
    int currentComponent = 1;

    // Main loop: continue until target is assigned a component
    while (componentAssignment.get(pTarget) == 0) {
      if (currentComponent == 1) {
        // Initialize with source node
        nodeQueue.add(pSource);
        componentAssignment.put(pSource, currentComponent);
      } else {
        // Find the next bridge edge on the path
        for (int i = path.size() - 1; i >= 0; i--) {
          CFAEdge edge = path.get(i);
          if (componentAssignment.get(edge.getPredecessor()) != 0
              && componentAssignment.get(edge.getSuccessor()) == 0) {
            bridges.add(edge);
            nodeQueue.add(edge.getSuccessor());
            componentAssignment.put(edge.getSuccessor(), currentComponent);
            break;
          }
        }
      }

      // BFS to assign all reachable nodes to current component
      // (using flipped path edges)
      while (!nodeQueue.isEmpty()) {
        CFANode current = nodeQueue.poll();
        for (CFANode successor : getSuccessorsWithFlippedPath(current, pathEdges, pNodes)) {
          if (componentAssignment.get(successor) == 0) {
            nodeQueue.add(successor);
            componentAssignment.put(successor, currentComponent);
          }
        }
      }

      currentComponent++;
    }

    return ImmutableSet.copyOf(bridges);
  }

  /**
   * Finds an arbitrary path from source to target using BFS.
   *
   * @param pSource the source node
   * @param pTarget the target node
   * @param pNodes the set of nodes to consider
   * @return a list of edges forming a path from source to target, or empty list if no path exists
   */
  private static ImmutableList<CFAEdge> findPath(
      CFANode pSource, CFANode pTarget, Set<CFANode> pNodes) {
    Map<CFANode, CFAEdge> parentEdge = new HashMap<>();
    Set<CFANode> visited = new HashSet<>();
    Queue<CFANode> queue = new ArrayDeque<>();

    queue.add(pSource);
    visited.add(pSource);

    while (!queue.isEmpty()) {
      CFANode current = queue.poll();

      if (current.equals(pTarget)) {
        // Reconstruct path
        ImmutableList.Builder<CFAEdge> path = ImmutableList.builder();
        CFANode node = pTarget;
        while (parentEdge.containsKey(node)) {
          CFAEdge edge = parentEdge.get(node);
          path.add(edge);
          node = edge.getPredecessor();
        }
        return path.build().reverse();
      }

      for (CFAEdge edge : current.getLeavingEdges()) {
        CFANode successor = edge.getSuccessor();
        if (pNodes.contains(successor) && visited.add(successor)) {
          parentEdge.put(successor, edge);
          queue.add(successor);
        }
      }
    }

    return ImmutableList.of();
  }

  /**
   * Gets successors of a node with path edges flipped (reversed).
   *
   * <p>For normal edges not in the path, return normal successors. For edges in the path, treat
   * them as reversed.
   *
   * @param pNode the current node
   * @param pPathEdges edges that should be treated as flipped
   * @param pNodes the set of valid nodes
   * @return successors considering flipped path edges
   */
  private static Iterable<CFANode> getSuccessorsWithFlippedPath(
      CFANode pNode, Set<CFAEdge> pPathEdges, Set<CFANode> pNodes) {
    ImmutableSet.Builder<CFANode> successors = ImmutableSet.builder();

    // Normal successors (edges not in path)
    for (CFAEdge edge : pNode.getLeavingEdges()) {
      if (!pPathEdges.contains(edge) && pNodes.contains(edge.getSuccessor())) {
        successors.add(edge.getSuccessor());
      }
    }

    // Flipped successors (edges in path where current node is successor)
    for (CFAEdge edge : pPathEdges) {
      if (edge.getSuccessor().equals(pNode) && pNodes.contains(edge.getPredecessor())) {
        successors.add(edge.getPredecessor());
      }
    }

    return successors.build();
  }

  /**
   * Result class for Tarjan's SCC algorithm containing strongly connected components and utilities
   * for analyzing inter-SCC edges.
   */
  public static final class StronglyConnectedComponents {
    private final ImmutableList<ImmutableSet<CFANode>> components;
    private final ImmutableMap<CFANode, Integer> nodeToComponentId;
    private final ImmutableSet<CFAEdge> interComponentEdges;

    private StronglyConnectedComponents(
        List<Set<CFANode>> pComponents,
        Map<CFANode, Integer> pNodeToComponentId,
        Set<CFAEdge> pInterComponentEdges) {

      components =
          pComponents.stream().map(ImmutableSet::copyOf).collect(ImmutableList.toImmutableList());
      nodeToComponentId = ImmutableMap.copyOf(pNodeToComponentId);
      interComponentEdges = ImmutableSet.copyOf(pInterComponentEdges);
    }

    /**
     * Returns all strongly connected components found in the graph.
     *
     * @return list of SCCs, where each SCC is a set of nodes
     */
    public ImmutableList<ImmutableSet<CFANode>> getComponents() {
      return components;
    }

    /**
     * Returns the number of strongly connected components.
     *
     * @return number of SCCs
     */
    public int getNumberOfComponents() {
      return components.size();
    }

    /**
     * Returns the component ID for a given node.
     *
     * @param pNode the node to query
     * @return component ID (0-indexed), or -1 if node not found
     */
    public int getComponentId(CFANode pNode) {
      return nodeToComponentId.getOrDefault(pNode, -1);
    }

    /**
     * Checks if two nodes are in the same strongly connected component.
     *
     * @param pNode1 first node
     * @param pNode2 second node
     * @return true if both nodes are in the same SCC
     */
    public boolean areInSameComponent(CFANode pNode1, CFANode pNode2) {
      int id1 = getComponentId(pNode1);
      int id2 = getComponentId(pNode2);
      return id1 >= 0 && id1 == id2;
    }

    /**
     * Returns all edges that cross between different strongly connected components. These are edges
     * (u, v) where u and v belong to different SCCs.
     *
     * @return set of inter-component edges
     */
    public ImmutableSet<CFAEdge> getInterComponentEdges() {
      return interComponentEdges;
    }

    /**
     * Returns all edges within strongly connected components (i.e., edges that don't cross
     * component boundaries).
     *
     * @param pAllEdges all edges in the graph
     * @return set of intra-component edges
     */
    public ImmutableSet<CFAEdge> getIntraComponentEdges(Collection<CFAEdge> pAllEdges) {
      return pAllEdges.stream()
          .filter(edge -> !interComponentEdges.contains(edge))
          .collect(ImmutableSet.toImmutableSet());
    }

    /**
     * Checks if an edge crosses between different strongly connected components.
     *
     * @param pEdge the edge to check
     * @return true if the edge connects nodes in different SCCs
     */
    public boolean isInterComponentEdge(CFAEdge pEdge) {
      return interComponentEdges.contains(pEdge);
    }

    /**
     * Returns the component containing the given node.
     *
     * @param pNode the node to query
     * @return the SCC containing the node, or empty if not found
     */
    public Optional<ImmutableSet<CFANode>> getComponentContaining(CFANode pNode) {
      int id = getComponentId(pNode);
      if (id >= 0 && id < components.size()) {
        return Optional.of(components.get(id));
      }
      return Optional.empty();
    }

    @Override
    public String toString() {
      return String.format(
          "SCCs[components=%d, interComponentEdges=%d]",
          components.size(), interComponentEdges.size());
    }
  }

  /**
   * Computes strongly connected components using Tarjan's algorithm.
   *
   * <p>Tarjan's algorithm is a DFS-based algorithm that finds all strongly connected components in
   * O(V + E) time. An SCC is a maximal set of nodes where every node is reachable from every other
   * node in the set.
   *
   * <p>This implementation:
   *
   * <ul>
   *   <li>Finds all SCCs in the subgraph defined by pNodes
   *   <li>Assigns a unique ID to each SCC
   *   <li>Identifies edges that cross between different SCCs
   * </ul>
   *
   * <p>Use cases:
   *
   * <ul>
   *   <li>Loop detection (SCCs with size > 1 contain cycles)
   *   <li>Finding back edges (inter-component edges going to earlier components)
   *   <li>Graph condensation (creating a DAG of SCCs)
   *   <li>Detecting irreducible control flow
   * </ul>
   *
   * @param pNodes all nodes to consider in the analysis
   * @return StronglyConnectedComponents containing SCCs and inter-component edges
   */
  public static StronglyConnectedComponents computeStronglyConnectedComponents(
      Set<CFANode> pNodes) {

    TarjanSCCFinder finder = new TarjanSCCFinder(pNodes);
    return finder.findSCCs();
  }

  public static ImmutableList<Integer> getLineNumbersInsideSCCs(CFA pCFA) {
    StronglyConnectedComponents components = computeStronglyConnectedComponents(pCFA.nodes());
    return ImmutableList.sortedCopyOf(
        FluentIterable.from(components.getIntraComponentEdges(pCFA.edges()))
            .transformAndConcat(
                e ->
                    ImmutableList.copyOf(
                        IntStream.range(
                                e.getFileLocation().getStartingLineNumber(),
                                e.getFileLocation().getEndingLineNumber() + 1)
                            .iterator()))
            .toSet());
  }

  /** Helper class implementing Tarjan's SCC algorithm. */
  private static class TarjanSCCFinder {
    private final Set<CFANode> nodes;
    private final Map<CFANode, Integer> indices = new HashMap<>();
    private final Map<CFANode, Integer> lowLinks = new HashMap<>();
    private final Set<CFANode> onStack = new HashSet<>();
    private final Deque<CFANode> stack = new ArrayDeque<>();
    private final List<Set<CFANode>> components = new ArrayList<>();
    private int index = 0;

    TarjanSCCFinder(Set<CFANode> pNodes) {
      nodes = pNodes;
    }

    StronglyConnectedComponents findSCCs() {
      // Run Tarjan's algorithm on all unvisited nodes
      for (CFANode node : nodes) {
        if (!indices.containsKey(node)) {
          strongConnect(node);
        }
      }

      // Build component ID mapping
      Map<CFANode, Integer> nodeToComponentId = new HashMap<>();
      for (int i = 0; i < components.size(); i++) {
        for (CFANode node : components.get(i)) {
          nodeToComponentId.put(node, i);
        }
      }

      // Find inter-component edges
      Set<CFAEdge> interComponentEdges = new HashSet<>();
      for (CFANode node : nodes) {
        int sourceComponentId = nodeToComponentId.get(node);
        for (CFAEdge edge : node.getLeavingEdges()) {
          CFANode successor = edge.getSuccessor();
          if (nodes.contains(successor)) {
            int targetComponentId = nodeToComponentId.get(successor);
            if (sourceComponentId != targetComponentId) {
              interComponentEdges.add(edge);
            }
          }
        }
      }

      return new StronglyConnectedComponents(components, nodeToComponentId, interComponentEdges);
    }

    /** Tarjan's algorithm core recursive function. */
    private void strongConnect(CFANode v) {
      // Set the depth index for v
      indices.put(v, index);
      lowLinks.put(v, index);
      index++;
      stack.push(v);
      onStack.add(v);

      // Consider successors of v
      for (CFAEdge edge : v.getLeavingEdges()) {
        CFANode w = edge.getSuccessor();

        if (!nodes.contains(w)) {
          // Skip nodes not in our analysis set
          continue;
        }

        if (!indices.containsKey(w)) {
          // Successor w has not yet been visited; recurse on it
          strongConnect(w);
          lowLinks.put(v, Math.min(lowLinks.get(v), lowLinks.get(w)));
        } else if (onStack.contains(w)) {
          // Successor w is in stack and hence in the current SCC
          lowLinks.put(v, Math.min(lowLinks.get(v), indices.get(w)));
        }
      }

      // If v is a root node, pop the stack and create an SCC
      if (lowLinks.get(v).equals(indices.get(v))) {
        Set<CFANode> component = new HashSet<>();
        CFANode w;
        do {
          w = stack.pop();
          onStack.remove(w);
          component.add(w);
        } while (!w.equals(v));

        components.add(component);
      }
    }
  }

  /**
   * This method returns the location in the "original program (i.e., before simplifications done by
   * CPAchecker) of the closest full expression as defined in section (§6.8 (4) of the C11 standard)
   * encompassing the expression in the given edge. This is only well-defined for edges in C
   * programs. The closest full expression is defined as one of the following:
   *
   * <ul>
   *   <li>1. when the edge represents a statement, it is the full expression contained in the
   *       statement, of which only one exists.
   *   <li>2. when the edge contains an expression, we look for the full expression that contains
   *       the expression inside the edge in the original source code. This is where the
   *       pCfaAstRelation comes into play. For example for example if `x > 0` is the expression of
   *       the edge and is part of the condition in `while (y != 0 && x > 0)` and therefore not a
   *       full expression we search for the full expression `y != 0 && x > 0` which contains it.
   * </ul>
   *
   * In summary, we either search for the full expression contained in the edge or for the full
   * expression containing the expression of the edge.
   *
   * <p>There are many limitations for this functions, so please check inside the test {@link
   * CFAUtilsTest#testFullExpression} for more details on what is supported and what is not.
   *
   * @param pEdge The edge for which the closest full expression should be found
   * @param pAstCfaRelation The relation between the AST and the CFA
   * @return The location of the closest full expression either encompassing the expression or
   *     contained in the statement represented by the given edge
   */
  public static Optional<FileLocation> getClosestFullExpression(
      CCfaEdge pEdge, AstCfaRelation pAstCfaRelation) {

    if (pEdge instanceof AssumeEdge assumeEdge) {
      // Find out the full expression encompassing the expression
      Optional<IfElement> optionalIfElement =
          pAstCfaRelation.getIfStructureForConditionEdge(assumeEdge);
      Optional<IterationElement> optionalIterationElement =
          pAstCfaRelation.getTightestIterationStructureForNode(assumeEdge.getPredecessor());
      if (optionalIfElement.isPresent()) {
        return Optional.of(optionalIfElement.orElseThrow().getConditionElement().location());
      } else if (optionalIterationElement.isPresent()) {
        Optional<ASTElement> optionalControlExpression =
            optionalIterationElement.orElseThrow().getControllingExpression();
        Optional<ASTElement> optionalInitClause =
            optionalIterationElement.orElseThrow().getInitClause();
        Optional<ASTElement> optionalIterationExpression =
            optionalIterationElement.orElseThrow().getIterationExpression();
        FileLocation location;
        if (optionalControlExpression.isPresent()
            && optionalControlExpression.orElseThrow().edges().contains(pEdge)) {
          location = optionalControlExpression.orElseThrow().location();
        } else if (optionalInitClause.isPresent()
            && optionalInitClause.orElseThrow().edges().contains(pEdge)) {
          location = optionalInitClause.orElseThrow().location();
        } else if (optionalIterationExpression.isPresent()
            && optionalIterationExpression.orElseThrow().edges().contains(pEdge)) {
          location = optionalIterationExpression.orElseThrow().location();
        } else {
          return Optional.empty();
        }
        // This fixes the column end of the location
        return pAstCfaRelation.getNextExpressionLocationBasedOnOffset(location);
      } else {
        // In this case the assume edge stems from another type of statement, like ternary
        // operators. In this case we can take the location of the next possible expression which is
        // contained in or equal to the statement from which the edge was created
        return pAstCfaRelation.getNextExpressionLocationBasedOnOffset(pEdge.getFileLocation());
      }
    }
    // This works, since the edge contains the location of the statement from which the edge was
    // generated. This means that when we take a look at the next possible expression we get the
    // closest full expression to it
    return pAstCfaRelation.getNextExpressionLocationBasedOnOffset(pEdge.getFileLocation());
  }

  /**
   * This Visitor searches for backwards edges in the CFA, if some backwards edges were found can be
   * obtained by calling the method hasBackwardsEdges()
   */
  private static class FindBackwardsEdgesVisitor extends DefaultCFAVisitor {

    private boolean hasBackwardsEdges = false;

    @Override
    public TraversalProcess visitNode(CFANode pNode) {

      if (pNode.getNumLeavingEdges() == 0) {
        return TraversalProcess.CONTINUE;
      } else if (pNode.getNumLeavingEdges() == 1
          && pNode.getLeavingEdge(0).getSuccessor().getReversePostorderId()
              >= pNode.getReversePostorderId()) {

        hasBackwardsEdges = true;
        return TraversalProcess.ABORT;
      } else if (pNode.getNumLeavingEdges() == 2
          && (pNode.getLeavingEdge(0).getSuccessor().getReversePostorderId()
                  >= pNode.getReversePostorderId()
              || pNode.getLeavingEdge(1).getSuccessor().getReversePostorderId()
                  >= pNode.getReversePostorderId())) {
        hasBackwardsEdges = true;
        return TraversalProcess.ABORT;
      } else if (pNode.getNumLeavingEdges() > 2) {
        throw new AssertionError("forgotten case in traversing CFA with more than 2 leaving edges");
      } else {
        return TraversalProcess.CONTINUE;
      }
    }

    boolean hasBackwardsEdges() {
      return hasBackwardsEdges;
    }
  }

  /**
   * Searches for backwards edges from a given starting node
   *
   * @param rootNode The node where the search is started
   * @return indicates if a backwards edge was found
   */
  static boolean hasBackWardsEdges(CFANode rootNode) {
    FindBackwardsEdgesVisitor visitor = new FindBackwardsEdgesVisitor();

    CFATraversal.dfs().ignoreSummaryEdges().traverseOnce(rootNode, visitor);

    return visitor.hasBackwardsEdges();
  }

  /**
   * This method allows to select from a set of variables all local variables from a given function.
   * This requires that the given set contains the qualified names of each variable as returned by
   * {@link AbstractSimpleDeclaration#getQualifiedName()}.
   *
   * @param variables Set of qualified names of variables.
   * @param function A function name.
   * @return A subset of "variables".
   */
  public static NavigableSet<String> filterVariablesOfFunction(
      NavigableSet<String> variables, String function) {
    // TODO: Currently, the format of the qualified name is not defined.
    // In theory, frontends could use different formats.
    // The best would be to eliminate all uses of this method
    // (code should not use Strings, but for example AIdExpressions).
    // For now, we just assume all variables are named as
    // {@link org.sosy_lab.cpachecker.cfa.parser.eclipse.c.FunctionScope#createQualifiedName(String,
    // String)}
    // produces them.
    String prefix = checkNotNull(function) + "::";
    return Collections3.subSetWithPrefix(variables, prefix);
  }

  /**
   * Get all (sub)-paths through the given nodes connected only via blank edges.
   *
   * @param pNode the node to get the blank paths for.
   * @return all (sub)-paths through the given nodes connected only via blank edges.
   */
  public static Iterable<List<CFANode>> getBlankPaths(CFANode pNode) {
    List<List<CFANode>> blankPaths = new ArrayList<>();
    Queue<List<CFANode>> waitlist = new ArrayDeque<>();
    waitlist.offer(ImmutableList.of(pNode));
    while (!waitlist.isEmpty()) {
      List<CFANode> currentPath = waitlist.poll();
      CFANode pathSucc = currentPath.getLast();
      List<BlankEdge> leavingBlankEdges =
          pathSucc.getLeavingEdges().filter(BlankEdge.class).toList();
      if (pathSucc.getNumLeavingEdges() <= 0
          || leavingBlankEdges.size() < pathSucc.getNumLeavingEdges()) {
        blankPaths.add(currentPath);
      } else {
        for (CFAEdge leavingEdge : leavingBlankEdges) {
          CFANode successor = leavingEdge.getSuccessor();
          if (!currentPath.contains(successor)) {
            List<CFANode> newPath = listAndElement(currentPath, successor);
            waitlist.offer(newPath);
          }
        }
      }
    }
    waitlist.addAll(blankPaths);
    blankPaths.clear();
    while (!waitlist.isEmpty()) {
      List<CFANode> currentPath = waitlist.poll();
      CFANode pathPred = currentPath.getFirst();
      List<BlankEdge> enteringBlankEdges =
          pathPred.getEnteringEdges().filter(BlankEdge.class).toList();
      if (pathPred.getNumEnteringEdges() <= 0
          || enteringBlankEdges.size() < pathPred.getNumEnteringEdges()) {
        blankPaths.add(currentPath);
      } else {
        for (CFAEdge enteringEdge : enteringBlankEdges) {
          CFANode predecessor = enteringEdge.getPredecessor();
          if (!currentPath.contains(predecessor)) {
            List<CFANode> newPath = elementAndList(predecessor, currentPath);
            waitlist.offer(newPath);
          }
        }
      }
    }
    return blankPaths;
  }

  /** Sees whether the file locations of two {@link CFANode} are disjoint or not. */
  public static boolean disjointFileLocations(CFANode pNode1, CFANode pNode2) {
    // Using the method getFileLocationsFromCfaEdge produces some weird results
    // when considering the initialized global variables and stuff like that
    Set<FileLocation> allFileLocationFirstNode =
        pNode1
            .getAllEnteringEdges()
            .append(pNode1.getAllLeavingEdges())
            .transform(CFAEdge::getFileLocation)
            .toSet();
    Set<FileLocation> allFileLocationSecondtNode =
        pNode2
            .getAllEnteringEdges()
            .append(pNode2.getAllLeavingEdges())
            .transform(CFAEdge::getFileLocation)
            .toSet();

    for (FileLocation file1 : allFileLocationFirstNode) {
      for (FileLocation file2 : allFileLocationSecondtNode) {
        // Check if the ranges overlap
        if (file1.getStartingLineInOrigin() <= file2.getEndingLineInOrigin()
            && file1.getEndingLineInOrigin() >= file2.getStartingLineInOrigin()) {
          return false;
        }
      }
    }

    return true;
  }

  /** Get all {@link FileLocation} objects that are attached to an edge or its AST nodes. */
  public static ImmutableSet<FileLocation> getFileLocationsFromCfaEdge(CFAEdge pEdge) {
    ImmutableSet<FileLocation> result =
        from(getAstNodesFromCfaEdge(pEdge))
            .transformAndConcat(CFAUtils::traverseRecursively)
            .transform(AAstNode::getFileLocation)
            .append(pEdge.getFileLocation())
            .filter(FileLocation::isRealLocation)
            .toSet();

    if (result.isEmpty() && pEdge.getPredecessor() instanceof FunctionEntryNode functionEntryNode) {

      if (functionEntryNode.getFileLocation().isRealLocation()) {
        return ImmutableSet.of(functionEntryNode.getFileLocation());
      }
    }
    return result;
  }

  public static Iterable<AAstNode> getAstNodesFromCfaEdge(final CFAEdge edge) {
    return switch (edge.getEdgeType()) {
      case CallToReturnEdge -> {
        FunctionSummaryEdge fnSumEdge = (FunctionSummaryEdge) edge;
        yield ImmutableSet.of(fnSumEdge.getExpression());
      }
      default -> Optionals.asSet(edge.getRawAST());
    };
  }

  /**
   * Return all variable names that are referenced in an expression, in pre-order and possibly with
   * duplicates.
   */
  public static FluentIterable<String> getVariableNamesOfExpression(CExpression expr) {
    return getCIdExpressionsOfExpression(expr)
        .transform(id -> id.getDeclaration().getQualifiedName());
  }

  /**
   * Return all {@link CIdExpression}s that appear in an expression, in pre-order and possibly with
   * duplicates.
   */
  public static FluentIterable<CIdExpression> getCIdExpressionsOfExpression(CExpression expr) {
    return traverseRecursively(expr).filter(CIdExpression.class);
  }

  /**
   * Return all {@link AIdExpression}s that appear in an expression, in pre-order and possibly with
   * duplicates.
   */
  public static FluentIterable<AIdExpression> getIdExpressionsOfExpression(AExpression expr) {
    return traverseRecursively(expr).filter(AIdExpression.class);
  }

  /** Get an iterable that recursively lists all AST nodes that occur in an AST (in pre-order). */
  public static FluentIterable<AAstNode> traverseRecursively(AAstNode root) {
    return FluentIterable.from(AST_TRAVERSER.depthFirstPreOrder(root));
  }

  /** Get an iterable that recursively lists all AST nodes that occur in a C AST (in pre-order). */
  @SuppressWarnings(
      "unchecked") // by construction, we only get CAstNodes if we start with a CAstNode
  public static FluentIterable<CAstNode> traverseRecursively(CAstNode root) {
    return (FluentIterable<CAstNode>)
        (FluentIterable<?>) FluentIterable.from(AST_TRAVERSER.depthFirstPreOrder(root));
  }

  /**
   * Get an iterable that recursively lists all AST nodes that occur in a Java AST (in pre-order).
   */
  @SuppressWarnings(
      "unchecked") // by construction, we only get JAstNodes if we start with a jAstNode
  public static FluentIterable<JAstNode> traverseRecursively(JAstNode root) {
    return (FluentIterable<JAstNode>)
        (FluentIterable<?>) FluentIterable.from(AST_TRAVERSER.depthFirstPreOrder(root));
  }

  /**
   * Get an iterable that recursively lists all AST nodes that occur in a CRightHandSide (in
   * pre-order).
   */
  @SuppressWarnings("unchecked") // by construction, we only get CRHS if we start with a CRHS
  public static FluentIterable<CRightHandSide> traverseRecursively(CRightHandSide root) {
    return (FluentIterable<CRightHandSide>)
        (FluentIterable<?>) FluentIterable.from(AST_TRAVERSER.depthFirstPreOrder(root));
  }

  /**
   * Get an iterable that recursively lists all AST nodes that occur in a CExpression (in
   * pre-order).
   */
  @SuppressWarnings("unchecked") // by construction, we only get CExps if we start with a CExp
  public static FluentIterable<CExpression> traverseRecursively(CExpression root) {
    return (FluentIterable<CExpression>)
        (FluentIterable<?>) FluentIterable.from(AST_TRAVERSER.depthFirstPreOrder(root));
  }

  /** Checks whether the given edge has the form VAR = __VERIFIER_nondet_TYPE() */
  public static boolean assignsNondetFunctionCall(CFAEdge pEdge) {
    if (pEdge instanceof CStatementEdge statementEdge) {
      if (statementEdge.getStatement() instanceof CFunctionCallAssignmentStatement statement) {
        CLeftHandSide leftHandSide = statement.getLeftHandSide();
        // We do not want the cases where the variable is assigned to a TMP variable
        if (leftHandSide.toString().contains("__CPAchecker_TMP")) {
          return false;
        }
        CFunctionCallExpression expression = statement.getRightHandSide();
        if (expression.getFunctionNameExpression() instanceof CIdExpression functionName) {
          if (functionName.getName().startsWith("__VERIFIER_nondet_")) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Extracts all {@link CVariableDeclaration} from {@code pCfa} that are global, including
   * duplicates, e.g. {@code int x; int x = 0;}.
   */
  public static ImmutableList<AVariableDeclaration> getGlobalVariableDeclarations(CFA pCfa) {
    ImmutableList.Builder<AVariableDeclaration> rGlobalVariables = ImmutableList.builder();

    CFAEdge currentEdge = Iterables.getOnlyElement(pCfa.getMainFunction().getLeavingEdges());
    // consider only if currentEdge is declaration or blank, since all global variables
    // declarations are before any actual statement
    while (currentEdge instanceof ADeclarationEdge || currentEdge instanceof BlankEdge) {
      // if declaration edge, check for global CVariableDeclarations
      if (currentEdge instanceof ADeclarationEdge declarationEdge) {
        ADeclaration declaration = declarationEdge.getDeclaration();
        if (declaration.isGlobal()) {
          if (declaration instanceof AVariableDeclaration variableDeclaration) {
            rGlobalVariables.add(variableDeclaration);
          }
        }
      }
      if (currentEdge.getSuccessor().getLeavingEdges().size() > 1) {
        break;
      }
      currentEdge = Iterables.getOnlyElement(currentEdge.getSuccessor().getLeavingEdges());
    }
    return rGlobalVariables.build();
  }

  /**
   * Get an iterable that recursively lists AST nodes that occur in a {@link ALeftHandSide} (in
   * pre-order).
   *
   * <p>Note: contrary to {@link #traverseRecursively(AAstNode)}, this iterable does not contain the
   * subscript expressions of arrays, which are often not interesting when looking at the left-hand
   * side of an array assignment.
   */
  @SuppressWarnings("unchecked") // by construction, we only get AExps if we start with a ALHS
  public static FluentIterable<AExpression> traverseLeftHandSideRecursively(ALeftHandSide root) {
    return (FluentIterable<AExpression>)
        (FluentIterable<?>) FluentIterable.from(AST_LHS_TRAVERSER.depthFirstPreOrder(root));
  }

  private static final Traverser<AAstNode> AST_LHS_TRAVERSER =
      Traverser.forTree(node -> node.accept_(LeftHandSideVisitor.INSTANCE));

  private static final Traverser<AAstNode> AST_TRAVERSER =
      Traverser.forTree(node -> node.accept_(ChildExpressionVisitor.INSTANCE));

  private static final class LeftHandSideVisitor extends ChildExpressionVisitor {

    private static final LeftHandSideVisitor INSTANCE = new LeftHandSideVisitor();

    @Override
    public Iterable<AAstNode> visit(AArraySubscriptExpression pE) {
      return ImmutableList.of(pE.getArrayExpression());
    }
  }

  private static class ChildExpressionVisitor
      extends AAstNodeVisitor<Iterable<? extends AAstNode>, NoException> {

    private static final ChildExpressionVisitor INSTANCE = new ChildExpressionVisitor();

    @Override
    public Iterable<AAstNode> visit(AArraySubscriptExpression pE) {
      return ImmutableList.of(pE.getArrayExpression(), pE.getSubscriptExpression());
    }

    @Override
    public Iterable<AAstNode> visit(ABinaryExpression pE) {
      return ImmutableList.of(pE.getOperand1(), pE.getOperand2());
    }

    @Override
    public Iterable<AAstNode> visit(ACastExpression pE) {
      return ImmutableList.of(pE.getOperand());
    }

    @Override
    public Iterable<CAstNode> visit(CComplexCastExpression pE) {
      return ImmutableList.of(pE.getOperand());
    }

    @Override
    public Iterable<CAstNode> visit(CFieldReference pE) {
      return ImmutableList.of(pE.getFieldOwner());
    }

    @Override
    public Iterable<CAstNode> visit(CPointerExpression pE) {
      return ImmutableList.of(pE.getOperand());
    }

    @Override
    public Iterable<AAstNode> visit(AUnaryExpression pE) {
      return ImmutableList.of(pE.getOperand());
    }

    @Override
    protected Iterable<? extends AAstNode> visit(AInitializerExpression pExp) {
      return ImmutableList.of(pExp.getExpression());
    }

    @Override
    public Iterable<AAstNode> visit(AIdExpression pExp) {
      return ImmutableList.of();
    }

    @Override
    public Iterable<AAstNode> visit(ACharLiteralExpression pExp) {
      return ImmutableList.of();
    }

    @Override
    public Iterable<AAstNode> visit(AFloatLiteralExpression pExp) {
      return ImmutableList.of();
    }

    @Override
    public Iterable<AAstNode> visit(AIntegerLiteralExpression pExp) {
      return ImmutableList.of();
    }

    @Override
    public Iterable<AAstNode> visit(AStringLiteralExpression pExp) {
      return ImmutableList.of();
    }

    @Override
    public Iterable<AAstNode> visit(AFunctionCallExpression pE) {
      return Iterables.concat(
          ImmutableList.of(pE.getFunctionNameExpression()), pE.getParameterExpressions());
    }

    @Override
    public Iterable<AAstNode> visit(AExpressionAssignmentStatement pS) {
      return ImmutableList.of(pS.getLeftHandSide(), pS.getRightHandSide());
    }

    @Override
    public Iterable<AAstNode> visit(AExpressionStatement pS) {
      return ImmutableList.of(pS.getExpression());
    }

    @Override
    public Iterable<AAstNode> visit(AFunctionCallAssignmentStatement pS) {
      return ImmutableList.of(pS.getLeftHandSide(), pS.getRightHandSide());
    }

    @Override
    public Iterable<AAstNode> visit(AFunctionCallStatement pS) {
      return ImmutableList.of(pS.getFunctionCallExpression());
    }

    @Override
    public Iterable<? extends AAstNode> visit(AReturnStatement pNode) {
      return Optionals.asSet(pNode.getReturnValue());
    }

    @Override
    public Iterable<CAstNode> visit(CComplexTypeDeclaration pNode) {
      return ImmutableList.of();
    }

    @Override
    public Iterable<? extends CAstNode> visit(CEnumerator pNode) {
      return ImmutableList.of();
    }

    @Override
    public Iterable<? extends AAstNode> visit(AFunctionDeclaration pNode) {
      return pNode.getParameters();
    }

    @Override
    public Iterable<CAstNode> visit(AParameterDeclaration pNode) {
      return ImmutableList.of();
    }

    @Override
    public Iterable<AAstNode> visit(AVariableDeclaration pNode) {
      return pNode.getInitializer() == null
          ? ImmutableList.of()
          : ImmutableList.of(pNode.getInitializer());
    }

    @Override
    public Iterable<CAstNode> visit(CTypeDefDeclaration pNode) {
      return ImmutableList.of();
    }

    @Override
    public Iterable<CAstNode> visit(CDesignatedInitializer pNode) {
      return Iterables.concat(pNode.getDesignators(), ImmutableList.of(pNode.getRightHandSide()));
    }

    @Override
    public Iterable<CAstNode> visit(CInitializerExpression pNode) {
      return ImmutableList.of(pNode.getExpression());
    }

    @Override
    public Iterable<CInitializer> visit(CInitializerList pNode) {
      return pNode.getInitializers();
    }

    @Override
    public Iterable<CAstNode> visit(CArrayDesignator pNode) {
      return ImmutableList.of(pNode.getSubscriptExpression());
    }

    @Override
    public Iterable<CAstNode> visit(CArrayRangeDesignator pNode) {
      return ImmutableList.of(pNode.getFloorExpression(), pNode.getCeilExpression());
    }

    @Override
    public Iterable<CAstNode> visit(CFieldDesignator pNode) {
      return ImmutableList.of();
    }

    @Override
    public Iterable<AAstNode> visit(CTypeIdExpression pExp) {
      return ImmutableList.of();
    }

    @Override
    public Iterable<AAstNode> visit(CImaginaryLiteralExpression pExp) {
      return ImmutableList.of();
    }

    @Override
    public Iterable<AAstNode> visit(CAddressOfLabelExpression pExp) {
      return ImmutableList.of();
    }

    @Override
    public Iterable<AAstNode> visit(JClassInstanceCreation pExp) {
      return ImmutableList.of(pExp.getFunctionNameExpression());
    }

    @Override
    public Iterable<AAstNode> visit(JBooleanLiteralExpression pExp) {
      return ImmutableList.of();
    }

    @Override
    public Iterable<AAstNode> visit(JArrayCreationExpression pExp) {
      if (pExp.getInitializer() == null) {
        return ImmutableList.copyOf(pExp.getLength()); // no actual copy, avoids unchecked cast
      }
      return Iterables.concat(pExp.getLength(), ImmutableList.of(pExp.getInitializer()));
    }

    @Override
    public Iterable<? extends AAstNode> visit(JArrayInitializer pNode) {
      return pNode.getInitializerExpressions();
    }

    @Override
    public Iterable<AAstNode> visit(JArrayLengthExpression pExp) {
      return ImmutableList.of(pExp.getQualifier());
    }

    @Override
    public Iterable<AAstNode> visit(JVariableRunTimeType pExp) {
      return ImmutableList.of(pExp.getReferencedVariable());
    }

    @Override
    public Iterable<AAstNode> visit(JRunTimeTypeEqualsType pExp) {
      return ImmutableList.of(pExp.getRunTimeTypeExpression());
    }

    @Override
    public Iterable<AAstNode> visit(JNullLiteralExpression pExp) {
      return ImmutableList.of();
    }

    @Override
    public Iterable<AAstNode> visit(JEnumConstantExpression pExp) {
      return ImmutableList.of();
    }

    @Override
    public Iterable<AAstNode> visit(JThisExpression pExp) {
      return ImmutableList.of();
    }

    @Override
    public Iterable<? extends AAstNode> visit(JClassLiteralExpression pJClassLiteralExpression)
        throws NoException {
      return ImmutableList.of();
    }

    @Override
    public Iterable<? extends AAstNode> visit(SvLibVariableDeclaration pSvLibVariableDeclaration) {
      return ImmutableList.of();
    }

    @Override
    public Iterable<? extends AAstNode> visit(
        SvLibParameterDeclaration pSvLibParameterDeclaration) {
      return ImmutableList.of();
    }

    @Override
    public Iterable<? extends AAstNode> accept(
        SvLibFunctionCallExpression pSvLibFunctionCallExpression) throws NoException {
      return FluentIterable.from(pSvLibFunctionCallExpression.getParameterExpressions());
    }

    @Override
    public Iterable<? extends AAstNode> accept(SvLibFunctionDeclaration pSvLibFunctionDeclaration)
        throws NoException {
      return FluentIterable.from(pSvLibFunctionDeclaration.getParameters());
    }

    @Override
    public Iterable<? extends AAstNode> accept(SvLibParameterDeclaration pSvLibParameterDeclaration)
        throws NoException {
      return ImmutableList.of();
    }

    @Override
    public Iterable<? extends AAstNode> accept(
        SvLibVariableDeclarationTuple pSvLibVariableDeclarationTuple) throws NoException {
      return pSvLibVariableDeclarationTuple.getDeclarations();
    }

    @Override
    public Iterable<? extends AAstNode> accept(SvLibAtTerm pSvLibAtTerm) throws NoException {
      return ImmutableList.of(pSvLibAtTerm.getTerm());
    }

    @Override
    public Iterable<? extends AAstNode> accept(
        SvLibSymbolApplicationTerm pSvLibSymbolApplicationTerm) {
      return FluentIterable.concat(
          pSvLibSymbolApplicationTerm.getTerms(),
          ImmutableList.of(pSvLibSymbolApplicationTerm.getSymbol()));
    }

    @Override
    public Iterable<? extends AAstNode> accept(SvLibIdTerm pSvLibIdTerm) {
      return ImmutableList.of(pSvLibIdTerm.getDeclaration());
    }

    @Override
    public Iterable<? extends AAstNode> accept(SvLibIntegerConstantTerm pSvLibIntegerConstantTerm)
        throws NoException {
      return ImmutableList.of();
    }

    @Override
    public Iterable<? extends AAstNode> accept(
        SvLibSymbolApplicationRelationalTerm pSvLibSymbolApplicationRelationalTerm)
        throws NoException {
      return pSvLibSymbolApplicationRelationalTerm.getTerms();
    }

    @Override
    public Iterable<? extends AAstNode> accept(SvLibBooleanConstantTerm pSvLibBooleanConstantTerm)
        throws NoException {
      return ImmutableList.of();
    }

    @Override
    public Iterable<? extends AAstNode> accept(SvLibRealConstantTerm pSvLibRealConstantTerm)
        throws NoException {
      return ImmutableList.of();
    }

    @Override
    public Iterable<? extends AAstNode> accept(SvLibTagReference pSvLibTagReference) {
      return ImmutableList.of();
    }

    @Override
    public Iterable<? extends AAstNode> accept(SvLibCheckTrueTag pSvLibCheckTrueTag) {
      return ImmutableList.of(pSvLibCheckTrueTag.getTerm());
    }

    @Override
    public Iterable<? extends AAstNode> accept(SvLibRequiresTag pSvLibRequiresTag)
        throws NoException {
      return ImmutableList.of(pSvLibRequiresTag.getTerm());
    }

    @Override
    public Iterable<? extends AAstNode> accept(SvLibEnsuresTag pSvLibEnsuresTag)
        throws NoException {
      return ImmutableList.of(pSvLibEnsuresTag.getTerm());
    }

    @Override
    public Iterable<? extends AAstNode> accept(SvLibInvariantTag pSvLibInvariantTag)
        throws NoException {
      return ImmutableList.of(pSvLibInvariantTag.getTerm());
    }

    @Override
    public Iterable<? extends AAstNode> accept(
        SvLibTermAssignmentCfaStatement pSvLibTermAssignmentCfaStatement) throws NoException {
      return ImmutableList.of(
          pSvLibTermAssignmentCfaStatement.getLeftHandSide(),
          pSvLibTermAssignmentCfaStatement.getRightHandSide());
    }

    @Override
    public Iterable<? extends AAstNode> accept(
        SvLibFunctionCallAssignmentStatement pSvLibFunctionCallAssignmentStatement)
        throws NoException {
      return ImmutableList.of(
          pSvLibFunctionCallAssignmentStatement.getLeftHandSide(),
          pSvLibFunctionCallAssignmentStatement.getRightHandSide());
    }

    @Override
    public Iterable<? extends AAstNode> accept(SvLibIdTermTuple pSvLibIdTermTuple)
        throws NoException {
      return pSvLibIdTermTuple.getIdTerms();
    }
  }
}
