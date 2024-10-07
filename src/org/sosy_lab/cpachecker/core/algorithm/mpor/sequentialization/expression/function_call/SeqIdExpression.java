// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.function_call;

import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.SeqExpression;

/** Wraps a SeqExpression around a CIdExpression so that it can be used as a parameter. */
public class SeqIdExpression implements SeqExpression {

  private final CIdExpression idExpression;

  public SeqIdExpression(CIdExpression pIdExpression) {
    idExpression = pIdExpression;
  }

  @Override
  public String toASTString() {
    return idExpression.toASTString();
  }
}
