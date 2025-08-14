// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction;

import com.google.common.collect.ImmutableCollection;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteUtil;
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
      ImmutableCollection<SubstituteEdge> pSubstituteEdges,
      ImmutableListMultimap<MPORThread, SeqThreadStatementClause> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    if (pOptions.linkReduction) {
      ImmutableSetMultimap<CVariableDeclaration, CSimpleDeclaration> pointerAssignments =
          SubstituteUtil.mapPointerAssignments(pSubstituteEdges);
      ImmutableTable<ThreadEdge, CParameterDeclaration, CSimpleDeclaration>
          pointerParameterAssignments =
              SeqThreadStatementClauseUtil.mapPointerParameterAssignments(pClauses);

      if (pOptions.bitVectorReduction && pOptions.conflictReduction) {
        ImmutableListMultimap<MPORThread, SeqThreadStatementClause> linked =
            StatementLinker.link(pClauses, pointerAssignments, pointerParameterAssignments);
        ImmutableListMultimap<MPORThread, SeqThreadStatementClause> withBitVectors =
            BitVectorInjector.injectWithEvaluations(
                pOptions,
                pBitVectorVariables.orElseThrow(),
                linked,
                pointerAssignments,
                pointerParameterAssignments,
                pBinaryExpressionBuilder,
                pLogger);
        return ConflictResolver.resolve(
            pOptions,
            withBitVectors,
            pointerAssignments,
            pointerParameterAssignments,
            pBitVectorVariables.orElseThrow(),
            pPcVariables,
            pBinaryExpressionBuilder,
            pLogger);

      } else if (pOptions.bitVectorReduction) {
        ImmutableListMultimap<MPORThread, SeqThreadStatementClause> linked =
            StatementLinker.link(pClauses, pointerAssignments, pointerParameterAssignments);
        return BitVectorInjector.injectWithEvaluations(
            pOptions,
            pBitVectorVariables.orElseThrow(),
            linked,
            pointerAssignments,
            pointerParameterAssignments,
            pBinaryExpressionBuilder,
            pLogger);

      } else if (pOptions.conflictReduction) {
        ImmutableListMultimap<MPORThread, SeqThreadStatementClause> linked =
            StatementLinker.link(pClauses, pointerAssignments, pointerParameterAssignments);
        ImmutableListMultimap<MPORThread, SeqThreadStatementClause> withBitVectors =
            BitVectorInjector.injectWithoutEvaluations(
                pOptions,
                pBitVectorVariables.orElseThrow(),
                linked,
                pointerAssignments,
                pointerParameterAssignments,
                pBinaryExpressionBuilder,
                pLogger);
        return ConflictResolver.resolve(
            pOptions,
            withBitVectors,
            pointerAssignments,
            pointerParameterAssignments,
            pBitVectorVariables.orElseThrow(),
            pPcVariables,
            pBinaryExpressionBuilder,
            pLogger);

      } else {
        return StatementLinker.link(pClauses, pointerAssignments, pointerParameterAssignments);
      }
    }
    return pClauses;
  }
}
