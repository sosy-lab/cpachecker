// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.types.c.CType;

final class Variable {
  private final String name;
  private final CType type;

  private Variable(String pName, CType pType) {
    name = pName;
    type = pType;
  }

  String getName() {
    return name;
  }

  CType getType() {
    return type;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Variable)) {
      return false;
    }
    Variable other = (Variable) obj;
    return name.equals(other.name);
  }

  @Override
  public String toString() {
    return type.toASTString(name);
  }

  static Variable create(String pName, CType pT) {
    CTypeUtils.checkIsSimplified(pT);
    return new Variable(checkNotNull(pName), checkNotNull(pT));
  }
}
