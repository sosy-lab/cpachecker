// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;

public class SeqExpressions {

  // CIntegerLiteralExpression ===================================================================

  public static final CIntegerLiteralExpression INT_EXIT_PC = buildIntLiteralExpr(SeqUtil.EXIT_PC);

  public static final CIntegerLiteralExpression INT_0 = buildIntLiteralExpr(0);

  public static final CIntegerLiteralExpression INT_1 = buildIntLiteralExpr(1);

  // CIdExpression ===============================================================================

  public static final CIdExpression COND = buildIdExpr(SeqDeclarations.COND);

  public static final CIdExpression PC = buildIdExpr(SeqDeclarations.PC);

  public static final CIdExpression NEXT_THREAD = buildIdExpr(SeqDeclarations.NEXT_THREAD);

  public static final CIdExpression ASSUME = buildIdExpr(SeqDeclarations.ASSUME);

  public static final CIdExpression ABORT = buildIdExpr(SeqDeclarations.ABORT);

  public static final CIdExpression VERIFIER_NONDET_INT =
      buildIdExpr(SeqDeclarations.VERIFIER_NONDET_INT);

  // Helper Functions ============================================================================

  public static CIntegerLiteralExpression buildIntLiteralExpr(int pValue) {
    return new CIntegerLiteralExpression(
        FileLocation.DUMMY, SeqTypes.INT, BigInteger.valueOf(pValue));
  }

  /** Returns a {@link CIdExpression} with a declaration of the form {@code int {pVarName} = 0;}. */
  public static CIdExpression buildIntVar(String pVarName) {
    CVariableDeclaration varDec =
        SeqDeclarations.buildVarDec(true, SeqTypes.INT, pVarName, SeqInitializers.INT_0);
    return new CIdExpression(FileLocation.DUMMY, varDec);
  }

  public static CIdExpression buildIdExpr(CSimpleDeclaration pDec) {
    return new CIdExpression(FileLocation.DUMMY, pDec);
  }

  public static CExpressionAssignmentStatement buildExprAssignStmt(
      CLeftHandSide pLhs, CExpression pRhs) {
    return new CExpressionAssignmentStatement(FileLocation.DUMMY, pLhs, pRhs);
  }

  public static CArraySubscriptExpression buildPcSubscriptExpr(CExpression pSubscriptExpr) {
    return new CArraySubscriptExpression(
        FileLocation.DUMMY, SeqTypes.INT_ARRAY, SeqExpressions.PC, pSubscriptExpr);
  }
}
