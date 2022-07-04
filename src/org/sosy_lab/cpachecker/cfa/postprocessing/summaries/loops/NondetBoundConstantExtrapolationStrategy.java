// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops;

import java.util.Iterator;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.factories.AFunctionFactory;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.GhostCFA;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategiesEnum;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependency;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

public class NondetBoundConstantExtrapolationStrategy extends ConstantExtrapolationStrategy {

  public static final String TMP_VAR_NONDET_BOUND = "__VERIFIER_LA_NONDET_BOUND_tmp";

  public NondetBoundConstantExtrapolationStrategy(
      final LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      StrategyDependency pStrategyDependencies,
      CFA pCFA) {
    super(
        pLogger,
        pShutdownNotifier,
        pStrategyDependencies,
        StrategiesEnum.NONDETBOUNDCONSTANTEXTRAPOLATION,
        pCFA);
  }

  private Optional<GhostCFA> createSumaryCFA(
      CFANode beforeWhile, AExpression loopBoundExpression, Loop pLoopStructure) {
    CVariableDeclaration iterationsVariableDeclaration =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            false,
            CStorageClass.AUTO,
            CNumericTypes.LONG_LONG_INT, // TODO Improve this
            TMP_VAR_NONDET_BOUND,
            TMP_VAR_NONDET_BOUND,
            beforeWhile.getFunctionName() + "::" + TMP_VAR_NONDET_BOUND,
            null);

    CIdExpression iterationsVariableExpression =
        new CIdExpression(FileLocation.DUMMY, iterationsVariableDeclaration);
    CFunctionCallExpression rightHandSide =
        (CFunctionCallExpression)
            new AFunctionFactory().callNondetFunction(iterationsVariableDeclaration.getType());
    if (rightHandSide == null) {
      return Optional.empty();
    }
    CFunctionCallAssignmentStatement cStatementEdge =
        new CFunctionCallAssignmentStatement(
            FileLocation.DUMMY, iterationsVariableExpression, rightHandSide);

    CFANode startNodeGhostCFA = CFANode.newDummyCFANode(beforeWhile.getFunctionName());
    CFANode endNodeGhostCFA = CFANode.newDummyCFANode(beforeWhile.getFunctionName());

    CFANode currentNode = CFANode.newDummyCFANode(beforeWhile.getFunctionName());

    CFAEdge dummyEdge =
        new CStatementEdge(
            iterationsVariableDeclaration.getName() + " = NONDET",
            cStatementEdge,
            FileLocation.DUMMY,
            startNodeGhostCFA,
            currentNode);
    CFACreationUtils.addEdgeUnconditionallyToCFA(dummyEdge);

    Optional<GhostCFA> summarizedLoopMaybe =
        super.summarizeLoop(
            iterationsVariableExpression, loopBoundExpression, pLoopStructure, beforeWhile);

    if (summarizedLoopMaybe.isEmpty()) {
      return Optional.empty();
    }

    CFACreationUtils.connectNodes(
        currentNode, summarizedLoopMaybe.orElseThrow().getStartGhostCfaNode());

    currentNode = CFANode.newDummyCFANode(beforeWhile.getFunctionName());

    CFACreationUtils.connectNodes(
        summarizedLoopMaybe.orElseThrow().getStopGhostCfaNode(), currentNode);

    CFAEdge loopBoundCFAEdgeEnd =
        new CAssumeEdge(
            "Loop Bound Assumption",
            FileLocation.DUMMY,
            currentNode,
            CFANode.newDummyCFANode(beforeWhile.getFunctionName()),
            (CExpression) loopBoundExpression,
            true);
    CFACreationUtils.addEdgeUnconditionallyToCFA(loopBoundCFAEdgeEnd);

    CAssumeEdge negatedBoundCFAEdgeEnd =
        ((CAssumeEdge) loopBoundCFAEdgeEnd).negate().copyWith(currentNode, endNodeGhostCFA);
    CFACreationUtils.addEdgeUnconditionallyToCFA(negatedBoundCFAEdgeEnd);

    CFAEdge leavingEdge;
    Iterator<CFAEdge> iter = pLoopStructure.getOutgoingEdges().iterator();
    if (iter.hasNext()) {
      leavingEdge = iter.next();
      if (iter.hasNext()) {
        return Optional.empty();
      }
    } else {
      return Optional.empty();
    }

    return Optional.of(
        new GhostCFA(
            startNodeGhostCFA,
            endNodeGhostCFA,
            beforeWhile,
            leavingEdge.getSuccessor(),
            this.strategyEnum));
  }

  @Override
  public synchronized Optional<GhostCFA> summarize(final CFANode beforeWhile) {
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

    if (loop.containsFunctionCalls()) {
      return Optional.empty();
    }

    if (!hasOnlyConstantVariableModifications(loop) || loop.amountOfInnerAssumeEdges() != 1) {
      return Optional.empty();
    }

    Optional<AExpression> loopBoundExpressionMaybe = loop.getBound();
    if (loopBoundExpressionMaybe.isEmpty()) {
      return Optional.empty();
    }
    AExpression loopBoundExpression = loopBoundExpressionMaybe.orElseThrow();

    return this.createSumaryCFA(beforeWhile, loopBoundExpression, loop);
  }

  @Override
  public boolean isPrecise() {
    return false;
  }
}
