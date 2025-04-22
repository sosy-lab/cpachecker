// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.io.Serial;

public enum AcslBuiltinLogicType implements AcslLogicType {
  BOOLEAN("boolean"),
  INTEGER("integer"),
  REAL("real"),
  ;

  @Serial private static final long serialVersionUID = 7011236361956900L;

  private final String type;

  AcslBuiltinLogicType(String pType) {
    type = pType;
  }

  @Override
  public String toASTString(String declarator) {
    return type;
  }
}
