// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction;

import com.google.common.collect.ImmutableListMultimap;
import java.util.Optional;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.statement_injector.StatementInjector;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class PartialOrderReducer {

  /**
   * Applies a Partial Order Reduction based on the settings in {@code pOptions}, or returns {@code
   * pClauses} as is if disabled.
   */
  public static ImmutableListMultimap<MPORThread, SeqThreadStatementClause> reduce(
      MPOROptions pOptions,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      Optional<BitVectorVariables> pBitVectorVariables,
      Optional<MemoryModel> pMemoryModel,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    if (pOptions.linkReduction) {
      MemoryModel memoryModel = pMemoryModel.orElseThrow();
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> linked =
          StatementLinker.link(pOptions, pClauses, memoryModel);
      if (pOptions.areBitVectorsEnabled()) {
        return StatementInjector.injectStatements(
            pOptions,
            linked,
            pBitVectorVariables.orElseThrow(),
            memoryModel,
            pBinaryExpressionBuilder,
            pLogger);
      }
      return linked;
    }
    return pClauses;
  }
}
