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

record Variable(String name, CType type) {

  Variable {
    checkNotNull(name);
    checkNotNull(type);
    CTypeUtils.checkIsSimplified(type);
  }

  PointerBase asPointerBase() {
    return new PointerBase(name);
  }

  @Override
  public String toString() {
    return type.toASTString(name);
  }
}
