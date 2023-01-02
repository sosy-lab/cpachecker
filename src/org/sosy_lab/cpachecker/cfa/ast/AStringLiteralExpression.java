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

public abstract class AStringLiteralExpression extends ALiteralExpression {

  private static final long serialVersionUID = 182481690634464284L;
  private final String value;

  protected AStringLiteralExpression(FileLocation pFileLocation, Type pType, String pValue) {
    super(pFileLocation, pType);
    value = pValue;
  }

  @Override
  public String toASTString() {
    return value;
  }

  @Override
  public String getValue() {
    return value;
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

    if (!(obj instanceof AStringLiteralExpression) || !super.equals(obj)) {
      return false;
    }

    AStringLiteralExpression other = (AStringLiteralExpression) obj;

    return Objects.equals(other.value, value);
  }
}
