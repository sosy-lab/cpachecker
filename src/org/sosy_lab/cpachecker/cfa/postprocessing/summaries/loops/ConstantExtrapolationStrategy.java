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
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.GhostCFA;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategiesEnum;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependencyInterface;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.expressions.AExpressionsFactory;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.expressions.AExpressionsFactory.ExpressionType;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.expressions.LoopVariableDeltaVisitor;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;

public class ConstantExtrapolationStrategy extends AbstractLoopExtrapolationStrategy {

  private StrategiesEnum strategyEnum;

  public ConstantExtrapolationStrategy(
      final LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      StrategyDependencyInterface pStrategyDependencies,
      CFA pCFA) {
    super(pLogger, pShutdownNotifier, pStrategyDependencies, pCFA);

    this.strategyEnum = StrategiesEnum.LoopConstantExtrapolation;
  }

  /**
   * This method returns the Amount of iterations the loop will go through, if it is possible to
   * calculate this
   *
   * @param loopBoundExpression the expression of the loop while (EXPR) { something; }
   * @param loopStructure The loop structure which is being summarized
   */
  public Optional<AExpression> loopIterations(AExpression loopBoundExpression, Loop loopStructure) {
    // This expression is the amount of iterations given in symbols
    Optional<AExpression> iterationsMaybe = Optional.empty();
    // TODO For now it only works for c programs
    if (loopBoundExpression instanceof CBinaryExpression) {
      LoopVariableDeltaVisitor<Exception> variableVisitor =
          new LoopVariableDeltaVisitor<>(loopStructure, true);

      CExpression operand1 = ((CBinaryExpression) loopBoundExpression).getOperand1();
      CExpression operand2 = ((CBinaryExpression) loopBoundExpression).getOperand2();
      BinaryOperator operator = ((CBinaryExpression) loopBoundExpression).getOperator();

      Optional<Integer> operand1variableDelta = operand1.accept(variableVisitor);
      Optional<Integer> operand2variableDelta = operand2.accept(variableVisitor);

      if (operand1variableDelta.isPresent() && operand2variableDelta.isPresent()) {

        switch (operator) {
          case EQUALS:
            // Should iterate at most once if the Deltas are non zero
            // If the deltas are zero and the integer is zero this loop would not terminate
            // TODO: What do we do if the loop does not terminate?
            // TODO: this can be improved if the value of the variables is known.
            if (operand1variableDelta.get() - operand2variableDelta.get() != 0) {
              // Returning this works because for any number of iterations less than or equal to 2
              // The loop is simply unrolled. Since because of overflows no extrapolation can be
              // made
              iterationsMaybe =
                  Optional.of(new AExpressionsFactory(ExpressionType.C).from(1, ExpressionType.C));
            }
            break;
          case GREATER_EQUAL:
            if (operand1variableDelta.get() - operand2variableDelta.get() < 0) {
              iterationsMaybe =
                  Optional.of(
                      (AExpression)
                          new AExpressionsFactory(ExpressionType.C)
                              .from(operand1)
                              .arithmeticExpression(
                                  operand2, CBinaryExpression.BinaryOperator.MINUS)
                              .divide(operand2variableDelta.get() - operand1variableDelta.get())
                              .build());
            }
            break;
          case GREATER_THAN:
            if (operand1variableDelta.get() - operand2variableDelta.get() < 0) {
              iterationsMaybe =
                  Optional.of(
                      (AExpression)
                          new AExpressionsFactory(ExpressionType.C)
                              .from(operand1)
                              .arithmeticExpression(
                                  operand2, CBinaryExpression.BinaryOperator.MINUS)
                              .divide(operand2variableDelta.get() - operand1variableDelta.get())
                              .add(1)
                              .build());
            }
            break;
          case LESS_EQUAL:
            if (operand2variableDelta.get() - operand1variableDelta.get() < 0) {
              iterationsMaybe =
                  Optional.of(
                      (AExpression)
                          new AExpressionsFactory(ExpressionType.C)
                              .from(operand2)
                              .arithmeticExpression(
                                  operand1, CBinaryExpression.BinaryOperator.MINUS)
                              .divide(operand1variableDelta.get() - operand2variableDelta.get())
                              .add(1)
                              .build());
            }
            break;
          case LESS_THAN:
            if (operand2variableDelta.get() - operand1variableDelta.get() < 0) {
              iterationsMaybe =
                  Optional.of(
                      (AExpression)
                          new AExpressionsFactory(ExpressionType.C)
                              .from(operand2)
                              .arithmeticExpression(
                                  operand1, CBinaryExpression.BinaryOperator.MINUS)
                              .divide(operand1variableDelta.get() - operand2variableDelta.get())
                              .build());
            }
            break;
          case NOT_EQUALS:
            // Should iterate at most once if the Deltas are zero
            // If the deltas are non zero and the integer is zero this loop could terminate, but
            // it is not known when this could happen
            // TODO: What do we do if the loop does not terminate?
            // TODO: this can be improved if the value of the variables is known.
            if (operand1variableDelta.get() - operand2variableDelta.get() == 0) {
              // Returning this works because for any number of iterations less than or equal to 2
              // The loop is simply unrolled. Since because of overflows no extrapolation can be
              // made
              iterationsMaybe =
                  Optional.of(new AExpressionsFactory(ExpressionType.C).from(1, ExpressionType.C));
            }
            break;
          default:
            break;
        }
      }
    }
    return iterationsMaybe;
  }

