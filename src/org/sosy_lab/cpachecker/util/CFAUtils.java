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

import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.graph.Traverser;
import com.google.errorprone.annotations.DoNotCall;
import com.google.errorprone.annotations.InlineMe;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.sosy_lab.common.Optionals;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.AAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.ACastExpression;
import org.sosy_lab.cpachecker.cfa.ast.ACharLiteralExpression;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
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
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.ast.ASTElement;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.ast.IfElement;
import org.sosy_lab.cpachecker.util.ast.IterationElement;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

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
  public static FluentIterable<CFAEdge> allEnteringEdges(final CFANode node) {
    checkNotNull(node);
    return new FluentIterable<>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return new UnmodifiableIterator<>() {

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
  public static FluentIterable<CFAEdge> enteringEdges(final CFANode node) {
    checkNotNull(node);
    return new FluentIterable<>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return new UnmodifiableIterator<>() {

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
   * Return an {@link Iterable} that contains all leaving edges of a given CFANode, including the
   * summary edge if the node as one.
   *
   * <p>WARNING: Summary edges are included, so the returned {@link FluentIterable} may contain
   * parallel edges (i.e., multiple directed edges from some node {@code u} to some node {@code v}).
   * These edges are equal, so a set would only contain one of the parallel edges.
   */
  public static FluentIterable<CFAEdge> allLeavingEdges(final CFANode node) {
    checkNotNull(node);
    return new FluentIterable<>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return new UnmodifiableIterator<>() {

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
   * Returns all edges which are reachable in the forward direction without any branchings from the
   * current edge.
   *
   * @param edge The edge where to start the reachability analysis from
   * @return All the edges which can be reached from the current edge in the forward direction
   */
  public static Set<CFAEdge> forwardLinearReach(CFAEdge edge) {
    CFAEdge current = edge;
    ImmutableSet.Builder<CFAEdge> builder = ImmutableSet.builder();
    while (CFAUtils.leavingEdges(current.getSuccessor()).size() == 1) {
      current = CFAUtils.leavingEdges(current.getSuccessor()).first().get();
      builder.add(current);
    }
    return builder.build();
  }

  /**
   * Return an {@link Iterable} that contains the leaving edges of a given CFANode, excluding the
   * summary edge.
   */
  public static FluentIterable<CFAEdge> leavingEdges(final CFANode node) {
    checkNotNull(node);
    return new FluentIterable<>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return new UnmodifiableIterator<>() {

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

  @SuppressWarnings("unchecked")
  public static FluentIterable<FunctionCallEdge> enteringEdges(final FunctionEntryNode node) {
    return (FluentIterable<FunctionCallEdge>) (FluentIterable<?>) enteringEdges((CFANode) node);
  }

  @SuppressWarnings("unchecked")
  public static FluentIterable<FunctionReturnEdge> leavingEdges(final FunctionExitNode node) {
    return (FluentIterable<FunctionReturnEdge>) (FluentIterable<?>) leavingEdges((CFANode) node);
  }

  @Deprecated // entry nodes do not have summary edges
  @InlineMe(
      replacement = "CFAUtils.enteringEdges(node)",
      imports = "org.sosy_lab.cpachecker.util.CFAUtils")
  public static FluentIterable<FunctionCallEdge> allEnteringEdges(final FunctionEntryNode node) {
    return enteringEdges(node);
  }

  @Deprecated // exit nodes do not have summary edges
  @InlineMe(
      replacement = "CFAUtils.enteringEdges(node)",
      imports = "org.sosy_lab.cpachecker.util.CFAUtils")
  public static FluentIterable<CFAEdge> allEnteringEdges(final FunctionExitNode node) {
    return enteringEdges(node);
  }

  @Deprecated // entry nodes do not have summary edges
  @InlineMe(
      replacement = "CFAUtils.leavingEdges(node)",
      imports = "org.sosy_lab.cpachecker.util.CFAUtils")
  public static FluentIterable<CFAEdge> allLeavingEdges(final FunctionEntryNode node) {
    return leavingEdges(node);
  }

  @Deprecated // exit nodes do not have summary edges
  @InlineMe(
      replacement = "CFAUtils.leavingEdges(node)",
      imports = "org.sosy_lab.cpachecker.util.CFAUtils")
  public static FluentIterable<FunctionReturnEdge> allLeavingEdges(final FunctionExitNode node) {
    return leavingEdges(node);
  }

  @Deprecated // termination nodes do not have leaving edges
  @DoNotCall
  public static FluentIterable<CFAEdge> leavingEdges(
      @SuppressWarnings("unused") final CFATerminationNode node) {
    throw new AssertionError("useless method");
  }

  @Deprecated // termination nodes do not have leaving edges
  @DoNotCall
  public static FluentIterable<CFAEdge> allLeavingEdges(
      @SuppressWarnings("unused") final CFATerminationNode node) {
    throw new AssertionError("useless method");
  }

  /**
   * Return an {@link Iterable} that contains the predecessor nodes of a given CFANode, excluding
   * the one reachable via the summary edge (if there is one).
   */
  public static FluentIterable<CFANode> predecessorsOf(final CFANode node) {
    return enteringEdges(node).transform(CFAEdge::getPredecessor);
  }

  /**
   * Return an {@link Iterable} that contains all the predecessor nodes of a given CFANode,
   * including the one reachable via the summary edge (if there is one).
   */
  public static FluentIterable<CFANode> allPredecessorsOf(final CFANode node) {
    return allEnteringEdges(node).transform(CFAEdge::getPredecessor);
  }

  /**
   * Return an {@link Iterable} that contains the successor nodes of a given CFANode, excluding the
   * one reachable via the summary edge (if there is one).
   */
  public static FluentIterable<CFANode> successorsOf(final CFANode node) {
    return leavingEdges(node).transform(CFAEdge::getSuccessor);
  }

  /**
   * Return an {@link Iterable} that contains all the successor nodes of a given CFANode, including
   * the one reachable via the summary edge (if there is one).
   */
  public static FluentIterable<CFANode> allSuccessorsOf(final CFANode node) {
    return allLeavingEdges(node).transform(CFAEdge::getSuccessor);
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
        CFAUtils.leavingEdges(edge.getPredecessor())
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
   * <p>Currently this is quite inefficient, so use with caution and only for small sets of nodes.
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
        throw new AssertionError("forgotten case in traversing cfa with more than 2 leaving edges");
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
    // TODO: Currently the format of the qualified name is not defined.
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
   * Extracts all variables involved in the edges of the given CFA and their types.
   *
   * @return Map of memory locations of variables within cfa to their types.
   */
  public static Map<MemoryLocation, CType> extractVariableTypes(CFA cfa) {
    Map<MemoryLocation, CType> types = new HashMap<>();

    for (CFAEdge edge : CFAUtils.allEdges(cfa)) {
      switch (edge.getEdgeType()) {
        case DeclarationEdge -> {
          // Handle variable declarations (with/without initializer)
          if (edge instanceof ADeclarationEdge declEdge) {
            var decl = declEdge.getDeclaration();
            if (decl instanceof CVariableDeclaration cVarDecl) {
              MemoryLocation memLoc = MemoryLocation.forDeclaration(cVarDecl);
              CType type = cVarDecl.getType();
              types.put(memLoc, type);
              var initializer = cVarDecl.getInitializer();
              if (initializer != null) {
                types.putAll(extractTypesFromCInitializer(initializer));
              }
            }
          }
        }
        case StatementEdge -> {
          // Handle assignments and function calls
          if (edge instanceof AStatementEdge stmtEdge) {
            var stmt = stmtEdge.getStatement();
            if (stmt instanceof AExpressionAssignmentStatement assign) {
              types.putAll(extractTypesFromALeftHandSide(assign.getLeftHandSide()));
              types.putAll(extractTypesFromAExpression(assign.getRightHandSide()));
            } else if (stmt instanceof AFunctionCallAssignmentStatement funCallAssign) {
              types.putAll(extractTypesFromALeftHandSide(funCallAssign.getLeftHandSide()));
              var funCallExpr = funCallAssign.getFunctionCallExpression();
              for (var arg : funCallExpr.getParameterExpressions()) {
                types.putAll(extractTypesFromAExpression(arg));
              }
            } else if (stmt instanceof AExpressionStatement exprStmt) {
              types.putAll(extractTypesFromAExpression(exprStmt.getExpression()));
            } else if (stmt instanceof AFunctionCallStatement funCallStmt) {
              var funCallExpr = funCallStmt.getFunctionCallExpression();
              for (var arg : funCallExpr.getParameterExpressions()) {
                types.putAll(extractTypesFromAExpression(arg));
              }
            }
          }
        }
        case AssumeEdge -> {
          // Handle assume edges (conditions)
          if (edge instanceof AssumeEdge assumeEdge) {
            types.putAll(extractTypesFromAExpression(assumeEdge.getExpression()));
          }
        }
        case ReturnStatementEdge -> {
          // Handle return statements
          if (edge instanceof AReturnStatementEdge retEdge) {
            if (retEdge.getExpression().isPresent()) {
              // If the edge has an assignment (i.e., return variable = expr), extract both sides
              Optional<? extends AAssignment> assignmentOpt = retEdge.asAssignment();
              if (assignmentOpt.isPresent()) {
                AAssignment assignment = assignmentOpt.get();
                if (assignment instanceof AExpressionAssignmentStatement exprAssign) {
                  types.putAll(extractTypesFromALeftHandSide(exprAssign.getLeftHandSide()));
                  types.putAll(extractTypesFromAExpression(exprAssign.getRightHandSide()));
                } else if (assignment instanceof AFunctionCallAssignmentStatement funCallAssign) {
                  types.putAll(extractTypesFromALeftHandSide(funCallAssign.getLeftHandSide()));
                  var funCallExpr = funCallAssign.getFunctionCallExpression();
                  for (var arg : funCallExpr.getParameterExpressions()) {
                    types.putAll(extractTypesFromAExpression(arg));
                  }
                }
              }
              // Always extract from the returned expression as well
              types.putAll(extractTypesFromAExpression(retEdge.getExpression().get()));
            }
          }
        }
        case FunctionCallEdge -> {
          // Handle function call edges (arguments and parameters)
          if (edge instanceof FunctionCallEdge callEdge) {
            for (var arg : callEdge.getArguments()) {
              types.putAll(extractTypesFromAExpression(arg));
            }
            for (var param : callEdge.getSuccessor().getFunctionParameters()) {
              if (param.getType() instanceof CType cType) {
                types.put(MemoryLocation.forDeclaration(param), cType);
              }
            }
          }
        }
        case FunctionReturnEdge -> {
          // Handle function return edges (return variable)
          if (edge instanceof FunctionReturnEdge retEdge) {
            var entry = retEdge.getFunctionEntry();
            var retVarOpt = entry.getReturnVariable();
            if (retVarOpt.isPresent()) {
              var retVar = retVarOpt.get();
              if (retVar.getType() instanceof CType cType) {
                types.put(MemoryLocation.forDeclaration(retVar), cType);
              }
            }
          }
        }
        default -> {
          // Ignore blank, call-to-return, etc.
        }
      }
    }
    return types;
  }

  private static Map<MemoryLocation, CType> extractTypesFromAExpression(AExpression expr) {
    Map<MemoryLocation, CType> result = new HashMap<>();
    if (expr == null) return result;
    if (expr instanceof CExpression cExpr) {
      for (var id : CFAUtils.getIdExpressionsOfExpression(cExpr)) {
        var decl = id.getDeclaration();
        if (decl != null && decl.getType() != null) {
          CType cType = decl.getType();
          result.put(MemoryLocation.forDeclaration(decl), cType);
        }
      }
    }
    // Could add more cases for Java expressions if needed
    return result;
  }

  private static Map<MemoryLocation, CType> extractTypesFromALeftHandSide(ALeftHandSide lhs) {
    Map<MemoryLocation, CType> result = new HashMap<>();
    if (lhs == null) return result;
    if (lhs instanceof CLeftHandSide cLhs) {
      if (cLhs instanceof CIdExpression id) {
        var decl = id.getDeclaration();
        if (decl != null && decl.getType() != null) {
          CType cType = decl.getType();
          result.put(MemoryLocation.forDeclaration(decl), cType);
        }
      } else if (cLhs instanceof CFieldReference fieldRef) {
        // Recursively extract from field owner
        result.putAll(extractTypesFromAExpression(fieldRef.getFieldOwner()));
      } else if (cLhs instanceof CArraySubscriptExpression arrSub) {
        result.putAll(extractTypesFromAExpression(arrSub.getArrayExpression()));
        result.putAll(extractTypesFromAExpression(arrSub.getSubscriptExpression()));
      } else if (cLhs instanceof CPointerExpression ptr) {
        result.putAll(extractTypesFromAExpression(ptr.getOperand()));
      }
    }
    return result;
  }

  private static Map<MemoryLocation, CType> extractTypesFromCInitializer(CInitializer initializer) {
    Map<MemoryLocation, CType> result = new HashMap<>();
    if (initializer == null) return result;
    if (initializer instanceof CDesignatedInitializer desInit) {
      result.putAll(extractTypesFromCInitializer(desInit.getRightHandSide()));
    } else if (initializer instanceof CInitializerExpression initExpr) {
      result.putAll(extractTypesFromAExpression(initExpr.getExpression()));
    } else if (initializer instanceof CInitializerList initList) {
      for (var subInit : initList.getInitializers()) {
        result.putAll(extractTypesFromCInitializer(subInit));
      }
    }
    return result;
  }

  /** Sees whether the file locations of two {@link CFANode} are disjoint or not. */
  public static boolean disjointFileLocations(CFANode pNode1, CFANode pNode2) {
    // Using the method getFileLocationsFromCfaEdge produces some weird results
    // when considering the initialized global variables and stuff like that
    Set<FileLocation> allFileLocationFirstNode =
        CFAUtils.allEnteringEdges(pNode1)
            .append(CFAUtils.allLeavingEdges(pNode1))
            .transform(CFAEdge::getFileLocation)
            .toSet();
    Set<FileLocation> allFileLocationSecondtNode =
        CFAUtils.allEnteringEdges(pNode2)
            .append(CFAUtils.allLeavingEdges(pNode2))
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

    if (result.isEmpty() && pEdge.getPredecessor() instanceof FunctionEntryNode) {
      FunctionEntryNode functionEntryNode = (FunctionEntryNode) pEdge.getPredecessor();
      if (functionEntryNode.getFileLocation().isRealLocation()) {
        return ImmutableSet.of(functionEntryNode.getFileLocation());
      }
    }
    return result;
  }

  public static Iterable<AAstNode> getAstNodesFromCfaEdge(final CFAEdge edge) {
    switch (edge.getEdgeType()) {
      case CallToReturnEdge -> {
        FunctionSummaryEdge fnSumEdge = (FunctionSummaryEdge) edge;
        return ImmutableSet.of(fnSumEdge.getExpression());
      }
      default -> {
        return Optionals.asSet(edge.getRawAST());
      }
    }
  }

  /**
   * Return all variable names that are referenced in an expression, in pre-order and possibly with
   * duplicates.
   */
  public static FluentIterable<String> getVariableNamesOfExpression(CExpression expr) {
    return getIdExpressionsOfExpression(expr)
        .transform(id -> id.getDeclaration().getQualifiedName());
  }

  /**
   * Return all {@link CIdExpression}s that appear in an expression, in pre-order and possibly with
   * duplicates.
   */
  public static FluentIterable<CIdExpression> getIdExpressionsOfExpression(CExpression expr) {
    return traverseRecursively(expr).filter(CIdExpression.class);
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
  }
}
