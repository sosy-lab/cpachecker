// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CStatementWrapper;

public record SeqLastBitVectorUpdateStatement(
    CExpressionAssignmentStatement lastThreadUpdate,
    ImmutableList<CExpressionAssignmentStatement> lastBitVectorUpdates)
    implements SeqInjectedStatement {

  @Override
  public ImmutableList<CExportStatement> toCExportStatements() throws UnrecognizedCodeException {
    ImmutableList.Builder<CExportStatement> exportStatements = ImmutableList.builder();
    exportStatements.add(new CStatementWrapper(lastThreadUpdate));
    for (CExpressionAssignmentStatement lastBitVectorUpdate : lastBitVectorUpdates) {
      exportStatements.add(new CStatementWrapper(lastBitVectorUpdate));
    }
    return exportStatements.build();
  }

  @Override
  public boolean isPrunedWithTargetGoto() {
    return true;
  }

  @Override
  public boolean isPrunedWithEmptyBitVectorEvaluation() {
    return true;
  }
}
