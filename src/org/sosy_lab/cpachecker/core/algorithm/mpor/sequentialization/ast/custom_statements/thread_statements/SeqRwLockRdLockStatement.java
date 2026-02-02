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
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions.SeqAssumeFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.RwLockNumReadersWritersFlag;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public final class SeqRwLockRdLockStatement extends CSeqThreadStatement {

  private final RwLockNumReadersWritersFlag rwLockFlags;

  SeqRwLockRdLockStatement(
      RwLockNumReadersWritersFlag pRwLockFlags,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pTargetPc) {

    super(pSubstituteEdges, pPcLeftHandSide, pTargetPc);
    rwLockFlags = pRwLockFlags;
  }

  private SeqRwLockRdLockStatement(
      RwLockNumReadersWritersFlag pRwLockFlags,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    super(pSubstituteEdges, pPcLeftHandSide, pTargetPc, pTargetGoto, pInjectedStatements);
    rwLockFlags = pRwLockFlags;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    CFunctionCallStatement assumption =
        SeqAssumeFunction.buildAssumeFunctionCallStatement(rwLockFlags.writerEqualsZero());
    String injected =
        SeqThreadStatementUtil.buildInjectedStatementsString(
            pcLeftHandSide, targetPc, targetGoto, injectedStatements, pAAstNodeRepresentation);

    return assumption.toASTString(pAAstNodeRepresentation)
        + rwLockFlags.readersIncrement().toASTString(pAAstNodeRepresentation)
        + injected;
  }

  @Override
  public CSeqThreadStatement withTargetPc(int pTargetPc) {
    return new SeqRwLockRdLockStatement(
        rwLockFlags,
        pcLeftHandSide,
        substituteEdges,
        Optional.of(pTargetPc),
        Optional.empty(),
        injectedStatements);
  }

  @Override
  public CSeqThreadStatement withTargetGoto(SeqBlockLabelStatement pLabel) {
    return new SeqRwLockRdLockStatement(
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
    return new SeqRwLockRdLockStatement(
        rwLockFlags, pcLeftHandSide, substituteEdges, targetPc, targetGoto, pInjectedStatements);
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
