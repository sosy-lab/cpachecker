// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression;

import static com.google.common.base.Preconditions.checkArgument;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;

/**
 * Wraps a SeqExpression around a CExpression so that it can be used as a parameter in a function
 * call.
 */
public class CToSeqExpression implements SeqExpression {

  private final CExpression expression;

  /**
   * Wraps a SeqExpression around a CExpression so that it can be used as a parameter in a function
   * call and in logical expressions.
   */
  public CToSeqExpression(CExpression pExpression) {
    checkArgument(
        isPermittedType(pExpression),
        "expression type is not allowed: " + "%s",
        pExpression.getClass());
    expression = pExpression;
  }

  private boolean isPermittedType(CExpression pExpression) {
    return pExpression instanceof CIdExpression
        || pExpression instanceof CBinaryExpression
        || pExpression instanceof CIntegerLiteralExpression;
  }

  @Override
  public String toASTString() {
    return expression.toASTString();
  }
}
