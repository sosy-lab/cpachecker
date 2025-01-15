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
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue;

public abstract class AFloatLiteralExpression extends ALiteralExpression {

  @Serial private static final long serialVersionUID = 8161363025296340648L;
  private final FloatValue value;

  protected AFloatLiteralExpression(FileLocation pFileLocation, Type pType, FloatValue pValue) {
    super(pFileLocation, pType);
    value = pValue;
  }

  @Override
  public FloatValue getValue() {
    return value;
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

    return obj instanceof AFloatLiteralExpression other
        && super.equals(obj)
        && Objects.equals(other.value, value);
  }
}
