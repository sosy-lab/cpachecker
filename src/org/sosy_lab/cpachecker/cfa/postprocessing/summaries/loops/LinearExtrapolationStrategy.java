// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops;

import java.util.ArrayList;
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
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.GhostCFA;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependencyInterface;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class LinearExtrapolationStrategy extends AbstractLoopExtrapolationStrategy {

  // See
  // https://math.stackexchange.com/questions/2079950/compute-the-n-th-power-of-triangular-3-times3-matrix

  public LinearExtrapolationStrategy(
      final LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      StrategyDependencyInterface pStrategyDependencies) {
    super(pLogger, pShutdownNotifier, pStrategyDependencies);
  }

  @Override
  public Optional<GhostCFA> summarize(final CFANode loopStartNode) {

    if (loopStartNode.getNumLeavingEdges() != 1) {
      return Optional.empty();
    }

    if (!loopStartNode.getLeavingEdge(0).getDescription().equals("while")) {
      return Optional.empty();
    }

    CFANode loopStartNodeLocal = loopStartNode.getLeavingEdge(0).getSuccessor();

    Optional<Integer> loopBranchIndexOptional = getLoopBranchIndex(loopStartNodeLocal);
    Integer loopBranchIndex;

    if (loopBranchIndexOptional.isEmpty()) {
      return Optional.empty();
    } else {
      loopBranchIndex = loopBranchIndexOptional.orElseThrow();
    }

    Optional<CExpression> loopBoundOptional = bound(loopStartNodeLocal);

    CExpression loopBoundExpression;
    if (loopBoundOptional.isEmpty()) {
      return Optional.empty();
    } else {
      loopBoundExpression = loopBoundOptional.orElseThrow();
    }

    if (!linearArithmeticExpressionsLoop(loopStartNodeLocal, loopBranchIndex)) {
      return Optional.empty();
    }

    // Get the Variables for the Matrix
    Map<String, Map<String, Integer>> loopVariableDependencies =
        getLoopVariableDependencies(loopStartNodeLocal, loopBranchIndex);
    Map<String, Integer> constantMap = new HashMap<>();
    constantMap.put("1", 1);
    loopVariableDependencies.put("1", constantMap);


    // For simplicity check that all self dependencies are 1
    // This can be improved upon, but for the start this implementation will do
    // TODO Improve to the general case where self dependencies are not 1
    for (Entry<String, Map<String, Integer>> e: loopVariableDependencies.entrySet()) {
      if (e.getValue().get(e.getKey()) != 1) {
        return Optional.empty();
      }
    }

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
            loopVariableDelta,
            loopBoundExpression,
            matrixRepresentation,
            variableOrdering,
            loopStartNodeLocal,
            loopBranchIndex);
    GhostCFA ghostCFA;
    if (optionalGhostCFA.isEmpty()) {
      return Optional.empty();
    } else {
      ghostCFA = optionalGhostCFA.orElseThrow();
    }

    return Optional.of(ghostCFA);
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

  private List<CExpression> generateAggregatedVariableAssignments(
      CExpression loopIterations,
      List<List<Integer>> pMatrixRepresentation,
      List<String> pVariableOrdering) {

    CType calculationType =
        ((CBinaryExpression) loopIterations).getCalculationType(); // TODO Improve this
    CType expressionType =
        ((CBinaryExpression) loopIterations).getExpressionType(); // TODO Improve this

    // Generate the closed Expression for each Variable by multiplying Matrices according to
    // https://math.stackexchange.com/questions/2079950/compute-the-n-th-power-of-triangular-3-times3-matrix
    Integer maximalPower = pVariableOrdering.size() - 1;

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
        // Update n choose k to n choose k + 1
        currentBoundExpression =
            new CBinaryExpression(
                FileLocation.DUMMY,
                expressionType,
                calculationType,
                currentBoundExpression,
                new CBinaryExpression(
                    FileLocation.DUMMY,
                    expressionType,
                    calculationType,
                    new CBinaryExpression(
                        FileLocation.DUMMY,
                        expressionType,
                        calculationType,
                        loopBoundtwiceUnrollingExpression,
                        CIntegerLiteralExpression.createDummyLiteral(i, CNumericTypes.INT),
                        BinaryOperator.MINUS),
                    CIntegerLiteralExpression.createDummyLiteral(i, CNumericTypes.INT),
                    BinaryOperator.DIVIDE),
                BinaryOperator.MULTIPLY);
        currentMatrix = matrixPower(pMatrixRepresentation, i);
      }
    }
    return leftHandSideVariableAssignments;
  }

  private Optional<GhostCFA> buildGhostCFA(
      Integer pLoopVariableDelta,
      CExpression pLoopBoundExpression,
      List<List<Integer>> pMatrixRepresentation,
      List<String> pVariableOrdering,
      CFANode loopStartNode,
      Integer loopBranchIndex) {

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

    List<CExpression> leftHandSideVariableAssignments =
        generateAggregatedVariableAssignments(
            loopIterations, pMatrixRepresentation, pVariableOrdering);

    // Initialize Ghost CFA

    CFANode startNodeGhostCFA = CFANode.newDummyCFANode("LSSTARTGHHOST");
    CFANode endNodeGhostCFA = CFANode.newDummyCFANode("LSENDGHHOST");
    CFANode currentEndNodeGhostCFA = CFANode.newDummyCFANode("LS2");
    CFAEdge loopIngoingConditionEdge = loopStartNode.getLeavingEdge(loopBranchIndex);
    CFAEdge loopIngoingConditionDummyEdgeTrue =
        overwriteStartEndStateEdge(
            (CAssumeEdge) loopIngoingConditionEdge,
            true,
            startNodeGhostCFA,
            currentEndNodeGhostCFA);
    CFAEdge loopIngoingConditionDummyEdgeFalse =
        overwriteStartEndStateEdge(
            (CAssumeEdge) loopIngoingConditionEdge, false, startNodeGhostCFA, endNodeGhostCFA);
    startNodeGhostCFA.addLeavingEdge(loopIngoingConditionDummyEdgeTrue);
    currentEndNodeGhostCFA.addEnteringEdge(loopIngoingConditionDummyEdgeTrue);
    startNodeGhostCFA.addLeavingEdge(loopIngoingConditionDummyEdgeFalse);
    endNodeGhostCFA.addEnteringEdge(loopIngoingConditionDummyEdgeFalse);
    CFANode currentStartNodeGhostCFA = currentEndNodeGhostCFA;
    currentEndNodeGhostCFA = CFANode.newDummyCFANode("LS3");

    // Unroll Loop once for plus minus relation, since the values are aggregated
    CFAEdge twiceLoopUnrollingAssumptionTrue =
        new CAssumeEdge(
            loopIterations + " - 2 > 0",
            FileLocation.DUMMY,
            currentStartNodeGhostCFA,
            currentEndNodeGhostCFA,
            new CBinaryExpression(
                FileLocation.DUMMY,
                expressionType,
                calculationType,
                CIntegerLiteralExpression.createDummyLiteral(0, CNumericTypes.INT),
                loopIterations,
                BinaryOperator.LESS_THAN),
            true);

    currentStartNodeGhostCFA.addLeavingEdge(twiceLoopUnrollingAssumptionTrue);
    currentEndNodeGhostCFA.addEnteringEdge(twiceLoopUnrollingAssumptionTrue);

    // Unroll Loop Twice in case the loop cannot be Unrolled more than two times
    CFANode twiceLoopUnrollingCurrentNode = CFANode.newDummyCFANode("LS5");

    CFAEdge twiceLoopUnrollingAssumptionFalse =
        new CAssumeEdge(
            loopIterations + " - 2 > 0",
            FileLocation.DUMMY,
            currentStartNodeGhostCFA,
            endNodeGhostCFA,
            new CBinaryExpression(
                FileLocation.DUMMY,
                expressionType,
                calculationType,
                CIntegerLiteralExpression.createDummyLiteral(0, CNumericTypes.INT),
                loopIterations,
                BinaryOperator.LESS_THAN),
            false);

    currentStartNodeGhostCFA.addLeavingEdge(twiceLoopUnrollingAssumptionFalse);
    twiceLoopUnrollingCurrentNode.addEnteringEdge(twiceLoopUnrollingAssumptionFalse);

    Optional<CFANode> loopUnrollingSuccess =
        unrollLoopOnce(
            loopStartNode, loopBranchIndex, twiceLoopUnrollingCurrentNode, endNodeGhostCFA);
    if (loopUnrollingSuccess.isEmpty()) {
      return Optional.empty();
    } else {
      twiceLoopUnrollingCurrentNode = loopUnrollingSuccess.orElseThrow();
    }

    loopUnrollingSuccess =
        unrollLoopOnce(
            loopStartNode, loopBranchIndex, twiceLoopUnrollingCurrentNode, endNodeGhostCFA);
    if (loopUnrollingSuccess.isEmpty()) {
      return Optional.empty();
    } else {
      twiceLoopUnrollingCurrentNode = loopUnrollingSuccess.orElseThrow();
    }
    CFAEdge blankOutgoingEdge =
        new BlankEdge(
            "Blank", FileLocation.DUMMY, twiceLoopUnrollingCurrentNode, endNodeGhostCFA, "Blank");
    twiceLoopUnrollingCurrentNode.addLeavingEdge(blankOutgoingEdge);
    endNodeGhostCFA.addEnteringEdge(blankOutgoingEdge);

    // If the Loop can be Unrolled more than 2 times, unroll it once, make the summary and unroll it
    // again

    CFANode loopUnrollingCurrentNode = CFANode.newDummyCFANode("LS5");
    loopUnrollingSuccess =
        unrollLoopOnce(loopStartNode, loopBranchIndex, currentEndNodeGhostCFA, endNodeGhostCFA);
    if (loopUnrollingSuccess.isEmpty()) {
      return Optional.empty();
    } else {
      loopUnrollingCurrentNode = loopUnrollingSuccess.orElseThrow();
    }
    currentStartNodeGhostCFA = loopUnrollingCurrentNode;
    currentEndNodeGhostCFA = CFANode.newDummyCFANode("LS6");

    // Set Variables to the calculated Expressions
    for (int i = 0; i < pVariableOrdering.size(); i++) {
      String varName = pVariableOrdering.get(i);
      CExpression varValue = leftHandSideVariableAssignments.get(i);
      CVariableDeclaration pc =
          new CVariableDeclaration(
              FileLocation.DUMMY,
              true,
              CStorageClass.EXTERN, // TODO is this Correct?
              CNumericTypes.INT, // TODO improve this
              varName,
              varName,
              varName,
              null);
      CIdExpression leftHandSide = new CIdExpression(FileLocation.DUMMY, pc);
      CExpressionAssignmentStatement cStatementEdge =
          new CExpressionAssignmentStatement(FileLocation.DUMMY, leftHandSide, varValue);
      CFAEdge dummyEdge =
          new CStatementEdge(
              varName + " = " + varValue,
              cStatementEdge,
              FileLocation.DUMMY,
              currentStartNodeGhostCFA,
              currentEndNodeGhostCFA);
      currentStartNodeGhostCFA.addLeavingEdge(dummyEdge);
      currentEndNodeGhostCFA.addEnteringEdge(dummyEdge);
      currentStartNodeGhostCFA = currentEndNodeGhostCFA;
      currentEndNodeGhostCFA = CFANode.newDummyCFANode("LSI");
    }

    // Unroll Loop again for plus minus relation, since the values are aggregated
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
        new GhostCFA(startNodeGhostCFA, endNodeGhostCFA, loopStartNode, afterLoopNode));
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
      assert currentNode.getNumLeavingEdges() == 1
          : "The edge does not have a single outgoing edge, as was expected";
      CFAEdge edge = currentNode.getLeavingEdge(0);
      if (edge instanceof CStatementEdge) {
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
      }

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
            if (pLoopVariableDependencies.containsKey(k)) {
              pLoopVariableDependencies.put(k, pLoopVariableDependencies.get(k) + e.getValue());
            } else {
              pLoopVariableDependencies.put(k, e.getValue());
            }
          }

          for (Entry<String, Integer> e : operand2Map.entrySet()) {
            String k = e.getKey();
            if (pLoopVariableDependencies.containsKey(k)) {
              pLoopVariableDependencies.put(k, pLoopVariableDependencies.get(k) + e.getValue());
            } else {
              pLoopVariableDependencies.put(k, e.getValue());
            }
          }
          break;
        case "-":
          for (Entry<String, Integer> e : operand1Map.entrySet()) {
            String k = e.getKey();
            if (pLoopVariableDependencies.containsKey(k)) {
              pLoopVariableDependencies.put(k, pLoopVariableDependencies.get(k) + e.getValue());
            } else {
              pLoopVariableDependencies.put(k, e.getValue());
            }
          }

          for (Entry<String, Integer> e : operand2Map.entrySet()) {
            String k = e.getKey();
            if (pLoopVariableDependencies.containsKey(k)) {
              pLoopVariableDependencies.put(k, pLoopVariableDependencies.get(k) - e.getValue());
            } else {
              pLoopVariableDependencies.put(k, e.getValue());
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
                  k, pLoopVariableDependencies.get(k) + value * e.getValue());
            } else {
              pLoopVariableDependencies.put(k, value * e.getValue());
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
