// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.goto_labels.SeqLoopHeadLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

/** Represents a statement that simulates calls to {@code pthread_join}. */
public class SeqThreadJoinStatement implements SeqCaseBlockStatement {

  private final Optional<SeqLoopHeadLabelStatement> loopHeadLabel;

  public final CIdExpression threadJoinsThread;

  private final CLeftHandSide pcLeftHandSide;

  private final Optional<Integer> targetPc;

  private final Optional<String> targetGoto;

  private final ImmutableList<SeqInjectedStatement> injectedStatements;

  private final ImmutableList<SeqCaseBlockStatement> concatenatedStatements;

  SeqThreadJoinStatement(CIdExpression pThreadJoins, int pTargetPc, CLeftHandSide pPcLeftHandSide) {
    loopHeadLabel = Optional.empty();
    threadJoinsThread = pThreadJoins;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.of(pTargetPc);
    targetGoto = Optional.empty();
    injectedStatements = ImmutableList.of();
    concatenatedStatements = ImmutableList.of();
  }

  private SeqThreadJoinStatement(
      Optional<SeqLoopHeadLabelStatement> pLoopHeadLabel,
      CIdExpression pThreadJoins,
      CLeftHandSide pPcLeftHandSide,
      Optional<Integer> pTargetPc,
      Optional<String> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements,
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements) {

    loopHeadLabel = pLoopHeadLabel;
    threadJoinsThread = pThreadJoins;
    targetPc = pTargetPc;
    targetGoto = pTargetGoto;
    pcLeftHandSide = pPcLeftHandSide;
    injectedStatements = pInjectedStatements;
    concatenatedStatements = pConcatenatedStatements;
  }

  @Override
  public String toASTString() {
    CExpressionAssignmentStatement setJoinsFalse =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, threadJoinsThread, SeqIntegerLiteralExpression.INT_0);

    String targetStatements =
        SeqStringUtil.buildTargetStatements(
            pcLeftHandSide, targetPc, targetGoto, injectedStatements, concatenatedStatements);

    return SeqStringUtil.buildLoopHeadLabel(loopHeadLabel)
        + setJoinsFalse.toASTString()
        + SeqSyntax.SPACE
        + targetStatements;
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return targetPc;
  }

  @Override
  public ImmutableList<SeqInjectedStatement> getInjectedStatements() {
    return injectedStatements;
  }

  @Override
  public ImmutableList<SeqCaseBlockStatement> getConcatenatedStatements() {
    return concatenatedStatements;
  }

  @Override
  public SeqThreadJoinStatement cloneWithTargetPc(int pTargetPc) {
    return new SeqThreadJoinStatement(
        loopHeadLabel,
        threadJoinsThread,
        pcLeftHandSide,
        Optional.of(pTargetPc),
        Optional.empty(),
        injectedStatements,
        concatenatedStatements);
  }

  @Override
  public SeqCaseBlockStatement cloneWithTargetGoto(String pLabel) {
    return new SeqThreadJoinStatement(
        loopHeadLabel,
        threadJoinsThread,
        pcLeftHandSide,
        Optional.empty(),
        Optional.of(pLabel),
        injectedStatements,
        concatenatedStatements);
  }

  @Override
  public SeqCaseBlockStatement cloneWithInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    return new SeqThreadJoinStatement(
        loopHeadLabel,
        threadJoinsThread,
        pcLeftHandSide,
        targetPc,
        targetGoto,
        pInjectedStatements,
        concatenatedStatements);
  }

  @Override
  public SeqCaseBlockStatement cloneWithLoopHeadLabel(SeqLoopHeadLabelStatement pLoopHeadLabel) {
    return new SeqThreadJoinStatement(
        Optional.of(pLoopHeadLabel),
        threadJoinsThread,
        pcLeftHandSide,
        targetPc,
        targetGoto,
        injectedStatements,
        concatenatedStatements);
  }

  @Override
  public SeqCaseBlockStatement cloneWithConcatenatedStatements(
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements) {

    return new SeqThreadJoinStatement(
        loopHeadLabel,
        threadJoinsThread,
        pcLeftHandSide,
        Optional.empty(),
        Optional.empty(),
        injectedStatements,
        pConcatenatedStatements);
  }

  @Override
  public boolean isConcatenable() {
    return true;
  }

  @Override
  public boolean isCriticalSectionStart() {
    return false;
  }

  @Override
  public boolean onlyWritesPc() {
    return false;
  }
}
