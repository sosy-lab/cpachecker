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
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.BitVectorVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.evaluation.BitVectorEvaluationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqBitVectorEvaluationStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqIgnoreSleepReductionStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClause;
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

  SeqThreadStatement injectIgnoreSleepReductionIntoStatement(SeqThreadStatement pStatement)
      throws UnrecognizedCodeException {

    // if valid target pc found, inject bit vector write and evaluation statements
    if (pStatement.data().targetPc().isPresent()) {
      int targetPc = pStatement.data().targetPc().orElseThrow();
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
            return pStatement;
          }
          SeqIgnoreSleepReductionStatement ignoreSleepReductionStatement =
              buildIgnoreSleepReductionStatement(
                  pStatement, evaluationExpression.orElseThrow(), newTarget);
          return pStatement.withInjectedStatements(
              replaceReductionAssumptions(
                  pStatement.data().injectedStatements(), ignoreSleepReductionStatement));
        }
      }
    }
    // no injection possible -> return statement as is
    return pStatement;
  }

  private SeqIgnoreSleepReductionStatement buildIgnoreSleepReductionStatement(
      SeqThreadStatement pStatement,
      CExportExpression pBitVectorEvaluationExpression,
      SeqThreadStatementClause pTargetClause)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqInjectedStatement> reductionAssumptions = ImmutableList.builder();
    for (SeqInjectedStatement injectedStatement : pStatement.data().injectedStatements()) {
      if (injectedStatement instanceof SeqBitVectorEvaluationStatement bitVectorStatement) {
        reductionAssumptions.add(bitVectorStatement);
      }
    }
    CBinaryExpression roundMaxExpression =
        utils
            .binaryExpressionBuilder()
            .buildBinaryExpression(
                SeqIdExpressions.ROUND_MAX,
                SeqIntegerLiteralExpressions.INT_0,
                BinaryOperator.EQUALS);
    return new SeqIgnoreSleepReductionStatement(
        roundMaxExpression,
        pBitVectorEvaluationExpression,
        reductionAssumptions.build(),
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
