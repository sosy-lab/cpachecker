// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.timedautomata;

import java.io.Serializable;

/** Class that represent expressions of the form var op number. */
public class TaVariableExpression implements Serializable {
  private static final long serialVersionUID = -8225839668013023605L;

  public static enum Operator {
    EQUAL("=="),
    LESS("<"),
    LESS_EQUAL("<="),
    GREATER(">"),
    GREATER_EQUAL(">=");

    private final String op;

    Operator(String pOp) {
      op = pOp;
    }

    @Override
    public String toString() {
      return op;
    }
  }

  private final TaVariable variable;
  private final Operator operator;
  private final Number constant;

  public TaVariableExpression(TaVariable pVariable, Operator pOperator, Number pConstant) {
    variable = pVariable;
    operator = pOperator;
    constant = pConstant;
  }

  public TaVariable getVariable() {
    return variable;
  }

  public Operator getOperator() {
    return operator;
  }

  public Number getConstant() {
    return constant;
  }

  @Override
  public String toString() {
    return variable.getShortName() + " " + operator + " " + constant;
  }
}
