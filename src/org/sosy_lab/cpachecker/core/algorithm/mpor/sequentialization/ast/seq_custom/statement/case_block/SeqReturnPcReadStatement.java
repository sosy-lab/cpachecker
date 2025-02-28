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
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements.SeqExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Represents a {@code return_pc} read, i.e. assigning the {@code return_pc} to the current threads
 * {@code pc}.
 *
 * <p>E.g. {@code pc[thread_id] = __return_pc_{thread_id}_{func_name};}
 */
public class SeqReturnPcReadStatement implements SeqCaseBlockStatement {

  private final CLeftHandSide pcLeftHandSide;

  public final CIdExpression returnPcVariable;

  SeqReturnPcReadStatement(CLeftHandSide pPcLeftHandSide, CIdExpression pReturnPcVariable) {
    pcLeftHandSide = pPcLeftHandSide;
    returnPcVariable = pReturnPcVariable;
  }

  @Override
  public String toASTString() {
    CExpressionAssignmentStatement pcWrite =
        SeqExpressionAssignmentStatement.buildPcWrite(pcLeftHandSide, returnPcVariable);
    return pcWrite.toASTString();
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return Optional.empty();
  }

  @Override
  public Optional<CIdExpression> getTargetPcExpression() {
    return Optional.of(returnPcVariable);
  }

  @Override
  @NonNull
  public SeqCaseBlockStatement cloneWithTargetPc(int pTargetPc) throws UnrecognizedCodeException {
    // we never want to clone blank statements
    throw new UnsupportedOperationException(this.getClass().getSimpleName() + " cannot be cloned");
  }

  @Override
  @NonNull
  public SeqCaseBlockStatement cloneWithTargetPc(CIdExpression pTargetPc)
      throws UnrecognizedCodeException {
    // we never want to clone blank statements
    throw new UnsupportedOperationException(this.getClass().getSimpleName() + " cannot be cloned");
  }

  @Override
  public boolean alwaysWritesPc() {
    return true;
  }

  @Override
  public boolean onlyWritesPc() {
    return true;
  }
}
