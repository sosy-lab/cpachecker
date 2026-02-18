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
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/** Represents a conditional case block statement with {@code if} and {@code else} statements. */
public final class SeqAssumeStatement extends CSeqThreadStatement {

  public final Optional<CExpression> ifExpression;

  /** Use this constructor for the {@code if (expression)} statement. */
  SeqAssumeStatement(
      CExpression pIfExpression,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pTargetPc) {

    super(pSubstituteEdges, pPcLeftHandSide, pTargetPc);
    ifExpression = Optional.of(pIfExpression);
  }

  /** Use this constructor for the {@code else} statement without any expression. */
  SeqAssumeStatement(
      CLeftHandSide pPcLeftHandSide, ImmutableSet<SubstituteEdge> pSubstituteEdges, int pTargetPc) {

    super(pSubstituteEdges, pPcLeftHandSide, pTargetPc);
    ifExpression = Optional.empty();
  }

  private SeqAssumeStatement(
      Optional<CExpression> pIfExpression,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    super(pSubstituteEdges, pPcLeftHandSide, pTargetPc, pTargetGoto, pInjectedStatements);
    ifExpression = pIfExpression;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    // just return the injected statements, the block handles the if-else branch
    return SeqThreadStatementUtil.buildInjectedStatementsString(
        pcLeftHandSide, targetPc, targetGoto, injectedStatements);
  }

  @Override
  public SeqAssumeStatement withTargetPc(int pTargetPc) {
    return new SeqAssumeStatement(
        ifExpression,
        pcLeftHandSide,
        substituteEdges,
        Optional.of(pTargetPc),
        Optional.empty(),
        injectedStatements);
  }

  @Override
  public CSeqThreadStatement withTargetGoto(SeqBlockLabelStatement pLabel) {
    return new SeqAssumeStatement(
        ifExpression,
        pcLeftHandSide,
        substituteEdges,
        Optional.empty(),
        Optional.of(pLabel),
        injectedStatements);
  }

  @Override
  public CSeqThreadStatement withInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    return new SeqAssumeStatement(
        ifExpression, pcLeftHandSide, substituteEdges, targetPc, targetGoto, pInjectedStatements);
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
