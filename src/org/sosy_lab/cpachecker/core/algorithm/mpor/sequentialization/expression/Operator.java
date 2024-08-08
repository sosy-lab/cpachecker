// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

public enum Operator {
  AND("&&"),
  ASSIGN("="),
  EQUAL("=="),
  GREATER(">"),
  GREATER_OR_EQUAL(">="),
  LESS("<"),
  LESS_OR_EQUAL("<="),
  NOT_EQUAL("!="),
  OR("||");

  public final String string;

  // TODO negation?

  Operator(String pString) {
    string = pString;
  }
}
