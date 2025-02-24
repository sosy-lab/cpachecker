// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqFunctionDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqParameterDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqInitializers.SeqInitializer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqTypes.SeqArrayType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqTypes.SeqSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.GhostPcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqExpressions {

  public static class SeqArraySubscriptExpression {

    public static CArraySubscriptExpression buildPcSubscriptExpression(CExpression pSubscriptExpr) {
      return new CArraySubscriptExpression(
          FileLocation.DUMMY, SeqArrayType.INT_ARRAY, SeqIdExpression.DUMMY_PC, pSubscriptExpr);
    }

    static ImmutableList<CArraySubscriptExpression> buildArrayPcExpressions(int pNumThreads) {

      ImmutableList.Builder<CArraySubscriptExpression> rArrayPc = ImmutableList.builder();
      for (int i = 0; i < pNumThreads; i++) {
        rArrayPc.add(
            buildPcSubscriptExpression(
                SeqIntegerLiteralExpression.buildIntegerLiteralExpression(i)));
      }
      return rArrayPc.build();
    }
  }

  public static class SeqBinaryExpression {

    /**
     * Returns {@code pc[pJoinedThreadId] != -1} for array and {@code pc{pJoinedThreadId} != -1} for
     * scalar {@code pc}.
     */
    public static CBinaryExpression buildPcNotExitPc(
        GhostPcVariables pPcVariables,
        int pJoinedThreadId,
        CBinaryExpressionBuilder pBinaryExpressionBuilder)
        throws UnrecognizedCodeException {

      return pBinaryExpressionBuilder.buildBinaryExpression(
          pPcVariables.get(pJoinedThreadId),
          SeqIntegerLiteralExpression.INT_EXIT_PC,
          BinaryOperator.NOT_EQUALS);
    }
  }

  public static class SeqIntegerLiteralExpression {

    public static final CIntegerLiteralExpression INT_EXIT_PC =
        buildIntegerLiteralExpression(SeqUtil.EXIT_PC);

    public static final CIntegerLiteralExpression INT_MINUS_1 = buildIntegerLiteralExpression(-1);

    public static final CIntegerLiteralExpression INT_0 = buildIntegerLiteralExpression(0);

    public static final CIntegerLiteralExpression INT_1 = buildIntegerLiteralExpression(1);

    public static CIntegerLiteralExpression buildIntegerLiteralExpression(int pValue) {
      return new CIntegerLiteralExpression(
          FileLocation.DUMMY, SeqSimpleType.INT, BigInteger.valueOf(pValue));
    }
  }

  public static class SeqIdExpression {

    public static final CIdExpression COND = buildIdExpression(SeqParameterDeclaration.COND);

    public static final CIdExpression FILE = buildIdExpression(SeqParameterDeclaration.FILE);

    public static final CIdExpression LINE = buildIdExpression(SeqParameterDeclaration.LINE);

    public static final CIdExpression FUNCTION =
        buildIdExpression(SeqParameterDeclaration.FUNCTION);

    protected static final CIdExpression DUMMY_PC =
        buildIdExpression(SeqVariableDeclaration.DUMMY_PC);

    public static final CIdExpression PREV_THREAD =
        buildIdExpression(SeqVariableDeclaration.PREV_THREAD);

    public static final CIdExpression NEXT_THREAD =
        buildIdExpression(SeqVariableDeclaration.NEXT_THREAD);

    public static final CIdExpression REACH_ERROR =
        buildIdExpression(SeqFunctionDeclaration.REACH_ERROR);

    public static final CIdExpression VERIFIER_NONDET_INT =
        buildIdExpression(SeqFunctionDeclaration.VERIFIER_NONDET_INT);

    public static final CIdExpression ABORT = buildIdExpression(SeqFunctionDeclaration.ABORT);

    public static final CIdExpression ASSERT_FAIL =
        buildIdExpression(SeqFunctionDeclaration.ASSERT_FAIL);

    public static final CIdExpression ASSUME = buildIdExpression(SeqFunctionDeclaration.ASSUME);

    public static final CIdExpression MAIN = buildIdExpression(SeqFunctionDeclaration.MAIN);

    /**
     * Returns a {@link CIdExpression} with a declaration of the form {@code int {pVarName} =
     * {pInitializer};}.
     */
    public static CIdExpression buildIdExpressionWithIntegerInitializer(
        String pVarName, CInitializer pInitializer) {

      CVariableDeclaration varDec =
          SeqVariableDeclaration.buildVariableDeclaration(
              true, SeqSimpleType.INT, pVarName, pInitializer);
      return new CIdExpression(FileLocation.DUMMY, varDec);
    }

    public static CIdExpression buildIdExpression(CSimpleDeclaration pDec) {
      return new CIdExpression(FileLocation.DUMMY, pDec);
    }

    static ImmutableList<CIdExpression> buildScalarPcExpressions(int pNumThreads) {
      ImmutableList.Builder<CIdExpression> rScalarPc = ImmutableList.builder();
      for (int i = 0; i < pNumThreads; i++) {
        CInitializer initializer = i == 0 ? SeqInitializer.INT_0 : SeqInitializer.INT_MINUS_1;
        rScalarPc.add(
            new CIdExpression(
                FileLocation.DUMMY,
                SeqVariableDeclaration.buildVariableDeclaration(
                    false, SeqSimpleType.INT, SeqToken.pc + i, initializer)));
      }
      return rScalarPc.build();
    }
  }

  public static class SeqStringLiteralExpression {

    public static final CStringLiteralExpression STRING_0 =
        buildStringLiteralExpression(SeqToken._0);

    public static CStringLiteralExpression buildStringLiteralExpression(String pValue) {
      return new CStringLiteralExpression(FileLocation.DUMMY, pValue);
    }
  }
}
