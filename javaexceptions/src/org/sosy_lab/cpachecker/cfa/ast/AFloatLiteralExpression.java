// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import java.math.BigDecimal;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.types.Type;

public abstract class AFloatLiteralExpression extends ALiteralExpression {

  private static final long serialVersionUID = 8161363025296340648L;
  private final BigDecimal value;

  protected AFloatLiteralExpression(FileLocation pFileLocation, Type pType, BigDecimal pValue) {
    super(pFileLocation, pType);
    value = pValue;
  }

  @Override
  public BigDecimal getValue() {
    return value;
  }

  @Override
  public String toASTString() {
    // If the value is integral and has no zeroes after the decimal point yet, add one
    if (value.scale() <= 0) {
      return String.format("%.1f", value);
    }
    return value.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(value);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof AFloatLiteralExpression) || !super.equals(obj)) {
      return false;
    }

    AFloatLiteralExpression other = (AFloatLiteralExpression) obj;

    return Objects.equals(other.value, value);
  }
}
