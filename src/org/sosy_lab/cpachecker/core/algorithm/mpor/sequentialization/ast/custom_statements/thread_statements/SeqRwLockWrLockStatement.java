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
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.SeqAssumptionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.RwLockNumReadersWritersFlag;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqRwLockWrLockStatement extends CSeqThreadStatement {

  private final RwLockNumReadersWritersFlag rwLockFlags;

  SeqRwLockWrLockStatement(
      MPOROptions pOptions,
      RwLockNumReadersWritersFlag pRwLockFlags,
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
    rwLockFlags = pRwLockFlags;
  }

  private SeqRwLockWrLockStatement(
      MPOROptions pOptions,
      RwLockNumReadersWritersFlag pRwLockFlags,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    super(pOptions, pSubstituteEdges, pPcLeftHandSide, pTargetPc, pTargetGoto, pInjectedStatements);
    rwLockFlags = pRwLockFlags;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    CExpressionAssignmentStatement setWritersToOne =
        SeqStatementBuilder.buildExpressionAssignmentStatement(
            rwLockFlags.writersIdExpression(), SeqIntegerLiteralExpressions.INT_1);

    CFunctionCallStatement assumptionWriters =
        SeqAssumptionBuilder.buildAssumption(rwLockFlags.writerEqualsZero());
    CFunctionCallStatement assumptionReaders =
        SeqAssumptionBuilder.buildAssumption(rwLockFlags.readersEqualsZero());

    String injected =
        SeqThreadStatementUtil.buildInjectedStatementsString(
            options, pcLeftHandSide, targetPc, targetGoto, injectedStatements);

    return assumptionWriters.toASTString()
        + assumptionReaders.toASTString()
        + setWritersToOne
        + injected;
  }

  @Override
  public CSeqThreadStatement withTargetPc(int pTargetPc) {
    return new SeqRwLockWrLockStatement(
        options,
        rwLockFlags,
        pcLeftHandSide,
        substituteEdges,
        Optional.of(pTargetPc),
        Optional.empty(),
        injectedStatements);
  }

  @Override
  public CSeqThreadStatement withTargetGoto(SeqBlockLabelStatement pLabel) {
    return new SeqRwLockWrLockStatement(
        options,
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
    return new SeqRwLockWrLockStatement(
        options,
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
    return true;
  }

  @Override
  public boolean onlyWritesPc() {
    return false;
  }
}
