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

public enum AcslBuiltinLogicType implements AcslLogicType {
  BOOLEAN("boolean"),
  INTEGER("integer"),
  REAL("real"),
  // Not part of the ACSL spec, but necessary to represent
  // the type of an empty set, when we do not
  // know what it contains
  // DO NOT USE UNDER ANY CIRCUMSTANCE EXCEPT THAN WITH AN EMPTY SET
  ANY("any"),
  ;

  @Serial private static final long serialVersionUID = 7011236361956900L;

  private final String type;

  AcslBuiltinLogicType(String pType) {
    type = pType;
    checkNotNull(pType);
  }

  public static AcslType of(String pText) {
    for (AcslBuiltinLogicType builtin : values()) {
      if (builtin.type.equals(pText)) {
        return builtin;
      }
    }
    throw new IllegalArgumentException("Unknown builtin logic type: " + pText);
  }

  @Override
  public String toASTString(String declarator) {
    return type;
  }
}
