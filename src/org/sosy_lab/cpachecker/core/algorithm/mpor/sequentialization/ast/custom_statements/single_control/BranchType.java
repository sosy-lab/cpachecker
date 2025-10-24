// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Joiner;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

public enum BranchType {
  IF("if"),
  ELSE("else"),
  ELSE_IF("else if");

  private final String keyword;

  BranchType(String pKeyword) {
    keyword = pKeyword;
  }

  private String getKeyword() {
    return keyword;
  }

  private String buildPrefix(Optional<CExpression> pExpression) {
    return switch (this) {
      case IF ->
          Joiner.on(SeqSyntax.SPACE)
              .join(
                  getKeyword(),
                  SeqStringUtil.wrapInBrackets(pExpression.orElseThrow().toASTString()),
                  SeqSyntax.CURLY_BRACKET_LEFT);
      case ELSE ->
          Joiner.on(SeqSyntax.SPACE)
              .join(SeqSyntax.CURLY_BRACKET_RIGHT, getKeyword(), SeqSyntax.CURLY_BRACKET_LEFT);
      case ELSE_IF ->
          Joiner.on(SeqSyntax.SPACE)
              .join(
                  SeqSyntax.CURLY_BRACKET_RIGHT,
                  getKeyword(),
                  SeqStringUtil.wrapInBrackets(pExpression.orElseThrow().toASTString()),
                  SeqSyntax.CURLY_BRACKET_LEFT);
    };
  }

  /** Use only for {@link BranchType#ELSE}, since it doesn't require a {@link CExpression}. */
  public String buildPrefix() {
    checkArgument(this.equals(BranchType.ELSE), "BranchType must be ELSE");
    return buildPrefix(Optional.empty());
  }

  /**
   * Use only for {@link BranchType#IF} or {@link BranchType#ELSE_IF}, since they require a {@link
   * CExpression}.
   */
  public String buildPrefix(CExpression pExpression) {
    checkArgument(
        this.equals(BranchType.IF) || this.equals(BranchType.ELSE_IF),
        "BranchType must be IF or ELSE_IF");
    return buildPrefix(Optional.of(pExpression));
  }
}
