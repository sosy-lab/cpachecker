// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements.SeqExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.function_statements.FunctionReturnValueAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Handles the assignments of a <strong>single</strong> return value of functions, e.g. {@code int x
 * = fib(5);} where {@code fib} has a return statement {@code return fibNumber;} then we create a
 * statement {@code x = fibNumber;} (where {@code x} is declared beforehand) in the
 * sequentialization.
 *
 * <p>If the function {@code fib} is called <strong>multiple</strong> times by one thread, we create
 * a switch statement, see {@link SeqReturnValueAssignmentSwitchStatement}.
 */
public class SeqReturnValueAssignmentStatement implements SeqCaseBlockStatement {

  private final CExpressionAssignmentStatement assignment;

  private final CLeftHandSide pcLeftHandSide;

  private final Optional<Integer> targetPc;

  private final Optional<CExpression> targetPcExpression;

  SeqReturnValueAssignmentStatement(
      FunctionReturnValueAssignment pAssignment, CLeftHandSide pPcLeftHandSide, int pTargetPc) {

    assignment = pAssignment.statement;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.of(pTargetPc);
    targetPcExpression = Optional.empty();
  }

  private SeqReturnValueAssignmentStatement(
      CExpressionAssignmentStatement pAssignment,
      CLeftHandSide pPcLeftHandSide,
      CExpression pTargetPc) {

    assignment = pAssignment;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.empty();
    targetPcExpression = Optional.of(pTargetPc);
  }

  @Override
  public String toASTString() {
    CExpressionAssignmentStatement pcWrite =
        SeqExpressionAssignmentStatement.buildPcWriteByTargetPc(
            pcLeftHandSide, targetPc, targetPcExpression);
    return assignment.toASTString() + SeqSyntax.SPACE + pcWrite.toASTString();
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return targetPc;
  }

  @Override
  public Optional<CExpression> getTargetPcExpression() {
    return targetPcExpression;
  }

  @Override
  public @NonNull SeqCaseBlockStatement cloneWithTargetPc(CExpression pTargetPc)
      throws UnrecognizedCodeException {
    return new SeqReturnValueAssignmentStatement(assignment, pcLeftHandSide, pTargetPc);
  }

  @Override
  public boolean alwaysWritesPc() {
    return true;
  }

  @Override
  public boolean onlyWritesPc() {
    return false;
  }
}
