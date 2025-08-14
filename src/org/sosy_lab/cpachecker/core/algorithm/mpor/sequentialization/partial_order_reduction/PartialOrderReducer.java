// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableTable;
import java.util.Optional;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class PartialOrderReducer {

  /**
   * Applies a Partial Order Reduction based on the settings in {@code pOptions}, or returns {@code
   * pClauses} as is if disabled.
   */
  public static ImmutableListMultimap<MPORThread, SeqThreadStatementClause> reduce(
      MPOROptions pOptions,
      Optional<BitVectorVariables> pBitVectorVariables,
      PcVariables pPcVariables,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      ImmutableSetMultimap<CVariableDeclaration, CSimpleDeclaration> pPointerAssignments,
      ImmutableTable<ThreadEdge, CParameterDeclaration, CSimpleDeclaration>
          pPointerParameterAssignments,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    if (pOptions.linkReduction && pOptions.bitVectorReduction && pOptions.conflictReduction) {
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> linked =
          StatementLinker.link(pClauses, pPointerAssignments, pPointerParameterAssignments);
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> withBitVectors =
          BitVectorInjector.injectWithEvaluations(
              pOptions,
              pBitVectorVariables.orElseThrow(),
              linked,
              pPointerAssignments,
              pPointerParameterAssignments,
              pBinaryExpressionBuilder,
              pLogger);
      return ConflictResolver.resolve(
          pOptions,
          withBitVectors,
          pPointerAssignments,
          pPointerParameterAssignments,
          pBitVectorVariables.orElseThrow(),
          pPcVariables,
          pBinaryExpressionBuilder,
          pLogger);

    } else if (pOptions.linkReduction && pOptions.bitVectorReduction) {
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> linked =
          StatementLinker.link(pClauses, pPointerAssignments, pPointerParameterAssignments);
      return BitVectorInjector.injectWithEvaluations(
          pOptions,
          pBitVectorVariables.orElseThrow(),
          linked,
          pPointerAssignments,
          pPointerParameterAssignments,
          pBinaryExpressionBuilder,
          pLogger);

    } else if (pOptions.linkReduction && pOptions.conflictReduction) {
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> linked =
          StatementLinker.link(pClauses, pPointerAssignments, pPointerParameterAssignments);
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> withBitVectors =
          BitVectorInjector.injectWithoutEvaluations(
              pOptions,
              pBitVectorVariables.orElseThrow(),
              linked,
              pPointerAssignments,
              pPointerParameterAssignments,
              pBinaryExpressionBuilder,
              pLogger);
      return ConflictResolver.resolve(
          pOptions,
          withBitVectors,
          pPointerAssignments,
          pPointerParameterAssignments,
          pBitVectorVariables.orElseThrow(),
          pPcVariables,
          pBinaryExpressionBuilder,
          pLogger);

    } else if (pOptions.linkReduction) {
      return StatementLinker.link(pClauses, pPointerAssignments, pPointerParameterAssignments);
    }
    return pClauses;
  }
}
