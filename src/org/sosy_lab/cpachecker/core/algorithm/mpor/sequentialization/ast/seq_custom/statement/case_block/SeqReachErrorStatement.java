// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqLoopHeadLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;

/**
 * Represents an injected call to {@code reach_error} so that the sequentialization actually adopts
 * {@code reach_error}s from the input program for the property {@code unreach-call.prp} instead of
 * inlining the function.
 */
public class SeqReachErrorStatement implements SeqCaseBlockStatement {

  private final Optional<SeqLoopHeadLabelStatement> loopHeadLabel;

  private final CLeftHandSide pcLeftHandSide;

  private final ImmutableSet<SubstituteEdge> substituteEdges;

  private final int targetPc;

  SeqReachErrorStatement(
      CLeftHandSide pPcLeftHandSide, ImmutableSet<SubstituteEdge> pSubstituteEdges, int pTargetPc) {
    loopHeadLabel = Optional.empty();
    pcLeftHandSide = pPcLeftHandSide;
    substituteEdges = pSubstituteEdges;
    targetPc = pTargetPc;
  }

  private SeqReachErrorStatement(
      Optional<SeqLoopHeadLabelStatement> pLoopHeadLabel,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pTargetPc) {

    loopHeadLabel = pLoopHeadLabel;
    pcLeftHandSide = pPcLeftHandSide;
    substituteEdges = pSubstituteEdges;
    targetPc = pTargetPc;
  }

  @Override
  public String toASTString() {
    CExpressionAssignmentStatement pcWrite =
        SeqStatementBuilder.buildPcWrite(pcLeftHandSide, targetPc);
    return SeqStringUtil.buildLoopHeadLabel(loopHeadLabel)
        + Sequentialization.inputReachErrorDummy
        + SeqSyntax.SPACE
        + pcWrite.toASTString();
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
  public ImmutableList<SeqInjectedStatement> getInjectedStatements() {
    throw new UnsupportedOperationException(
        this.getClass().getSimpleName() + " do not have injected statements");
  }

  @Override
  public ImmutableList<SeqCaseBlockStatement> getConcatenatedStatements() {
    throw new UnsupportedOperationException(
        this.getClass().getSimpleName() + " do not have concatenated statements");
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
  public SeqCaseBlockStatement cloneWithTargetGoto(String pLabel) {
    throw new UnsupportedOperationException(
        this.getClass().getSimpleName() + " do not have target goto");
  }

  @Override
  public SeqCaseBlockStatement cloneWithInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {
    throw new UnsupportedOperationException(
        this.getClass().getSimpleName() + " do not have injected statements");
  }

  @Override
  public SeqCaseBlockStatement cloneWithLoopHeadLabel(SeqLoopHeadLabelStatement pLoopHeadLabel) {
    return new SeqReachErrorStatement(
        Optional.of(pLoopHeadLabel), pcLeftHandSide, substituteEdges, targetPc);
  }

  @Override
  public SeqCaseBlockStatement cloneWithConcatenatedStatements(
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements) {
    throw new UnsupportedOperationException(
        this.getClass().getSimpleName() + " do not have concatenated statements");
  }

  @Override
  public boolean isConcatenable() {
    return false;
  }

  @Override
  public boolean isCriticalSectionStart() {
    return true;
  }

  @Override
  public boolean onlyWritesPc() {
    return false;
  }
}
