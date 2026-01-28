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
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionTree;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.block.SeqThreadStatementBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqBitVectorEvaluationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.CSeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation.BitVectorEvaluationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

record ReduceUntilConflictInjector(
    MPOROptions options,
    ImmutableSet<MPORThread> otherThreads,
    ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap,
    ImmutableMap<Integer, SeqThreadStatementBlock> labelBlockMap,
    BitVectorVariables bitVectorVariables,
    MemoryModel memoryModel,
    SequentializationUtils utils) {

  CSeqThreadStatement injectUntilConflictReductionIntoStatement(CSeqThreadStatement pStatement)
      throws UnrecognizedCodeException {

    // if valid target pc found, inject bit vector write and evaluation statements
    if (pStatement.getTargetPc().isPresent()) {
      ImmutableList.Builder<SeqInjectedStatement> newInjected = ImmutableList.builder();
      int targetPc = pStatement.getTargetPc().orElseThrow();
      // exclude exit pc, don't want 'assume(conflict)' there
      if (targetPc != ProgramCounterVariables.EXIT_PC) {
        SeqThreadStatementClause newTarget = Objects.requireNonNull(labelClauseMap.get(targetPc));
        if (StatementInjector.isReductionAllowed(options, newTarget)) {
          SeqBitVectorEvaluationStatement evaluationStatement =
              buildBitVectorEvaluationStatement(newTarget.getFirstBlock());
          newInjected.add(evaluationStatement);
        }
        return SeqThreadStatementUtil.appendedInjectedStatementsToStatement(
            pStatement, newInjected.build());
      }
    }
    // no injection possible -> return statement as is
    return pStatement;
  }

  // Bit Vector Evaluations =======================================================================

  private SeqBitVectorEvaluationStatement buildBitVectorEvaluationStatement(
      SeqThreadStatementBlock pTargetBlock) throws UnrecognizedCodeException {

    Optional<CExpressionTree> evaluationExpression =
        BitVectorEvaluationBuilder.buildEvaluationByDirectVariableAccesses(
            options,
            otherThreads,
            labelBlockMap,
            pTargetBlock,
            bitVectorVariables,
            memoryModel,
            utils);
    return new SeqBitVectorEvaluationStatement(
        options, evaluationExpression, pTargetBlock.getLabel());
  }
}
