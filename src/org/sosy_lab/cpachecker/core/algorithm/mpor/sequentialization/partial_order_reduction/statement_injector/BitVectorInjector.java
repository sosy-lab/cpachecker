// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.statement_injector;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector.SeqBitVectorEvaluationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class BitVectorInjector {

  static SeqThreadStatement injectBitVectorReductionIntoStatement(
      MPOROptions pOptions,
      ImmutableSet<MPORThread> pOtherThreads,
      SeqThreadStatement pCurrentStatement,
      final ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      final ImmutableMap<Integer, SeqThreadStatementBlock> pLabelBlockMap,
      final BitVectorVariables pBitVectorVariables,
      final MemoryModel pMemoryModel,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    // if valid target pc found, inject bit vector write and evaluation statements
    if (pCurrentStatement.getTargetPc().isPresent()) {
      ImmutableList.Builder<SeqInjectedStatement> newInjected = ImmutableList.builder();
      int targetPc = pCurrentStatement.getTargetPc().orElseThrow();
      // exclude exit pc, don't want 'assume(conflict)' there
      if (targetPc != Sequentialization.EXIT_PC) {
        SeqThreadStatementClause newTarget = Objects.requireNonNull(pLabelClauseMap.get(targetPc));
        if (StatementInjector.isReductionAllowed(pOptions, newTarget)) {
          SeqBitVectorEvaluationStatement evaluationStatement =
              StatementInjector.buildBitVectorEvaluationStatement(
                  pOptions,
                  pOtherThreads,
                  pLabelBlockMap,
                  newTarget.getFirstBlock(),
                  pBitVectorVariables,
                  pMemoryModel,
                  pBinaryExpressionBuilder);
          newInjected.add(evaluationStatement);
        }
        return pCurrentStatement.cloneAppendingInjectedStatements(newInjected.build());
      }
    }
    // no injection possible -> return statement as is
    return pCurrentStatement;
  }
}
