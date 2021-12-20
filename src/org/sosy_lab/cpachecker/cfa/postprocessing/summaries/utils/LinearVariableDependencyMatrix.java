// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.ALiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.factories.AExpressionFactory;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;

public class LinearVariableDependencyMatrix {

  private Matrix<AExpression> matrixRepresentation;
  private List<AVariableDeclaration> variableOrdering;
  private Integer matrixSize;

  public LinearVariableDependencyMatrix(
      LinearVariableDependencyGraph pLinearVariableDependencyGraph,
      List<AVariableDeclaration> pVariableOrdering) {
    variableOrdering = pVariableOrdering;
    // Literal Expressions are always at the end, since they are only dependent on themselves
    matrixSize = pVariableOrdering.size() + 1;
    matrixRepresentation = new Matrix<>(matrixSize);
    for (int i = 0; i < matrixSize; i++) {
      for (int j = 0; j < matrixSize; j++) {
        matrixRepresentation.put(
            i,
            j,
            // TODO: Generalize for Java
            new AExpressionFactory()
                .from(
                    0,
                    new CSimpleType(
                        false,
                        false,
                        CBasicType.INT,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        true))
                .build());
      }
    }

    matrixRepresentation.put(
        pVariableOrdering.size(),
        pVariableOrdering.size(),
        // TODO: Generalize for Java
        new AExpressionFactory()
            .from(
                1,
                new CSimpleType(
                    false, false, CBasicType.INT, false, false, false, false, false, false, true))
            .build());

    for (LinearVariableDependency l : pLinearVariableDependencyGraph.getDependencies()) {
      Integer indexOfVariable = pVariableOrdering.indexOf(l.getDependentVariable());
      for (Entry<AVariableDeclaration, AExpression> v :
          l.getVariableDependencies().getFirst().entrySet()) {
        Integer indexOfDependencyVariable = pVariableOrdering.indexOf(v.getKey());
        matrixRepresentation.put(indexOfVariable, indexOfDependencyVariable, v.getValue());
      }
      matrixRepresentation.put(
          indexOfVariable,
          pVariableOrdering.size(),
          // TODO: Generalize for Java Expressions
          new AExpressionFactory()
              .from(matrixRepresentation.get(indexOfVariable, pVariableOrdering.size()))
              .binaryOperation(l.getVariableDependencies().getSecond(), CBinaryExpression.BinaryOperator.PLUS)
              .build());
    }
  }

  public LinearVariableDependencyMatrix(
      Matrix<AExpression> pMatrix, List<AVariableDeclaration> pVariableOrdering) {
    matrixRepresentation = pMatrix;
    variableOrdering = pVariableOrdering;
  }

  public List<AVariableDeclaration> getVariableOrdering() {
    return variableOrdering;
  }

  public boolean isUpperDiagonal() {
    for (int i = 0; i < matrixSize; i++) {
      for (int j = 0; j < i; j++) {
        AExpression matrixValue = matrixRepresentation.get(i, j);
        if (matrixValue instanceof ALiteralExpression) {
          Object value = ((ALiteralExpression) matrixValue).getValue();
          // Big integer is the default type of the initial value
          if (value instanceof BigInteger) {
            if (((BigInteger) value).longValue() != 0) {
              return false;
            }
          } else {
            return false;
          }
        } else {
          return false;
        }
      }
    }
    return true;
  }

  public boolean diagonalValueEquals(Integer pValue) {
    for (int i = 0; i < matrixSize; i++) {
      AExpression matrixValue = matrixRepresentation.get(i, i);
      if (matrixValue instanceof ALiteralExpression) {
        Object value = ((ALiteralExpression) matrixValue).getValue();
        // Big integer is the default type of the initial value
        if (value instanceof BigInteger) {
          if (((BigInteger) value).longValue() != pValue.longValue()) {
            return false;
          }
        } else {
          return false;
        }
      } else {
        return false;
      }
    }
    return true;
  }

  public void setDiagonalValue(Integer pValueOf) {
    for (int i = 0; i < matrixSize; i++) {
      matrixRepresentation.put(
          i,
          i,
          new AExpressionFactory()
              .from(
                  pValueOf,
                  new CSimpleType(
                      false, false, CBasicType.INT, false, false, false, false, false, false, true))
              .build());
    }
  }

