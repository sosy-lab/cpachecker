// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.acsl;

import static com.google.common.base.Preconditions.checkNotNull;

public enum AcslBuiltinLogicType implements AcslLogicType {
  BOOLEAN("boolean"),
  INTEGER("integer"),
  REAL("real"),
  ;

  private final String type;

  AcslBuiltinLogicType(String pType) {
    type = checkNotNull(pType);
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
    return (type + " " + declarator).trim();
  }
}
