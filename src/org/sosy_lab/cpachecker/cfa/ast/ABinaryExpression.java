// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.types.Type;

/** This is the abstract Class for all Expressions with two Operands and one Operator. */
public abstract class ABinaryExpression extends AbstractExpression {

  private static final long serialVersionUID = 516716556428189182L;
  private final AExpression operand1;
  private final AExpression operand2;
  private final ABinaryOperator operator;

  protected ABinaryExpression(
      FileLocation pFileLocation,
      Type pType,
      final AExpression pOperand1,
      final AExpression pOperand2,
      final ABinaryOperator pOperator) {
    super(pFileLocation, pType);
    operand1 = pOperand1;
    operand2 = pOperand2;
    operator = pOperator;
  }

  public AExpression getOperand1() {
    return operand1;
  }

  public AExpression getOperand2() {
    return operand2;
  }

  public ABinaryOperator getOperator() {
    return operator;
  }

  @Override
  public String toASTString(boolean pQualified) {
    return operand1.toParenthesizedASTString(pQualified)
        + " "
        + operator.getOperator()
        + " "
        + operand2.toParenthesizedASTString(pQualified);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(operand1);
    result = prime * result + Objects.hashCode(operand2);
    result = prime * result + Objects.hashCode(operator);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof ABinaryExpression) || !super.equals(obj)) {
      return false;
    }

    ABinaryExpression other = (ABinaryExpression) obj;

    return Objects.equals(other.operand1, operand1)
        && Objects.equals(other.operand2, operand2)
        && Objects.equals(other.operator, operator);
  }

  @Override
  public String toString() {
    return "operand1=["
        + getOperand1()
        + "], operand2=["
        + getOperand2()
        + "], operator=["
        + getOperator()
        + "]";
  }

  public interface ABinaryOperator {
    /** Returns the string representation of this operator (e.g. "*", "+"). */
    String getOperator();
  }
}
