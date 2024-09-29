// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

public class AssignExpr implements SeqExpression {

  // TODO restrictions? create assignable interface?
  public final SeqExpression preceding;

  public final SeqExpression subsequent;

  public AssignExpr(SeqExpression pPreceding, SeqExpression pSubsequent) {
    preceding = pPreceding;
    subsequent = pSubsequent;
  }

  @Override
  public String toString() {
    return preceding
        + SeqSyntax.SPACE
        + SeqOperator.ASSIGN
        + SeqSyntax.SPACE
        + subsequent
        + SeqSyntax.SEMICOLON
        + SeqSyntax.SPACE;
  }
}
