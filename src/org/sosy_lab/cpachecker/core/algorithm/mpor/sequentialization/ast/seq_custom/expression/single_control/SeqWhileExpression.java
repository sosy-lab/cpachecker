// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqWhileExpression implements SeqSingleControlExpression {

  private final CExpression loopCondition;

  public SeqWhileExpression(CExpression pLoopCondition) {
    loopCondition = pLoopCondition;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    return SingleControlExpressionUtil.buildStatementString(this, loopCondition.toASTString());
  }

  @Override
  public SingleControlExpressionEncoding getEncoding() {
    return SingleControlExpressionEncoding.WHILE;
  }
}