  public LinearVariableDependencyMatrix toThepower(AExpression pPower) {
    // The matrix should be upper diagonal in order for this algorithm to work. Other options may be
    // possible but are not currently being considered
    assert this.isUpperDiagonal()
        : "This algorithm only terminates in general for upper Diagonal matrices";

    Matrix<AExpression> currentMatrixPower = new Matrix<>(this.matrixSize);
    // TODO: Update the addition and multiplication operators, because the report cannot be read and
    // the overflow analysis has problems because of division by 0
    currentMatrixPower.setAdditionOperation(
        (p1, p2) ->
            new AExpressionFactory()
                .from(p1)
                // TODO: Generalize for Java
                .binaryOperation(p2, CBinaryExpression.BinaryOperator.PLUS)
                .build());
    currentMatrixPower.setMultiplicationOperation(
        (p1, p2) ->
            new AExpressionFactory()
                .from(p1)
                // TODO: Generalize for Java
                .binaryOperation(p2, CBinaryExpression.BinaryOperator.MULTIPLY)
                .build());
    // Init matrix to the power of zero
    for (int i = 0; i < matrixSize; i++) {
      for (int j = 0; j < matrixSize; j++) {
        Integer value = 0;
        if (i == j) {
          value = 1;
        }
        currentMatrixPower.put(
            i,
            j,
            // TODO: Generalize for Java
            new AExpressionFactory()
                .from(
                    value,
                    new CSimpleType(
                        false,
                        false,
                        CBasicType.INT,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        true))
                .build());
      }
    }

    // Create a new Matrix with the diagonal removed of the current matrix representation
    Matrix<AExpression> matrixRepresentationWithoutDiagonal = new Matrix<>(this.matrixSize);
    for (int i = 0; i < matrixSize; i++) {
      for (int j = 0; j < matrixSize; j++) {
        if (i == j) {
          matrixRepresentationWithoutDiagonal.put(i, j, this.matrixRepresentation.get(i, j));
        } else {
          matrixRepresentationWithoutDiagonal.put(
              i,
              j,
              // TODO: Generalize for Java
              new AExpressionFactory()
                  .from(
                      0,
                      new CSimpleType(
                          false,
                          false,
                          CBasicType.INT,
                          false,
                          false,
                          false,
                          false,
                          false,
                          false,
                          true))
                  .build());
        }
      }
    }
    // Init Result Matrix
    Matrix<AExpression> resultMatrix = new Matrix<>(this.matrixSize);
    resultMatrix.setAdditionOperation(
        (p1, p2) ->
            new AExpressionFactory()
                .from(p1)
                // TODO: Generalize for Java
                .binaryOperation(p2, CBinaryExpression.BinaryOperator.PLUS)
                .build());
    resultMatrix.setMultiplicationOperation(
        (p1, p2) ->
            new AExpressionFactory()
                .from(p1)
                // TODO: Generalize for Java
                .binaryOperation(p2, CBinaryExpression.BinaryOperator.MULTIPLY)
                .build());
    for (int i = 0; i < matrixSize; i++) {
      for (int j = 0; j < matrixSize; j++) {
        resultMatrix.put(
              i,
              j,
              // TODO: Generalize for Java
              new AExpressionFactory()
                  .from(
                      0,
                      new CSimpleType(
                          false,
                          false,
                          CBasicType.INT,
                          false,
                          false,
                          false,
                          false,
                          false,
                          false,
                          true))
                  .build());
      }
    }
    // Init the pPower choose k value
    AExpression nChooseKValue =
        new AExpressionFactory()
            .from(
                1,
                new CSimpleType(
                    false, false, CBasicType.INT, false, false, false, false, false, false, true))
            .build();
    // Generate the formula \sum_{k = 0}^{pPower} Identity^{pPower - k} (this.matrix - Identity)^{k}
    // (pPower choose k)
    for (int k = 0; k < this.matrixSize; k++) {
      // Generate the result matrix
      Matrix<AExpression> tmpMatrixForAddition = new Matrix<>(currentMatrixPower);
      tmpMatrixForAddition.multiplyWith(nChooseKValue);
      resultMatrix.addMatrix(tmpMatrixForAddition);
      // Update the values
      currentMatrixPower.multiplyWith(matrixRepresentationWithoutDiagonal);
      nChooseKValue =
          new AExpressionFactory()
              .from(pPower)
              .binaryOperation(
                  k + 1,
                  // TODO: Generalize for Java
                  new CSimpleType(
                      false, false, CBasicType.INT, false, false, false, false, false, false, true),
                  CBinaryExpression.BinaryOperator.MINUS)
              .binaryOperation(nChooseKValue, CBinaryExpression.BinaryOperator.MULTIPLY)
              .build();
    }
    // Create result dependency
    LinearVariableDependencyMatrix resultDependencyMatrix =
        new LinearVariableDependencyMatrix(currentMatrixPower, this.variableOrdering);
    return resultDependencyMatrix;
  }

  public List<AExpressionAssignmentStatement> asAssignments() {
    List<AExpressionAssignmentStatement> assignments = new ArrayList<>();
    for (int i = 0; i < this.variableOrdering.size(); i++) {
      AExpression leftHandSide =
          new AExpressionFactory()
              .from(this.variableOrdering.get(i))
              .binaryOperation(
                  this.matrixRepresentation.get(i, i), CBinaryExpression.BinaryOperator.MULTIPLY)
              .build();
      for (int j = i; j < this.variableOrdering.size(); j++) {
        leftHandSide =
            new AExpressionFactory()
                .from(this.variableOrdering.get(j))
                .binaryOperation(
                    this.matrixRepresentation.get(i, j),
                    CBinaryExpression.BinaryOperator.MULTIPLY)
                .binaryOperation(leftHandSide, CBinaryExpression.BinaryOperator.PLUS)
                .build();
      }

      leftHandSide =
          new AExpressionFactory()
              .from(leftHandSide)
              .binaryOperation(
                  this.matrixRepresentation.get(i, this.variableOrdering.size()),
                  CBinaryExpression.BinaryOperator.MULTIPLY)
              .build();
      assignments.add(
          new AExpressionFactory().from(leftHandSide).assignTo(this.variableOrdering.get(i)));
    }
    return assignments;
  }
}
