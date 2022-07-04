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
        x -> summaryFilter.filter(x, ImmutableSet.of(StrategiesEnum.BASE, this.strategyEnum)));
  }

  protected static void assumeNegatedLoopBound(
      String functionName, CFANode startNode, CFANode endNode, AExpression loopBoundExpression) {
    CFAEdge loopBoundCFAEdgeEnd =
        new CAssumeEdge(
            "Loop Bound Assumption",
            FileLocation.DUMMY,
            startNode,
            CFANode.newDummyCFANode(functionName),
            (CExpression) loopBoundExpression,
            true);
    CFACreationUtils.addEdgeUnconditionallyToCFA(loopBoundCFAEdgeEnd);

    CAssumeEdge negatedBoundCFAEdgeEnd =
        ((CAssumeEdge) loopBoundCFAEdgeEnd).negate().copyWith(startNode, endNode);
    CFACreationUtils.addEdgeUnconditionallyToCFA(negatedBoundCFAEdgeEnd);
  }

  protected static Optional<CFANode> havocNonLocalLoopVars(
      Loop loop, CFANode pBeforeWhile, CFANode currentNode, CFANode newNode) {
    Set<AVariableDeclaration> modifiedVariables =
        getModifiedNonLocalVariables(loop);
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
        newNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());
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

  protected static boolean havocModifiedNonLocalVarsAsCode(Loop loop, StringBuilder builder) {
    Set<AVariableDeclaration> modifiedVariables = getModifiedNonLocalVariables(loop);
    for (AVariableDeclaration pc : modifiedVariables) {
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

  protected static Set<AVariableDeclaration> getModifiedNonLocalVariables(Loop loop) {
    Set<AVariableDeclaration> modifiedVariables = loop.getModifiedVariables();
    Set<String> outofScopeVariables =
        FluentIterable.from(HavocStrategy.getOutOfScopeVariables(loop))
            .transform(x -> x.getQualifiedName())
            .toSet();
    modifiedVariables =
        FluentIterable.from(modifiedVariables)
            .filter(x -> !outofScopeVariables.contains(x.getQualifiedName()))
            .toSet();
    return modifiedVariables;
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

  /**
   * Returns true in case this node is the init of a loop, i.e., the single edge leaving this node
   * is the "while" blank edge marking the beginning of the loop.
   */
  public static final boolean isLoopInit(final CFANode node) {
    return !FluentIterable.from(node.getLeavingEdges())
        .filter(x -> x.getDescription().equals("while"))
        .isEmpty();
  }
}
