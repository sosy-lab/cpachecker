// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
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
      ImmutableSet<MemoryLocation> pAllMemoryLocations,
      Optional<BitVectorVariables> pBitVectorVariables,
      Optional<PointerAssignments> pPointerAssignments,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    if (pOptions.linkReduction) {
      PointerAssignments pointerAssignments = pPointerAssignments.orElseThrow();

      if (pOptions.bitVectorReduction && pOptions.conflictReduction) {
        ImmutableListMultimap<MPORThread, SeqThreadStatementClause> linked =
            StatementLinker.link(pOptions, pClauses, pAllMemoryLocations, pointerAssignments);
        ImmutableListMultimap<MPORThread, SeqThreadStatementClause> withBitVectors =
            BitVectorInjector.injectWithEvaluations(
                pOptions,
                linked,
                pAllMemoryLocations,
                pBitVectorVariables.orElseThrow(),
                pointerAssignments,
                pBinaryExpressionBuilder,
                pLogger);
        return ConflictResolver.resolve(
            pOptions,
            withBitVectors,
            pAllMemoryLocations,
            pBitVectorVariables.orElseThrow(),
            pointerAssignments,
            pBinaryExpressionBuilder,
            pLogger);

      } else if (pOptions.bitVectorReduction) {
        ImmutableListMultimap<MPORThread, SeqThreadStatementClause> linked =
            StatementLinker.link(pOptions, pClauses, pAllMemoryLocations, pointerAssignments);
        return BitVectorInjector.injectWithEvaluations(
            pOptions,
            linked,
            pAllMemoryLocations,
            pBitVectorVariables.orElseThrow(),
            pointerAssignments,
            pBinaryExpressionBuilder,
            pLogger);

      } else if (pOptions.conflictReduction) {
        ImmutableListMultimap<MPORThread, SeqThreadStatementClause> linked =
            StatementLinker.link(pOptions, pClauses, pAllMemoryLocations, pointerAssignments);
        ImmutableListMultimap<MPORThread, SeqThreadStatementClause> withBitVectors =
            BitVectorInjector.injectWithoutEvaluations(
                pOptions,
                linked,
                pAllMemoryLocations,
                pBitVectorVariables.orElseThrow(),
                pointerAssignments,
                pBinaryExpressionBuilder,
                pLogger);
        return ConflictResolver.resolve(
            pOptions,
            withBitVectors,
            pAllMemoryLocations,
            pBitVectorVariables.orElseThrow(),
            pointerAssignments,
            pBinaryExpressionBuilder,
            pLogger);

      } else {
        return StatementLinker.link(pOptions, pClauses, pAllMemoryLocations, pointerAssignments);
      }
    }
    return pClauses;
  }
}
