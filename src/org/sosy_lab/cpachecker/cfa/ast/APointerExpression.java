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

public abstract class APointerExpression extends AbstractLeftHandSide {

  @Serial private static final long serialVersionUID = -1287666395056820570L;
  private final AExpression operand;

  protected APointerExpression(FileLocation pFileLocation, Type pType, final AExpression pOperand) {
    super(pFileLocation, pType);
    operand = pOperand;
  }

  public AExpression getOperand() {
    return operand;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "*" + operand.toParenthesizedASTString(pAAstNodeRepresentation);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hash(operand);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof APointerExpression other
        && super.equals(obj)
        && Objects.equals(other.operand, operand);
  }
}
