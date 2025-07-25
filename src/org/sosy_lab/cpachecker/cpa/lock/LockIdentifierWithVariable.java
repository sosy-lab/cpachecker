// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.lock;

import java.util.Objects;

public final class LockIdentifierWithVariable extends LockIdentifier {

  private final String varName;

  LockIdentifierWithVariable(String pName, String var, LockType pType) {
    super(pName, pType);
    assert !var.isEmpty();
    varName = var;
  }

  @Override
  public int hashCode() {
    final int prime = 31;

    int result = super.hashCode();
    result = prime * result + Objects.hashCode(varName);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof LockIdentifierWithVariable other
        && super.equals(obj)
        && Objects.equals(varName, other.varName);
  }

  @Override
  public String toString() {
    return super.toString() + "(" + varName + ")";
  }

  @Override
  public int compareTo(LockIdentifier pO) {
    // FIXME This is broken because compareTo in LockIdentifier behaves incompatibly with this
    // method.
    int result = super.compareTo(pO);
    if (result != 0) {
      return result;
    }
    if (pO instanceof LockIdentifierWithVariable other) {
      return varName.compareTo(other.varName);
    } else {
      return 1;
    }
  }
}
