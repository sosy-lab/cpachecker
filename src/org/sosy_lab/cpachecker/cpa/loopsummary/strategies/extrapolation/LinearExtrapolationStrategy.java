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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.GhostCFA;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class LinearExtrapolationStrategy extends AbstractExtrapolationStrategy {

  // See
  // https://math.stackexchange.com/questions/2079950/compute-the-n-th-power-of-triangular-3-times3-matrix

  public LinearExtrapolationStrategy(
      final LogManager pLogger, ShutdownNotifier pShutdownNotifier, int strategyIndex) {
    super(pLogger, pShutdownNotifier, strategyIndex);
  }

  @Override
  public Optional<Collection<? extends AbstractState>> summarizeLoopState(
      AbstractState pState, Precision pPrecision, TransferRelation pTransferRelation)
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

    CExpression loopBoundExpression;
    if (loopBoundOptional.isEmpty()) {
      return Optional.empty();
    } else {
      loopBoundExpression = loopBoundOptional.get();
    }

    if (!linearArithmeticExpressionsLoop(loopStartNode, loopBranchIndex)) {
      return Optional.empty();
    }

    // Get the Variables for the Matrix
    Map<String, Map<String, Integer>> loopVariableDependencies =
        getLoopVariableDependencies(loopStartNode, loopBranchIndex);

    Integer loopVariableDelta;
    Optional<Integer> optionalLoopVariableDelta =
        getLoopVariableDelta(loopVariableDependencies, loopBoundExpression);
    if (optionalLoopVariableDelta.isEmpty()) {
      return Optional.empty();
    } else {
      loopVariableDelta = optionalLoopVariableDelta.get();
    }

    if (loopVariableDelta >= 0) {
      return Optional.empty();
    }

    List<String> variableOrdering;
    Optional<List<String>> optionalVariableOrdering = getVariableOrdering(loopVariableDependencies);
    if (optionalVariableOrdering.isEmpty()) {
      return Optional.empty();
    } else {
      variableOrdering = optionalVariableOrdering.get();
    }

    // TODO refactor matrix into its own class in utils, question: Where should it go?
    List<List<Float>> matrixRepresentation =
        getMatrixRepresentation(loopVariableDependencies, variableOrdering);

    Optional<GhostCFA> optionalGhostCFA =
        buildGhostCFA(
            loopVariableDelta, loopBoundExpression, matrixRepresentation, variableOrdering);
    GhostCFA ghostCFA;
    if (optionalGhostCFA.isEmpty()) {
      return Optional.empty();
    } else {
      ghostCFA = optionalGhostCFA.get();
    }

    Collection<? extends AbstractState> realStatesEndCollection =
        transverseGhostCFA(
            ghostCFA, pState, pPrecision, loopStartNode, loopBranchIndex, pTransferRelation);

    return Optional.of(realStatesEndCollection);
  }

  @SuppressWarnings("unused")
  private Optional<GhostCFA> buildGhostCFA(
      Integer pLoopVariableDelta,
      CExpression pLoopBoundExpression,
      List<List<Float>> pMatrixRepresentation,
      List<String> pVariableOrdering) {
    // TODO Auto-generated method stub
    return Optional.empty();
  }

  @SuppressWarnings("unused")
  private List<List<Float>> getMatrixRepresentation(
      Map<String, Map<String, Integer>> pLoopVariableDependencies, List<String> pVariableOrdering) {
    // TODO Auto-generated method stub
    return null;
  }

  @SuppressWarnings("unused")
  private Optional<List<String>> getVariableOrdering(
      Map<String, Map<String, Integer>> pLoopVariableDependencies) {
    // TODO Auto-generated method stub
    return null;
  }

  @SuppressWarnings("unused")
  private Optional<Integer> getLoopVariableDelta(
      Map<String, Map<String, Integer>> pLoopVariableDependencies,
      CExpression pLoopBoundExpression) {
    // TODO Auto-generated method stub
    return null;
  }

  private Map<String, Map<String, Integer>> getLoopVariableDependencies(
      CFANode pLoopStartNode, Integer pLoopBranchIndex) {
    Map<String, Map<String, Integer>> loopVariableDependencies = new HashMap<>();
    HashMap<String, Integer> constVariableHashMap = new HashMap<>();
    constVariableHashMap.put("1", 1);
    loopVariableDependencies.put(
        "1", constVariableHashMap); // Add Constant, which can only be mapped to itself

    CFANode currentNode = pLoopStartNode.getLeavingEdge(pLoopBranchIndex).getSuccessor();

    while (currentNode != pLoopStartNode) {
      assert currentNode.getNumLeavingEdges() == 2;
      CFAEdge edge = currentNode.getLeavingEdge(0);
      CExpressionAssignmentStatement statement =
          (CExpressionAssignmentStatement) ((CStatementEdge) edge).getStatement();

      CExpression rigthSide = statement.getRightHandSide();
      String variableToUpdate = ((CIdExpression) statement.getLeftHandSide()).getName();

      Map<String, Integer> thisVariableUpdate = new HashMap<>();
      updateVariableDependencies(thisVariableUpdate, rigthSide);
      loopVariableDependencies.put(variableToUpdate, thisVariableUpdate);

      currentNode = edge.getSuccessor();
    }

    return loopVariableDependencies;
  }

  @SuppressWarnings("unused")
  private void updateVariableDependencies(
      Map<String, Integer> pLoopVariableDependencies, CExpression pRigthSide) {

    /*if (pRigthSide instanceof CIdExpression || pRigthSide instanceof CIntegerLiteralExpression) {
      return true;
    } else if (expression instanceof CBinaryExpression) {
      String operator = ((CBinaryExpression) expression).getOperator().getOperator();
      CExpression operand1 = ((CBinaryExpression) expression).getOperand1();
      CExpression operand2 = ((CBinaryExpression) expression).getOperand2();
      switch (operator) {
        case "+":
        case "-":
          return linearArithemticExpression(operand1) && linearArithemticExpression(operand2);
        case "*":
          return (linearArithemticExpression(operand1)
                  && operand2 instanceof CIntegerLiteralExpression)
              || (operand1 instanceof CIntegerLiteralExpression
                  && linearArithemticExpression(operand2));
        default:
          return false;
      }
    } else {
      return false;
    }*/

  }
}
