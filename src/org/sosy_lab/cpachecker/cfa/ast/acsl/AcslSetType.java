// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.io.Serial;
import java.util.Objects;

public final class AcslSetType implements AcslType {

  @Serial private static final long serialVersionUID = -81412344571276L;

  private AcslType elementType;

  public AcslSetType(AcslType pElementType) {
    elementType = pElementType;
  }

  public AcslType getElementType() {
    return elementType;
  }

  @Override
  public String toASTString(String declarator) {
    return "Set<" + elementType.toASTString(declarator) + ">";
  }

  @Override
  public int hashCode() {
    final int prime = 12;
    int result = 8;
    result = prime * result + Objects.hashCode(elementType);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslSetType other && Objects.equals(other.elementType, elementType);
  }
}
