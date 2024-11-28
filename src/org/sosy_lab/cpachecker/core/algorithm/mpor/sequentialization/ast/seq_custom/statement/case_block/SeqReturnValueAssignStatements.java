// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause.CaseBlockTerminator;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqSwitchStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_vars.FunctionReturnValueAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

/**
 * Handles the assignments of return values of functions, e.g. {@code int x = fib(5);} where {@code
 * fib} has a return statement {@code return fibNumber;} then we create statements {@code x =
 * fibNumber;} (where {@code x} is declared beforehand) in the sequentialization. <br>
 * The function {@code fib} may be called multiple times by one thread, so we create a switch
 * statement with one or multiple {@link SeqReturnValueAssignCaseBlockStatement}s where only the
 * original calling context of the function {@code fib} is considered.
 */
public class SeqReturnValueAssignStatements implements SeqCaseBlockStatement {

  private final boolean anyGlobal;

  private final ImmutableSet<FunctionReturnValueAssignment> assigns;

  private final CIdExpression returnPc;

  private final int threadId;

  private final int targetPc;

  public SeqReturnValueAssignStatements(
      CIdExpression pReturnPc,
      ImmutableSet<FunctionReturnValueAssignment> pAssigns,
      int pThreadId,
      int pTargetPc) {

    checkArgument(!pAssigns.isEmpty(), "pAssigns must contain at least one entry");

    anyGlobal = anyGlobalAssign(pAssigns);
    assigns = pAssigns;
    returnPc = pReturnPc;
    threadId = pThreadId;
    targetPc = pTargetPc;
  }

  @Override
  public String toASTString() {
    ImmutableList.Builder<SeqCaseClause> caseClauses = ImmutableList.builder();
    for (FunctionReturnValueAssignment assignment : assigns) {
      int caseLabelValue = assignment.returnPcStorage.value;
      SeqReturnValueAssignCaseBlockStatement assignmentStatement =
          new SeqReturnValueAssignCaseBlockStatement(assignment.statement);
      caseClauses.add(
          new SeqCaseClause(
              anyGlobal,
              caseLabelValue,
              ImmutableList.of(assignmentStatement),
              CaseBlockTerminator.BREAK));
    }
    SeqSwitchStatement switchStatement = new SeqSwitchStatement(returnPc, caseClauses.build(), 5);
    CExpressionAssignmentStatement pcUpdate = SeqStatements.buildPcUpdate(threadId, targetPc);
    return SeqSyntax.NEWLINE
        + switchStatement.toASTString()
        + SeqSyntax.NEWLINE
        + SeqUtil.prependTabsWithoutNewline(5, pcUpdate.toASTString());
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
    return Optional.of(targetPc);
  }

  @Override
  public @NonNull SeqReturnValueAssignStatements cloneWithTargetPc(int pTargetPc) {
    return new SeqReturnValueAssignStatements(returnPc, assigns, threadId, pTargetPc);
  }

  private static class SeqReturnValueAssignCaseBlockStatement implements SeqCaseBlockStatement {

    private final CExpressionAssignmentStatement assignment;

    private SeqReturnValueAssignCaseBlockStatement(CExpressionAssignmentStatement pAssignment) {
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
    public @NonNull SeqReturnValueAssignCaseBlockStatement cloneWithTargetPc(int pTargetPc) {
      throw new UnsupportedOperationException(
          "SeqReturnValueAssignCaseBlockStatement do not have targetPcs");
    }
  }
}
