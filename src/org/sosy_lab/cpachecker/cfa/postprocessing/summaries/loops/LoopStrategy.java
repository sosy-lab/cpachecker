// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.io.Files;
import com.google.errorprone.annotations.CheckReturnValue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.factories.AFunctionFactory;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.AbstractStrategy;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.GhostCFA;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategiesEnum;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.SummaryUtils;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependency;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

public class LoopStrategy extends AbstractStrategy {

  protected static final CSimpleType SIGNED_LONG_INT = CNumericTypes.SIGNED_LONG_INT;
  protected StrategiesEnum strategyEnum;

  protected LoopStrategy(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      StrategyDependency pStrategyDependencies,
      StrategiesEnum pStrategyEnum,
      CFA pCFA) {
    super(pLogger, pShutdownNotifier, pStrategyDependencies, pCFA);
    strategyEnum = pStrategyEnum;
  }

  @Override
  public Optional<GhostCFA> summarize(final CFANode loopStartNode) {
    return Optional.empty();
  }

  protected final Optional<CFANode> determineLoopHead(final CFANode loopStartNode) {
    return determineLoopHead(
        loopStartNode,
        x ->
            SummaryUtils.containsStrategies(
                x, ImmutableSet.of(StrategiesEnum.BASE, this.strategyEnum)));
  }

  /**
   * Constructs an assume edge assuming that loopBoundExpression does hold that leads from startNode
   * to endNode. Also constructs a dummy node for the other half of the assumption that does not
   * have any outgoing edges.
   *
   * @return the CFA node after execution of the assume (can be used as new "current" node). This is
   *     identical to the parameter endNode that is passed to this method
   */
  @CheckReturnValue
  protected static CFANode assumeLoopCondition(
      String functionName, CFANode startNode, CFANode endNode, AExpression loopBoundExpression) {
    assumeLoopConditionImpl(functionName, startNode, endNode, loopBoundExpression, false);
    return endNode;
  }

  /**
   * Constructs an assume edge assuming that loopBoundExpression does not hold that leads from
   * startNode to endNode. Also constructs a dummy node for the other half of the assumption that
   * does not have any outgoing edges.
   *
   * @return the CFA node after execution of the assume (can be used as new "current" node). This is
   *     identical to the parameter endNode that is passed to this method
   */
  @CheckReturnValue
  protected static CFANode assumeNegatedLoopCondition(
      String functionName, CFANode startNode, CFANode endNode, AExpression loopBoundExpression) {
    assumeLoopConditionImpl(functionName, startNode, endNode, loopBoundExpression, true);
    return endNode;
  }

  private static void assumeLoopConditionImpl(
      String functionName,
      CFANode startNode,
      CFANode endNode,
      AExpression loopBoundExpression,
      boolean negated) {

    CFANode dummyNode = newDummyNode(functionName);
    CFANode trueNode = negated ? dummyNode : endNode;
    CFANode falseNode = negated ? endNode : dummyNode;

    CFAEdge loopBoundCFAEdgeEnd =
        new CAssumeEdge(
            "Loop Bound Assumption",
            FileLocation.DUMMY,
            startNode,
            trueNode,
            (CExpression) loopBoundExpression,
            true);
    CFACreationUtils.addEdgeUnconditionallyToCFA(loopBoundCFAEdgeEnd);

    CAssumeEdge negatedBoundCFAEdgeEnd =
        ((CAssumeEdge) loopBoundCFAEdgeEnd).negate().copyWith(startNode, falseNode);
    CFACreationUtils.addEdgeUnconditionallyToCFA(negatedBoundCFAEdgeEnd);
  }

