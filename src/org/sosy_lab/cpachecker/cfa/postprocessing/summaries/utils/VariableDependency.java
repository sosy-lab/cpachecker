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
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression.ABinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.factories.AExpressionFactory;

public class VariableDependency {

  private AVariableDeclaration variable;

  // TODO: Generalize Integer
  private Map<AVariableDeclaration, AExpression> dependencies = new HashMap<>();

  public VariableDependency(AVariableDeclaration pVariable) {
    variable = pVariable;
  }

  public void addOrOverwriteDependency(AVariableDeclaration pVariable, AExpression weight) {
    this.dependencies.put(pVariable, weight);
  }

  public void addDependency(
      AVariableDeclaration pVariable, AExpression weight, ABinaryOperator operator) {
    if (this.dependencies.containsKey(pVariable)) {
      AExpressionFactory expressionFactory =
          new AExpressionFactory(this.dependencies.get(pVariable));
      // expressionFactory.arithmeticExpression(weight, operator);
      this.dependencies.put(pVariable, expressionFactory.build());
    }
  }
}
