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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
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

      if (pOptions.bitVectorReduction && pOptions.conflictReduction) {
        ImmutableListMultimap<MPORThread, SeqThreadStatementClause> linked =
            StatementLinker.link(pOptions, pClauses, memoryModel);
        ImmutableListMultimap<MPORThread, SeqThreadStatementClause> withBitVectorReduction =
            BitVectorInjector.injectBitVectorReduction(
                pOptions,
                linked,
                pBitVectorVariables.orElseThrow(),
                memoryModel,
                pBinaryExpressionBuilder,
                pLogger);
        return ConflictResolver.resolve(
            pOptions,
            withBitVectorReduction,
            pBitVectorVariables.orElseThrow(),
            memoryModel,
            pBinaryExpressionBuilder,
            pLogger);

      } else if (pOptions.bitVectorReduction) {
        ImmutableListMultimap<MPORThread, SeqThreadStatementClause> linked =
            StatementLinker.link(pOptions, pClauses, memoryModel);
        return BitVectorInjector.injectBitVectorReduction(
            pOptions,
            linked,
            pBitVectorVariables.orElseThrow(),
            memoryModel,
            pBinaryExpressionBuilder,
            pLogger);

      } else if (pOptions.conflictReduction) {
        ImmutableListMultimap<MPORThread, SeqThreadStatementClause> linked =
            StatementLinker.link(pOptions, pClauses, memoryModel);
        ImmutableListMultimap<MPORThread, SeqThreadStatementClause> withBitVectorAssignments =
            BitVectorAssignmentInjector.injectBitVectorAssignments(
                pOptions, linked, pBitVectorVariables.orElseThrow(), memoryModel, pLogger);
        return ConflictResolver.resolve(
            pOptions,
            withBitVectorAssignments,
            pBitVectorVariables.orElseThrow(),
            memoryModel,
            pBinaryExpressionBuilder,
            pLogger);

      } else {
        return StatementLinker.link(pOptions, pClauses, memoryModel);
      }
    }
    return pClauses;
  }

  // boolean helpers ===============================================================================

  /**
   * Checks whether bit vector injections are allowed, i.e. if they do not result in interleaving
   * loss.
   */
  static boolean isReductionAllowed(MPOROptions pOptions, SeqThreadStatementClause pTarget) {

    // if the target starts with a thread synchronization (i.e. assume), do not inject
    return !SeqThreadStatementUtil.anySynchronizesThreads(pTarget.getAllStatements())
        // check based on pOptions if the target is a loop head that must remain separate
        && !SeqThreadStatementClauseUtil.isSeparateLoopStart(pOptions, pTarget);
  }
}
