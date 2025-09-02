// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Represents an injected call to {@code reach_error} so that the sequentialization actually adopts
 * {@code reach_error}s from the input program for the property {@code unreach-call.prp} instead of
 * inlining the function.
 */
public class SeqReachErrorStatement implements SeqThreadStatement {

  private final CLeftHandSide pcLeftHandSide;

  private final ImmutableSet<SubstituteEdge> substituteEdges;

  private final int targetPc;

  private final ImmutableList<SeqInjectedStatement> injectedStatements;

  SeqReachErrorStatement(
      CLeftHandSide pPcLeftHandSide, ImmutableSet<SubstituteEdge> pSubstituteEdges, int pTargetPc) {

    pcLeftHandSide = pPcLeftHandSide;
    substituteEdges = pSubstituteEdges;
    targetPc = pTargetPc;
    injectedStatements = ImmutableList.of();
  }

  private SeqReachErrorStatement(
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pTargetPc,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    pcLeftHandSide = pPcLeftHandSide;
    substituteEdges = pSubstituteEdges;
    targetPc = pTargetPc;
    injectedStatements = pInjectedStatements;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    String injected =
        SeqThreadStatementUtil.buildInjectedStatements(
            pcLeftHandSide, Optional.of(targetPc), Optional.empty(), injectedStatements);
    return Sequentialization.inputReachErrorDummy + SeqSyntax.SPACE + injected;
  }

  @Override
  public ImmutableSet<SubstituteEdge> getSubstituteEdges() {
    return substituteEdges;
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return Optional.of(targetPc);
  }

  @Override
  public Optional<SeqBlockLabelStatement> getTargetGoto() {
    return Optional.empty();
  }

  @Override
  public ImmutableList<SeqInjectedStatement> getInjectedStatements() {
    return injectedStatements;
  }

  @Override
  public SeqReachErrorStatement cloneWithTargetPc(int pTargetPc) {
    checkArgument(
        pTargetPc == Sequentialization.EXIT_PC,
        "reach_errors should only be cloned with exit pc %s",
        Sequentialization.EXIT_PC);
    return new SeqReachErrorStatement(pcLeftHandSide, substituteEdges, pTargetPc);
  }

  @Override
  public SeqThreadStatement cloneWithTargetGoto(SeqBlockLabelStatement pLabel) {
    throw new UnsupportedOperationException(
        this.getClass().getSimpleName() + " do not have target goto");
  }

  @Override
  public SeqThreadStatement cloneReplacingInjectedStatements(
      ImmutableList<SeqInjectedStatement> pReplacingInjectedStatements) {

    return new SeqReachErrorStatement(
        pcLeftHandSide, substituteEdges, targetPc, pReplacingInjectedStatements);
  }

  @Override
  public SeqThreadStatement cloneAppendingInjectedStatements(
      ImmutableList<SeqInjectedStatement> pAppendedInjectedStatements) {

    return new SeqReachErrorStatement(
        pcLeftHandSide,
        substituteEdges,
        targetPc,
        SeqThreadStatementUtil.appendInjectedStatements(this, pAppendedInjectedStatements));
  }

  @Override
  public boolean isLinkable() {
    return false;
  }

  @Override
  public boolean synchronizesThreads() {
    return false;
  }

  @Override
  public boolean onlyWritesPc() {
    return false;
  }
}
