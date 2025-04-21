// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class PartialOrderReducer {

  /**
   * Applies a Partial Order Reduction based on the settings in {@code pOptions}, or returns {@code
   * pCaseClauses} as is if disabled.
   */
  public static ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> reduce(
      MPOROptions pOptions,
      ImmutableList.Builder<CIdExpression> pUpdatedVariables,
      BitVectorVariables pBitVectors,
      ImmutableMap<MPORThread, ImmutableList<SeqCaseClause>> pCaseClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    if (pOptions.porConcat && pOptions.porBitVector) {
      return BitVectorInjector.inject(
          pOptions,
          pBitVectors,
          StatementConcatenator.concat(pOptions, pUpdatedVariables, pCaseClauses),
          pBinaryExpressionBuilder);
    } else if (pOptions.porConcat) {
      return StatementConcatenator.concat(pOptions, pUpdatedVariables, pCaseClauses);
    }
    return pCaseClauses;
  }
}
