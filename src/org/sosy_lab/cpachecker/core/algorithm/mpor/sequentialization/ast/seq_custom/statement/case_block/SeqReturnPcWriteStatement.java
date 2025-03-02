// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
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

  @NonNull
  @Override
  public SeqReturnPcWriteStatement cloneWithTargetPc(CExpression pTargetPc) {
    return new SeqReturnPcWriteStatement(returnPcVariable, pTargetPc);
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
