// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public record SeqSyncUpdateStatement(
    MPOROptions options, CIdExpression syncVariable, CIntegerLiteralExpression newSyncValue)
    implements SeqInjectedStatement {

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    StringBuilder stringBuilder = new StringBuilder();

    CExpressionAssignmentStatement syncAssignment =
        SeqStatementBuilder.buildExpressionAssignmentStatement(syncVariable, newSyncValue);
    stringBuilder.append(syncAssignment.toASTString());

    // if reduceLastThreadOrder is enabled, then also add 'LAST_THREAD_SYNC = T*_SYNC;'
    if (options.reduceLastThreadOrder()) {
      CExpressionAssignmentStatement lastThreadSyncAssignment =
          SeqStatementBuilder.buildExpressionAssignmentStatement(
              SeqIdExpressions.LAST_THREAD_SYNC, syncVariable);
      stringBuilder.append(SeqSyntax.NEWLINE).append(lastThreadSyncAssignment.toASTString());
    }

    return stringBuilder.toString();
  }
}
