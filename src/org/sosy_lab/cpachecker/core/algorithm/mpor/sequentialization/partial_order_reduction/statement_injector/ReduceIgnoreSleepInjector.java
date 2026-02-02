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
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.evaluation.BitVectorEvaluationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.clause.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqBitVectorEvaluationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqIgnoreSleepReductionStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements.CSeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExportExpression;

record ReduceIgnoreSleepInjector(
    MPOROptions options,
    MPORThread activeThread,
    ImmutableSet<MPORThread> otherThreads,
    ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap,
    BitVectorVariables bitVectorVariables,
    SequentializationUtils utils) {

  CSeqThreadStatement injectIgnoreSleepReductionIntoStatement(CSeqThreadStatement pCurrentStatement)
      throws UnrecognizedCodeException {

    // if valid target pc found, inject bit vector write and evaluation statements
    if (pCurrentStatement.getTargetPc().isPresent()) {
      int targetPc = pCurrentStatement.getTargetPc().orElseThrow();
      // exclude exit pc, don't want 'assume(conflict)' there
      if (targetPc != ProgramCounterVariables.EXIT_PC) {
        SeqThreadStatementClause newTarget = Objects.requireNonNull(labelClauseMap.get(targetPc));
        if (StatementInjector.isReductionAllowed(options, newTarget)) {
          Optional<CExportExpression> evaluationExpression =
              BitVectorEvaluationBuilder.buildVariableOnlyEvaluation(
                  options, activeThread, otherThreads, bitVectorVariables, utils);
          // if the bv evaluation is empty, then the program contains no global memory locations
          // -> no injection necessary
          if (evaluationExpression.isEmpty()) {
            return pCurrentStatement;
          }
          SeqIgnoreSleepReductionStatement ignoreSleepReductionStatement =
              buildIgnoreSleepReductionStatement(
                  pCurrentStatement, evaluationExpression.orElseThrow(), newTarget);
          return pCurrentStatement.withInjectedStatements(
              replaceReductionAssumptions(
                  pCurrentStatement.getInjectedStatements(), ignoreSleepReductionStatement));
        }
      }
    }
    // no injection possible -> return statement as is
    return pCurrentStatement;
  }

  private SeqIgnoreSleepReductionStatement buildIgnoreSleepReductionStatement(
      CSeqThreadStatement pStatement,
      CExportExpression pBitVectorEvaluationExpression,
      SeqThreadStatementClause pTargetClause) {

    ImmutableList.Builder<SeqInjectedStatement> reductionAssumptions = ImmutableList.builder();
    for (SeqInjectedStatement injectedStatement : pStatement.getInjectedStatements()) {
      if (injectedStatement instanceof SeqBitVectorEvaluationStatement bitVectorStatement) {
        reductionAssumptions.add(bitVectorStatement);
      }
    }
    return new SeqIgnoreSleepReductionStatement(
        SeqIdExpressions.ROUND_MAX,
        pBitVectorEvaluationExpression,
        reductionAssumptions.build(),
        utils.binaryExpressionBuilder(),
        pTargetClause.getFirstBlock().getLabel());
  }

  private static ImmutableList<SeqInjectedStatement> replaceReductionAssumptions(
      ImmutableList<SeqInjectedStatement> pInjectedStatements,
      SeqIgnoreSleepReductionStatement pIgnoreSleepStatements) {

    ImmutableList.Builder<SeqInjectedStatement> newInjected = ImmutableList.builder();
    newInjected.add(pIgnoreSleepStatements);
    for (SeqInjectedStatement injectedStatement : pInjectedStatements) {
      if (!(injectedStatement instanceof SeqBitVectorEvaluationStatement)) {
        newInjected.add(injectedStatement);
      }
    }
    return newInjected.build();
  }
}
