// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function_vars;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIntegerLiteralExpression;

public class FunctionReturnPcRetrieval {

  private final int threadId;

  private final CIdExpression returnPc;

  public final CExpressionAssignmentStatement assignmentStatement;

  public FunctionReturnPcRetrieval(int pThreadId, CIdExpression pReturnPc) {
    threadId = pThreadId;
    returnPc = pReturnPc;
    CArraySubscriptExpression pcArray =
        SeqExpressions.buildPcSubscriptExpr(
            SeqIntegerLiteralExpression.buildIntLiteralExpr(threadId));
    assignmentStatement = SeqExpressions.buildExprAssignStmt(pcArray, returnPc);
  }
}