  protected static Optional<CFANode> havocNonLocalLoopVars(
      Set<CFAEdge> edges,
      Set<AVariableDeclaration> pModifiedVariables,
      CFANode pBeforeWhile,
      CFANode currentNode,
      CFANode newNode) {
    Set<AVariableDeclaration> modifiedVariables =
        SummaryUtils.getModifiedNonLocalVariables(edges, pModifiedVariables);
    for (AVariableDeclaration pc : modifiedVariables) {
      CIdExpression leftHandSide = new CIdExpression(FileLocation.DUMMY, (CSimpleDeclaration) pc);
      CFunctionCallExpression rightHandSide =
          (CFunctionCallExpression) new AFunctionFactory().callNondetFunction(pc.getType());
      if (rightHandSide == null) {
        return Optional.empty();
      }
      CFunctionCallAssignmentStatement cStatementEdge =
          new CFunctionCallAssignmentStatement(FileLocation.DUMMY, leftHandSide, rightHandSide);
      if (newNode == null) {
        newNode = newDummyNode(pBeforeWhile.getFunctionName());
      }
      CFAEdge dummyEdge =
          new CStatementEdge(
              pc.getName() + " = NONDET", cStatementEdge, FileLocation.DUMMY, currentNode, newNode);
      CFACreationUtils.addEdgeUnconditionallyToCFA(dummyEdge);
      currentNode = newNode;
      newNode = null;
    }
    return Optional.of(currentNode);
  }

  protected static boolean havocModifiedNonLocalVarsAsCode(
      Set<CFAEdge> pEdges, Set<AVariableDeclaration> pModifiedVariables, StringBuilder builder) {
    return havocVarsAsCode(
        SummaryUtils.getModifiedNonLocalVariables(pEdges, pModifiedVariables), builder);
  }

  protected static boolean havocVarsAsCode(
      Set<AVariableDeclaration> variables, StringBuilder builder) {
    for (AVariableDeclaration pc : variables) {
      CSimpleDeclaration decl = (CSimpleDeclaration) pc;
      // it is important to use the decl.getOrigName here, otherwise of the variable
      // exists in multiple scopes it will e.g. be called x__1 instead of 1!
      CIdExpression leftHandSide =
          new CIdExpression(FileLocation.DUMMY, decl.getType(), decl.getOrigName(), decl);
      CFunctionCallExpression rightHandSide =
          (CFunctionCallExpression) new AFunctionFactory().callNondetFunction(pc.getType());
      if (rightHandSide == null) {
        return false;
      }
      CFunctionCallAssignmentStatement cStatement =
          new CFunctionCallAssignmentStatement(FileLocation.DUMMY, leftHandSide, rightHandSide);
      builder.append(String.format("%s\n", cStatement.toASTString()));
    }
    return true;
  }

  private static final Optional<CFANode> determineLoopHead(
      final CFANode loopStartNode, Predicate<? super CFAEdge> filterFunction) {
    List<CFAEdge> filteredOutgoingEdges =
        FluentIterable.from(loopStartNode.getLeavingEdges()).filter(filterFunction).toList();

    if (filteredOutgoingEdges.size() != 1) {
      return Optional.empty();
    }

    if (!isLoopInit(loopStartNode)) {
      return Optional.empty();
    }

    CFANode loopHead = filteredOutgoingEdges.get(0).getSuccessor();
    return Optional.of(loopHead);
  }

  public static CFANode newDummyNode(String functionName) {
    return CFANode.newDummyCFANode(functionName);
  }

  /**
   * Returns true in case this node is the init of a loop, i.e., the single edge leaving this node
   * is the "while" blank edge marking the beginning of the loop.
   */
  public static final boolean isLoopInit(final CFANode node) {
    return !FluentIterable.from(node.getLeavingEdges())
        .filter(x -> x.getDescription().equals("while"))
        .isEmpty();
  }

  protected static void executeLoopBodyAsCode(Loop loop, StringBuilder builder) throws IOException {
    CFAEdge e = Iterables.getOnlyElement(loop.getIncomingEdges());
    int offset = e.getFileLocation().getNodeOffset();
    int len = e.getFileLocation().getNodeLength();
    String content =
        Files.asCharSource(e.getFileLocation().getFileName().toFile(), StandardCharsets.UTF_8)
            .read();
    builder.append(content.substring(offset, offset + len).replaceAll("^while", "if"));
    builder.append("\n");
  }

  protected boolean isSupportedLoop(Loop loop) {
    CFAEdge incomingEdge = Iterators.getOnlyElement(loop.getIncomingEdges().iterator());
    Optional<CFANode> loopHead =
        determineLoopHead(
            incomingEdge.getPredecessor(),
            x -> SummaryUtils.containsStrategies(x, ImmutableSet.of(StrategiesEnum.BASE)));
    return loopHead.isPresent() && !loop.containsUserDefinedFunctionCalls();
  }
}
