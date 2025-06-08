// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.java;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * Description of a simple Java structure's type.
 *
 * <p>These descriptions are mostly merely primitive types, but also a special unspecified type.
 */
public enum JSimpleType implements JType {
  BOOLEAN("boolean"),
  BYTE("byte"),
  SHORT("short"),
  CHAR("char"),
  INT("int"),
  LONG("long"),
  FLOAT("float"),
  DOUBLE("double"),
  UNSPECIFIED(""),
  VOID("void");

  @Serial private static final long serialVersionUID = 7153757299840260748L;

  private final String code;

  JSimpleType(String pCode) {
    code = pCode;
  }

  public boolean isFloatingPointType() {
    return this == FLOAT || this == DOUBLE;
  }

  public boolean isIntegerType() {
    return this == BYTE || this == CHAR || this == SHORT || this == INT || this == LONG;
  }

  @Override
  public String toASTString(String pDeclarator) {
    List<String> parts = new ArrayList<>();

    parts.add(Strings.emptyToNull(code));
    parts.add(Strings.emptyToNull(pDeclarator));

    return Joiner.on(' ').skipNulls().join(parts);
  }

  @Override
  public String toString() {
    return switch (this) {
      case UNSPECIFIED -> "unspecified";
      default -> code;
    };
  }
}
