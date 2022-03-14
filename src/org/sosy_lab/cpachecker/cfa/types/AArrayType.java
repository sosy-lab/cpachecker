// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

public abstract class AArrayType implements Type {

  private static final long serialVersionUID = -2838888440949947901L;

  private final Type elementType;

  protected AArrayType(Type pElementType) {
    elementType = checkNotNull(pElementType);
  }

  public Type getType() {
    return elementType;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(elementType);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof AArrayType)) {
      return false;
    }

    AArrayType other = (AArrayType) obj;

    return Objects.equals(elementType, other.elementType);
  }
}
