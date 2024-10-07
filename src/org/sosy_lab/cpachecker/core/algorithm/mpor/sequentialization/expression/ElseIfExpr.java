// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;

public class ElseIfExpr implements SeqExpression {

  public final IfExpr ifExpr;

  public ElseIfExpr(IfExpr pIfExpr) {
    ifExpr = pIfExpr;
  }

  @Override
  public String toASTString() {
    return SeqToken.ELSE + SeqSyntax.SPACE + ifExpr.toASTString();
  }
}
