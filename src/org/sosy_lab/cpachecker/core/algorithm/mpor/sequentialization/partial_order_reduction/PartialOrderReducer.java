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
import java.util.Optional;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadCreationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorReduction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class PartialOrderReducer {

  /**
   * Applies a Partial Order Reduction based on the settings in {@code pOptions}, or returns {@code
   * pClauses} as is if disabled.
   */
  public static ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> reduce(
      MPOROptions pOptions,
      Optional<BitVectorVariables> pBitVectorVariables,
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> pClauses,
      CBinaryExpressionBuilder pBinaryExpressionBuilder,
      LogManager pLogger)
      throws UnrecognizedCodeException {

    if (pOptions.linkReduction
        && pOptions.bitVectorReduction.equals(BitVectorReduction.ACCESS_ONLY)) {
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> linked =
          StatementLinker.link(pClauses);
      return BitVectorAccessInjector.inject(
          pOptions, pBitVectorVariables.orElseThrow(), linked, pBinaryExpressionBuilder, pLogger);

    } else if (pOptions.linkReduction
        && pOptions.bitVectorReduction.equals(BitVectorReduction.READ_AND_WRITE)) {
      ImmutableMap<MPORThread, ImmutableList<SeqThreadStatementClause>> linked =
          StatementLinker.link(pClauses);
      return BitVectorReadWriteInjector.inject(
          pOptions, pBitVectorVariables.orElseThrow(), linked, pBinaryExpressionBuilder, pLogger);

    } else if (pOptions.linkReduction) {
      return StatementLinker.link(pClauses);
    }
    return pClauses;
  }

  public static boolean requiresAssumeEvaluation(
      SeqThreadStatement pCurrentStatement, SeqThreadStatementClause pTarget) {

    if (pCurrentStatement instanceof SeqThreadCreationStatement) {
      if (!(pTarget.block.getFirstStatement() instanceof SeqThreadCreationStatement)) {
        // if this statement creates a thread and the target does not, enforce context switch.
        // this is needed e.g. in pthread-wmm/mix000.oepc - otherwise an assume call triggers a
        // pre-emptive thread termination and an incorrect 'true' verdict
        return true;
      }
    }
    return false;
  }
}
