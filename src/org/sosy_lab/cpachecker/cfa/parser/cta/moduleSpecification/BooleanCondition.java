// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.cta.moduleSpecification;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.ibm.icu.math.BigDecimal;
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
      variableExpression.variableName = checkNotNull(variableName);
      variableExpression.operator = checkNotNull(operator);
      variableExpression.constant = checkNotNull(constant);

      checkArgument(!variableName.isEmpty(), "Empty variable names are not allowed");
      checkArgument(
          (new BigDecimal(constant.toString()).signum() >= 0),
          "Negative values are not allowed for constants.");

      expressions.add(variableExpression);

      return this;
    }

    public Builder expression(String variableName, Operator operator, String constant) {
      var variableExpression = new ParametricVariableExpression();
      variableExpression.variableName = checkNotNull(variableName);
      variableExpression.operator = checkNotNull(operator);
      variableExpression.constant = checkNotNull(constant);

      checkArgument(!variableName.isEmpty(), "Empty variable names are not allowed");
      checkArgument(!constant.isEmpty(), "Empty constant variable names are not allowed");
      expressions.add(variableExpression);

      return this;
    }

    public BooleanCondition build() {
      checkNotNull(expressions);

      checkState(!expressions.isEmpty(), "Conditions without any expressions are not allowed.");

      return new BooleanCondition(expressions);
    }
  }
}
