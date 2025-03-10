// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.injected;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;

public class SeqThreadBeginsAtomicStatement implements SeqCaseBlockInjectedStatement {

  private final CIdExpression threadBeginsAtomic;

  public SeqThreadBeginsAtomicStatement(CIdExpression pThreadBeginsAtomic) {
    threadBeginsAtomic = pThreadBeginsAtomic;
  }

  @Override
  public String toASTString() {
    CExpressionAssignmentStatement setBeginsTrue =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, threadBeginsAtomic, SeqIntegerLiteralExpression.INT_1);
    return setBeginsTrue.toASTString();
  }
}
