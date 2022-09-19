// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import java.math.BigInteger;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.types.Type;

public abstract class AIntegerLiteralExpression extends ALiteralExpression {

  private static final long serialVersionUID = -4414816900579078042L;
  private final BigInteger value;

  protected AIntegerLiteralExpression(FileLocation pFileLocation, Type pType, BigInteger pValue) {
    super(pFileLocation, pType);
    value = pValue;
  }

  @Override
  public BigInteger getValue() {
    return value;
  }

  public long asLong() {
    // TODO handle values that are bigger than MAX_LONG
    return value.longValue();
  }

  @Override
  public String toASTString() {
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

    if (!(obj instanceof AIntegerLiteralExpression) || !super.equals(obj)) {
      return false;
    }

    AIntegerLiteralExpression other = (AIntegerLiteralExpression) obj;

    return Objects.equals(other.value, value);
  }
}
