// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary.strategies.extrapolation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.GhostCFA;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class ConstantExtrapolationStrategy extends AbstractExtrapolationStrategy {

  public ConstantExtrapolationStrategy(
      final LogManager pLogger, ShutdownNotifier pShutdownNotifier, int strategyIndex) {
    super(pLogger, pShutdownNotifier, strategyIndex);
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
                  && secondOperand instanceof CIntegerLiteralExpression) {
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
    CFANode startNodeGhostCFA = CFANode.newDummyCFANode("LS1");
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
                CIntegerLiteralExpression.createDummyLiteral(2, CNumericTypes.INT),
                BinaryOperator.MINUS),
            BinaryOperator.LESS_THAN);
    CFAEdge twiceLoopUnrollingConditionEdgeTrue =
        new CAssumeEdge(
            ((CBinaryExpression) loopBound).toString() + "- 2 > 0",
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
            ((CBinaryExpression) loopBound).toString() + "- 2 > 0",
            FileLocation.DUMMY,
            currentStartNodeGhostCFA,
            loopUnrollingCurrentNode,
            loopBoundtwiceUnrollingExpression,
            false);

    // When the loopbound - 2 <= 0 we need to unroll the loop twice

    currentStartNodeGhostCFA.addLeavingEdge(twiceLoopUnrollingConditionEdgeFalse);
    loopUnrollingCurrentNode.addEnteringEdge(twiceLoopUnrollingConditionEdgeFalse);
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
    loopIngoingConditionDummyEdgeFalse =
        overwriteStartEndStateEdge(
            (CAssumeEdge) loopIngoingConditionEdge,
            false,
            loopUnrollingCurrentNode,
            endNodeGhostCFA);
    loopUnrollingCurrentNode.addLeavingEdge(loopIngoingConditionDummyEdgeFalse);
    endNodeGhostCFA.addEnteringEdge(loopIngoingConditionDummyEdgeFalse);

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

    loopIngoingConditionDummyEdgeFalse =
        overwriteStartEndStateEdge(
            (CAssumeEdge) loopIngoingConditionEdge,
            false,
            currentStartNodeGhostCFA,
            endNodeGhostCFA);
    currentStartNodeGhostCFA.addLeavingEdge(loopIngoingConditionDummyEdgeFalse);
    endNodeGhostCFA.addEnteringEdge(loopIngoingConditionDummyEdgeFalse);

    return Optional.of(new GhostCFA(startNodeGhostCFA, endNodeGhostCFA));
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
  public Optional<Collection<? extends AbstractState>> summarizeLoopState(
      final AbstractState pState, final Precision pPrecision, TransferRelation pTransferRelation)
      throws CPATransferException, InterruptedException {

    CFANode loopStartNode = AbstractStates.extractLocation(pState);

    if (loopStartNode.getNumLeavingEdges() != 1) {
      return Optional.empty();
    }

    if (!loopStartNode.getLeavingEdge(0).getDescription().equals("while")) {
      return Optional.empty();
    }

    loopStartNode = loopStartNode.getLeavingEdge(0).getSuccessor();

    Optional<Integer> loopBranchIndexOptional = getLoopBranchIndex(loopStartNode);
    Integer loopBranchIndex;

    if (loopBranchIndexOptional.isEmpty()) {
      return Optional.empty();
    } else {
      loopBranchIndex = loopBranchIndexOptional.orElseThrow();
    }

    Optional<CExpression> loopBoundOptional = bound(loopStartNode);
    CExpression loopBound;
    if (loopBoundOptional.isEmpty()) {
      return Optional.empty();
    } else {
      loopBound = loopBoundOptional.orElseThrow();
    }

    if (!linearArithmeticExpressionsLoop(loopStartNode, loopBranchIndex)) {
      return Optional.empty();
    }

    Map<String, Integer> loopVariableDelta = getLoopVariableDeltas(loopStartNode, loopBranchIndex);

    int boundDelta = boundDelta(loopVariableDelta, loopBound);
    if (boundDelta >= 0) { // TODO How do you treat non Termination?
      return Optional.empty();
    }

    GhostCFA ghostCFA;
    Optional<GhostCFA> ghostCFASuccess =
        summaryCFA(
            loopStartNode,
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

    Collection<? extends AbstractState> realStatesEndCollection =
        transverseGhostCFA(
            ghostCFA, pState, pPrecision, loopStartNode, loopBranchIndex, pTransferRelation);

    return Optional.of(realStatesEndCollection);
  }
}
