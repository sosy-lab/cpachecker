// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import java.io.Serial;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.types.Type;

public abstract class AUnaryExpression extends AbstractExpression {

  @Serial private static final long serialVersionUID = 8458828004302590886L;
  private final AExpression operand;
  private final AUnaryOperator operator;

  protected AUnaryExpression(
      FileLocation pFileLocation,
      Type pType,
      final AExpression pOperand,
      final AUnaryOperator pOperator) {
    super(pFileLocation, pType);
    operand = pOperand;
    operator = pOperator;
  }

  public AExpression getOperand() {
    return operand;
  }

  public AUnaryOperator getOperator() {
    return operator;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return operator.getOperator() + operand.toParenthesizedASTString(pAAstNodeRepresentation);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hash(operand);
    result = prime * result + Objects.hash(operator);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AUnaryExpression other
        && super.equals(obj)
        && Objects.equals(other.operand, operand)
        && Objects.equals(other.operator, operator);
  }

  public interface AUnaryOperator {
    /** Returns the string representation of this operator (e.g. "*", "+"). */
    String getOperator();
  }
}
