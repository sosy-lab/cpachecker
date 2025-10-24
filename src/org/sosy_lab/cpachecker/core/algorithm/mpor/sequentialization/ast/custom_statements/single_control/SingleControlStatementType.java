// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control;

import com.google.common.base.Joiner;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

public enum SingleControlStatementType {
  FOR("for"),
  SWITCH("switch"),
  WHILE("while");

  private final String keyword;

  SingleControlStatementType(String pKeyword) {
    keyword = pKeyword;
  }

  public String getKeyword() {
    return keyword;
  }

  public String buildControlFlowPrefix(CExpression pExpression) {
    return switch (this) {
      case FOR ->
          throw new UnsupportedOperationException(
              String.format("cannot build prefix for encoding %s", this));
      case SWITCH, WHILE ->
          Joiner.on(SeqSyntax.SPACE)
              .join(
                  getKeyword(),
                  SeqStringUtil.wrapInBrackets(pExpression.toASTString()),
                  SeqSyntax.CURLY_BRACKET_LEFT);
    };
  }
}
