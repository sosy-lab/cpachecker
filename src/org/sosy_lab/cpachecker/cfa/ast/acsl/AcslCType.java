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
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public final class AcslCType implements AcslType {

  @Serial
  private static final long serialVersionUID = -81123243801151276L;

  private final CType type;

  public AcslCType(CType pType) {
    type = pType;
  }

  @Override
  public String toASTString(String declarator) {
    return type.toASTString(declarator);
  }

  @Override
  public int hashCode() {
    return type.hashCode();
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    return pObj instanceof AcslCType other && Objects.equals(type, other.type);
  }
}
