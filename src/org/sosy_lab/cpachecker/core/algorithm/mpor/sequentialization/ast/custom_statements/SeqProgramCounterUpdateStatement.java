// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CStatementWrapper;

record SeqProgramCounterUpdateStatement(CExpressionAssignmentStatement pcUpdate)
    implements SeqInjectedStatement {

  @Override
  public boolean isPrunedWithTargetGoto() {
    return false;
  }

  @Override
  public boolean isPrunedWithEmptyBitVectorEvaluation() {
    return false;
  }

  @Override
  public ImmutableList<CExportStatement> toCExportStatements() throws UnrecognizedCodeException {
    return ImmutableList.of(new CStatementWrapper(pcUpdate));
  }
}
