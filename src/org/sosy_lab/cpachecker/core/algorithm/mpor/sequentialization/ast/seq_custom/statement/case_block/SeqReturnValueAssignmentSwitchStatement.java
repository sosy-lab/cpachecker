// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqCaseClause;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqSwitchStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

/**
 * Handles the assignments of return values of functions, e.g. {@code int x = fib(5);} where {@code
 * fib} has a return statement {@code return fibNumber;} then we create statements {@code x =
 * fibNumber;} (where {@code x} is declared beforehand) in the sequentialization.
 *
 * <p>The function {@code fib} may be called multiple times by one thread, so we create a switch
 * statement with one or multiple {@link SeqReturnValueAssignmentCaseBlockStatement}s where only the
 * original calling context i.e. the {@code return_pc} of the function {@code fib} and the
 * respective thread is considered.
 */
public class SeqReturnValueAssignmentSwitchStatement implements SeqCaseBlockStatement {

  public final ImmutableList<SeqCaseClause> caseClauses;

  /** The switch expression, e.g. {@code switch(RETURN_PC) { ... }}. */
  private final CIdExpression returnPc;

  private final CLeftHandSide pcLeftHandSide;

  private final Optional<Integer> targetPc;

  private final Optional<CExpression> targetPcExpression;

  private final Optional<ImmutableList<SeqCaseBlockStatement>> concatenatedStatements;

  SeqReturnValueAssignmentSwitchStatement(
      CIdExpression pReturnPc,
      ImmutableList<SeqCaseClause> pCaseClauses,
      CLeftHandSide pPcLeftHandSide,
      int pTargetPc) {

    caseClauses = pCaseClauses;
    returnPc = pReturnPc;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.of(pTargetPc);
    targetPcExpression = Optional.empty();
    concatenatedStatements = Optional.empty();
  }

  private SeqReturnValueAssignmentSwitchStatement(
      CIdExpression pReturnPc,
      ImmutableList<SeqCaseClause> pCaseClauses,
      CLeftHandSide pPcLeftHandSide,
      CExpression pTargetPc) {

    caseClauses = pCaseClauses;
    returnPc = pReturnPc;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.empty();
    targetPcExpression = Optional.of(pTargetPc);
    concatenatedStatements = Optional.empty();
  }

  private SeqReturnValueAssignmentSwitchStatement(
      CIdExpression pReturnPc,
      ImmutableList<SeqCaseClause> pCaseClauses,
      CLeftHandSide pPcLeftHandSide,
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements) {

    caseClauses = pCaseClauses;
    returnPc = pReturnPc;
    pcLeftHandSide = pPcLeftHandSide;
    targetPc = Optional.empty();
    targetPcExpression = Optional.empty();
    concatenatedStatements = Optional.of(pConcatenatedStatements);
  }

  public CIdExpression getReturnPc() {
    return returnPc;
  }

  @Override
  public String toASTString() {
    // TODO remove hardcoded int values for tabs?
    SeqSwitchStatement switchStatement = new SeqSwitchStatement(returnPc, caseClauses, 5);
    String targetStatements =
        SeqStringUtil.buildTargetStatements(
            pcLeftHandSide, targetPc, targetPcExpression, concatenatedStatements);
    return SeqSyntax.NEWLINE
        + switchStatement.toASTString()
        + SeqSyntax.NEWLINE
        + SeqStringUtil.buildTab(5)
        + targetStatements;
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
  public Optional<ImmutableList<SeqCaseBlockStatement>> getConcatenatedStatements() {
    return concatenatedStatements;
  }

  @Override
  public SeqReturnValueAssignmentSwitchStatement cloneWithTargetPc(CExpression pTargetPc) {
    return new SeqReturnValueAssignmentSwitchStatement(
        returnPc, caseClauses, pcLeftHandSide, pTargetPc);
  }

  @Override
  public SeqCaseBlockStatement cloneWithConcatenatedStatements(
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements) {

    return new SeqReturnValueAssignmentSwitchStatement(
        returnPc, caseClauses, pcLeftHandSide, pConcatenatedStatements);
  }

  // TODO it would be great to create a switch statement interface and let the main switch also use
  //  this
  public SeqReturnValueAssignmentSwitchStatement cloneWithCaseClauses(
      ImmutableList<SeqCaseClause> pCaseClauses) {

    if (targetPc.isPresent()) {
      return new SeqReturnValueAssignmentSwitchStatement(
          returnPc, pCaseClauses, pcLeftHandSide, targetPc.orElseThrow());
    } else {
      Verify.verify(targetPcExpression.isPresent());
      return new SeqReturnValueAssignmentSwitchStatement(
          returnPc, pCaseClauses, pcLeftHandSide, targetPcExpression.orElseThrow());
    }
  }

  @Override
  public boolean isConcatenable() {
    return true;
  }

  @Override
  public boolean alwaysWritesPc() {
    return true;
  }

  @Override
  public boolean onlyWritesPc() {
    return false;
  }

  public static class SeqReturnValueAssignmentCaseBlockStatement implements SeqCaseBlockStatement {

    public final CExpressionAssignmentStatement assignment;

    SeqReturnValueAssignmentCaseBlockStatement(CExpressionAssignmentStatement pAssignment) {
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

    @Override
    public Optional<ImmutableList<SeqCaseBlockStatement>> getConcatenatedStatements() {
      throw new UnsupportedOperationException(
          this.getClass().getSimpleName() + " do not have concatenated statements");
    }

    @Override
    public SeqReturnValueAssignmentCaseBlockStatement cloneWithTargetPc(CExpression pTargetPc) {
      throw new UnsupportedOperationException(
          this.getClass().getSimpleName() + " do not have targetPcs");
    }

    @Override
    public SeqCaseBlockStatement cloneWithConcatenatedStatements(
        ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements) {
      throw new UnsupportedOperationException(
          this.getClass().getSimpleName() + " cannot be cloned with concatenated statements");
    }

    @Override
    public boolean isConcatenable() {
      return false;
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
