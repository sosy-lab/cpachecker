// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.statement_injector;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableMap;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqInstrumentationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementClauseUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public record PrevThreadAssignmentInjector(
    MPOROptions options,
    int numThreads,
    MPORThread activeThread,
    ImmutableMap<Integer, SeqThreadStatementClause> labelClauseMap) {

  SeqThreadStatement injectPrevThreadUpdatesIntoStatement(SeqThreadStatement pStatement) {
    checkState(options.isPrevThreadVariableRequired());

    if (pStatement.targetPc().isPresent()) {
      int targetPc = pStatement.targetPc().orElseThrow();

      // if a thread exits, set prev_thread to NUM_THREADS
      if (targetPc == ProgramCounterVariables.EXIT_PC) {
        return injectPrevThreadUpdateIntoStatement(pStatement, numThreads);
      }
      // if targetPc != EXIT_PC, then pLabelClause contains targetPc, otherwise NPE
      SeqThreadStatementClause targetClause = Objects.requireNonNull(labelClauseMap.get(targetPc));

      if (options.abortCommutingContextSwitches()) {
        // For sync locations, set prev_thread to NUM_THREADS. This is necessary when
        // abortCommutingContextSwitches (ACS) is enabled, otherwise the analysis is unsound.
        //
        // Simple example: The previous thread T_p is at a sync location that uses assume. The
        // current thread T_c has an ACS instrumentation and because it is not in conflict with T_p,
        // T_c aborts. But T_p may e.g. call pthread_join (= sync location) on T_c which results in
        // both aborting, and then the simulation is an unsound underapproximation.
        if (SeqThreadStatementUtil.anySynchronizesThreads(targetClause.getAllStatements())) {
          return injectPrevThreadUpdateIntoStatement(pStatement, numThreads);
        }
      }

      if (options.abortPreviousThreadReentry()) {
        // For loop heads that must remain separate, set prev_thread to NUM_THREADS. This is
        // necessary when abortPreviousReentry is enabled, otherwise the analysis is unsound.
        //
        // This is because the 'round < round_max' instrumentation is not placed when the target
        // loop head must remain separate. In this case the thread simulation was not
        // nondeterministically exited but deterministically, which means that aborting the re-entry
        // of prev_thread can result in some lost interleavings.
        if (SeqThreadStatementClauseUtil.isSeparateLoopStart(options, targetClause)) {
          return injectPrevThreadUpdateIntoStatement(pStatement, numThreads);
        }
      }

      // for all other target pc, set prev_thread to current thread id and update prev bitvectors
      return injectPrevThreadUpdateIntoStatement(pStatement, activeThread.id());
    }
    // no valid target pc -> no conflict order required
    return pStatement;
  }

  private SeqThreadStatement injectPrevThreadUpdateIntoStatement(
      SeqThreadStatement pStatement, int pPrevThreadValue) {

    CExpressionAssignmentStatement prevThreadExit =
        SeqStatementBuilder.buildExpressionAssignmentStatement(
            SeqIdExpressions.PREV_THREAD,
            SeqExpressionBuilder.buildIntegerLiteralExpression(pPrevThreadValue));
    return SeqThreadStatementUtil.appendedInstrumentationStatement(
        pStatement, SeqInstrumentationBuilder.buildPrevThreadUpdateStatement(prevThreadExit));
  }
}
