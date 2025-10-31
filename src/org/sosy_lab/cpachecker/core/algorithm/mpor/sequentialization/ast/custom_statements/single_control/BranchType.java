// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

public enum BranchType {
  IF("if"),
  ELSE("else");

  private final String keyword;

  BranchType(String pKeyword) {
    keyword = pKeyword;
  }

  private String getKeyword() {
    return keyword;
  }

  private String buildPrefix(Optional<String> pExpression) {
    return switch (this) {
      case IF ->
          getKeyword() + SeqSyntax.SPACE + SeqStringUtil.wrapInBrackets(pExpression.orElseThrow());
      case ELSE -> SeqSyntax.SPACE + getKeyword() + SeqSyntax.SPACE;
    };
  }

  /** Use only for {@link BranchType#ELSE}, since it doesn't require a {@link CExpression}. */
  public String buildPrefix() {
    checkArgument(this.equals(BranchType.ELSE), "BranchType must be ELSE");
    return buildPrefix(Optional.empty());
  }

  /** Use only for {@link BranchType#IF}, since it requires a {@link CExpression}. */
  public String buildPrefix(String pExpression) {
    checkArgument(this.equals(BranchType.IF), "BranchType must be IF or ELSE_IF");
    return buildPrefix(Optional.of(pExpression));
  }
}
