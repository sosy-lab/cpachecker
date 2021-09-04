// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.GhostCFA;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategiesEnum;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.ExpressionVisitors.AggregateConstantsVisitor;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.ExpressionVisitors.LoopVariableDeltaVisitor;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependencyInterface;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

public class ConstantExtrapolationStrategy extends AbstractLoopExtrapolationStrategy {

  public ConstantExtrapolationStrategy(
      final LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      StrategyDependencyInterface pStrategyDependencies,
      CFA pCFA) {
    super(pLogger, pShutdownNotifier, pStrategyDependencies, pCFA);
  }

  protected Map<String, Integer> getLoopVariableDeltas(
      CFANode loopStartNode, final Integer loopBranchIndex) {
    Map<String, Integer> loopVariableDelta = new HashMap<>();
    loopStartNode = loopStartNode.getLeavingEdge(loopBranchIndex).getSuccessor();
    // Calculate deltas in one Loop Iteration
    CFANode currentNode = loopStartNode;
    boolean initial = true;
    while (currentNode != loopStartNode || initial) {
      initial = false;
      CFAEdge edge = currentNode.getLeavingEdge(0);
      if (edge instanceof CStatementEdge) {
        CStatement statement = ((CStatementEdge) edge).getStatement();
        CExpression leftSide = ((CExpressionAssignmentStatement) statement).getLeftHandSide();
        CExpression rigthSide = ((CExpressionAssignmentStatement) statement).getRightHandSide();
        if (leftSide instanceof CIdExpression && rigthSide instanceof CBinaryExpression) {
            Integer value = 0;
          CExpression operand1 = ((CBinaryExpression) rigthSide).getOperand1();
            CExpression operand2 = ((CBinaryExpression) rigthSide).getOperand2();
            if (operand1 instanceof CIntegerLiteralExpression) {
              value = ((CIntegerLiteralExpression) operand1).getValue().intValue();
            } else if (operand2 instanceof CIntegerLiteralExpression) {
              value = ((CIntegerLiteralExpression) operand2).getValue().intValue();
            }

            switch (((CBinaryExpression) rigthSide).getOperator().getOperator()) {
              case "+":
              if (loopVariableDelta.containsKey(((CIdExpression) leftSide).getName())) {
                loopVariableDelta.put(
                    ((CIdExpression) leftSide).getName(),
                    value + loopVariableDelta.get(((CIdExpression) leftSide).getName()));
                } else {
                loopVariableDelta.put(((CIdExpression) leftSide).getName(), value);
                }
                break;
              case "-":
              if (loopVariableDelta.containsKey(((CIdExpression) leftSide).getName())) {
                loopVariableDelta.put(
                    ((CIdExpression) leftSide).getName(),
                    -value + loopVariableDelta.get(((CIdExpression) leftSide).getName()));
              } else {
                loopVariableDelta.put(((CIdExpression) leftSide).getName(), -value);
              }
                break;
              default:
                break;
            }
        }
      }
      currentNode = edge.getSuccessor();
    }
    return loopVariableDelta;
  }

