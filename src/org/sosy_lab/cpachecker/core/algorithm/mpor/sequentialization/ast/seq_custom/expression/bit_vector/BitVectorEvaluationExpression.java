// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqExpression;

public class BitVectorEvaluationExpression implements SeqExpression {

  public final Optional<CBinaryExpression> binaryExpression;
  public final Optional<SeqExpression> logicalExpression;

  public BitVectorEvaluationExpression(
      Optional<CBinaryExpression> pBinaryExpression, Optional<SeqExpression> pLogicalExpression) {

    checkArgument(
        pBinaryExpression.isEmpty() || pLogicalExpression.isEmpty(),
        "either pBinary- or pLogicalExpression must be empty");
    checkArgument(
        pBinaryExpression.isPresent() || pLogicalExpression.isPresent(),
        "either pBinary- or pLogicalExpression must be present");
    binaryExpression = pBinaryExpression;
    logicalExpression = pLogicalExpression;
  }

  @Override
  public String toASTString() {
    if (binaryExpression.isPresent()) {
      return binaryExpression.orElseThrow().toASTString();
    } else {
      return logicalExpression.orElseThrow().toASTString();
    }
  }
}
