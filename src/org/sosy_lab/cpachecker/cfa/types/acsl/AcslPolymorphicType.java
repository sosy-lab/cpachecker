// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.acsl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;

public final class AcslPolymorphicType implements AcslLogicType {

  @Serial private static final long serialVersionUID = 7112186973639694412L;

  private final String name;

  public AcslPolymorphicType(String pName) {
    name = checkNotNull(pName);
  }

  @Override
  public String toASTString(String declarator) {
    return (name + " " + declarator).trim();
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public int hashCode() {
    final int prime = 5;
    int result = 7;
    result = prime * result + name.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslPolymorphicType other && name.equals(other.name);
  }
}
