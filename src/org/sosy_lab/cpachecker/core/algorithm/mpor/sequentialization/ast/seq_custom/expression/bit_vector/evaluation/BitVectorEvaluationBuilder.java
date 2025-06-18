// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.evaluation;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class BitVectorEvaluationBuilder {

  public static BitVectorEvaluationExpression buildEvaluationByReduction(
      MPOROptions pOptions,
      ImmutableSet<MPORThread> pOtherThreads,
      ImmutableSet<CVariableDeclaration> pDirectAccessVariables,
      ImmutableSet<CVariableDeclaration> pDirectReadVariables,
      ImmutableSet<CVariableDeclaration> pDirectWriteVariables,
      BitVectorVariables pBitVectorVariables,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return switch (pOptions.bitVectorReduction) {
      case NONE ->
          throw new IllegalArgumentException(
              "bitVectorReduction must be enabled to build evaluation expression");
      case ACCESS_ONLY ->
          BitVectorAccessEvaluationBuilder.buildEvaluationByEncoding(
              pOptions,
              pOtherThreads,
              pDirectAccessVariables,
              pBitVectorVariables,
              pBinaryExpressionBuilder);
      case READ_AND_WRITE ->
          BitVectorReadWriteEvaluationBuilder.buildEvaluationByEncoding(
              pOptions,
              pOtherThreads,
              pDirectReadVariables,
              pDirectWriteVariables,
              pBitVectorVariables,
              pBinaryExpressionBuilder);
    };
  }
}