  @Override
  protected boolean linearArithmeticExpressionsLoop(final CFANode pLoopStartNode, int branchIndex) {
    CFANode nextNode0 = pLoopStartNode.getLeavingEdge(branchIndex).getSuccessor();
    boolean nextNode0Valid = true;
    while (nextNode0 != pLoopStartNode && nextNode0Valid) {
      CFAEdge currentEdge = nextNode0.getLeavingEdge(0);
      if (nextNode0Valid
          && nextNode0.getNumLeavingEdges() == 1
          && linearArithmeticExpressionEdge(currentEdge)) {

        if (currentEdge instanceof CStatementEdge) {

          CStatement statement = ((CStatementEdge) currentEdge).getStatement();
          if (!(statement instanceof CExpressionAssignmentStatement)) {
            return false;
          }

          CExpression leftSide = ((CExpressionAssignmentStatement) statement).getLeftHandSide();
          CExpression rigthSide = ((CExpressionAssignmentStatement) statement).getRightHandSide();

          if (rigthSide instanceof CBinaryExpression) {
            CExpression firstOperand = ((CBinaryExpression) rigthSide).getOperand1();
            CExpression secondOperand = ((CBinaryExpression) rigthSide).getOperand2();
            if (firstOperand instanceof CIdExpression) {
              if (!((CIdExpression) firstOperand)
                      .getName()
                      .equals(((CIdExpression) leftSide).getName())
                  && secondOperand instanceof CIntegerLiteralExpression) {
                return false;
              }
            } else if (secondOperand instanceof CIdExpression) {
              if (!((CIdExpression) secondOperand)
                      .getName()
                      .equals(((CIdExpression) leftSide).getName())
                  && firstOperand instanceof CIntegerLiteralExpression) {
                return false;
              }
            } else {
              return false;
            }
          } else if (rigthSide instanceof CIdExpression) {
            if (!((CIdExpression) rigthSide)
                .getName()
                .equals(((CIdExpression) leftSide).getName())) {
              return false;
            }
          } else {
            // TODO: sometimes rigthSide can be an instanceof CIntegerLiteralExpression
            // Improve this, since this kind of summary should be capable of dealing with this case
            return false;
          }
        }
        nextNode0 = nextNode0.getLeavingEdge(0).getSuccessor();
      } else {
        nextNode0Valid = false;
      }
      if (nextNode0 == pLoopStartNode) {
        return true;
      }
    }
    return false;
  }

