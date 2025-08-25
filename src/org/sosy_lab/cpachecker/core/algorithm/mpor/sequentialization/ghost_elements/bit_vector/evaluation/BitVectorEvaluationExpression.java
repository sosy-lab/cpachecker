// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.logical.SeqLogicalNotExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class BitVectorEvaluationExpression implements SeqExpression {

  public final Optional<CBinaryExpression> binaryExpression;
  // TODO use SeqLogicalExpression here
  public final Optional<SeqExpression> logicalExpression;

  public BitVectorEvaluationExpression(
      Optional<CBinaryExpression> pBinaryExpression, Optional<SeqExpression> pLogicalExpression) {

    // both the binary and logical expression can be empty due to local thread synchronizations
    binaryExpression = pBinaryExpression;
    logicalExpression = pLogicalExpression;
  }

  public static BitVectorEvaluationExpression empty() {
    return new BitVectorEvaluationExpression(Optional.empty(), Optional.empty());
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    if (binaryExpression.isPresent()) {
      return binaryExpression.orElseThrow().toASTString();
    } else if (logicalExpression.isPresent()) {
      return logicalExpression.orElseThrow().toASTString();
    }
    return SeqSyntax.EMPTY_STRING;
  }

  public SeqLogicalNotExpression negate() {
    if (binaryExpression.isPresent()) {
      return new SeqLogicalNotExpression(binaryExpression.orElseThrow());
    }
    if (logicalExpression.isPresent()) {
      return new SeqLogicalNotExpression(logicalExpression.orElseThrow());
    }
    throw new IllegalArgumentException(
        "cannot negate, both binary- and logical expressions are empty");
  }

  public boolean isEmpty() {
    return binaryExpression.isEmpty() && logicalExpression.isEmpty();
  }
}
