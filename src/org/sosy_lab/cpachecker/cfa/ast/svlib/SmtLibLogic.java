// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

public enum SmtLibLogic {
  LIA,
  QF_LIA,
  ;

  public static SmtLibLogic fromString(String pLogic) {
    return switch (pLogic) {
      case "LIA" -> LIA;
      case "QF_LIA" -> QF_LIA;
      default -> throw new IllegalArgumentException("Unknown SMT-LIB logic: " + pLogic);
    };
  }
}
