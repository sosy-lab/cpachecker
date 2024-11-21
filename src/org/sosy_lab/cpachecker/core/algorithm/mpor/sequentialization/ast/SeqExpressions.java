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
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORStatics;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqFunctionDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqParameterDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqTypes.SeqArrayType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqTypes.SeqSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqExpressions {

  public static class SeqBinaryExpression {

    public static CBinaryExpression buildBinaryExpression(
        CExpression pOperand1, CExpression pOperand2, BinaryOperator pOperator)
        throws UnrecognizedCodeException {

      return MPORStatics.binExprBuilder().buildBinaryExpression(pOperand1, pOperand2, pOperator);
    }
  }

  public static class SeqIntegerLiteralExpression {

    public static final CIntegerLiteralExpression INT_EXIT_PC =
        buildIntLiteralExpr(SeqUtil.EXIT_PC);

    public static final CIntegerLiteralExpression INT_MINUS_1 = buildIntLiteralExpr(-1);

    public static final CIntegerLiteralExpression INT_0 = buildIntLiteralExpr(0);

    public static final CIntegerLiteralExpression INT_1 = buildIntLiteralExpr(1);

    public static CIntegerLiteralExpression buildIntLiteralExpr(int pValue) {
      return new CIntegerLiteralExpression(
          FileLocation.DUMMY, SeqSimpleType.INT, BigInteger.valueOf(pValue));
    }
  }

  public static class SeqIdExpression {

    public static final CIdExpression COND = buildIdExpr(SeqParameterDeclaration.COND);

    public static final CIdExpression FILE = buildIdExpr(SeqParameterDeclaration.FILE);

    public static final CIdExpression LINE = buildIdExpr(SeqParameterDeclaration.LINE);

    public static final CIdExpression FUNCTION = buildIdExpr(SeqParameterDeclaration.FUNCTION);

    protected static final CIdExpression DUMMY_PC = buildIdExpr(SeqVariableDeclaration.DUMMY_PC);

    public static final CIdExpression PREV_THREAD = buildIdExpr(SeqVariableDeclaration.PREV_THREAD);

    public static final CIdExpression NEXT_THREAD = buildIdExpr(SeqVariableDeclaration.NEXT_THREAD);

    public static final CIdExpression REACH_ERROR = buildIdExpr(SeqFunctionDeclaration.REACH_ERROR);

    public static final CIdExpression VERIFIER_NONDET_INT =
        buildIdExpr(SeqFunctionDeclaration.VERIFIER_NONDET_INT);

    public static final CIdExpression ABORT = buildIdExpr(SeqFunctionDeclaration.ABORT);

    public static final CIdExpression ASSERT_FAIL = buildIdExpr(SeqFunctionDeclaration.ASSERT_FAIL);

    public static final CIdExpression ASSUME = buildIdExpr(SeqFunctionDeclaration.ASSUME);

    public static final CIdExpression MAIN = buildIdExpr(SeqFunctionDeclaration.MAIN);

    /**
     * Returns a {@link CIdExpression} with a declaration of the form {@code int {pVarName} =
     * {pInitializer};}.
     */
    public static CIdExpression buildIntIdExpr(String pVarName, CInitializer pInitializer) {
      CVariableDeclaration varDec =
          SeqVariableDeclaration.buildVarDec(true, SeqSimpleType.INT, pVarName, pInitializer);
      return new CIdExpression(FileLocation.DUMMY, varDec);
    }

    public static CIdExpression buildIdExpr(CSimpleDeclaration pDec) {
      return new CIdExpression(FileLocation.DUMMY, pDec);
    }
  }

  public static class SeqStringLiteralExpression {

    public static final CStringLiteralExpression STRING_0 =
        buildStringLiteralExpr(SeqToken.STRING_O);

    public static final CStringLiteralExpression SEQUENTIALIZATION_ERROR =
        buildStringLiteralExpr(SeqToken.__SEQUENTIALIZATION_ERROR__);

    public static final CStringLiteralExpression PRETTY_FUNCTION =
        buildStringLiteralExpr(SeqToken.__PRETTY_FUNCTION__);

    public static CStringLiteralExpression buildStringLiteralExpr(String pValue) {
      return new CStringLiteralExpression(FileLocation.DUMMY, pValue);
    }
  }

  // Helper Functions ============================================================================

  public static CExpressionAssignmentStatement buildExprAssignStmt(
      CLeftHandSide pLhs, CExpression pRhs) {
    return new CExpressionAssignmentStatement(FileLocation.DUMMY, pLhs, pRhs);
  }

  public static CArraySubscriptExpression buildPcSubscriptExpr(CExpression pSubscriptExpr) {
    return new CArraySubscriptExpression(
        FileLocation.DUMMY, SeqArrayType.INT_ARRAY, SeqIdExpression.DUMMY_PC, pSubscriptExpr);
  }
}
