// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.svlib;

import org.sosy_lab.java_smt.api.FormulaType;

public enum SvLibSmtLibPredefinedType implements SvLibSmtLibType {
  INT,
  BOOL,
  STRING,
  REAL,
  /** The sort of the rounding modes of the theory of floating point numbers. */
  ROUNDING_MODE;

  @Override
  public FormulaType<?> toFormulaType() {
    return switch (this) {
      case INT -> FormulaType.IntegerType;
      case BOOL -> FormulaType.BooleanType;
      case STRING -> FormulaType.StringType;
      case REAL -> FormulaType.RationalType;
      case ROUNDING_MODE -> FormulaType.FloatingPointRoundingModeType;
    };
  }

  @Override
  public String toASTString(String declarator) {
    return declarator + " : " + toASTString();
  }

  @Override
  public String toASTString() {
    return switch (this) {
      case INT -> "Int";
      case BOOL -> "Bool";
      case STRING -> "String";
      case REAL -> "Real";
      case ROUNDING_MODE -> "RoundingMode";
    };
  }
}
