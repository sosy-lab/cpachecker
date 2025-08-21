// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants;

import static org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder.buildIdExpression;
import static org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder.buildIntegerLiteralExpression;
import static org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder.buildStringLiteralExpression;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqDeclarations.SeqFunctionDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqDeclarations.SeqParameterDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqDeclarations.SeqVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;

public class SeqExpressions {

  public static class SeqFunctionCallExpressions {

    public static final CFunctionCallExpression ABORT =
        SeqExpressionBuilder.buildFunctionCallExpression(
            SeqVoidType.VOID,
            SeqIdExpression.ABORT,
            ImmutableList.of(),
            SeqFunctionDeclaration.ABORT);

    public static final CFunctionCallExpression VERIFIER_NONDET_INT =
        SeqExpressionBuilder.buildFunctionCallExpression(
            SeqSimpleType.INT,
            SeqIdExpression.VERIFIER_NONDET_INT,
            ImmutableList.of(),
            SeqFunctionDeclaration.VERIFIER_NONDET_INT);

    public static final CFunctionCallExpression VERIFIER_NONDET_UINT =
        SeqExpressionBuilder.buildFunctionCallExpression(
            SeqSimpleType.UNSIGNED_INT,
            SeqIdExpression.VERIFIER_NONDET_UINT,
            ImmutableList.of(),
            SeqFunctionDeclaration.VERIFIER_NONDET_UINT);
  }

  public static class SeqIntegerLiteralExpression {

    public static final CIntegerLiteralExpression INT_INIT_PC =
        buildIntegerLiteralExpression(Sequentialization.INIT_PC);

    public static final CIntegerLiteralExpression INT_EXIT_PC =
        buildIntegerLiteralExpression(Sequentialization.EXIT_PC);

    public static final CIntegerLiteralExpression INT_MINUS_1 = buildIntegerLiteralExpression(-1);

    public static final CIntegerLiteralExpression INT_0 = buildIntegerLiteralExpression(0);

    public static final CIntegerLiteralExpression INT_1 = buildIntegerLiteralExpression(1);
  }

  public static class SeqIdExpression {

    // parameters:

    public static final CIdExpression COND = buildIdExpression(SeqParameterDeclaration.COND);

    public static final CIdExpression FILE = buildIdExpression(SeqParameterDeclaration.FILE);

    public static final CIdExpression LINE = buildIdExpression(SeqParameterDeclaration.LINE);

    public static final CIdExpression FUNCTION =
        buildIdExpression(SeqParameterDeclaration.FUNCTION);

    // variables:

    public static final CIdExpression DUMMY_PC = buildIdExpression(SeqVariableDeclaration.DUMMY_PC);

    public static final CIdExpression LAST_THREAD =
        buildIdExpression(SeqVariableDeclaration.LAST_THREAD_UNSIGNED);

    // TODO we should use the separate signed/unsigned declarations here
    public static final CIdExpression NEXT_THREAD =
        buildIdExpression(SeqVariableDeclaration.NEXT_THREAD_UNSIGNED);

    public static final CIdExpression CNT = buildIdExpression(SeqVariableDeclaration.CNT);

    public static final CIdExpression K = buildIdExpression(SeqVariableDeclaration.K_SIGNED);

    public static final CIdExpression R = buildIdExpression(SeqVariableDeclaration.R);

    public static final CIdExpression I = buildIdExpression(SeqVariableDeclaration.I);

    // functions:

    public static final CIdExpression REACH_ERROR =
        buildIdExpression(SeqFunctionDeclaration.REACH_ERROR);

    public static final CIdExpression VERIFIER_NONDET_INT =
        buildIdExpression(SeqFunctionDeclaration.VERIFIER_NONDET_INT);

    public static final CIdExpression VERIFIER_NONDET_UINT =
        buildIdExpression(SeqFunctionDeclaration.VERIFIER_NONDET_UINT);

    public static final CIdExpression ABORT = buildIdExpression(SeqFunctionDeclaration.ABORT);

    public static final CIdExpression ASSERT_FAIL =
        buildIdExpression(SeqFunctionDeclaration.ASSERT_FAIL);

    public static final CIdExpression ASSUME = buildIdExpression(SeqFunctionDeclaration.ASSUME);

    public static final CIdExpression MAIN = buildIdExpression(SeqFunctionDeclaration.MAIN);
  }

  public static class SeqStringLiteralExpression {

    public static final CStringLiteralExpression STRING_0 =
        buildStringLiteralExpression(SeqToken.STRING_0);
  }
}
