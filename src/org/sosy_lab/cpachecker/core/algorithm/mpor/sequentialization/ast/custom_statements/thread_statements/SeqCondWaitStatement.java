// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.SeqAssumeFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.CondSignaledFlag;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.MutexLockedFlag;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.ReductionOrder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public final class SeqCondWaitStatement extends CSeqThreadStatement {

  private final CondSignaledFlag condSignaledFlag;

  private final MutexLockedFlag mutexLockedFlag;

  SeqCondWaitStatement(
      ReductionOrder pReductionOrder,
      CondSignaledFlag pCondSignaledFlag,
      MutexLockedFlag pMutexLockedFlag,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pTargetPc) {

    super(pReductionOrder, pSubstituteEdges, pPcLeftHandSide, pTargetPc);
    condSignaledFlag = pCondSignaledFlag;
    mutexLockedFlag = pMutexLockedFlag;
  }

  private SeqCondWaitStatement(
      ReductionOrder pReductionOrder,
      CondSignaledFlag pCondSignaledFlag,
      MutexLockedFlag pMutexLockedFlag,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    super(
        pReductionOrder,
        pSubstituteEdges,
        pPcLeftHandSide,
        pTargetPc,
        pTargetGoto,
        pInjectedStatements);
    condSignaledFlag = pCondSignaledFlag;
    mutexLockedFlag = pMutexLockedFlag;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    // for a breakdown on this behavior, cf. https://linux.die.net/man/3/pthread_cond_wait
    // step 1: the calling thread blocks on the condition variable -> assume(signaled == 1)
    CFunctionCallStatement assumeSignaled =
        SeqAssumeFunction.buildAssumeFunctionCallStatement(condSignaledFlag.isSignaledExpression());
    CExpressionAssignmentStatement setSignaledFalse =
        SeqStatementBuilder.buildExpressionAssignmentStatement(
            condSignaledFlag.idExpression(), SeqIntegerLiteralExpressions.INT_0);
    // step 2: on return, the mutex is locked and owned by the calling thread -> mutex_locked = 1
    CExpressionAssignmentStatement setMutexLockedTrue =
        SeqStatementBuilder.buildExpressionAssignmentStatement(
            mutexLockedFlag.idExpression(), SeqIntegerLiteralExpressions.INT_1);

    String injected =
        SeqThreadStatementUtil.buildInjectedStatementsString(
            reductionOrder, pcLeftHandSide, targetPc, targetGoto, injectedStatements);

    return Joiner.on(SeqSyntax.SPACE)
        .join(
            assumeSignaled.toASTString(),
            setSignaledFalse.toASTString(),
            setMutexLockedTrue.toASTString(),
            injected);
  }

  @Override
  public CSeqThreadStatement withTargetPc(int pTargetPc) {
    return new SeqCondWaitStatement(
        reductionOrder,
        condSignaledFlag,
        mutexLockedFlag,
        pcLeftHandSide,
        substituteEdges,
        Optional.of(pTargetPc),
        Optional.empty(),
        injectedStatements);
  }

  @Override
  public CSeqThreadStatement withTargetGoto(SeqBlockLabelStatement pLabel) {
    return new SeqCondWaitStatement(
        reductionOrder,
        condSignaledFlag,
        mutexLockedFlag,
        pcLeftHandSide,
        substituteEdges,
        Optional.empty(),
        Optional.of(pLabel),
        injectedStatements);
  }

  @Override
  public CSeqThreadStatement withInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    return new SeqCondWaitStatement(
        reductionOrder,
        condSignaledFlag,
        mutexLockedFlag,
        pcLeftHandSide,
        substituteEdges,
        targetPc,
        targetGoto,
        pInjectedStatements);
  }

  @Override
  public boolean isLinkable() {
    return true;
  }

  @Override
  public boolean synchronizesThreads() {
    return true;
  }
}
