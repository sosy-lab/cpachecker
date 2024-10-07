// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIntegerLiteralExpression;

public class SeqStatements {

  public static CExpressionAssignmentStatement buildExprAssign(
      CLeftHandSide pLhs, CExpression pRhs) {
    return new CExpressionAssignmentStatement(FileLocation.DUMMY, pLhs, pRhs);
  }

  public static CExpressionAssignmentStatement buildPcAssign(int pThreadId, int targetPc) {
    CIntegerLiteralExpression index = SeqIntegerLiteralExpression.buildIntLiteralExpr(pThreadId);
    CArraySubscriptExpression pcExpr = SeqExpressions.buildPcSubscriptExpr(index);
    CIntegerLiteralExpression targetInt = SeqIntegerLiteralExpression.buildIntLiteralExpr(targetPc);
    return SeqExpressions.buildExprAssignStmt(pcExpr, targetInt);
  }
}
