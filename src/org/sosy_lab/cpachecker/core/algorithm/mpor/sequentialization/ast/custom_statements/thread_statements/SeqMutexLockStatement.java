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
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.SeqAssumptionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.MutexLockedFlag;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Represents a statement that simulates calls to {@code pthread_mutex_lock} of the form:
 *
 * <p>{@code assume(!m_LOCKED);}
 */
public class SeqMutexLockStatement extends ASeqThreadStatement {

  private final MutexLockedFlag mutexLockedFlag;

  SeqMutexLockStatement(
      MPOROptions pOptions,
      MutexLockedFlag pMutexLockedFlag,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pTargetPc) {

    super(
        pOptions,
        pSubstituteEdges,
        pPcLeftHandSide,
        Optional.of(pTargetPc),
        Optional.empty(),
        ImmutableList.of());
    mutexLockedFlag = pMutexLockedFlag;
  }

  private SeqMutexLockStatement(
      MPOROptions pOptions,
      MutexLockedFlag pMutexLockedFlag,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    super(pOptions, pSubstituteEdges, pPcLeftHandSide, pTargetPc, pTargetGoto, pInjectedStatements);
    mutexLockedFlag = pMutexLockedFlag;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    CFunctionCallStatement assumeCall =
        SeqAssumptionBuilder.buildAssumption(mutexLockedFlag.notLockedExpression);
    CExpressionAssignmentStatement setMutexLockedTrue =
        SeqStatementBuilder.buildExpressionAssignmentStatement(
            mutexLockedFlag.idExpression, SeqIntegerLiteralExpressions.INT_1);

    String injected =
        SeqThreadStatementUtil.buildInjectedStatementsString(
            options, pcLeftHandSide, targetPc, targetGoto, injectedStatements);

    return Joiner.on(SeqSyntax.SPACE)
        .join(assumeCall.toASTString(), setMutexLockedTrue.toASTString(), injected);
  }

  @Override
  public SeqMutexLockStatement withTargetPc(int pTargetPc) {
    return new SeqMutexLockStatement(
        options,
        mutexLockedFlag,
        pcLeftHandSide,
        substituteEdges,
        Optional.of(pTargetPc),
        Optional.empty(),
        injectedStatements);
  }

  @Override
  public ASeqThreadStatement withTargetGoto(SeqBlockLabelStatement pLabel) {
    return new SeqMutexLockStatement(
        options,
        mutexLockedFlag,
        pcLeftHandSide,
        substituteEdges,
        Optional.empty(),
        Optional.of(pLabel),
        injectedStatements);
  }

  @Override
  public ASeqThreadStatement withInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    return new SeqMutexLockStatement(
        options,
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

  @Override
  public boolean onlyWritesPc() {
    return false;
  }
}