  private int boundDelta(
      final Map<String, Integer> loopVariableDelta, final CExpression loopBound) {
    if (!(loopBound instanceof CBinaryExpression)) {
      if (loopBound instanceof CIdExpression) {
        if (loopVariableDelta.containsKey(((CIdExpression) loopBound).getName())) {
          return loopVariableDelta.get(((CIdExpression) loopBound).getName());
        } else {
          return 0;
        }
      } else {
        return 0;
      }
    } else {
      switch (((CBinaryExpression) loopBound).getOperator().getOperator()) {
        case "+":
          return boundDelta(loopVariableDelta, ((CBinaryExpression) loopBound).getOperand1())
              + boundDelta(loopVariableDelta, ((CBinaryExpression) loopBound).getOperand2());
        case "-":
          return boundDelta(loopVariableDelta, ((CBinaryExpression) loopBound).getOperand1())
              - boundDelta(loopVariableDelta, ((CBinaryExpression) loopBound).getOperand2());
        default:
          return 0;
      }
    }
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

  @Override
  protected boolean linearArithemticExpression(final CExpression expression) {
    if (expression instanceof CIdExpression) {
      return true;
    } else if (expression instanceof CBinaryExpression) {
      String operator = ((CBinaryExpression) expression).getOperator().getOperator();
      CExpression operand1 = ((CBinaryExpression) expression).getOperand1();
      CExpression operand2 = ((CBinaryExpression) expression).getOperand2();
      switch (operator) {
        case "+":
        case "-":
          return ((operand1 instanceof CIdExpression)
                  && (operand2 instanceof CIntegerLiteralExpression))
              || ((operand2 instanceof CIdExpression)
                  && (operand1 instanceof CIntegerLiteralExpression));
        default:
          return false;
      }
    } else {
      return false;
    }
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

    Optional<Integer> iterationsMaybe = Optional.empty();
    try {
      // TODO For now it only works for c programs
      if (loopBoundExpression instanceof CBinaryExpression) {
        LoopVariableDeltaVisitor<Exception> variableVisitor =
            new LoopVariableDeltaVisitor<>(loopStructure);
        AggregateConstantsVisitor<Exception> constantsVisitor =
            new AggregateConstantsVisitor<>(Optional.of(loopStructure.getLoopIncDecVariables()));

        Optional<Integer> operand1variableDelta =
            ((CBinaryExpression) loopBoundExpression).getOperand1().accept(variableVisitor);
        Optional<Integer> operand2variableDelta =
            ((CBinaryExpression) loopBoundExpression).getOperand2().accept(variableVisitor);
        Optional<Integer> operand1Constants =
            ((CBinaryExpression) loopBoundExpression).getOperand1().accept(constantsVisitor);
        Optional<Integer> operand2Constants =
            ((CBinaryExpression) loopBoundExpression).getOperand2().accept(constantsVisitor);

        if (operand1variableDelta.isPresent()
            && operand2variableDelta.isPresent()
            && operand1Constants.isPresent()
            && operand2Constants.isPresent()) {

          switch (((CBinaryExpression) loopBoundExpression).getOperator()) {
            case EQUALS:
              // Should iterate at most once if the Deltas are non zero
              // If the deltas are zero and the integer is zero this loop would not terminate
              // TODO: What do we do if the loop does not terminate?
              // TODO: this can be improved if the value of the variables is known.
              if (operand1variableDelta.get() - operand2variableDelta.get() != 0) {
                // Returning this works because for any number of iterations less than or equal to 2
                // The loop is simply unrolled. Since because of overflows no extrapolation can be
                // made
                iterationsMaybe = Optional.of(1);
              }
              break;
            case GREATER_EQUAL:
              // TODO Revise
              if (operand1variableDelta.get() - operand2variableDelta.get() < 0) {
                iterationsMaybe =
                    Optional.of(
                        (operand1Constants.get() - operand2Constants.get())
                                / -(operand1variableDelta.get() - operand2variableDelta.get())
                            + 1);
              }
              break;
            case GREATER_THAN:
              if (operand1variableDelta.get() - operand2variableDelta.get() < 0) {
                iterationsMaybe =
                    Optional.of(
                        (operand1Constants.get() - operand2Constants.get())
                            / -(operand1variableDelta.get() - operand2variableDelta.get()));
              }
              break;
            case LESS_EQUAL:
              if (operand2variableDelta.get() - operand1variableDelta.get() < 0) {
                iterationsMaybe =
                    Optional.of(
                        (operand2Constants.get() - operand1Constants.get())
                                / -(operand2variableDelta.get() - operand1variableDelta.get())
                            + 1);
              }
              break;
            case LESS_THAN:
              if (operand2variableDelta.get() - operand1variableDelta.get() < 0) {
                iterationsMaybe =
                    Optional.of(
                        (operand2Constants.get() - operand1Constants.get())
                            / -(operand2variableDelta.get() - operand1variableDelta.get()));
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
                iterationsMaybe = Optional.of(1);
              }
              break;
            default:
              break;
          }
        }
      }
    } catch (Exception e) {
      return Optional.empty();
    }

    if (iterationsMaybe.isEmpty()) {
      return Optional.empty();
    }
    Integer iterations = iterationsMaybe.get();
    if (iterations < 0) {
      return Optional.empty();
    }

    return Optional.empty();

    /*Optional<Integer> loopBranchIndexOptional = getLoopBranchIndex(loopStartNodeLocal);
    Integer loopBranchIndex;

    if (loopBranchIndexOptional.isEmpty()) {
      return Optional.empty();
    } else {
      loopBranchIndex = loopBranchIndexOptional.orElseThrow();
    }

    Optional<CExpression> loopBoundOptional = bound(loopStartNodeLocal);
    CExpression loopBound;
    if (loopBoundOptional.isEmpty()) {
      return Optional.empty();
    } else {
      loopBound = loopBoundOptional.orElseThrow();
    }

    if (!linearArithmeticExpressionsLoop(loopStartNodeLocal, loopBranchIndex)) {
      return Optional.empty();
    }

    Map<String, Integer> loopVariableDelta = getLoopVariableDeltas(loopStartNodeLocal, loopBranchIndex);

    int boundDelta = boundDelta(loopVariableDelta, loopBound);
    if (boundDelta >= 0) { // TODO How do you treat non Termination?
      return Optional.empty();
    }

    GhostCFA ghostCFA;
    Optional<GhostCFA> ghostCFASuccess =
        summaryCFA(
            loopStartNodeLocal,
            loopVariableDelta,
            loopBound,
            Math.abs(boundDelta),
            boundDelta,
            loopBranchIndex);

    if (ghostCFASuccess.isEmpty()) {
      return Optional.empty();
    } else {
      ghostCFA = ghostCFASuccess.orElseThrow();
    }

    return Optional.of(ghostCFA);*/
  }
}
