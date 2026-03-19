// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.OptionalInt;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

final class Variable {
  private final String name;
  private final CType type;

  private final OptionalInt callStackDepth;

  private Variable(String pName, CType pType, OptionalInt pCallStackDepth) {
    name = pName;
    type = pType;
    callStackDepth = pCallStackDepth;
  }

  String getName() {
    return name;
  }

  PointerBase asPointerBase() {
    return new PointerBase(name, callStackDepth);
  }

  CType getType() {
    return type;
  }

  public OptionalInt getCallStackDepth() {
    return callStackDepth;
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
    return obj instanceof Variable other && name.equals(other.name);
  }

  @Override
  public String toString() {
    return type.toASTString(name);
  }

  static Variable create(String pName, CType pT, OptionalInt pCallStackDepth) {
    CTypeUtils.checkIsSimplified(pT);
    return new Variable(checkNotNull(pName), checkNotNull(pT), checkNotNull(pCallStackDepth));
  }
}
