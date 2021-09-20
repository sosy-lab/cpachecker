// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.GhostCFA;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategiesEnum;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependencyInterface;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.expressions.AExpressionsFactory;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.expressions.AExpressionsFactory.ExpressionType;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.expressions.AggregateConstantsVisitor;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.expressions.LoopVariableDeltaVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
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

  // There was a method unrollLoopOnce which overrided the unroll loop once method in the abstract
  // class, see it if there is some error, it was still here in commit
  // a272d189e10d05880102c4a29450c113f9f80bee

  protected Optional<GhostCFA> summaryCFA(
      CFANode loopStartNode,
      final Map<String, Integer> loopVariableDelta,
      final CExpression loopBound,
      final int boundDelta,
      final int boundVariableDelta,
      final Integer loopBranchIndex) {
    int CFANodeCounter = 1;
    CFANode startNodeGhostCFA = CFANode.newDummyCFANode("LSSTARTGHHOST");
    CFANode endNodeGhostCFA = CFANode.newDummyCFANode("LSENDGHHOST");
    CFANode currentEndNodeGhostCFA = CFANode.newDummyCFANode("LS2");
    CFAEdge loopIngoingConditionEdge = loopStartNode.getLeavingEdge(loopBranchIndex);
    CFAEdge loopIngoingConditionDummyEdgeTrue =
        overwriteStartEndStateEdge(
            (CAssumeEdge) loopIngoingConditionEdge, true, startNodeGhostCFA, currentEndNodeGhostCFA);
    CFAEdge loopIngoingConditionDummyEdgeFalse =
        overwriteStartEndStateEdge(
            (CAssumeEdge) loopIngoingConditionEdge, false, startNodeGhostCFA, endNodeGhostCFA);
    startNodeGhostCFA.addLeavingEdge(loopIngoingConditionDummyEdgeTrue);
    currentEndNodeGhostCFA.addEnteringEdge(loopIngoingConditionDummyEdgeTrue);
    startNodeGhostCFA.addLeavingEdge(loopIngoingConditionDummyEdgeFalse);
    endNodeGhostCFA.addEnteringEdge(loopIngoingConditionDummyEdgeFalse);
    CFANode currentStartNodeGhostCFA = currentEndNodeGhostCFA;
    currentEndNodeGhostCFA = CFANode.newDummyCFANode("LS3");

    CType calculationType = ((CBinaryExpression) loopBound).getCalculationType();
    CType expressionType = ((CBinaryExpression) loopBound).getExpressionType();
    // Check for Overflows by unrolling the Loop once before doing the Summary and once after doing
    // the summary
    CBinaryExpression loopBoundtwiceUnrollingExpression =
        new CBinaryExpression(
            FileLocation.DUMMY,
            expressionType,
            calculationType,
            CIntegerLiteralExpression.createDummyLiteral(0, CNumericTypes.INT),
            new CBinaryExpression(
                FileLocation.DUMMY,
                expressionType,
                calculationType,
                loopBound,
                CIntegerLiteralExpression.createDummyLiteral(
                    2 * ((long) boundDelta), CNumericTypes.INT),
                BinaryOperator.MINUS),
            BinaryOperator.LESS_THAN);
    CFAEdge twiceLoopUnrollingConditionEdgeTrue =
        new CAssumeEdge(
            ((CBinaryExpression) loopBound).toString() + "- " + 2 * boundDelta + " > 0",
            FileLocation.DUMMY,
            currentStartNodeGhostCFA,
            currentEndNodeGhostCFA,
            loopBoundtwiceUnrollingExpression,
            true);
    currentStartNodeGhostCFA.addLeavingEdge(twiceLoopUnrollingConditionEdgeTrue);
    currentEndNodeGhostCFA.addEnteringEdge(twiceLoopUnrollingConditionEdgeTrue);
    CFANode loopUnrollingCurrentNode = CFANode.newDummyCFANode("LS5");
    CFAEdge twiceLoopUnrollingConditionEdgeFalse =
        new CAssumeEdge(
            ((CBinaryExpression) loopBound).toString() + "- " + 2 * boundDelta + "> 0",
            FileLocation.DUMMY,
            currentStartNodeGhostCFA,
            loopUnrollingCurrentNode,
            loopBoundtwiceUnrollingExpression,
            false);
    currentStartNodeGhostCFA.addLeavingEdge(twiceLoopUnrollingConditionEdgeFalse);
    loopUnrollingCurrentNode.addEnteringEdge(twiceLoopUnrollingConditionEdgeFalse);


    // When the loopbound - 2 <= 0 we need to unroll the loop twice
    Optional<CFANode> loopUnrollingSuccess = unrollLoopOnce(loopStartNode, loopBranchIndex, loopUnrollingCurrentNode, endNodeGhostCFA);
    if (loopUnrollingSuccess.isEmpty()) {
      return Optional.empty();
    } else {
      loopUnrollingCurrentNode =
          loopUnrollingSuccess.orElseThrow();
    }

    loopUnrollingSuccess =
        unrollLoopOnce(loopStartNode, loopBranchIndex, loopUnrollingCurrentNode, endNodeGhostCFA);
    if (loopUnrollingSuccess.isEmpty()) {
      return Optional.empty();
    } else {
      loopUnrollingCurrentNode = loopUnrollingSuccess.orElseThrow();
    }
    CFAEdge blankOutgoingEdge = new BlankEdge("Blank", FileLocation.DUMMY, loopUnrollingCurrentNode, endNodeGhostCFA, "Blank");
    loopUnrollingCurrentNode.addLeavingEdge(blankOutgoingEdge);
    endNodeGhostCFA.addEnteringEdge(blankOutgoingEdge);

    // Unroll the loop once to check for overflows

    loopUnrollingSuccess =
        unrollLoopOnce(loopStartNode, loopBranchIndex, currentEndNodeGhostCFA, endNodeGhostCFA);
    if (loopUnrollingSuccess.isEmpty()) {
      return Optional.empty();
    } else {
      loopUnrollingCurrentNode = loopUnrollingSuccess.orElseThrow();
    }
    currentEndNodeGhostCFA = CFANode.newDummyCFANode("LS6");
    currentStartNodeGhostCFA = loopUnrollingCurrentNode;

    // Make Summary
    for (Map.Entry<String, Integer> set : loopVariableDelta.entrySet()) {
      CVariableDeclaration pc =
          new CVariableDeclaration(
              FileLocation.DUMMY,
              true,
              CStorageClass.EXTERN,
              CNumericTypes.INT, // TODO improve this
              set.getKey(),
              set.getKey(),
              set.getKey(),
              null);
      CExpression rightHandSide =
          new CBinaryExpression(
              FileLocation.DUMMY,
              expressionType,
              calculationType,
              new CIdExpression(FileLocation.DUMMY, pc),
              new CBinaryExpression(
                  FileLocation.DUMMY,
                  expressionType,
                  calculationType,
                  CIntegerLiteralExpression.createDummyLiteral(set.getValue(), CNumericTypes.INT),
                  new CBinaryExpression(
                      FileLocation.DUMMY,
                      expressionType,
                      calculationType,
                      new CBinaryExpression(
                          FileLocation.DUMMY,
                          expressionType,
                          calculationType,
                          loopBound,
                          CIntegerLiteralExpression.createDummyLiteral(
                              boundVariableDelta, CNumericTypes.INT),
                          BinaryOperator.PLUS),
                      CIntegerLiteralExpression.createDummyLiteral(boundDelta, CNumericTypes.INT),
                      BinaryOperator.DIVIDE),
                  BinaryOperator.MULTIPLY),
              BinaryOperator.PLUS);
      CIdExpression leftHandSide = new CIdExpression(FileLocation.DUMMY, pc);
      CExpressionAssignmentStatement cStatementEdge =
          new CExpressionAssignmentStatement(FileLocation.DUMMY, leftHandSide, rightHandSide);
      CFAEdge dummyEdge =
          new CStatementEdge(
              set.getKey() + " = " + set.getValue() + " - 2",
              cStatementEdge,
              FileLocation.DUMMY,
              currentStartNodeGhostCFA,
              currentEndNodeGhostCFA);
      currentStartNodeGhostCFA.addLeavingEdge(dummyEdge);
      currentEndNodeGhostCFA.addEnteringEdge(dummyEdge);
      currentStartNodeGhostCFA = currentEndNodeGhostCFA;
      currentEndNodeGhostCFA = CFANode.newDummyCFANode("LSI" + CFANodeCounter);
      CFANodeCounter += 1;
    }

    loopUnrollingSuccess =
        unrollLoopOnce(loopStartNode, loopBranchIndex, currentStartNodeGhostCFA, endNodeGhostCFA);

    if (loopUnrollingSuccess.isEmpty()) {
      return Optional.empty();
    } else {

      currentStartNodeGhostCFA = loopUnrollingSuccess.orElseThrow();
    }

    blankOutgoingEdge =
        new BlankEdge(
            "Blank", FileLocation.DUMMY, currentStartNodeGhostCFA, endNodeGhostCFA, "Blank");
    currentStartNodeGhostCFA.addLeavingEdge(blankOutgoingEdge);
    endNodeGhostCFA.addEnteringEdge(blankOutgoingEdge);

    CFANode afterLoopNode = loopStartNode.getLeavingEdge(1 - loopBranchIndex).getSuccessor();

    return Optional.of(
        new GhostCFA(
            startNodeGhostCFA,
            endNodeGhostCFA,
            loopStartNode,
            afterLoopNode,
            StrategiesEnum.LoopConstantExtrapolation));
  }

  /**
   * This method returns the Amount of iterations the loop will go through, if it is possible to
   * calculate this
   *
   * @param loopBoundExpression the expression of the loop while (EXPR) { something; }
   * @param loopStructure The loop structure which is being summarized
   */
  public Optional<AIntegerLiteralExpression> loopIterations(
      AExpression loopBoundExpression, Loop loopStructure) {
    // This expression is the amount of iterations given in symbols
    try {
      Optional<AIntegerLiteralExpression> iterationsMaybe = Optional.empty();
      // TODO For now it only works for c programs
      if (loopBoundExpression instanceof CBinaryExpression) {
        LoopVariableDeltaVisitor<Exception> variableVisitor =
            new LoopVariableDeltaVisitor<>(loopStructure, true);
        AggregateConstantsVisitor<Exception> constantsVisitor =
            new AggregateConstantsVisitor<>(
                Optional.of(loopStructure.getLoopIncDecVariables()), true);

        CExpression operand1 = ((CBinaryExpression) loopBoundExpression).getOperand1();
        CExpression operand2 = ((CBinaryExpression) loopBoundExpression).getOperand2();
        BinaryOperator operator = ((CBinaryExpression) loopBoundExpression).getOperator();

        Optional<Integer> operand1variableDelta = operand1.accept(variableVisitor);
        Optional<Integer> operand2variableDelta = operand2.accept(variableVisitor);
        Optional<Integer> operand1Constants = operand1.accept(constantsVisitor);
        Optional<Integer> operand2Constants = operand2.accept(constantsVisitor);

        if (operand1variableDelta.isPresent()
            && operand2variableDelta.isPresent()
            && operand1Constants.isPresent()
            && operand2Constants.isPresent()) {

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
                    Optional.of(this.getExpressionFactory().from(1, ExpressionType.C));
              }
              break;
            case GREATER_EQUAL:
              if (operand1variableDelta.get() - operand2variableDelta.get() < 0) {
                Integer iterationsAmnt =
                    (operand1Constants.get() - operand2Constants.get())
                            / -(operand1variableDelta.get() - operand2variableDelta.get())
                        + 1;
                iterationsMaybe =
                    Optional.of(this.getExpressionFactory().from(iterationsAmnt, ExpressionType.C));
              }
              break;
            case GREATER_THAN:
              if (operand1variableDelta.get() - operand2variableDelta.get() < 0) {
                Integer iterationsAmnt =
                    (operand1Constants.get() - operand2Constants.get())
                        / -(operand1variableDelta.get() - operand2variableDelta.get());
                iterationsMaybe =
                    Optional.of(this.getExpressionFactory().from(iterationsAmnt, ExpressionType.C));
              }
              break;
            case LESS_EQUAL:
              if (operand2variableDelta.get() - operand1variableDelta.get() < 0) {
                Integer iterationsAmnt =
                    (operand2Constants.get() - operand1Constants.get())
                            / -(operand2variableDelta.get() - operand1variableDelta.get())
                        + 1;
                iterationsMaybe =
                    Optional.of(this.getExpressionFactory().from(iterationsAmnt, ExpressionType.C));
              }
              break;
            case LESS_THAN:
              if (operand2variableDelta.get() - operand1variableDelta.get() < 0) {
                iterationsMaybe =
                    Optional.of(
                        this.getExpressionFactory()
                            .from(
                                (operand2Constants.get() - operand1Constants.get()),
                                ExpressionType.C));
              }
              break;
            case NOT_EQUALS:
              // Should iterate at most once if the Deltas are non zero
              // If the deltas are zero and the integer is zero this loop would not terminate
              // TODO: What do we do if the loop does not terminate?
              // TODO: this can be improved if the value of the variables is known.
              if (operand1variableDelta.get() - operand2variableDelta.get() == 0) {
                // Returning this works because for any number of iterations less than or equal to 2
                // The loop is simply unrolled. Since because of overflows no extrapolation can be
                // made
                iterationsMaybe =
                    Optional.of(this.getExpressionFactory().from(1, ExpressionType.C));
              }
              break;
            default:
              break;
          }
        }
      }
      return iterationsMaybe;
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  private Optional<GhostCFA> summarizeLoop(
      AIntegerLiteralExpression pIterations,
      AExpression pLoopBoundExpression,
      Loop pLoopStructure,
      CFANode pBeforeWhile) {

    CFANode startNodeGhostCFA = CFANode.newDummyCFANode("STARTGHOST");
    CFANode endNodeGhostCFA = CFANode.newDummyCFANode("ENDGHHOST");

    Optional<Pair<CFANode, CFANode>> unrolledLoopNodesMaybe = pLoopStructure.unrollOutermostLoop();
    if (unrolledLoopNodesMaybe.isEmpty()) {
      return Optional.empty();
    }

    CFANode startUnrolledLoopNode = unrolledLoopNodesMaybe.get().getFirst();
    CFANode endUnrolledLoopNode = unrolledLoopNodesMaybe.get().getSecond();

    startNodeGhostCFA.connectTo(startUnrolledLoopNode);

    CFANode currentSummaryNodeCFA = CFANode.newDummyCFANode("Start Summary Node");

    CFAEdge loopBoundCFAEdge =
        new AssumeEdge(
            "Loop Bound Assumption",
            FileLocation.DUMMY,
            endUnrolledLoopNode,
            currentSummaryNodeCFA,
            pLoopBoundExpression,
            true); // TODO: this may not be the correct way to do this; Review
    loopBoundCFAEdge.connect();

    CFAEdge negatedBoundCFAEdge = ((AssumeEdge) loopBoundCFAEdge).negate();
    negatedBoundCFAEdge.connect();

    CFANode nextSummaryNode = CFANode.newDummyCFANode("Inner Summary Node");

    // Make Summary of Loop

    for (AVariableDeclaration var : pLoopStructure.getModifiedVariables()) {
      Optional<Integer> deltaMaybe = pLoopStructure.getDelta(var.getName());
      if (deltaMaybe.isEmpty()) {
        return Optional.empty();
      }

      Integer delta = deltaMaybe.get();

      // TODO: Refactor expression Factory
      // TODO: the use of a C expression should be replaced for selecting if a Java or C
      // expression should be used in this context
      AExpressionsFactory expressionFactory = new AExpressionsFactory(ExpressionType.C);
      AExpression assignmentExpression =
          (AExpression)
              expressionFactory
                  .from(pIterations)
                  .minus(1)
                  .multiply(delta)
                  .add(var)
                  .assignTo(var)
                  .build();

      CFAEdge dummyEdge =
          new CStatementEdge(
              assignmentExpression.toString(),
              (CStatement) assignmentExpression,
              FileLocation.DUMMY,
              currentSummaryNodeCFA,
              nextSummaryNode);
      dummyEdge.connect();

      currentSummaryNodeCFA = nextSummaryNode;
      nextSummaryNode = CFANode.newDummyCFANode("Inner Summary Node");
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

    Optional<AIntegerLiteralExpression> iterationsMaybe =
        this.loopIterations(loopBoundExpression, loopStructure);

    if (iterationsMaybe.isEmpty()) {
      return Optional.empty();
    }

    AIntegerLiteralExpression iterations = iterationsMaybe.get();
    if (iterations.getValue().intValue() < 0) {
      return Optional.empty();
    }

    Optional<GhostCFA> summarizedLoopMaybe =
        summarizeLoop(iterations, loopBoundExpression, loopStructure, beforeWhile);

    return summarizedLoopMaybe;

  }
}
