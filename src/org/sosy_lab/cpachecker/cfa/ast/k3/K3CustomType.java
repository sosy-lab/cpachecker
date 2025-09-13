// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import java.io.Serial;
import java.util.Objects;

public final class K3CustomType implements K3Type {

  public static final K3CustomType InternalAnyType = new K3CustomType("#any");

  @Serial private static final long serialVersionUID = -1560683119379278009L;
  private final String type;

  public K3CustomType(String pType) {
    type = pType;
  }

  public String getType() {
    return type;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof K3CustomType other && Objects.equals(type, other.type);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(type);
  }

  @Override
  public String toASTString(String declarator) {
    return type + " " + declarator;
  }
}
