// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
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
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.GhostCFA;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategiesEnum;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependency;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;

public class NaiveLoopAccelerationStrategy extends LoopStrategy {

  public NaiveLoopAccelerationStrategy(
      final LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      StrategyDependency pStrategyDependencies,
      CFA pCFA) {
    super(
        pLogger,
        pShutdownNotifier,
        pStrategyDependencies,
        StrategiesEnum.NAIVELOOPACCELERATION,
        pCFA);
  }

  private Optional<GhostCFA> summarizeLoop(
      Loop pLoopStructure,
      Set<AVariableDeclaration> pModifiedVariables,
      CFANode pBeforeWhile,
      AExpression pLoopBoundExpression) {

    CFANode startNodeGhostCFA = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());
    CFANode endNodeGhostCFA = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

    CFANode currentNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());
    CFANode newNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

    CFAEdge loopBoundCFAEdge =
        new CAssumeEdge(
            "Loop Bound Assumption",
            FileLocation.DUMMY,
            startNodeGhostCFA,
            currentNode,
            (CExpression) pLoopBoundExpression,
            true);
    CFACreationUtils.addEdgeUnconditionallyToCFA(loopBoundCFAEdge);

    CAssumeEdge negatedBoundCFAEdge =
        ((CAssumeEdge) loopBoundCFAEdge).negate().copyWith(startNodeGhostCFA, endNodeGhostCFA);
    CFACreationUtils.addEdgeUnconditionallyToCFA(negatedBoundCFAEdge);

    for (AVariableDeclaration pc : pModifiedVariables) {
      // TODO improve for Java
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

    CFANode startUnrolledLoopNode = unrolledLoopNodesMaybe.orElseThrow().getFirst();
    CFANode endUnrolledLoopNode = unrolledLoopNodesMaybe.orElseThrow().getSecond();

    CFACreationUtils.connectNodes(currentNode, startUnrolledLoopNode);
    currentNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());
    CFACreationUtils.connectNodes(endUnrolledLoopNode, currentNode);

    CFAEdge loopBoundCFAEdgeEnd =
        new CAssumeEdge(
            "Loop Bound Assumption",
            FileLocation.DUMMY,
            currentNode,
            CFANode.newDummyCFANode(pBeforeWhile.getFunctionName()),
            (CExpression) pLoopBoundExpression,
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

    List<CFAEdge> filteredOutgoingEdges =
        this.summaryFilter.getEdgesForStrategies(
            beforeWhile.getLeavingEdges(),
            new HashSet<>(Arrays.asList(StrategiesEnum.BASE, this.strategyEnum)));

    if (filteredOutgoingEdges.size() != 1) {
      return Optional.empty();
    }

    if (!filteredOutgoingEdges.get(0).getDescription().equals("while")) {
      return Optional.empty();
    }

    CFANode loopStartNode = filteredOutgoingEdges.get(0).getSuccessor();

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

    Set<AVariableDeclaration> modifiedVariables = loop.getModifiedVariables();

    Optional<AExpression> loopBoundExpressionMaybe = loop.getBound();
    if (loopBoundExpressionMaybe.isEmpty()) {
      return Optional.empty();
    }
    AExpression loopBoundExpression = loopBoundExpressionMaybe.orElseThrow();

    Optional<GhostCFA> summarizedLoopMaybe =
        summarizeLoop(loop, modifiedVariables, beforeWhile, loopBoundExpression);

    return summarizedLoopMaybe;
  }
}
