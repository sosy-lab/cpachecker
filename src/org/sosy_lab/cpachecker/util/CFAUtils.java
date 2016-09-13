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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Queues;
import com.google.common.collect.TreeTraverser;
import com.google.common.collect.UnmodifiableIterator;

import org.sosy_lab.common.Optionals;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayCreationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayInitializer;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayLengthExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JAstNode;
import org.sosy_lab.cpachecker.cfa.ast.java.JBooleanLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JClassInstanceCreation;
import org.sosy_lab.cpachecker.cfa.ast.java.JEnumConstantExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JNullLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JRunTimeTypeEqualsType;
import org.sosy_lab.cpachecker.cfa.ast.java.JThisExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableRunTimeType;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;

public class CFAUtils {

  /**
   * Return an {@link Iterable} that contains all entering edges of a given CFANode,
   * including the summary edge if the node has one.
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

  /**
   * Return an {@link Iterable} that contains the predecessor nodes of a given CFANode,
   * excluding the one reachable via the summary edge (if there is one).
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
   * Return an {@link Iterable} that contains the successor nodes of a given CFANode,
   * excluding the one reachable via the summary edge (if there is one).
   */
  public static FluentIterable<CFANode> successorsOf(final CFANode node) {
    return leavingEdges(node).transform(CFAEdge::getSuccessor);
  }

  /**
   * Return an {@link Iterable} that contains all the successor nodes of a given CFANode,
   * including the one reachable via the summary edge (if there is one).
   */
  public static FluentIterable<CFANode> allSuccessorsOf(final CFANode node) {
    return allLeavingEdges(node).transform(CFAEdge::getSuccessor);
  }

  /**
   * Returns a predicate for CFA edges with the given edge type.
   * The predicate is not null safe.
   *
   * @param pType the edge type matched on.
   */
  public static Predicate<CFAEdge> edgeHasType(final CFAEdgeType pType) {
    checkNotNull(pType);
    return pInput -> pInput.getEdgeType() == pType;
  }

  /**
   * Returns the other AssumeEdge (with the negated condition)
   * of a given AssumeEdge.
   */
  public static AssumeEdge getComplimentaryAssumeEdge(AssumeEdge edge) {
    checkArgument(edge.getPredecessor().getNumLeavingEdges() == 2);
    return (AssumeEdge)Iterables.getOnlyElement(
        CFAUtils.leavingEdges(edge.getPredecessor())
                .filter(not(Predicates.<CFAEdge>equalTo(edge))));
  }

