// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops;

import com.google.common.collect.FluentIterable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
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
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.GhostCFA;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategiesEnum;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependency;
import org.sosy_lab.cpachecker.core.counterexample.CExpressionToOrinalCodeVisitor;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;

public class HavocStrategy extends LoopStrategy {

  public HavocStrategy(
      final LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      StrategyDependency pStrategyDependencies,
      CFA pCFA) {
    super(pLogger, pShutdownNotifier, pStrategyDependencies, StrategiesEnum.HAVOCSTRATEGY, pCFA);
  }

  private Optional<GhostCFA> summarizeLoop(Loop pLoopStructure, CFANode pBeforeWhile) {

    CFANode startNodeGhostCFA = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());
    CFANode endNodeGhostCFA = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

    CFANode currentNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());
    CFANode newNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

    Set<AVariableDeclaration> modifiedVariables = pLoopStructure.getModifiedVariables();

    Optional<AExpression> loopBoundExpressionMaybe = pLoopStructure.getBound();
    if (loopBoundExpressionMaybe.isEmpty()) {
      return Optional.empty();
    }
    AExpression loopBoundExpression = loopBoundExpressionMaybe.orElseThrow();

    CFAEdge loopBoundCFAEdge =
        new CAssumeEdge(
            "Loop Bound Assumption",
            FileLocation.DUMMY,
            startNodeGhostCFA,
            currentNode,
            (CExpression) loopBoundExpression,
            true);
    CFACreationUtils.addEdgeUnconditionallyToCFA(loopBoundCFAEdge);

    CAssumeEdge negatedBoundCFAEdge =
        ((CAssumeEdge) loopBoundCFAEdge).negate().copyWith(startNodeGhostCFA, endNodeGhostCFA);
    CFACreationUtils.addEdgeUnconditionallyToCFA(negatedBoundCFAEdge);

    for (AVariableDeclaration pc : modifiedVariables) {
      CIdExpression leftHandSide = new CIdExpression(FileLocation.DUMMY, (CSimpleDeclaration) pc);
      CFunctionCallExpression rightHandSide =
          (CFunctionCallExpression) new AFunctionFactory().callNondetFunction(pc.getType());
      if (rightHandSide == null) {
        return Optional.empty();
      }
      CFunctionCallAssignmentStatement cStatementEdge =
          new CFunctionCallAssignmentStatement(FileLocation.DUMMY, leftHandSide, rightHandSide);
      CFAEdge dummyEdge =
          new CStatementEdge(
              pc.getName() + " = NONDET", cStatementEdge, FileLocation.DUMMY, currentNode, newNode);
      CFACreationUtils.addEdgeUnconditionallyToCFA(dummyEdge);
      currentNode = newNode;
      newNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());
    }

    Optional<Pair<CFANode, CFANode>> unrolledLoopNodesMaybe = pLoopStructure.unrollOutermostLoop();
    if (unrolledLoopNodesMaybe.isEmpty()) {
      return Optional.empty();
    }

    CFAEdge loopBoundCFAEdgeEnd =
        new CAssumeEdge(
            "Loop Bound Assumption",
            FileLocation.DUMMY,
            currentNode,
            CFANode.newDummyCFANode(pBeforeWhile.getFunctionName()),
            (CExpression) loopBoundExpression,
            true);
    CFACreationUtils.addEdgeUnconditionallyToCFA(loopBoundCFAEdgeEnd);

    CAssumeEdge negatedBoundCFAEdgeEnd =
        ((CAssumeEdge) loopBoundCFAEdgeEnd).negate().copyWith(currentNode, endNodeGhostCFA);
    CFACreationUtils.addEdgeUnconditionallyToCFA(negatedBoundCFAEdgeEnd);

    CFANode leavingSuccessor;
    Iterator<CFAEdge> iter = pLoopStructure.getOutgoingEdges().iterator();
    if (iter.hasNext()) {
      leavingSuccessor = iter.next().getSuccessor();
    } else {
      return Optional.empty();
    }

    for (CFAEdge e : pLoopStructure.getOutgoingEdges()) {
      if (e.getSuccessor().getNodeNumber() != leavingSuccessor.getNodeNumber()) {
        return Optional.empty();
      }
    }

    return Optional.of(
        new GhostCFA(
            startNodeGhostCFA, endNodeGhostCFA, pBeforeWhile, leavingSuccessor, this.strategyEnum));
  }

  @Override
  public Optional<GhostCFA> summarize(final CFANode beforeWhile) {
    Optional<CFANode> maybeLoopHead = this.determineLoopHead(beforeWhile);
    if (maybeLoopHead.isEmpty()) {
      return Optional.empty();
    }
    CFANode loopStartNode = maybeLoopHead.orElseThrow();

    Optional<Loop> loopMaybe = summaryInformation.getLoop(loopStartNode);
    if (loopMaybe.isEmpty()) {
      return Optional.empty();
    }

    Loop loop = loopMaybe.orElseThrow();

    // Function calls may change global variables, or have assert statements, which cannot be
    // summarized correctly
    if (loop.containsUserDefinedFunctionCalls()) {
      return Optional.empty();
    }

    Optional<GhostCFA> summarizedLoopMaybe = summarizeLoop(loop, beforeWhile);

    return summarizedLoopMaybe;
  }

  public static Optional<String> summarizeAsCode(Loop loop) {
    StringBuilder builder = new StringBuilder();

    Optional<AExpression> loopBoundExpressionMaybe = loop.getBound();
    if (loopBoundExpressionMaybe.isEmpty()) {
      return Optional.empty();
    }
    AExpression loopBoundExpression = loopBoundExpressionMaybe.orElseThrow();

    // 1. add a wrapping if  with the loop condition. Only in this case
    // we need to havoc the variables modified in the loop.

    builder.append(
        String.format(
            "if (%s) {\n",
            // the visitor will help to use the original variable name,
            // which is important when there are multiple variables with the same
            // name, e.g. via different scopes.
            ((CExpression) loopBoundExpression)
                .accept(CExpressionToOrinalCodeVisitor.BASIC_TRANSFORMER)));

    // 2. add a line <varname> = __VERIFIER_nondet_X(); for all modified variables
    Set<AVariableDeclaration> modifiedVariables = loop.getModifiedVariables();
    Set<String> outofScopeVariables = FluentIterable.from(getOutOfScopeVariables(loop)).transform(x->x.getQualifiedName()).toSet();
    modifiedVariables =
        FluentIterable.from(modifiedVariables)
            .filter(x -> !outofScopeVariables.contains(x.getQualifiedName()))
            .toSet();
    for (AVariableDeclaration pc : modifiedVariables) {
      CSimpleDeclaration decl = (CSimpleDeclaration) pc;
      // it is important to use the decl.getOrigName here, otherwise of the variable
      // exists in multiple scopes it will e.g. be called x__1 instead of 1!
      CIdExpression leftHandSide =
          new CIdExpression(FileLocation.DUMMY, decl.getType(), decl.getOrigName(), decl);
      CFunctionCallExpression rightHandSide =
          (CFunctionCallExpression) new AFunctionFactory().callNondetFunction(pc.getType());
      if (rightHandSide == null) {
        return Optional.empty();
      }
      CFunctionCallAssignmentStatement cStatement =
          new CFunctionCallAssignmentStatement(FileLocation.DUMMY, leftHandSide, rightHandSide);
      builder.append(String.format("  %s\n", cStatement.toASTString()));
    }
    builder.append("}\n");

    // 3. assume that the loop condition does not hold anymore
    builder.append(
        String.format(
            "if ((%s)) abort();\n",
            ((CExpression) loopBoundExpression)
                .accept(CExpressionToOrinalCodeVisitor.BASIC_TRANSFORMER)));

    return Optional.of(builder.toString());
  }

  private static Set<ASimpleDeclaration> getOutOfScopeVariables(Loop loop) {
    Set<ASimpleDeclaration> outofScopeVariables = new HashSet<>();
    for (CFAEdge e : loop.getInnerLoopEdges()) {
      outofScopeVariables.addAll(e.getSuccessor().getOutOfScopeVariables());
      outofScopeVariables.addAll(e.getPredecessor().getOutOfScopeVariables());
    }

    return outofScopeVariables;
  }
}
