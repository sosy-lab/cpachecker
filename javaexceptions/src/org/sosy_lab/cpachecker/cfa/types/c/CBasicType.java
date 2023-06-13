// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

public enum CBasicType {
  UNSPECIFIED(""),
  BOOL("_Bool"),
  CHAR("char"),
  INT("int"),
  INT128("__int128"),
  FLOAT("float"),
  DOUBLE("double"),
  FLOAT128("__float128"),
  ;

  private final String code;

  CBasicType(String pCode) {
    code = pCode;
  }

  /** Returns true if a type is a floating type as defined by the C standard §6.2.5. */
  public boolean isFloatingPointType() {
    return this == FLOAT || this == DOUBLE || this == FLOAT128;
  }

  /** Returns true if a type is an integer type as defined by the C standard §6.2.5. */
  public boolean isIntegerType() {
    return this == BOOL || this == CHAR || this == INT || this == INT128 || this == UNSPECIFIED;
  }

  public String toASTString() {
    return code;
  }
}
