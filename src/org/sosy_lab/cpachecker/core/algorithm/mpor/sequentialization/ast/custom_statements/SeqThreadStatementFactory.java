// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions.SeqAssumeFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.CondSignaledFlag;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.MutexLockedFlag;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CStatementWrapper;

public class SeqThreadStatementFactory {

  // Functions for all SeqThreadStatementType

  /**
   * Takes the given {@link CExportStatement}s and appends the {@link SeqInjectedStatement} to them.
   * Given that all {@link CSeqThreadStatement}s have injected statements that are placed after the
   * actual statements, this is handled here and not by each specific {@link CSeqThreadStatement}s.
   */
  private static ImmutableList<CExportStatement> finalizeExportStatements(
      SeqThreadStatementData pData, ImmutableList<CExportStatement> pExportStatements) {

    checkState(
        pData.targetPc().isPresent() || pData.targetGoto().isPresent(),
        "Either targetPc or targetGoto must be present.");

    // first build the CExportStatements of the SeqInjectedStatement
    ImmutableList<SeqInjectedStatement> preparedInjectedStatements =
        pData.targetPc().isPresent()
            ? SeqThreadStatementUtil.prepareInjectedStatementsByTargetPc(
                pData.pcLeftHandSide(), pData.targetPc().orElseThrow(), pData.injectedStatements())
            : SeqThreadStatementUtil.prepareInjectedStatementsByTargetGoto(
                pData.targetGoto().orElseThrow(), pData.injectedStatements());

    ImmutableList<CExportStatement> injectedExportStatements =
        preparedInjectedStatements.stream()
            .flatMap(injected -> injected.toCExportStatements().stream())
            .collect(ImmutableList.toImmutableList());

    return ImmutableList.<CExportStatement>builder()
        .addAll(pExportStatements)
        .addAll(injectedExportStatements)
        .build();
  }

  // Functions for specific SeqThreadStatementType

  public static SeqThreadStatement buildCondWaitStatement(
      SeqThreadStatementData pData,
      CondSignaledFlag pCondSignaledFlag,
      MutexLockedFlag pMutexLockedFlag) {

    // for a breakdown on this behavior, cf. https://linux.die.net/man/3/pthread_cond_wait

    // step 1: the calling thread blocks on the condition variable -> assume(signaled == 1)
    CFunctionCallStatement assumeSignaled =
        SeqAssumeFunction.buildAssumeFunctionCallStatement(
            pCondSignaledFlag.isSignaledExpression());
    CExpressionAssignmentStatement setSignaledFalse =
        SeqStatementBuilder.buildExpressionAssignmentStatement(
            pCondSignaledFlag.idExpression(), SeqIntegerLiteralExpressions.INT_0);

    // step 2: on return, the mutex is locked and owned by the calling thread -> mutex_locked = 1
    CExpressionAssignmentStatement setMutexLockedTrue =
        SeqStatementBuilder.buildExpressionAssignmentStatement(
            pMutexLockedFlag.idExpression(), SeqIntegerLiteralExpressions.INT_1);

    return new SeqThreadStatement(
        pData,
        finalizeExportStatements(
            pData,
            ImmutableList.of(
                new CStatementWrapper(assumeSignaled),
                new CStatementWrapper(setSignaledFalse),
                new CStatementWrapper(setMutexLockedTrue))));
  }
}
