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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqInstrumentation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqInstrumentationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqInstrumentationType;
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
          SeqInstrumentation ignoreSleepReductionStatement =
              buildIgnoreSleepReductionStatement(
                  pStatement, evaluationExpression.orElseThrow(), newTarget);
          return pStatement.withInstrumentation(
              replaceReductionAssumptions(
                  pStatement.data().instrumentation(), ignoreSleepReductionStatement));
        }
      }
    }
    // no injection possible -> return statement as is
    return pStatement;
  }

  private SeqInstrumentation buildIgnoreSleepReductionStatement(
      SeqThreadStatement pStatement,
      CExportExpression pBitVectorEvaluationExpression,
      SeqThreadStatementClause pTargetClause)
      throws UnrecognizedCodeException {

    ImmutableList.Builder<SeqInstrumentation> reductionAssumptions = ImmutableList.builder();
    for (SeqInstrumentation instrumentation : pStatement.data().instrumentation()) {
      if (instrumentation.type().equals(SeqInstrumentationType.UNTIL_CONFLICT_REDUCTION)) {
        reductionAssumptions.add(instrumentation);
      }
    }
    CBinaryExpression roundMaxExpression =
        utils
            .binaryExpressionBuilder()
            .buildBinaryExpression(
                SeqIdExpressions.ROUND_MAX,
                SeqIntegerLiteralExpressions.INT_0,
                BinaryOperator.EQUALS);
    return SeqInstrumentationBuilder.buildIgnoreSleepReductionStatement(
        roundMaxExpression,
        pBitVectorEvaluationExpression,
        reductionAssumptions.build(),
        pTargetClause.getFirstBlock().buildLabelStatement());
  }

  private static ImmutableList<SeqInstrumentation> replaceReductionAssumptions(
      ImmutableList<SeqInstrumentation> pInjectedStatements,
      SeqInstrumentation pIgnoreSleepStatements) {

    ImmutableList.Builder<SeqInstrumentation> newInstrumentation = ImmutableList.builder();
    newInstrumentation.add(pIgnoreSleepStatements);
    for (SeqInstrumentation instrumentation : pInjectedStatements) {
      if (instrumentation.type().equals(SeqInstrumentationType.UNTIL_CONFLICT_REDUCTION)) {
        newInstrumentation.add(instrumentation);
      }
    }
    return newInstrumentation.build();
  }
}
