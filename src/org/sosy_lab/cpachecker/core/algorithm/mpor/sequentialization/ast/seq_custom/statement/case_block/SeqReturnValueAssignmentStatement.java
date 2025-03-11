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
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.injected.SeqCaseBlockInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

/**
 * Handles the assignments of a <strong>single</strong> return value of functions, e.g. {@code int x
 * = fib(5);} where {@code fib} has a return statement {@code return fibNumber;} then we create a
 * statement {@code x = fibNumber;} (where {@code x} is declared beforehand) in the
 * sequentialization.
 */
public class SeqReturnValueAssignmentStatement implements SeqCaseBlockStatement {

  private final CExpressionAssignmentStatement assignment;

  private final CLeftHandSide pcLeftHandSide;

  private final Optional<Integer> targetPc;

  private final ImmutableList<SeqCaseBlockInjectedStatement> injectedStatements;

  private final ImmutableList<SeqCaseBlockStatement> concatenatedStatements;

  SeqReturnValueAssignmentStatement(
      CExpressionAssignmentStatement pAssignment, CLeftHandSide pPcLeftHandSide, int pTargetPc) {

    assignment = pAssignment;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.of(pTargetPc);
    injectedStatements = ImmutableList.of();
    concatenatedStatements = ImmutableList.of();
  }

  private SeqReturnValueAssignmentStatement(
      CExpressionAssignmentStatement pAssignment,
      CLeftHandSide pPcLeftHandSide,
      Optional<Integer> pTargetPc,
      ImmutableList<SeqCaseBlockInjectedStatement> pInjectedStatements,
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements) {

    assignment = pAssignment;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = pTargetPc;
    injectedStatements = pInjectedStatements;
    concatenatedStatements = pConcatenatedStatements;
  }

  @Override
  public String toASTString() {
    String targetStatements =
        SeqStringUtil.buildTargetStatements(
            pcLeftHandSide, targetPc, injectedStatements, concatenatedStatements);
    return assignment.toASTString() + SeqSyntax.SPACE + targetStatements;
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return targetPc;
  }

  @Override
  public ImmutableList<SeqCaseBlockInjectedStatement> getInjectedStatements() {
    return injectedStatements;
  }

  @Override
  public ImmutableList<SeqCaseBlockStatement> getConcatenatedStatements() {
    return concatenatedStatements;
  }

  @Override
  public SeqCaseBlockStatement cloneWithTargetPc(int pTargetPc) {

    return new SeqReturnValueAssignmentStatement(
        assignment,
        pcLeftHandSide,
        Optional.of(pTargetPc),
        injectedStatements,
        concatenatedStatements);
  }

  @Override
  public SeqCaseBlockStatement cloneWithInjectedStatements(
      ImmutableList<SeqCaseBlockInjectedStatement> pInjectedStatements) {

    return new SeqReturnValueAssignmentStatement(
        assignment, pcLeftHandSide, targetPc, pInjectedStatements, concatenatedStatements);
  }

  @Override
  public SeqCaseBlockStatement cloneWithConcatenatedStatements(
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements) {

    return new SeqReturnValueAssignmentStatement(
        assignment, pcLeftHandSide, Optional.empty(), injectedStatements, pConcatenatedStatements);
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
