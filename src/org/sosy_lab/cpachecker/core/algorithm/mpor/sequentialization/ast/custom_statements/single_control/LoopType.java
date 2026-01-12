// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control;

public enum LoopType {
  FOR("for"),
  WHILE("while");

  private final String keyword;

  LoopType(String pKeyword) {
    keyword = pKeyword;
  }

  public String getKeyword() {
    return keyword;
  }
}
