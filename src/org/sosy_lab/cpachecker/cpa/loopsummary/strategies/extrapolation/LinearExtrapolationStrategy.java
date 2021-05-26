// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary.strategies.extrapolation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
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
      loopBoundExpression = loopBoundOptional.orElseThrow();
    }

    if (!linearArithmeticExpressionsLoop(loopStartNode, loopBranchIndex)) {
      return Optional.empty();
    }

    // Get the Variables for the Matrix
    Map<String, Map<String, Integer>> loopVariableDependencies =
        getLoopVariableDependencies(loopStartNode, loopBranchIndex);
    Map<String, Integer> constantMap = new HashMap<>();
    constantMap.put("1", 1);
    loopVariableDependencies.put("1", constantMap);

    Integer loopVariableDelta;
    Optional<Integer> optionalLoopVariableDelta =
        getLoopVariableDelta(loopVariableDependencies, loopBoundExpression);
    if (optionalLoopVariableDelta.isEmpty()) {
      return Optional.empty();
    } else {
      loopVariableDelta = optionalLoopVariableDelta.orElseThrow();
    }

    if (loopVariableDelta >= 0) {
      return Optional.empty();
    }

    List<String> variableOrdering;
    Optional<List<String>> optionalVariableOrdering = getVariableOrdering(loopVariableDependencies);
    if (optionalVariableOrdering.isEmpty()) {
      return Optional.empty();
    } else {
      variableOrdering = optionalVariableOrdering.orElseThrow();
    }

    // TODO refactor matrix into its own class in utils, question: Where should it go?
    // See https://www.baeldung.com/java-matrix-multiplication for the dependencies
    // TODO Improve Data type from Integer to Float, since the generation must be the same
    List<List<Integer>> matrixRepresentation =
        getMatrixRepresentation(loopVariableDependencies, variableOrdering);

    Optional<GhostCFA> optionalGhostCFA =
        buildGhostCFA(
            loopVariableDelta, loopBoundExpression, matrixRepresentation, variableOrdering);
    GhostCFA ghostCFA;
    if (optionalGhostCFA.isEmpty()) {
      return Optional.empty();
    } else {
      ghostCFA = optionalGhostCFA.orElseThrow();
    }

    Collection<? extends AbstractState> realStatesEndCollection =
        transverseGhostCFA(
            ghostCFA, pState, pPrecision, loopStartNode, loopBranchIndex, pTransferRelation);

    return Optional.of(realStatesEndCollection);
  }

  private List<List<Integer>> matrixPower(List<List<Integer>> matrix, Integer power) {
    List<List<Integer>> resultMatrix = new ArrayList<>(matrix);

    List<List<Integer>> tmpMatrix = new ArrayList<>(matrix);
    for (int i = 0; i < matrix.size(); i++) {
      tmpMatrix.set(i, new ArrayList<>(matrix.get(i)));
      resultMatrix.set(i, new ArrayList<>(matrix.get(i)));
    }

    for (int t = 0; t < power; t++) {
      for (int i = 0; i < matrix.size(); i++) {
        for (int j = 0; j < matrix.get(0).size(); j++) {
          int sum = 0;
          for (int k = 0; k < matrix.get(0).size(); k++) {
            sum += tmpMatrix.get(i).get(k) * matrix.get(k).get(j);
          }
          resultMatrix.get(i).set(j, sum);
        }
      }
      tmpMatrix = new ArrayList<>(resultMatrix);
      for (int i = 0; i < matrix.size(); i++) {
        tmpMatrix.set(i, new ArrayList<>(resultMatrix.get(i)));
      }
    }
    return resultMatrix;
  }

  private Optional<GhostCFA> buildGhostCFA(
      Integer pLoopVariableDelta,
      CExpression pLoopBoundExpression,
      List<List<Integer>> pMatrixRepresentation,
      List<String> pVariableOrdering) {

    // Generate the closed Expression for each Variable by multiplying Matrices according to
    // https://math.stackexchange.com/questions/2079950/compute-the-n-th-power-of-triangular-3-times3-matrix
    Integer maximalPower = pVariableOrdering.size() - 1;

    CType calculationType = ((CBinaryExpression) pLoopBoundExpression).getCalculationType();
    CType expressionType = ((CBinaryExpression) pLoopBoundExpression).getExpressionType();
    CExpression loopIterations =
        new CBinaryExpression(
            FileLocation.DUMMY,
            expressionType,
            calculationType,
            pLoopBoundExpression,
            CIntegerLiteralExpression.createDummyLiteral(
                2 * ((long) pLoopVariableDelta), CNumericTypes.INT),
            BinaryOperator.MINUS);

    CBinaryExpression loopBoundtwiceUnrollingExpression =
        new CBinaryExpression(
            FileLocation.DUMMY,
            expressionType,
            calculationType,
            CIntegerLiteralExpression.createDummyLiteral(0, CNumericTypes.INT),
            loopIterations,
            BinaryOperator.LESS_THAN);

    List<CExpression> variableExpressions = new ArrayList<>();
    List<CExpression> leftHandSideVariableAssignments = new ArrayList<>();

    List<List<Integer>> currentMatrix = pMatrixRepresentation;
    CExpression currentBoundExpression = loopBoundtwiceUnrollingExpression;
    for (int i = 0; i <= maximalPower; i++) {
      if (i == 0) {
        for (int j = 0; j < pVariableOrdering.size(); j ++) {
          if (j != pVariableOrdering.size() - 1) {
            CVariableDeclaration pc =
                new CVariableDeclaration(
                    FileLocation.DUMMY,
                    true,
                    CStorageClass.EXTERN,
                    CNumericTypes.INT, // TODO improve this
                    pVariableOrdering.get(j),
                    pVariableOrdering.get(j),
                    pVariableOrdering.get(j),
                    null);
            variableExpressions.add(new CIdExpression(FileLocation.DUMMY, pc));
            leftHandSideVariableAssignments.add(variableExpressions.get(j));
          } else {
            variableExpressions.add(
                CIntegerLiteralExpression.createDummyLiteral(1, CNumericTypes.INT));
          }
        }
      } else {
        for (int j = 0; j < pVariableOrdering.size(); j++) {
          CExpression expressionThisVariable =
              new CBinaryExpression(
                  FileLocation.DUMMY,
                  expressionType,
                  calculationType,
                  CIntegerLiteralExpression.createDummyLiteral(
                      pMatrixRepresentation.get(j).get(0), CNumericTypes.INT),
                  variableExpressions.get(0),
                  BinaryOperator.MULTIPLY);
          for (int k = 1; k < pVariableOrdering.size(); k++) {
            expressionThisVariable =
                new CBinaryExpression(
                    FileLocation.DUMMY,
                    expressionType,
                    calculationType,
                    new CBinaryExpression(
                        FileLocation.DUMMY,
                        expressionType,
                        calculationType,
                        CIntegerLiteralExpression.createDummyLiteral(
                            currentMatrix.get(j).get(k), CNumericTypes.INT),
                        variableExpressions.get(k),
                        BinaryOperator.MULTIPLY),
                    expressionThisVariable,
                    BinaryOperator.PLUS);
          }
          leftHandSideVariableAssignments.set(
              j,
              new CBinaryExpression(
                  FileLocation.DUMMY,
                  expressionType,
                  calculationType,
                  new CBinaryExpression(
                      FileLocation.DUMMY,
                      expressionType,
                      calculationType,
                      currentBoundExpression,
                      expressionThisVariable,
                      BinaryOperator.MULTIPLY),
                  leftHandSideVariableAssignments.get(j),
                  BinaryOperator.PLUS));
        }
        currentBoundExpression =
            new CBinaryExpression(
                FileLocation.DUMMY,
                expressionType,
                calculationType,
                currentBoundExpression,
                loopBoundtwiceUnrollingExpression,
                BinaryOperator.MULTIPLY);
        currentMatrix = matrixPower(pMatrixRepresentation, i);
      }
    }

    // Unroll Loop once for plus minus relation, since the values are aggregated
    // TODO

    // Set Variables to the calculated Expressions
    // TODO

    // Unroll Loop again for plus minus relation, since the values are aggregated
    // TODO

    return Optional.empty();
  }

  private List<List<Integer>> getMatrixRepresentation(
      Map<String, Map<String, Integer>> pLoopVariableDependencies, List<String> pVariableOrdering) {
    List<List<Integer>> matrixRepresentation = new ArrayList<>();

    for (String varNameRow : pVariableOrdering) {
      List<Integer> matrixRow = new ArrayList<>();
      for (String varNameColumn : pVariableOrdering) {
        if (pLoopVariableDependencies.get(varNameRow).containsKey(varNameColumn)) {
          matrixRow.add(pLoopVariableDependencies.get(varNameRow).get(varNameColumn));
        } else {
          matrixRow.add(0);
        }
      }
      matrixRepresentation.add(matrixRow);
    }

    return matrixRepresentation;
  }

  private Optional<List<String>> getVariableOrdering(
      Map<String, Map<String, Integer>> pLoopVariableDependencies) {
    // Here we go through the dependancies to find some sorting, which generates a upper diagonal
    // matrix.

    List<String> variableOrdering = new ArrayList<>(); // Will order them in inverse order
    Set<String> alreadyOrderedVariables = new HashSet<>();

    variableOrdering.add("1");
    alreadyOrderedVariables.add("1");
    Integer maxIterationCounter = 0;
    boolean updated = true;

    while (variableOrdering.size() != pLoopVariableDependencies.size()) {
      if (maxIterationCounter > pLoopVariableDependencies.keySet().size()) {
        return Optional.empty();
      }
      if (!updated) {
        return Optional.empty();
      }
      updated = false;
      for (Entry<String, Map<String, Integer>> entry : pLoopVariableDependencies.entrySet()) {
        if (entry.getValue().size() <= variableOrdering.size() + 1
            && !alreadyOrderedVariables.contains(entry.getKey())) {
          Set<String> setDifference = new HashSet<>(alreadyOrderedVariables);
          setDifference.removeAll(entry.getValue().keySet());
          if (setDifference.size() == 1) { // Only a single variable has been added
            for (String variableName : setDifference) {
              alreadyOrderedVariables.add(variableName);
              variableOrdering.add(variableName);
              updated = true;
            }
          }
        }
      }
      maxIterationCounter += 1;
    }
    Collections.reverse(variableOrdering);

    return Optional.of(variableOrdering);
  }

  private Optional<Integer> getLoopVariableDelta(
      Map<String, Map<String, Integer>> pLoopVariableDependencies,
      CExpression pLoopBoundExpression) {
    if (!(pLoopBoundExpression instanceof CBinaryExpression)) {
      // The pLoopBoundExpression should be a CBinaryExpression with the bound format
      // 0 < pLoopBoundExpression
      return Optional.empty();
    } else {
      String variableName = "";
      CExpression operand1 = ((CBinaryExpression) pLoopBoundExpression).getOperand1();
      CExpression operand2 = ((CBinaryExpression) pLoopBoundExpression).getOperand2();
      if (operand1 instanceof CIdExpression) {
        variableName = ((CIdExpression) operand1).getName();
      } else if (operand2 instanceof CIdExpression) {
        variableName = ((CIdExpression) operand2).getName();
      } else {
        logger.log(
            Level.WARNING,
            "Somethig went wrong when building the bound of the loop. Since none of the terms is a Variable");
        return Optional.empty();
      }

      if (!pLoopVariableDependencies.containsKey(variableName)) {
        return Optional.empty();
      } else {
        Map<String, Integer> variableDependancy = pLoopVariableDependencies.get(variableName);
        if (!variableDependancy.containsKey("1") || variableDependancy.keySet().size() != 1) {
          return Optional.empty();
        } else {
          return Optional.of(variableDependancy.get("1"));
        }
      }
    }
  }

  private Map<String, Map<String, Integer>> getLoopVariableDependencies(
      CFANode pLoopStartNode, Integer pLoopBranchIndex) {
    Map<String, Map<String, Integer>> loopVariableDependencies = new HashMap<>();
    Map<String, Integer> constVariableHashMap = new HashMap<>();
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

      Map<String, Integer> thisVariableUpdate;
      if (loopVariableDependencies.containsKey(variableToUpdate)) {
        thisVariableUpdate = loopVariableDependencies.get(variableToUpdate);
      } else {
        thisVariableUpdate = new HashMap<>();
      }
      updateVariableDependencies(thisVariableUpdate, rigthSide);
      loopVariableDependencies.put(variableToUpdate, thisVariableUpdate);

      currentNode = edge.getSuccessor();
    }

    return loopVariableDependencies;
  }


  private void updateVariableDependencies(
      Map<String, Integer> pLoopVariableDependencies, CExpression pRigthSide) {

    if (pRigthSide instanceof CIntegerLiteralExpression) {
      if (pLoopVariableDependencies.containsKey("1")) {
        pLoopVariableDependencies.put(
            "1",
            (int)
                (pLoopVariableDependencies.get("1")
                    + ((CIntegerLiteralExpression) pRigthSide).getValue().longValueExact()));
      } else {
        pLoopVariableDependencies.put(
            "1", (int) ((CIntegerLiteralExpression) pRigthSide).getValue().longValueExact());
      }
    } else if (pRigthSide instanceof CIdExpression) {
      if (pLoopVariableDependencies.containsKey(((CIdExpression) pRigthSide).getName())) {
        pLoopVariableDependencies.put(
            ((CIdExpression) pRigthSide).getName(),
            pLoopVariableDependencies.get(((CIdExpression) pRigthSide).getName()) + 1);
      } else {
        pLoopVariableDependencies.put(((CIdExpression) pRigthSide).getName(), 1);
      }
    } else if (pRigthSide instanceof CBinaryExpression) {
      String operator = ((CBinaryExpression) pRigthSide).getOperator().getOperator();
      CExpression operand1 = ((CBinaryExpression) pRigthSide).getOperand1();
      CExpression operand2 = ((CBinaryExpression) pRigthSide).getOperand2();
      Map<String, Integer> operand1Map = new HashMap<>();
      Map<String, Integer> operand2Map = new HashMap<>();
      updateVariableDependencies(operand1Map, operand1);
      updateVariableDependencies(operand2Map, operand2);
      switch (operator) {
        case "+":
          for (Entry<String, Integer> e : operand1Map.entrySet()) {
            String k = e.getKey();
            if (pLoopVariableDependencies.keySet().contains(k)) {
              pLoopVariableDependencies.put(
                  k, pLoopVariableDependencies.get(k) + operand1Map.get(k));
            } else {
              pLoopVariableDependencies.put(k, operand1Map.get(k));
            }
          }

          for (Entry<String, Integer> e : operand2Map.entrySet()) {
            String k = e.getKey();
            if (pLoopVariableDependencies.containsKey(k)) {
              pLoopVariableDependencies.put(
                  k, pLoopVariableDependencies.get(k) + operand2Map.get(k));
            } else {
              pLoopVariableDependencies.put(k, operand2Map.get(k));
            }
          }
          break;
        case "-":
          for (Entry<String, Integer> e : operand1Map.entrySet()) {
            String k = e.getKey();
            if (pLoopVariableDependencies.containsKey(k)) {
              pLoopVariableDependencies.put(
                  k, pLoopVariableDependencies.get(k) + operand1Map.get(k));
            } else {
              pLoopVariableDependencies.put(k, operand1Map.get(k));
            }
          }

          for (Entry<String, Integer> e : operand2Map.entrySet()) {
            String k = e.getKey();
            if (pLoopVariableDependencies.containsKey(k)) {
              pLoopVariableDependencies.put(
                  k, pLoopVariableDependencies.get(k) - operand2Map.get(k));
            } else {
              pLoopVariableDependencies.put(k, operand2Map.get(k));
            }
          }
          break;
        case "*":
          int value = 0;
          Map<String, Integer> valuesMap = new HashMap<>();
          if (operand1Map.keySet().size() == 1 && operand1Map.containsKey("1")) {
            value = operand1Map.get("1");
            valuesMap = operand2Map;
          } else if (operand1Map.keySet().size() == 1 && operand1Map.containsKey("1")) {
            value = operand1Map.get("1");
            valuesMap = operand2Map;
          } else {
            logger.log(
                Level.WARNING,
                "The Expression "
                    + pRigthSide.toString()
                    + " was interpreted as a Linear Arithmetic Expression, which it is not. Because two linear expressions are being multiplied.");
          }

          for (Entry<String, Integer> e : valuesMap.entrySet()) {
            String k = e.getKey();
            if (pLoopVariableDependencies.containsKey(k)) {
              pLoopVariableDependencies.put(
                  k, pLoopVariableDependencies.get(k) + value * valuesMap.get(k));
            } else {
              pLoopVariableDependencies.put(k, value * valuesMap.get(k));
            }
          }
          break;
        default:
          logger.log(
              Level.WARNING,
              "The Expression "
                  + pRigthSide.toString()
                  + " was interpreted as a Linear Arithmetic Expression, which it is not. Because some other Operator than +, -, * is used");
          return;
      }
    }
  }
}
