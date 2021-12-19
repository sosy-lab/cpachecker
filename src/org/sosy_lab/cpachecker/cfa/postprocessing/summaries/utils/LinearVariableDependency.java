// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.utils;

import java.math.BigInteger;
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
import org.sosy_lab.cpachecker.cfa.ast.factories.TypeFactory;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.util.Pair;

public class LinearVariableDependency {

  private AVariableDeclaration variable;

  private Map<AVariableDeclaration, AExpression> dependencies = new HashMap<>();
  // TODO, make this more general to include floats and java
  private ALiteralExpression numericalValue =
      (ALiteralExpression)
          new AExpressionFactory()
              .from(
                  0,
                  new CSimpleType(
                      false, false, CBasicType.INT, true, false, true, false, false, false, false))
              .build();

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
        || operator == JBinaryExpression.BinaryOperator.PLUS) {
      // TODO generalize Integer to Number
      this.numericalValue =
          (ALiteralExpression)
              new AExpressionFactory()
                  .from(
                      ((BigInteger) this.numericalValue.getValue())
                          .add((BigInteger) pNumericalExpression.getValue()),
                      TypeFactory.getMostGeneralType(
                          this.numericalValue.getExpressionType(),
                          pNumericalExpression.getExpressionType()))
                  .build();
    } else if (operator == CBinaryExpression.BinaryOperator.MINUS
        || operator == JBinaryExpression.BinaryOperator.MINUS) {
      this.numericalValue =
          (ALiteralExpression)
              new AExpressionFactory()
                  .from(
                      ((BigInteger) this.numericalValue.getValue())
                          .subtract(((BigInteger) pNumericalExpression.getValue())),
                      TypeFactory.getMostGeneralType(
                          this.numericalValue.getExpressionType(),
                          pNumericalExpression.getExpressionType()))
                  .build();
    } else {
      this.numericalValue = null;
    }
  }

  public boolean modifyDependency(
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
      } else {
        return false;
      }
    } else {
      this.dependencies.put(pVariable, weight);
    }
    return true;
  }
  /*
   * returns true if the dependency could be modified successfully with the operator. Else it returns false.
   */
  public boolean modifyDependency(
      LinearVariableDependency pLinearVariableDependency, ABinaryOperator operator) {
    // TODO: Handle the multiplication and division case correctly. return false if they are not
    // possible.
    if (operator == CBinaryExpression.BinaryOperator.PLUS
        || operator == CBinaryExpression.BinaryOperator.MINUS
        || operator == JBinaryExpression.BinaryOperator.PLUS
        || operator == JBinaryExpression.BinaryOperator.MINUS) {
      this.modifyDependency(pLinearVariableDependency.numericalValue, operator);

      for (Entry<AVariableDeclaration, AExpression> e :
          pLinearVariableDependency.dependencies.entrySet()) {
        this.modifyDependency(e.getKey(), e.getValue(), operator);
      }
      return true;
    } else if (operator == CBinaryExpression.BinaryOperator.MULTIPLY
        || operator == JBinaryExpression.BinaryOperator.MULTIPLY) {
      if (this.dependencies.keySet().size() != 0 && pLinearVariableDependency.dependencies.keySet().size() != 0) {
        return false;
      } else {
        if (this.dependencies.keySet().size() == 0) {
          for (Entry<AVariableDeclaration, AExpression> s :
              pLinearVariableDependency.dependencies.entrySet()) {
            this.setDependency(s.getKey(), s.getValue());
            if (!this.modifyDependency(s.getKey(), this.numericalValue, operator)) {
              return false;
            }
          }
        } else if (pLinearVariableDependency.dependencies.keySet().size() == 0) {
          for (Entry<AVariableDeclaration, AExpression> s : this.dependencies.entrySet()) {
            if (!this.modifyDependency(
                s.getKey(), pLinearVariableDependency.numericalValue, operator)) {
              return false;
            }
          }
        } else {
          return false;
        }
      }
    } else if (operator == JBinaryExpression.BinaryOperator.DIVIDE
        || operator == CBinaryExpression.BinaryOperator.DIVIDE) {
      if (pLinearVariableDependency.dependencies.keySet().size() == 0) {
          for (Entry<AVariableDeclaration, AExpression> s : this.dependencies.entrySet()) {
            if (!this.modifyDependency(
                s.getKey(), pLinearVariableDependency.numericalValue, operator)) {
              return false;
            }
          }
        } else {
          return false;
        }
      return false;
    } else {
      return false;
    }
    return false;
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