  /**
   * Checks if a path from the source to the target exists, using the given
   * function to obtain the edges leaving a node.
   *
   * @param pSource the search start node.
   * @param pTarget the target.
   * @param pGetLeavingEdges the function used to obtain leaving edges and thus
   * the successors of a node.
   * @param pShutdownNotifier the shutdown notifier to be checked.
   *
   * @return {@code true} if a path from the source to the target exists,
   * {@code false} otherwise.
   *
   * @throws InterruptedException if a shutdown has been requested by the given
   * shutdown notifier.
   */
  public static boolean existsPath(CFANode pSource,
      CFANode pTarget, Function<CFANode, Iterable<CFAEdge>> pGetLeavingEdges,
      ShutdownNotifier pShutdownNotifier) throws InterruptedException {
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


  /**
   * This Visitor searches for backwards edges in the CFA, if some backwards edges
   * were found can be obtained by calling the method hasBackwardsEdges()
   */
  private static class FindBackwardsEdgesVisitor extends DefaultCFAVisitor {

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
   * @param rootNode The node where the search is started
   * @return indicates if a backwards edge was found
   */
  static boolean hasBackWardsEdges(CFANode rootNode) {
    FindBackwardsEdgesVisitor visitor = new FindBackwardsEdgesVisitor();

    CFATraversal.dfs().ignoreSummaryEdges().traverseOnce(rootNode, visitor);

    return visitor.hasBackwardsEdges();
  }

  /**
   * This method allows to select from a set of variables
   * all local variables from a given function.
   * This requires that the given set contains the qualified names of each variable
   * as returned by {@link AbstractSimpleDeclaration#getQualifiedName()}.
   *
   * @param variables Set of qualified names of variables.
   * @param function A function name.
   * @return A subset of "variables".
   */
  public static SortedSet<String> filterVariablesOfFunction(SortedSet<String> variables, String function) {
    // TODO: Currently the format of the qualified name is not defined.
    // In theory, frontends could use different formats.
    // The best would be to eliminate all uses of this method
    // (code should not use Strings, but for example AIdExpressions).
    // For now, we just assume all variables are named as
    // {@link org.sosy_lab.cpachecker.cfa.parser.eclipse.c.FunctionScope#createQualifiedName(String, String)}
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
    Queue<List<CFANode>> waitlist = Queues.newArrayDeque();
    waitlist.offer(ImmutableList.of(pNode));
    while (!waitlist.isEmpty()) {
      List<CFANode> currentPath = waitlist.poll();
      CFANode pathSucc = currentPath.get(currentPath.size() - 1);
      List<BlankEdge> leavingBlankEdges =
          CFAUtils.leavingEdges(pathSucc).filter(BlankEdge.class).toList();
      if (pathSucc.getNumLeavingEdges() <= 0
          || leavingBlankEdges.size() < pathSucc.getNumLeavingEdges()) {
        blankPaths.add(currentPath);
      } else {
        for (CFAEdge leavingEdge : leavingBlankEdges) {
          CFANode successor = leavingEdge.getSuccessor();
          if (!currentPath.contains(successor)) {
            List<CFANode> newPath =
                ImmutableList.<CFANode>builder().addAll(currentPath).add(successor).build();
            waitlist.offer(newPath);
          }
        }
      }
    }
    waitlist.addAll(blankPaths);
    blankPaths.clear();
    while (!waitlist.isEmpty()) {
      List<CFANode> currentPath = waitlist.poll();
      CFANode pathPred = currentPath.get(0);
      List<BlankEdge> enteringBlankEdges =
          CFAUtils.enteringEdges(pathPred).filter(BlankEdge.class).toList();
      if (pathPred.getNumEnteringEdges() <= 0
          || enteringBlankEdges.size() < pathPred.getNumEnteringEdges()) {
        blankPaths.add(currentPath);
      } else {
        for (CFAEdge enteringEdge : enteringBlankEdges) {
          CFANode predecessor = enteringEdge.getPredecessor();
          if (!currentPath.contains(predecessor)) {
            List<CFANode> newPath =
                ImmutableList.<CFANode>builder().add(predecessor).addAll(currentPath).build();
            waitlist.offer(newPath);
          }
        }
      }
    }
    return blankPaths;
  }

  /**
   * Get all {@link FileLocation} objects that are attached to an edge or its AST nodes.
   * The result has non-deterministic order.
   */
  public static Set<FileLocation> getFileLocationsFromCfaEdge(CFAEdge pEdge) {
    Set<FileLocation> result =
        from(getAstNodesFromCfaEdge(pEdge))
            .transformAndConcat(node -> traverseRecursively((CAstNode) node))
            .transform(CAstNode::getFileLocation)
            .copyInto(new HashSet<>());

    result.add(pEdge.getFileLocation());
    result.remove(FileLocation.DUMMY);

    if (result.isEmpty() && pEdge.getPredecessor() instanceof FunctionEntryNode) {
      FunctionEntryNode functionEntryNode = (FunctionEntryNode) pEdge.getPredecessor();
      if (!functionEntryNode.getFileLocation().equals(FileLocation.DUMMY)) {
        return Collections.singleton(functionEntryNode.getFileLocation());
      }
    }
    return Collections.unmodifiableSet(result);
  }

  private static Iterable<? extends AAstNode> getAstNodesFromCfaEdge(final CFAEdge edge) {
    switch (edge.getEdgeType()) {
      case CallToReturnEdge:
        FunctionSummaryEdge fnSumEdge = (FunctionSummaryEdge) edge;
        return ImmutableSet.of(fnSumEdge.getExpression());

      case FunctionCallEdge:
        FunctionCallEdge functionCallEdge = (FunctionCallEdge) edge;
        return Iterables.concat(
            Optionals.asSet(edge.getRawAST()),
            getAstNodesFromCfaEdge(functionCallEdge.getSummaryEdge()));

      default:
        return Optionals.asSet(edge.getRawAST());
    }
  }

  /**
   * Return all variable names that are referenced in an expression,
   * in pre-order and possibly with duplicates.
   */
  public static FluentIterable<String> getVariableNamesOfExpression(CExpression expr) {
    return getIdExpressionsOfExpression(expr)
        .transform(id -> id.getDeclaration().getQualifiedName());
  }

  /**
   * Return all {@link CIdExpression}s that appear in an expression,
   * in pre-order and possibly with duplicates.
   */
  public static FluentIterable<CIdExpression> getIdExpressionsOfExpression(CExpression expr) {
    return traverseRecursively(expr).filter(CIdExpression.class);
  }

  /**
   * Get an iterable that recursively lists all AST nodes that occur in an AST
   * (in pre-order).
   */
  public static FluentIterable<AAstNode> traverseRecursively(AAstNode root) {
    return AstNodeTraverser.REGULAR_INSTANCE.preOrderTraversal(root);
  }

  /**
   * Get an iterable that recursively lists all AST nodes that occur in a C AST
   * (in pre-order).
   */
  @SuppressWarnings(
      "unchecked") // by construction, we only get CAstNodes if we start with a CAstNode
  public static FluentIterable<CAstNode> traverseRecursively(CAstNode root) {
    return (FluentIterable<CAstNode>)
        (FluentIterable<?>) AstNodeTraverser.REGULAR_INSTANCE.preOrderTraversal(root);
  }

  /**
   * Get an iterable that recursively lists all AST nodes that occur in a Java AST
   * (in pre-order).
   */
  @SuppressWarnings(
      "unchecked") // by construction, we only get JAstNodes if we start with a jAstNode
  public static FluentIterable<JAstNode> traverseRecursively(JAstNode root) {
    return (FluentIterable<JAstNode>)
        (FluentIterable<?>) AstNodeTraverser.REGULAR_INSTANCE.preOrderTraversal(root);
  }

  /**
   * Get an iterable that recursively lists all AST nodes that occur in a CRightHandSide
   * (in pre-order).
   */
  @SuppressWarnings("unchecked") // by construction, we only get CRHS if we start with a CRHS
  public static FluentIterable<CRightHandSide> traverseRecursively(CRightHandSide root) {
    return (FluentIterable<CRightHandSide>)
        (FluentIterable<?>) AstNodeTraverser.REGULAR_INSTANCE.preOrderTraversal(root);
  }

  /**
   * Get an iterable that recursively lists all AST nodes that occur in a CExpression
   * (in pre-order).
   */
  @SuppressWarnings("unchecked") // by construction, we only get CExps if we start with a CExp
  public static FluentIterable<CExpression> traverseRecursively(CExpression root) {
    return (FluentIterable<CExpression>)
        (FluentIterable<?>) AstNodeTraverser.REGULAR_INSTANCE.preOrderTraversal(root);
  }

  /**
   * Get an iterable that recursively lists AST nodes that occur in a {@link ALeftHandSide}
   * (in pre-order).
   *
   * Note: contrary to {@link #traverseRecursively(AAstNode)},
   * this iterable does not contain the subscript expressions of arrays,
   * which are often not interesting when looking at the left-hand side of an array assignment.
   */
  @SuppressWarnings("unchecked") // by construction, we only get AExps if we start with a ALHS
  public static FluentIterable<AExpression> traverseLeftHandSideRecursively(ALeftHandSide root) {
    return (FluentIterable<AExpression>)
        (FluentIterable<?>) AstNodeTraverser.LHS_INSTANCE.preOrderTraversal(root);
  }

  private static final class AstNodeTraverser extends TreeTraverser<AAstNode> {

    private final AAstNodeVisitor<Iterable<? extends AAstNode>, RuntimeException> visitor;

    private AstNodeTraverser(AAstNodeVisitor<Iterable<? extends AAstNode>, RuntimeException> pV) {
      visitor = pV;
    }

    private static final AstNodeTraverser REGULAR_INSTANCE =
        new AstNodeTraverser(new ChildExpressionVisitor());
    private static final AstNodeTraverser LHS_INSTANCE =
        new AstNodeTraverser(new LeftHandSideVisitor());

    @SuppressWarnings("unchecked") // cast is safe for iterable
    @Override
    public Iterable<AAstNode> children(AAstNode pRoot) {
      return (Iterable<AAstNode>) pRoot.accept_(visitor);
    }
  }

  private static final class LeftHandSideVisitor extends ChildExpressionVisitor {

    @Override
    public Iterable<AAstNode> visit(AArraySubscriptExpression pE) {
      return ImmutableList.of(pE.getArrayExpression());
    }
  }

  private static class ChildExpressionVisitor
      extends AAstNodeVisitor<Iterable<? extends AAstNode>, RuntimeException> {

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
  }
}
