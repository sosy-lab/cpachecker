// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.acsl;

public enum UnaryOperator {
  BNEG("~"),
  PLUS("+"),
  MINUS("-"),
  POINTER_DEREF("*"),
  ADDRESS_OF("&"),
  SIZEOF("sizeof");

  private final String operator;

  UnaryOperator(String s) {
    operator = s;
  }

  @Override
  public String toString() {
    return operator;
  }
}
