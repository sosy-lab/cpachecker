// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

public enum SmtLibLogic {
  ALL,
  LIA,
  QF_ALIA,
  QF_LIA,
  QF_NIA,
  ;

  public boolean containsIntegerArithmetic() {
    return switch (this) {
      case ALL, LIA, QF_LIA, QF_NIA, QF_ALIA -> true;
    };
  }

  public boolean containsNonLinearIntegerArithmetic() {
    return switch (this) {
      case ALL, QF_NIA -> true;
      case LIA, QF_LIA, QF_ALIA -> false;
    };
  }

  public static SmtLibLogic fromString(String pLogic) {
    return switch (pLogic) {
      case "ALL" -> ALL;
      case "QF_ALIA" -> QF_ALIA;
      case "LIA" -> LIA;
      case "QF_LIA" -> QF_LIA;
      case "QF_NIA" -> QF_NIA;
      default -> throw new IllegalArgumentException("Unknown SMT-LIB logic: " + pLogic);
    };
  }
}
