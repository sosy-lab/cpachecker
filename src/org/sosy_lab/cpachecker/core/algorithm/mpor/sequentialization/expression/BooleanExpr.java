// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

public class BooleanExpr implements SeqExpression {

  // TODO this should be restricted to BooleanExpr, Value, Variable, ArrayExpr
  public final SeqExpression preceding;

  public final String operator;

  public final SeqExpression subsequent;

  public BooleanExpr(SeqExpression pPreceding, String pOperator, SeqExpression pSubsequent) {
    preceding = pPreceding;
    operator = pOperator;
    subsequent = pSubsequent;
  }

  @Override
  public String toString() {
    return preceding + SeqSyntax.SPACE + operator + SeqSyntax.SPACE + subsequent;
  }
}
