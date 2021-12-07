// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression.ABinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.factories.AExpressionFactory;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.util.Pair;

public class LinearVariableDependency {

  private AVariableDeclaration variable;

  private Map<AVariableDeclaration, AExpression> dependencies = new HashMap<>();
  private ALiteralExpression numericalValue = null;

  public LinearVariableDependency(
      AVariableDeclaration pVariable, ALiteralExpression pNumericalValue) {
    variable = pVariable;
    if (pNumericalValue.getValue() instanceof Number) {
      numericalValue = pNumericalValue;
    } else {
      numericalValue = null;
    }
  }

  public LinearVariableDependency(AVariableDeclaration pVariable) {
    variable = pVariable;
  }

  public LinearVariableDependency(ALiteralExpression pNumericalValue) {
    numericalValue = pNumericalValue;
  }

  public LinearVariableDependency() {
    variable = null;
  }

  public void insertOrOverwriteDependency(AVariableDeclaration pVariable, AExpression weight) {
    this.dependencies.put(pVariable, weight);
  }

  public void insertOrOverwriteDependency(AIdExpression pVariable, AExpression weight) {
    this.dependencies.put((AVariableDeclaration) pVariable.getDeclaration(), weight);
  }

  public void negateDependencies() {
    this.numericalValue =
        (ALiteralExpression)
            new AExpressionFactory()
                .from(
                    (Number) this.numericalValue.getValue(),
                    this.numericalValue.getExpressionType())
                .build();

    Map<AVariableDeclaration, AExpression> newDependencies = new HashMap<>();
    for (Entry<AVariableDeclaration, AExpression> e : this.dependencies.entrySet()) {
      newDependencies.put(e.getKey(), new AExpressionFactory(e.getValue()).negate().build());
    }
    this.dependencies = newDependencies;
  }

  public void modifyDependency(
      ALiteralExpression pNumericalExpression, ABinaryOperator operator) {
    if (operator == CBinaryExpression.BinaryOperator.PLUS
        || operator == CBinaryExpression.BinaryOperator.MINUS
        || operator == JBinaryExpression.BinaryOperator.PLUS
        || operator == JBinaryExpression.BinaryOperator.MINUS) {
      this.numericalValue =
          (ALiteralExpression)
              new AExpressionFactory(this.numericalValue)
                  .binaryOperation(pNumericalExpression, operator)
                  .build();
    }
  }

  public void modifyDependency(
      AVariableDeclaration pVariable, AExpression weight, ABinaryOperator operator) {
    if (this.dependencies.containsKey(pVariable)) {
      if (operator == CBinaryExpression.BinaryOperator.PLUS
          || operator == CBinaryExpression.BinaryOperator.MINUS
          || operator == JBinaryExpression.BinaryOperator.PLUS
          || operator == JBinaryExpression.BinaryOperator.MINUS) {
        AExpressionFactory expressionFactory =
            new AExpressionFactory(this.dependencies.get(pVariable));
        expressionFactory.binaryOperation(weight, operator);
        this.dependencies.put(pVariable, expressionFactory.build());
      }
    } else {
      this.dependencies.put(pVariable, weight);
    }
  }

  public void modifyDependency(
      LinearVariableDependency pLinearVariableDependency, ABinaryOperator operator) {
    this.modifyDependency(pLinearVariableDependency.numericalValue, operator);
    for (Entry<AVariableDeclaration, AExpression> e :
        pLinearVariableDependency.dependencies.entrySet()) {
      this.modifyDependency(e.getKey(), e.getValue(), operator);
    }
  }

  public void setDependency(AVariableDeclaration pVariable, AExpression weight) {
    this.dependencies.put(pVariable, weight);
  }

  public void setDependency(ALiteralExpression pNumericalValue) {
    if (pNumericalValue.getValue() instanceof Number) {
      this.numericalValue = pNumericalValue;
    } else {
      this.numericalValue = null;
    }
  }

  public AVariableDeclaration getDependentVariable() {
    return variable;
  }

  public Pair<Map<AVariableDeclaration, AExpression>, ALiteralExpression>
      getVariableDependencies() {
    return Pair.of(this.dependencies, this.numericalValue);
  }


}
