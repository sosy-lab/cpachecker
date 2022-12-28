// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops;

import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.factories.AExpressionFactory;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
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
import org.sosy_lab.cpachecker.util.Pair;

public class BoundedLoopUnrollingStrategy extends LoopStrategy {

  private Integer initialLoopUnrollingsBound = 10;

  public BoundedLoopUnrollingStrategy(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      StrategyDependency pStrategyDependencies,
      CFA pCFA) {
    super(
        pLogger,
        pShutdownNotifier,
        pStrategyDependencies,
        StrategiesEnum.BOUNDEDLOOPUNROLLINGSTRATEGY,
        pCFA);
  }

  private Optional<GhostCFA> summarizeLoop(Loop pLoop, CFANode pBeforeWhile) {
    CFANode startNodeGhostCFA = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());
    CFANode endNodeGhostCFA = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

    // Intialize the parameter giving the amount of loop unrollings
    CFANode currentGhostNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());
    AExpressionFactory expressionFactory = new AExpressionFactory();
    CVariableDeclaration loopUnrollingsAmountVariable =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            false,
            CStorageClass.AUTO,
            CNumericTypes.SIGNED_LONG_INT,
            "__amountOfLoopUnrollings",
            "__amountOfLoopUnrollings",
            "__amountOfLoopUnrollings",
            null);

    CExpression amountOfLoopUnrollings =
        (CExpression)
            expressionFactory
                .from(initialLoopUnrollingsBound, CNumericTypes.SIGNED_LONG_INT)
                .build();

    CExpressionAssignmentStatement amountOfLoopUnrollingsVariableIntialization =
        (CExpressionAssignmentStatement)
            expressionFactory
                .from(0, CNumericTypes.SIGNED_LONG_INT)
                .assignTo(loopUnrollingsAmountVariable);

    CFACreationUtils.addEdgeUnconditionallyToCFA(
        new CStatementEdge(
            amountOfLoopUnrollingsVariableIntialization.toString(),
            amountOfLoopUnrollingsVariableIntialization,
            FileLocation.DUMMY,
            startNodeGhostCFA,
            currentGhostNode));

    List<AExpression> parametersGhostCFA = Lists.newArrayList(amountOfLoopUnrollings);

    // Generate the for loop which will provide the bounded unrollings of the loop
    CFANode newGhostNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());
    CFACreationUtils.addEdgeUnconditionallyToCFA(
        new BlankEdge("", FileLocation.DUMMY, currentGhostNode, newGhostNode, "while"));

    CFANode loopStartNode = newGhostNode;
    newGhostNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

    CExpression startLoopUnrollingsWhileExpression =
        (CExpression)
            new AExpressionFactory()
                .from(loopUnrollingsAmountVariable)
                .binaryOperation(amountOfLoopUnrollings, CBinaryExpression.BinaryOperator.LESS_THAN)
                .build();

    CFACreationUtils.addEdgeUnconditionallyToCFA(
        new CAssumeEdge(
            startLoopUnrollingsWhileExpression.toString(),
            FileLocation.DUMMY,
            loopStartNode,
            newGhostNode,
            startLoopUnrollingsWhileExpression,
            true));

    CFACreationUtils.addEdgeUnconditionallyToCFA(
        new CAssumeEdge(
            startLoopUnrollingsWhileExpression.toString(),
            FileLocation.DUMMY,
            loopStartNode,
            endNodeGhostCFA,
            startLoopUnrollingsWhileExpression,
            false));

    // Unroll the loop
    Optional<Pair<CFANode, CFANode>> unrolledLoopNodesMaybe = pLoop.unrollOutermostLoop();
    if (unrolledLoopNodesMaybe.isEmpty()) {
      return Optional.empty();
    }

    CFANode startUnrolledLoopNode = unrolledLoopNodesMaybe.orElseThrow().getFirst();
    CFANode endUnrolledLoopNode = unrolledLoopNodesMaybe.orElseThrow().getSecond();

    CFACreationUtils.connectNodes(newGhostNode, startUnrolledLoopNode);
    CFACreationUtils.connectNodes(endUnrolledLoopNode, loopStartNode);

    // Get the leaving successor of the loop
    CFANode leavingSuccessor;
    Iterator<CFAEdge> iter = pLoop.getOutgoingEdges().iterator();
    if (iter.hasNext()) {
      leavingSuccessor = iter.next().getSuccessor();
    } else {
      return Optional.empty();
    }

    for (CFAEdge e : pLoop.getOutgoingEdges()) {
      if (e.getSuccessor().getNodeNumber() != leavingSuccessor.getNodeNumber()) {
        return Optional.empty();
      }
    }

    return Optional.of(
        new GhostCFA(
            startNodeGhostCFA,
            endNodeGhostCFA,
            pBeforeWhile,
            leavingSuccessor,
            strategyEnum,
            parametersGhostCFA,
            StrategyQualifier.Underapproximating));
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

    return summarizeLoop(loop, beforeWhile);
  }
}
