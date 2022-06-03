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

/** This is the abstract Class for Casted Expressions. */
public abstract class ACastExpression extends AbstractLeftHandSide {

  private static final long serialVersionUID = 7047818239785351507L;
  private final AExpression operand;
  private final Type castType;

  protected ACastExpression(
      FileLocation pFileLocation, Type castExpressionType, AExpression pOperand) {
    super(pFileLocation, castExpressionType);

    operand = pOperand;
    castType = castExpressionType;
  }

  public AExpression getOperand() {
    return operand;
  }

  @Override
  public String toASTString(boolean pQualified) {
    return "("
        + getExpressionType().toASTString("")
        + ")"
        + operand.toParenthesizedASTString(pQualified);
  }

  public Type getCastType() {
    return castType;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(castType);
    result = prime * result + Objects.hashCode(operand);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof ACastExpression) || !super.equals(obj)) {
      return false;
    }

    ACastExpression other = (ACastExpression) obj;

    return Objects.equals(other.operand, operand) && Objects.equals(other.castType, castType);
  }
}
