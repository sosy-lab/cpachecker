// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

public class BitVectorEvaluationExpression implements SeqExpression {

  public final Optional<CBinaryExpression> binaryExpression;
  public final Optional<SeqExpression> logicalExpression;

  public BitVectorEvaluationExpression(
      Optional<CBinaryExpression> pBinaryExpression, Optional<SeqExpression> pLogicalExpression) {

    // both binary and logical expression can be empty, when the program has no global variables
    binaryExpression = pBinaryExpression;
    logicalExpression = pLogicalExpression;
  }

  @Override
  public String toASTString() {
    if (binaryExpression.isPresent()) {
      return binaryExpression.orElseThrow().toASTString();
    } else if (logicalExpression.isPresent()) {
      return logicalExpression.orElseThrow().toASTString();
    }
    return SeqSyntax.EMPTY_STRING;
  }
}