  protected Optional<GhostCFA> summarizeLoop(
      AExpression pIterations,
      AExpression pLoopBoundExpression,
      Loop pLoopStructure,
      CFANode pBeforeWhile) {

    CFANode startNodeGhostCFA = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());
    CFANode endNodeGhostCFA = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

    Optional<Pair<CFANode, CFANode>> unrolledLoopNodesMaybe = pLoopStructure.unrollOutermostLoop();
    if (unrolledLoopNodesMaybe.isEmpty()) {
      return Optional.empty();
    }

    CFANode startUnrolledLoopNode = unrolledLoopNodesMaybe.get().getFirst();
    CFANode endUnrolledLoopNode = unrolledLoopNodesMaybe.get().getSecond();

    startNodeGhostCFA.connectTo(startUnrolledLoopNode);

    CFANode currentSummaryNodeCFA = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

    CFAEdge loopBoundCFAEdge =
        new CAssumeEdge(
            "Loop Bound Assumption",
            FileLocation.DUMMY,
            endUnrolledLoopNode,
            currentSummaryNodeCFA,
            (CExpression) pLoopBoundExpression,
            true); // TODO: this may not be the correct way to do this; Review
    loopBoundCFAEdge.connect();

    CAssumeEdge negatedBoundCFAEdge =
        ((CAssumeEdge) loopBoundCFAEdge).negate().copyWith(endUnrolledLoopNode, endNodeGhostCFA);
    negatedBoundCFAEdge.connect();

    CFANode nextSummaryNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

    // Make Summary of Loop

    for (AVariableDeclaration var : pLoopStructure.getModifiedVariables()) {
      Optional<Integer> deltaMaybe = pLoopStructure.getDelta(var.getQualifiedName());
      if (deltaMaybe.isEmpty()) {
        return Optional.empty();
      }

      Integer delta = deltaMaybe.get();

      // TODO: Refactor expression Factory
      // TODO: the use of a C expression should be replaced for selecting if a Java or C
      // expression should be used in this context
      AExpressionsFactory expressionFactory = new AExpressionsFactory(ExpressionType.C);
      CExpressionAssignmentStatement assignmentExpression =
          (CExpressionAssignmentStatement)
              expressionFactory
                  .from(pIterations)
                  .minus(1)
                  .multiply(delta)
                  .add(
                      new CIdExpression(
                          FileLocation.DUMMY, (CSimpleDeclaration) var)) // TODO Improve this
                  .assignTo(var)
                  .build();

      CFAEdge dummyEdge =
          new CStatementEdge(
              assignmentExpression.toString(),
              assignmentExpression,
              FileLocation.DUMMY,
              currentSummaryNodeCFA,
              nextSummaryNode);
      dummyEdge.connect();

      currentSummaryNodeCFA = nextSummaryNode;
      nextSummaryNode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());
    }

    // Unroll Loop Once again

    unrolledLoopNodesMaybe = pLoopStructure.unrollOutermostLoop();
    if (unrolledLoopNodesMaybe.isEmpty()) {
      return Optional.empty();
    }

    startUnrolledLoopNode = unrolledLoopNodesMaybe.get().getFirst();
    endUnrolledLoopNode = unrolledLoopNodesMaybe.get().getSecond();
    currentSummaryNodeCFA.connectTo(startUnrolledLoopNode);
    endUnrolledLoopNode.connectTo(endNodeGhostCFA);

    CFAEdge leavingEdge;
    Iterator<CFAEdge> iter =
        pLoopStructure.getOutgoingEdges().iterator();
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

  @Override
  public Optional<GhostCFA> summarize(final CFANode beforeWhile) {

    if (beforeWhile.getNumLeavingEdges() != 1) {
      return Optional.empty();
    }

    if (!beforeWhile.getLeavingEdge(0).getDescription().equals("while")) {
      return Optional.empty();
    }

    CFANode loopStartNode = beforeWhile.getLeavingEdge(0).getSuccessor();

    Optional<Loop> loopStructureMaybe = summaryInformation.getLoop(loopStartNode);
    if (loopStructureMaybe.isEmpty()) {
      return Optional.empty();
    }
    Loop loopStructure = loopStructureMaybe.get();

    if (!loopStructure.onlyConstantVarModification()) {
      return Optional.empty();
    }

    Optional<AExpression> loopBoundExpressionMaybe = loopStructure.getBound();
    if (loopBoundExpressionMaybe.isEmpty()) {
      return Optional.empty();
    }
    AExpression loopBoundExpression = loopBoundExpressionMaybe.get();

    Optional<AExpression> iterationsMaybe = this.loopIterations(loopBoundExpression, loopStructure);

    if (iterationsMaybe.isEmpty()) {
      return Optional.empty();
    }

    AExpression iterations = iterationsMaybe.get();

    Optional<GhostCFA> summarizedLoopMaybe =
        summarizeLoop(iterations, loopBoundExpression, loopStructure, beforeWhile);

    return summarizedLoopMaybe;

  }
}
