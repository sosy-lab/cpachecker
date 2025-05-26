// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import java.util.Objects;

public final class AcslPointerType implements AcslType {

  @Serial private static final long serialVersionUID = 145845279875456789L;

  private final AcslType type;

  public AcslPointerType(AcslType pType) {
    type = pType;
    checkNotNull(pType);
  }

  public AcslType getType() {
    return type;
  }

  @Override
  public String toASTString(String declarator) {
    return type.toASTString(declarator) + "*";
  }

  @Override
  public int hashCode() {
    final int prime = 5;
    int result = 7;
    result = prime * result + Objects.hashCode(type);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslPointerType other && Objects.equals(type, other.type);
  }
}
