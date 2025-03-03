// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements.SeqExpressionAssignmentStatement;

/**
 * Represents a {@code return_pc} write, i.e. assigning the successor {@code pc} to the {@code
 * return_pc}.
 *
 * <p>E.g. {@code __return_pc_{thread_id}_{func_name} = n;}
 */
public class SeqReturnPcWriteStatement implements SeqCaseBlockStatement {

  private final CIdExpression returnPcVariable;

  private final Optional<Integer> returnPc;

  private final Optional<CExpression> returnPcExpression;

  SeqReturnPcWriteStatement(CIdExpression pReturnPcVariable, int pReturnPc) {
    returnPcVariable = pReturnPcVariable;
    returnPc = Optional.of(pReturnPc);
    returnPcExpression = Optional.empty();
  }

  private SeqReturnPcWriteStatement(
      CIdExpression pReturnPcVariable, CExpression pReturnPcExpression) {

    returnPcVariable = pReturnPcVariable;
    returnPc = Optional.empty();
    returnPcExpression = Optional.of(pReturnPcExpression);
  }

  public CIdExpression getReturnPcVariable() {
    return returnPcVariable;
  }

  @Override
  public String toASTString() {
    CExpressionAssignmentStatement assignment =
        SeqExpressionAssignmentStatement.buildReturnPcWriteByTargetPc(
            returnPcVariable, returnPc, returnPcExpression);
    return assignment.toASTString();
  }

  @Override
  public Optional<Integer> getTargetPc() {
    // the return_pc is treated as a targetPc because it must be a valid targetPc
    return returnPc;
  }

  @Override
  public Optional<CExpression> getTargetPcExpression() {
    return returnPcExpression;
  }

  @Override
  public Optional<ImmutableList<SeqCaseBlockStatement>> getConcatenatedStatements() {
    // a return pc write has no conca
    throw new UnsupportedOperationException(
        this.getClass().getSimpleName() + " do not have concatenated statements");
  }

  @Override
  public SeqReturnPcWriteStatement cloneWithTargetPc(CExpression pTargetPc) {
    return new SeqReturnPcWriteStatement(returnPcVariable, pTargetPc);
  }

  @Override
  public SeqCaseBlockStatement cloneWithConcatenatedStatements(
      ImmutableList<SeqCaseBlockStatement> pConcatenatedStatements) {
    // we do not want to clone the statements to
    throw new UnsupportedOperationException(
        this.getClass().getSimpleName() + " do not have concatenated statements");
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
