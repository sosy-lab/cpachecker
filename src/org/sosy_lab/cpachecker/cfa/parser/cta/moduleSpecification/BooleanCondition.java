// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification;

import java.util.ArrayList;
import java.util.List;

public class BooleanCondition {
  public static enum Operator {
    EQUAL,
    LESS,
    LESS_EQUAL,
    GREATER,
    GREATER_EQUAL
  }

  private BooleanCondition(List<VariableExpression> pExpressions) {
    expressions = pExpressions;
  }

  public abstract static class VariableExpression {
    public String variableName;
    public Operator operator;
  }

  public static class NumericVariableExpression extends VariableExpression {
    public Number constant;
  }

  public static class ParametricVariableExpression extends VariableExpression {
    public String constant;
  }

  public List<VariableExpression> expressions;

  public static class Builder {
    private List<VariableExpression> expressions = new ArrayList<>();

    public Builder expression(String variableName, Operator operator, Number constant) {
      var variableExpression = new NumericVariableExpression();
      variableExpression.variableName = variableName;
      variableExpression.operator = operator;
      variableExpression.constant = constant;

      expressions.add(variableExpression);

      return this;
    }

    public Builder expression(String variableName, Operator operator, String constant) {
      var variableExpression = new ParametricVariableExpression();
      variableExpression.variableName = variableName;
      variableExpression.operator = operator;
      variableExpression.constant = constant;

      expressions.add(variableExpression);

      return this;
    }

    public BooleanCondition build() {
      return new BooleanCondition(expressions);
    }
  }
}
