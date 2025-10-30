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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.bit_vector.SeqBitVectorEvaluationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.bit_vector.SeqConflictOrderStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.bit_vector.SeqIgnoreSleepReductionStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.CSeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation.BitVectorEvaluationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation.BitVectorEvaluationExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

class ReduceIgnoreSleepInjector {

  // Public Interface ==============================================================================

  static CSeqThreadStatement injectIgnoreSleepReductionIntoStatement(
      MPOROptions pOptions,
      MPORThread pActiveThread,
      ImmutableSet<MPORThread> pOtherThreads,
      CSeqThreadStatement pCurrentStatement,
      ImmutableMap<Integer, SeqThreadStatementClause> pLabelClauseMap,
      BitVectorVariables pBitVectorVariables,
      SequentializationUtils pUtils)
      throws UnrecognizedCodeException {

    // if valid target pc found, inject bit vector write and evaluation statements
    if (pCurrentStatement.getTargetPc().isPresent()) {
      int targetPc = pCurrentStatement.getTargetPc().orElseThrow();
      // exclude exit pc, don't want 'assume(conflict)' there
      if (targetPc != Sequentialization.EXIT_PC) {
        SeqThreadStatementClause newTarget = Objects.requireNonNull(pLabelClauseMap.get(targetPc));
        if (StatementInjector.isReductionAllowed(pOptions, newTarget)) {
          BitVectorEvaluationExpression evaluationExpression =
              BitVectorEvaluationBuilder.buildVariableOnlyEvaluation(
                  pOptions, pActiveThread, pOtherThreads, pBitVectorVariables, pUtils);
          SeqIgnoreSleepReductionStatement ignoreSleepReductionStatement =
              buildIgnoreSleepReductionStatement(
                  pCurrentStatement,
                  evaluationExpression,
                  newTarget,
                  pUtils.getBinaryExpressionBuilder());
          return pCurrentStatement.withInjectedStatements(
              replaceReductionAssumptions(
                  pCurrentStatement.getInjectedStatements(), ignoreSleepReductionStatement));
        }
      }
    }
    // no injection possible -> return statement as is
    return pCurrentStatement;
  }

  private static SeqIgnoreSleepReductionStatement buildIgnoreSleepReductionStatement(
      CSeqThreadStatement pStatement,
      BitVectorEvaluationExpression pBitVectorEvaluationExpression,
      SeqThreadStatementClause pTargetClause,
      CBinaryExpressionBuilder pBinaryExpressionBuilder) {

    ImmutableList.Builder<SeqInjectedStatement> reductionAssumptions = ImmutableList.builder();
    for (SeqInjectedStatement injectedStatement : pStatement.getInjectedStatements()) {
      if (injectedStatement instanceof SeqBitVectorEvaluationStatement bitVectorStatement) {
        reductionAssumptions.add(bitVectorStatement);
      }
      if (injectedStatement instanceof SeqConflictOrderStatement conflictOrderStatement) {
        reductionAssumptions.add(conflictOrderStatement);
      }
    }
    return new SeqIgnoreSleepReductionStatement(
        SeqIdExpressions.ROUND_MAX,
        pBitVectorEvaluationExpression,
        pTargetClause.getFirstBlock().getLabel(),
        reductionAssumptions.build(),
        pBinaryExpressionBuilder);
  }

  private static ImmutableList<SeqInjectedStatement> replaceReductionAssumptions(
      ImmutableList<SeqInjectedStatement> pInjectedStatements,
      SeqIgnoreSleepReductionStatement pIgnoreSleepStatements) {

    ImmutableList.Builder<SeqInjectedStatement> newInjected = ImmutableList.builder();
    newInjected.add(pIgnoreSleepStatements);
    for (SeqInjectedStatement injectedStatement : pInjectedStatements) {
      if (!(injectedStatement instanceof SeqBitVectorEvaluationStatement)
          && !(injectedStatement instanceof SeqConflictOrderStatement)) {
        newInjected.add(injectedStatement);
      }
    }
    return newInjected.build();
  }
}
