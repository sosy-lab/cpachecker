// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.SeqAssumptionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.RwLockNumReadersWritersFlag;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.CFAEdgeSubstitute;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqRwLockWrLockStatement implements SeqThreadStatement {

  private final MPOROptions options;

  private final RwLockNumReadersWritersFlag rwLockFlags;

  private final CLeftHandSide pcLeftHandSide;

  private final ImmutableSet<CFAEdgeSubstitute> substituteEdges;

  private final Optional<Integer> targetPc;

  private final Optional<SeqBlockLabelStatement> targetGoto;

  private final ImmutableList<SeqInjectedStatement> injectedStatements;

  SeqRwLockWrLockStatement(
      MPOROptions pOptions,
      RwLockNumReadersWritersFlag pRwLockFlags,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<CFAEdgeSubstitute> pSubstituteEdges,
      int pTargetPc) {

    options = pOptions;
    rwLockFlags = pRwLockFlags;
    pcLeftHandSide = pPcLeftHandSide;
    substituteEdges = pSubstituteEdges;
    targetPc = Optional.of(pTargetPc);
    targetGoto = Optional.empty();
    injectedStatements = ImmutableList.of();
  }

  private SeqRwLockWrLockStatement(
      MPOROptions pOptions,
      RwLockNumReadersWritersFlag pRwLockFlags,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<CFAEdgeSubstitute> pSubstituteEdges,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    options = pOptions;
    rwLockFlags = pRwLockFlags;
    pcLeftHandSide = pPcLeftHandSide;
    substituteEdges = pSubstituteEdges;
    targetPc = pTargetPc;
    targetGoto = pTargetGoto;
    injectedStatements = pInjectedStatements;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    CExpressionAssignmentStatement setWritersToOne =
        SeqStatementBuilder.buildExpressionAssignmentStatement(
            rwLockFlags.writersIdExpression, SeqIntegerLiteralExpressions.INT_1);

    CFunctionCallStatement assumptionWriters =
        SeqAssumptionBuilder.buildAssumption(rwLockFlags.writerEqualsZero);
    CFunctionCallStatement assumptionReaders =
        SeqAssumptionBuilder.buildAssumption(rwLockFlags.readersEqualsZero);

    String injected =
        SeqThreadStatementUtil.buildInjectedStatementsString(
            options, pcLeftHandSide, targetPc, targetGoto, injectedStatements);

    return assumptionWriters.toASTString()
        + assumptionReaders.toASTString()
        + setWritersToOne
        + injected;
  }

  @Override
  public ImmutableSet<CFAEdgeSubstitute> getSubstituteEdges() {
    return substituteEdges;
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return targetPc;
  }

  @Override
  public Optional<SeqBlockLabelStatement> getTargetGoto() {
    return targetGoto;
  }

  @Override
  public ImmutableList<SeqInjectedStatement> getInjectedStatements() {
    return injectedStatements;
  }

  @Override
  public SeqThreadStatement cloneWithTargetPc(int pTargetPc) {
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
  public SeqThreadStatement cloneWithTargetGoto(SeqBlockLabelStatement pLabel) {
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
  public SeqThreadStatement cloneReplacingInjectedStatements(
      ImmutableList<SeqInjectedStatement> pReplacingInjectedStatements) {
    return new SeqRwLockWrLockStatement(
        options,
        rwLockFlags,
        pcLeftHandSide,
        substituteEdges,
        targetPc,
        targetGoto,
        pReplacingInjectedStatements);
  }

  @Override
  public SeqThreadStatement cloneAppendingInjectedStatements(
      ImmutableList<SeqInjectedStatement> pAppendingInjectedStatements) {
    return new SeqRwLockWrLockStatement(
        options,
        rwLockFlags,
        pcLeftHandSide,
        substituteEdges,
        targetPc,
        targetGoto,
        SeqThreadStatementUtil.appendInjectedStatements(this, pAppendingInjectedStatements));
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
