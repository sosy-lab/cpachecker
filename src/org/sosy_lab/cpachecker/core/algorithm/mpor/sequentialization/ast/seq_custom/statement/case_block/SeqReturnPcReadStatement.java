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
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements.SeqExpressionAssignmentStatement;

/**
 * Represents a {@code return_pc} read, i.e. assigning the {@code return_pc} to the current threads
 * {@code pc}.
 *
 * <p>E.g. {@code pc[thread_id] = __return_pc_{thread_id}_{func_name};}
 */
public class SeqReturnPcReadStatement implements SeqCaseBlockStatement {

  private final CLeftHandSide pcLeftHandSide;

  public final CIdExpression returnPcVar;

  public SeqReturnPcReadStatement(CLeftHandSide pPcLeftHandSide, CIdExpression pReturnPcVar) {

    pcLeftHandSide = pPcLeftHandSide;
    returnPcVar = pReturnPcVar;
  }

  @Override
  public String toASTString() {
    CExpressionAssignmentStatement pcWrite =
        SeqExpressionAssignmentStatement.buildPcWrite(pcLeftHandSide, returnPcVar);
    return pcWrite.toASTString();
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return Optional.empty();
  }

  @NonNull
  @Override
  public SeqReturnPcReadStatement cloneWithTargetPc(int pTargetPc) {
    throw new UnsupportedOperationException(
        this.getClass().getSimpleName() + " do not have targetPcs");
  }

  @Override
  public boolean alwaysWritesPc() {
    return true;
  }
}
