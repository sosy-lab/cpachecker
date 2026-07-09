// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.svlib;

import com.google.common.base.Ascii;
import org.sosy_lab.java_smt.api.FormulaType;

public enum SvLibSmtLibPredefinedType implements SvLibSmtLibType {
  INT,
  BOOL,
  STRING,
  REAL;

  @Override
  public FormulaType<?> toFormulaType() {
    return switch (this) {
      case INT -> FormulaType.IntegerType;
      case BOOL -> FormulaType.BooleanType;
      case STRING -> FormulaType.BooleanType;
      case REAL -> FormulaType.RationalType;
    };
  }

  @Override
  public String toASTString(String declarator) {
    return declarator + " : " + Ascii.toLowerCase(name());
  }

  @Override
  public String toASTString() {
    return switch (this) {
      case INT -> "Int";
      case BOOL -> "Bool";
      case STRING -> "String";
      case REAL -> "Real";
    };
  }
}
