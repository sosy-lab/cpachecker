// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.statement_injector;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqInstrumentation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqInstrumentationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

record ReduceSingleActiveThreadInjector(
    MPOROptions options,
    ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap,
    CBinaryExpressionBuilder binaryExpressionBuilder) {

  ReduceSingleActiveThreadInjector {
    checkArgument(
        options.reduceSingleActiveThread(),
        "reduceSingleActiveThread must be enabled when a ReduceSingleActiveThreadInjector is"
            + " created.");
  }

  /**
   * Injects updates to {@code thread_count} when a thread is created or terminated, and the
   * reduction instrumentation:
   *
   * <pre>{@code
   * if (thread_count == 1) {
   *    goto next_statement;
   * }
   * }</pre>
   */
  SeqThreadStatement injectSingleActiveThreadReduction(SeqThreadStatement pStatement)
      throws UnrecognizedCodeException {

    SeqThreadStatement withThreadCountUpdates = injectThreadCountUpdates(pStatement);
    return tryInjectSingleActiveThreadGoto(withThreadCountUpdates);
  }

  private SeqThreadStatement injectThreadCountUpdates(SeqThreadStatement pStatement)
      throws UnrecognizedCodeException {

    // use a list for thread_count updates, since we can increment and decrement in one statement
    // if the statement creates another thread but also terminates the current thread.
    List<CExpressionAssignmentStatement> countUpdates = new ArrayList<>();
    // if a thread is created -> thread_count = thread_count + 1
    if (pStatement.data().getType().equals(SeqThreadStatementType.THREAD_CREATION)) {
      countUpdates.add(
          SeqStatementBuilder.buildIncrementStatement(
              SeqIdExpressions.THREAD_COUNT, binaryExpressionBuilder));
    }
    // if a thread exits -> thread_count = thread_count - 1
    if (pStatement.isTargetPcExit()) {
      countUpdates.add(
          SeqStatementBuilder.buildDecrementStatement(
              SeqIdExpressions.THREAD_COUNT, binaryExpressionBuilder));
    }
    ImmutableList<SeqInstrumentation> guardedGotos =
        countUpdates.stream()
            .map(SeqInstrumentationBuilder::buildThreadCountUpdateStatement)
            .collect(ImmutableList.toImmutableList());
    return SeqThreadStatementUtil.appendedInstrumentationStatement(pStatement, guardedGotos);
  }

  private SeqThreadStatement tryInjectSingleActiveThreadGoto(SeqThreadStatement pStatement)
      throws UnrecognizedCodeException {

    if (pStatement.isTargetPcValid()) {
      int targetPc = pStatement.targetPc().orElseThrow();
      SeqThreadStatementClause target = Objects.requireNonNull(labelClauseMap.get(targetPc));
      // check if the target is a separate loop
      if (!SeqThreadStatementClauseUtil.isSeparateLoopStart(options, target)) {
        // thread_count == 1
        CBinaryExpression threadCountEqualsOne =
            binaryExpressionBuilder.buildBinaryExpression(
                SeqIdExpressions.THREAD_COUNT,
                SeqIntegerLiteralExpressions.INT_1,
                BinaryOperator.EQUALS);
        SeqInstrumentation singleActiveThreadGoto =
            SeqInstrumentationBuilder.buildGuardedGotoStatement(
                threadCountEqualsOne,
                ImmutableList.of(),
                Objects.requireNonNull(target).getFirstBlock().buildLabelStatement());
        return SeqThreadStatementUtil.appendedInstrumentationStatement(
            pStatement, singleActiveThreadGoto);
      } else {
        // if the target is a loop head + noBackwardLoopGoto is enabled -> no injection
        return pStatement;
      }
    }
    // no valid target pc -> no injection
    return pStatement;
  }
}
