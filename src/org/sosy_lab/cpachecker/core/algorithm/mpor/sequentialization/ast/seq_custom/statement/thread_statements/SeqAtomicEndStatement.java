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
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqBlockGotoLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqAtomicEndStatement implements SeqThreadStatement {

  private final CLeftHandSide pcLeftHandSide;

  private final ImmutableSet<SubstituteEdge> substituteEdges;

  private final Optional<Integer> targetPc;

  private final Optional<SeqBlockGotoLabelStatement> targetGoto;

  private final ImmutableList<SeqInjectedStatement> injectedStatements;

  SeqAtomicEndStatement(
      CLeftHandSide pPcLeftHandSide, ImmutableSet<SubstituteEdge> pSubstituteEdges, int pTargetPc) {

    pcLeftHandSide = pPcLeftHandSide;
    substituteEdges = pSubstituteEdges;
    targetPc = Optional.of(pTargetPc);
    targetGoto = Optional.empty();
    injectedStatements = ImmutableList.of();
  }

  private SeqAtomicEndStatement(
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      Optional<Integer> pTargetPc,
      Optional<SeqBlockGotoLabelStatement> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    pcLeftHandSide = pPcLeftHandSide;
    substituteEdges = pSubstituteEdges;
    targetPc = pTargetPc;
    targetGoto = pTargetGoto;
    injectedStatements = pInjectedStatements;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    String targetStatements =
        SeqStringUtil.buildTargetStatements(
            pcLeftHandSide, targetPc, targetGoto, injectedStatements);
    return SeqStringUtil.wrapInBlockComment(
            PthreadFunctionType.__VERIFIER_ATOMIC_END.name + SeqSyntax.SEMICOLON)
        + SeqSyntax.SPACE
        + targetStatements;
  }

  @Override
  public ImmutableSet<SubstituteEdge> getSubstituteEdges() {
    return substituteEdges;
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return targetPc;
  }

  @Override
  public Optional<SeqBlockGotoLabelStatement> getTargetGoto() {
    return targetGoto;
  }

  @Override
  public ImmutableList<SeqInjectedStatement> getInjectedStatements() {
    return injectedStatements;
  }

  @Override
  public SeqThreadStatement cloneWithTargetPc(int pTargetPc) {
    return new SeqAtomicEndStatement(
        pcLeftHandSide,
        substituteEdges,
        Optional.of(pTargetPc),
        Optional.empty(),
        injectedStatements);
  }

  @Override
  public SeqThreadStatement cloneWithTargetGoto(SeqBlockGotoLabelStatement pLabel) {
    return new SeqAtomicEndStatement(
        pcLeftHandSide, substituteEdges, Optional.empty(), Optional.of(pLabel), injectedStatements);
  }

  @Override
  public SeqThreadStatement cloneWithInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    return new SeqAtomicEndStatement(
        pcLeftHandSide, substituteEdges, targetPc, targetGoto, pInjectedStatements);
  }

  @Override
  public boolean isLinkable() {
    return true;
  }

  @Override
  public boolean synchronizesThreads() {
    return false;
  }

  @Override
  public boolean onlyWritesPc() {
    // this statement does only write a pc, but we still include it for improved overview
    return false;
  }
}
