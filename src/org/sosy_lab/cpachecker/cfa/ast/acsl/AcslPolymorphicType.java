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

public final class AcslPolymorphicType implements AcslLogicType {

  @Serial private static final long serialVersionUID = 1456598245975456789L;

  private final String name;

  public AcslPolymorphicType(String pName) {
    name = pName;
  }

  @Override
  public String toASTString(String declarator) {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public int hashCode() {
    final int prime = 5;
    int result = 7;
    result = prime * result + Objects.hashCode(name);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslPolymorphicType other && Objects.equals(name, other.name);
  }
}
