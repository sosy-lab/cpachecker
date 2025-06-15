// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control;

public enum SingleControlExpressionEncoding {
  ELSE("else"),
  ELSE_IF("else if"),
  FOR("for"),
  IF("if"),
  SWITCH("switch"),
  WHILE("while");

  public final String keyword;

  SingleControlExpressionEncoding(String pKeyword) {
    keyword = pKeyword;
  }
}
