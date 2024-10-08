// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom.expression;

import static com.google.common.base.Preconditions.checkArgument;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;

/**
 * Wraps a SeqExpression around a CExpression so that it can be used as a parameter in a function
 * call.
 */
public class CToSeqExpression implements SeqExpression {

  private final CExpression expression;

  /**
   * Wraps a SeqExpression around a CExpression so that it can be used as a parameter in a function
   * call.
   */
  public CToSeqExpression(CExpression pExpression) {
    checkArgument(isPermittedType(pExpression), "pExpression is not allowed: " + pExpression);
    expression = pExpression;
  }

  private boolean isPermittedType(CExpression pExpression) {
    return pExpression instanceof CIdExpression || pExpression instanceof CBinaryExpression;
  }

  @Override
  public String toASTString() {
    return expression.toASTString();
  }
}
