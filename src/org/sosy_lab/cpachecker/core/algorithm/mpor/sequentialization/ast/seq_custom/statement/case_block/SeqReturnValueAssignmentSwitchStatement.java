// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements.SeqExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseBlock;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseBlock.Terminator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqSwitchStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.function_statements.FunctionReturnValueAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

/**
 * Handles the assignments of return values of functions, e.g. {@code int x = fib(5);} where {@code
 * fib} has a return statement {@code return fibNumber;} then we create statements {@code x =
 * fibNumber;} (where {@code x} is declared beforehand) in the sequentialization.
 *
 * <p>The function {@code fib} may be called multiple times by one thread, so we create a switch
 * statement with one or multiple {@link SeqReturnValueAssignmentStatement}s where only the original
 * calling context i.e. the {@code return_pc} of the function {@code fib} and the respective thread
 * is considered.
 */
public class SeqReturnValueAssignmentSwitchStatement implements SeqCaseBlockStatement {

  public final ImmutableSet<FunctionReturnValueAssignment> assignments;

  private final CIdExpression returnPc;

  private final CLeftHandSide pcLeftHandSide;

  private final Optional<Integer> targetPc;

  private final Optional<CExpression> targetPcExpression;

  protected SeqReturnValueAssignmentSwitchStatement(
      CIdExpression pReturnPc,
      ImmutableSet<FunctionReturnValueAssignment> pAssigns,
      CLeftHandSide pPcLeftHandSide,
      int pTargetPc) {

    assignments = pAssigns;
    returnPc = pReturnPc;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.of(pTargetPc);
    targetPcExpression = Optional.empty();
  }

  protected SeqReturnValueAssignmentSwitchStatement(
      CIdExpression pReturnPc,
      ImmutableSet<FunctionReturnValueAssignment> pAssigns,
      CLeftHandSide pPcLeftHandSide,
      CExpression pTargetPc) {

    assignments = pAssigns;
    returnPc = pReturnPc;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.empty();
    targetPcExpression = Optional.of(pTargetPc);
  }

  public CIdExpression getReturnPc() {
    return returnPc;
  }

  @Override
  public String toASTString() {
    ImmutableList.Builder<SeqCaseClause> caseClauses = ImmutableList.builder();
    for (FunctionReturnValueAssignment assignment : assignments) {
      int caseLabelValue = assignment.returnPcWrite.value;
      SeqReturnValueAssignmentStatement assignmentStatement =
          new SeqReturnValueAssignmentStatement(assignment.statement);
      caseClauses.add(
          new SeqCaseClause(
              anyGlobalAssign(assignments),
              false,
              caseLabelValue,
              new SeqCaseBlock(ImmutableList.of(assignmentStatement), Terminator.BREAK)));
    }
    // TODO remove hardcoded int values?
    SeqSwitchStatement switchStatement = new SeqSwitchStatement(returnPc, caseClauses.build(), 5);
    CExpressionAssignmentStatement pcWrite =
        SeqExpressionAssignmentStatement.buildPcWriteByTargetPc(
            pcLeftHandSide, targetPc, targetPcExpression);
    return SeqSyntax.NEWLINE
        + switchStatement.toASTString()
        + SeqSyntax.NEWLINE
        + SeqStringUtil.buildTab(5)
        + pcWrite.toASTString();
  }

  /** Returns {@code true} if any {@link CLeftHandSide} in pAssignments is a global variable. */
  private boolean anyGlobalAssign(ImmutableSet<FunctionReturnValueAssignment> pAssignments) {
    for (FunctionReturnValueAssignment assignment : pAssignments) {
      if (assignment.statement.getLeftHandSide() instanceof CIdExpression idExpr) {
        if (idExpr.getDeclaration() instanceof CVariableDeclaration varDec) {
          if (varDec.isGlobal()) {
            return true;
          }
        }
      }
    }
    return false;
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return targetPc;
  }

  @Override
  public Optional<CExpression> getTargetPcExpression() {
    return targetPcExpression;
  }

  @NonNull
  @Override
  public SeqReturnValueAssignmentSwitchStatement cloneWithTargetPc(CExpression pTargetPc) {
    return new SeqReturnValueAssignmentSwitchStatement(
        returnPc, assignments, pcLeftHandSide, pTargetPc);
  }

  @Override
  public boolean alwaysWritesPc() {
    return true;
  }

  @Override
  public boolean onlyWritesPc() {
    return false;
  }

  private static class SeqReturnValueAssignmentStatement implements SeqCaseBlockStatement {

    private final CExpressionAssignmentStatement assignment;

    private SeqReturnValueAssignmentStatement(CExpressionAssignmentStatement pAssignment) {
      assignment = pAssignment;
    }

    @Override
    public String toASTString() {
      return assignment.toASTString();
    }

    @Override
    public Optional<Integer> getTargetPc() {
      return Optional.empty();
    }

    @Override
    public Optional<CExpression> getTargetPcExpression() {
      return Optional.empty();
    }

    @NonNull
    @Override
    public SeqReturnValueAssignmentStatement cloneWithTargetPc(CExpression pTargetPc) {
      throw new UnsupportedOperationException(
          this.getClass().getSimpleName() + " do not have targetPcs");
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
}
