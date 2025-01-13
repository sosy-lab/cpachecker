// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqFunctionCallExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

public class SeqFunctionCallStatement implements SeqStatement {

  private final SeqFunctionCallExpression expression;

  public SeqFunctionCallStatement(SeqFunctionCallExpression pExpression) {
    expression = pExpression;
  }

  @Override
  public String toASTString() {
    return expression.toASTString() + SeqSyntax.SEMICOLON;
  }
}
