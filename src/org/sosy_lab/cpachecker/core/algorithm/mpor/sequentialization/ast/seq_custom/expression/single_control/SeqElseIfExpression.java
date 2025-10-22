// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqElseIfExpression implements SeqSingleControlExpression {

  private final Optional<CExpression> cExpression;

  private final Optional<SeqExpression> seqExpression;

  public SeqElseIfExpression(CExpression pCExpression) {
    cExpression = Optional.of(pCExpression);
    seqExpression = Optional.empty();
  }

  public SeqElseIfExpression(SeqExpression pSeqExpression) {
    cExpression = Optional.empty();
    seqExpression = Optional.of(pSeqExpression);
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    String expression =
        SingleControlExpressionUtil.buildExpressionString(cExpression, seqExpression);
    return SingleControlExpressionUtil.buildStatementString(this, expression);
  }

  @Override
  public SingleControlStatementType getEncoding() {
    return SingleControlStatementType.ELSE_IF;
  }
}
