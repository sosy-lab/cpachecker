// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control.SeqBranchStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.RwLockNumReadersWritersFlag;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.ReductionOrder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public final class SeqRwLockUnlockStatement extends CSeqThreadStatement {

  private final RwLockNumReadersWritersFlag rwLockFlags;

  SeqRwLockUnlockStatement(
      ReductionOrder pReductionOrder,
      RwLockNumReadersWritersFlag pRwLockFlags,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pTargetPc) {

    super(pReductionOrder, pSubstituteEdges, pPcLeftHandSide, pTargetPc);
    rwLockFlags = pRwLockFlags;
  }

  private SeqRwLockUnlockStatement(
      ReductionOrder pReductionOrder,
      RwLockNumReadersWritersFlag pRwLockFlags,
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
    rwLockFlags = pRwLockFlags;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    CExpressionAssignmentStatement setNumWritersToZero =
        SeqStatementBuilder.buildExpressionAssignmentStatement(
            rwLockFlags.writersIdExpression(), SeqIntegerLiteralExpressions.INT_0);

    SeqBranchStatement ifStatement =
        new SeqBranchStatement(
            rwLockFlags.writerEqualsZero().toASTString(),
            ImmutableList.of(rwLockFlags.readersDecrement().toASTString()),
            ImmutableList.of(setNumWritersToZero.toASTString()));
    String injected =
        SeqThreadStatementUtil.buildInjectedStatementsString(
            reductionOrder, pcLeftHandSide, targetPc, targetGoto, injectedStatements);
    return ifStatement.toASTString() + injected;
  }

  @Override
  public CSeqThreadStatement withTargetPc(int pTargetPc) {
    return new SeqRwLockUnlockStatement(
        reductionOrder,
        rwLockFlags,
        pcLeftHandSide,
        substituteEdges,
        Optional.of(pTargetPc),
        Optional.empty(),
        injectedStatements);
  }

  @Override
  public CSeqThreadStatement withTargetGoto(SeqBlockLabelStatement pLabel) {
    return new SeqRwLockUnlockStatement(
        reductionOrder,
        rwLockFlags,
        pcLeftHandSide,
        substituteEdges,
        Optional.empty(),
        Optional.of(pLabel),
        injectedStatements);
  }

  @Override
  public CSeqThreadStatement withInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    return new SeqRwLockUnlockStatement(
        reductionOrder,
        rwLockFlags,
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
    return false;
  }
}
