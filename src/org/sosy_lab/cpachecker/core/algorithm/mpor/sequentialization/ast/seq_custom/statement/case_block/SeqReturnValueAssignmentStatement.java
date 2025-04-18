// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqLoopHeadLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;

/**
 * Handles the assignments of a <strong>single</strong> return value of functions, e.g. {@code int x
 * = fib(5);} where {@code fib} has a return statement {@code return fibNumber;} then we create a
 * statement {@code x = fibNumber;} (where {@code x} is declared beforehand) in the
 * sequentialization.
 */
public class SeqReturnValueAssignmentStatement implements SeqCaseBlockStatement {

  private final Optional<SeqLoopHeadLabelStatement> loopHeadLabel;

  private final CExpressionAssignmentStatement assignment;

  private final CLeftHandSide pcLeftHandSide;

  private final ImmutableSet<SubstituteEdge> substituteEdges;
  private final Optional<Integer> targetPc;

  private final Optional<String> targetGoto;

  private final ImmutableList<SeqInjectedStatement> injectedStatements;

  private final ImmutableList<SeqCaseBlockStatement> concatenatedStatements;

  SeqReturnValueAssignmentStatement(
      CExpressionAssignmentStatement pAssignment,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pTargetPc) {

    loopHeadLabel = Optional.empty();
    assignment = pAssignment;
    pcLeftHandSide = pPcLeftHandSide;
    substituteEdges = pSubstituteEdges;
    targetPc = Optional.of(pTargetPc);
    targetGoto = Optional.empty();
    injectedStatements = ImmutableList.of();
    concatenatedStatements = ImmutableList.of();
  }

  private SeqReturnValueAssignmentStatement(
      Optional<SeqLoopHeadLabelStatement> pLoopHeadLabel,
      CExpressionAssignmentStatement pAssignment,
      CLeftHandSide pPcLeftHandSide,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      Optional<Integer> pTargetPc,
      Optional<String> pTargetGoto,
      ImmutableList<SeqInjectedStatement> pInjectedStatements,
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements) {

    loopHeadLabel = pLoopHeadLabel;
    assignment = pAssignment;
    pcLeftHandSide = pPcLeftHandSide;
    substituteEdges = pSubstituteEdges;
    targetPc = pTargetPc;
    targetGoto = pTargetGoto;
    injectedStatements = pInjectedStatements;
    concatenatedStatements = pConcatenatedStatements;
  }

  @Override
  public String toASTString() {
    String targetStatements =
        SeqStringUtil.buildTargetStatements(
            pcLeftHandSide, targetPc, targetGoto, injectedStatements, concatenatedStatements);
    return SeqStringUtil.buildLoopHeadLabel(loopHeadLabel)
        + assignment.toASTString()
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
  public ImmutableList<SeqInjectedStatement> getInjectedStatements() {
    return injectedStatements;
  }

  @Override
  public ImmutableList<SeqCaseBlockStatement> getConcatenatedStatements() {
    return concatenatedStatements;
  }

  @Override
  public SeqCaseBlockStatement cloneWithTargetPc(int pTargetPc) {

    return new SeqReturnValueAssignmentStatement(
        loopHeadLabel,
        assignment,
        pcLeftHandSide,
        substituteEdges,
        Optional.of(pTargetPc),
        Optional.empty(),
        injectedStatements,
        concatenatedStatements);
  }

  @Override
  public SeqCaseBlockStatement cloneWithTargetGoto(String pLabel) {
    return new SeqReturnValueAssignmentStatement(
        loopHeadLabel,
        assignment,
        pcLeftHandSide,
        substituteEdges,
        Optional.empty(),
        Optional.of(pLabel),
        injectedStatements,
        concatenatedStatements);
  }

  @Override
  public SeqCaseBlockStatement cloneWithInjectedStatements(
      ImmutableList<SeqInjectedStatement> pInjectedStatements) {

    return new SeqReturnValueAssignmentStatement(
        loopHeadLabel,
        assignment,
        pcLeftHandSide,
        substituteEdges,
        targetPc,
        targetGoto,
        pInjectedStatements,
        concatenatedStatements);
  }

  @Override
  public SeqCaseBlockStatement cloneWithLoopHeadLabel(SeqLoopHeadLabelStatement pLoopHeadLabel) {
    return new SeqReturnValueAssignmentStatement(
        Optional.of(pLoopHeadLabel),
        assignment,
        pcLeftHandSide,
        substituteEdges,
        targetPc,
        targetGoto,
        injectedStatements,
        concatenatedStatements);
  }

  @Override
  public SeqCaseBlockStatement cloneWithConcatenatedStatements(
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements) {

    return new SeqReturnValueAssignmentStatement(
        loopHeadLabel,
        assignment,
        pcLeftHandSide,
        substituteEdges,
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
    return true;
  }

  @Override
  public boolean onlyWritesPc() {
    return false;
  }
}
