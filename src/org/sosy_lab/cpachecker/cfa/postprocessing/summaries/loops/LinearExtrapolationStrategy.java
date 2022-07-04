// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.factories.AExpressionFactory;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.GhostCFA;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategiesEnum;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependency;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.utils.LinearVariableDependencyGraph;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.utils.LinearVariableDependencyMatrix;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;

public class LinearExtrapolationStrategy extends LoopExtrapolationStrategy {

  // See
  // https://math.stackexchange.com/questions/2079950/compute-the-n-th-power-of-triangular-3-times3-matrix

  public LinearExtrapolationStrategy(
      final LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      StrategyDependency pStrategyDependencies,
      CFA pCFA) {
    super(
        pLogger,
        pShutdownNotifier,
        pStrategyDependencies,
        StrategiesEnum.LOOPLINEAREXTRAPOLATION,
        pCFA);
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

    if (hasOnlyConstantVariableModifications(loop)
        || loop.amountOfInnerAssumeEdges() != 1
        || !hasOnlyLinearVariableModifications(loop)) {
      return Optional.empty();
    }

    Optional<AExpression> loopBoundExpressionMaybe = loop.getBound();
    if (loopBoundExpressionMaybe.isEmpty()) {
      return Optional.empty();
    }
    AExpression loopBoundExpression = loopBoundExpressionMaybe.orElseThrow();

    Optional<AExpression> iterationsMaybe = this.loopIterations(loopBoundExpression, loop);

    if (iterationsMaybe.isEmpty()) {
      return Optional.empty();
    }

    AExpression iterations = iterationsMaybe.orElseThrow();

    Optional<GhostCFA> summarizedLoopMaybe =
        createGhostCFA(iterations, loopBoundExpression, loop, beforeWhile);

    return summarizedLoopMaybe;
  }

  private Optional<GhostCFA> createGhostCFA(
      AExpression pIterations,
      AExpression pLoopBoundExpression,
      Loop pLoopStructure,
      CFANode pBeforeWhile) {

    LinearVariableDependencyGraph variableDependencyGraph =
        pLoopStructure.getLinearVariableDependencies();

    LinearVariableDependencyMatrix variableDependencyMatrix = variableDependencyGraph.asMatrix();

    if (!variableDependencyMatrix.isUpperDiagonal()) {
      return Optional.empty();
    }

    if (!variableDependencyMatrix.diagonalValueEquals(Integer.valueOf(1))) {
      return Optional.empty();
    }

    CFANode startNodeGhostCFA = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());
    CFANode endNodeGhostCFA = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

    Optional<Pair<CFANode, CFANode>> unrolledLoopNodesMaybe = pLoopStructure.unrollOutermostLoop();
    if (unrolledLoopNodesMaybe.isEmpty()) {
      return Optional.empty();
    }

    CFANode startUnrolledLoopNode = unrolledLoopNodesMaybe.orElseThrow().getFirst();
    CFANode endUnrolledLoopNode = unrolledLoopNodesMaybe.orElseThrow().getSecond();

    CFACreationUtils.connectNodes(startNodeGhostCFA, startUnrolledLoopNode);

    CFANode currentSummaryNodeCFA = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

    CFAEdge loopBoundCFAEdge =
        new CAssumeEdge(
            "Loop Bound Assumption",
            FileLocation.DUMMY,
            endUnrolledLoopNode,
            currentSummaryNodeCFA,
            (CExpression) pLoopBoundExpression,
            true); // TODO: this may not be the correct way to do this; Review
    CFACreationUtils.addEdgeUnconditionallyToCFA(loopBoundCFAEdge);

    CAssumeEdge negatedBoundCFAEdge =
        ((CAssumeEdge) loopBoundCFAEdge).negate().copyWith(endUnrolledLoopNode, endNodeGhostCFA);
    CFACreationUtils.addEdgeUnconditionallyToCFA(negatedBoundCFAEdge);

    Optional<CFANode> nextNode =
        initializeIterationsVariable(currentSummaryNodeCFA, pIterations, pBeforeWhile);

    if (nextNode.isEmpty()) {
      return Optional.empty();
    }

    currentSummaryNodeCFA = nextNode.orElseThrow();
    AVariableDeclaration iterationsVariable =
        createIterationsVariable(pBeforeWhile.getFunctionName());

    LinearVariableDependencyMatrix powerOfMatrix =
        variableDependencyMatrix.toThepower(
            new AExpressionFactory().from(iterationsVariable).build());
    List<AExpressionAssignmentStatement> extrapolationAssignments = powerOfMatrix.asAssignments();

    // TODO: This may be wrong, since the extrapolation with the matrix calculation may be wrong.
    // This would need to be fixed.

    CFANode nextSummaryNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

    // Make the Summary
    for (AExpressionAssignmentStatement e : extrapolationAssignments) {

      // TODO: Overflows may be detected incorrectly when assigning variables. See solution in
      // ConstantExtrapolationStrategy. This occurs since a new variable needs to be made in order
      // to implement a special statement to check if an overflow occurs

      CFAEdge dummyEdge =
          new CStatementEdge(
              e.toString(),
              // TODO: Generalize for Java
              (CExpressionAssignmentStatement) e,
              FileLocation.DUMMY,
              currentSummaryNodeCFA,
              nextSummaryNode);
      CFACreationUtils.addEdgeUnconditionallyToCFA(dummyEdge);

      currentSummaryNodeCFA = nextSummaryNode;
      nextSummaryNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());
    }

    // Unroll Loop two times

    unrolledLoopNodesMaybe = pLoopStructure.unrollOutermostLoop();
    if (unrolledLoopNodesMaybe.isEmpty()) {
      return Optional.empty();
    }

    startUnrolledLoopNode = unrolledLoopNodesMaybe.orElseThrow().getFirst();
    endUnrolledLoopNode = unrolledLoopNodesMaybe.orElseThrow().getSecond();
    CFACreationUtils.connectNodes(currentSummaryNodeCFA, startUnrolledLoopNode);

    unrolledLoopNodesMaybe = pLoopStructure.unrollOutermostLoop();
    if (unrolledLoopNodesMaybe.isEmpty()) {
      return Optional.empty();
    }

    CFANode secondStartUnrolledNode = unrolledLoopNodesMaybe.orElseThrow().getFirst();
    CFANode secondEndUnrolledNode = unrolledLoopNodesMaybe.orElseThrow().getSecond();

    CFACreationUtils.connectNodes(endUnrolledLoopNode, secondStartUnrolledNode);
    CFACreationUtils.connectNodes(secondEndUnrolledNode, endNodeGhostCFA);

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
            pBeforeWhile,
            leavingEdge.getSuccessor(),
            this.strategyEnum));
  }
}
