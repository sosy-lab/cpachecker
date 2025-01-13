// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import java.util.Optional;
import javax.annotation.Nonnull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIntegerLiteralExpression;

/**
 * Represents a {@code return_pc} storage, i.e. assigning the successor {@code pc} to the {@code
 * return_pc}.
 *
 * <p>E.g. {@code __return_pc_{thread_id}_{func_name} = n;}
 */
public class SeqReturnPcStorageStatement implements SeqCaseBlockStatement {

  private final CIdExpression returnPcVar;

  private final int returnPc;

  public SeqReturnPcStorageStatement(CIdExpression pReturnPcVar, int pReturnPc) {
    returnPcVar = pReturnPcVar;
    returnPc = pReturnPc;
  }

  @Override
  public String toASTString() {
    CExpressionAssignmentStatement assign =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY,
            returnPcVar,
            SeqIntegerLiteralExpression.buildIntLiteralExpr(returnPc));
    return assign.toASTString();
  }

  @Override
  public Optional<Integer> getTargetPc() {
    // the return_pc is treated as a targetPc because it must be a valid targetPc
    return Optional.of(returnPc);
  }

  @Nonnull
  @Override
  public @NonNull SeqReturnPcStorageStatement cloneWithTargetPc(int pTargetPc) {
    return new SeqReturnPcStorageStatement(returnPcVar, pTargetPc);
  }

  @Override
  public boolean alwaysUpdatesPc() {
    return true;
  }
}
