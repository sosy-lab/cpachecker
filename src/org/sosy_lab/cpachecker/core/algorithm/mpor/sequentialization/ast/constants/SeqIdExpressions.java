// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants;

import static org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder.buildIdExpression;

import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;

public class SeqIdExpressions {
  // parameters:

  public static final CIdExpression COND_PARAMETER_ASSUME =
      buildIdExpression(SeqParameterDeclarations.COND_PARAMETER_ASSUME);

  public static final CIdExpression FILE_PARAMETER_ASSERT_FAIL =
      buildIdExpression(SeqParameterDeclarations.FILE_PARAMETER_ASSERT_FAIL);

  public static final CIdExpression LINE_PARAMETER_ASSERT_FAIL =
      buildIdExpression(SeqParameterDeclarations.LINE_PARAMETER_ASSERT_FAIL);

  public static final CIdExpression FUNCTION_PARAMETER_ASSERT_FAIL =
      buildIdExpression(SeqParameterDeclarations.FUNCTION_PARAMETER_ASSERT_FAIL);

  // variables:

  public static final CIdExpression PC_ARRAY_DUMMY =
      buildIdExpression(SeqVariableDeclarations.PC_ARRAY_DUMMY);

  public static final CIdExpression LAST_THREAD =
      buildIdExpression(SeqVariableDeclarations.LAST_THREAD_DUMMY);

  public static final CIdExpression NEXT_THREAD =
      buildIdExpression(SeqVariableDeclarations.NEXT_THREAD_DUMMY);

  public static final CIdExpression THREAD_COUNT =
      buildIdExpression(SeqVariableDeclarations.THREAD_COUNT);

  public static final CIdExpression ROUND_MAX =
      buildIdExpression(SeqVariableDeclarations.ROUND_MAX_DUMMY);

  public static final CIdExpression ROUND = buildIdExpression(SeqVariableDeclarations.ROUND);

  public static final CIdExpression ITERATION =
      buildIdExpression(SeqVariableDeclarations.ITERATION);

  // functions:

  public static final CIdExpression REACH_ERROR =
      buildIdExpression(SeqFunctionDeclarations.REACH_ERROR);

  public static final CIdExpression VERIFIER_NONDET_INT =
      buildIdExpression(SeqFunctionDeclarations.VERIFIER_NONDET_INT);

  public static final CIdExpression VERIFIER_NONDET_UINT =
      buildIdExpression(SeqFunctionDeclarations.VERIFIER_NONDET_UINT);

  public static final CIdExpression ABORT = buildIdExpression(SeqFunctionDeclarations.ABORT);

  public static final CIdExpression ASSERT_FAIL =
      buildIdExpression(SeqFunctionDeclarations.ASSERT_FAIL);

  public static final CIdExpression ASSUME = buildIdExpression(SeqFunctionDeclarations.ASSUME);

  public static final CIdExpression MAIN = buildIdExpression(SeqFunctionDeclarations.MAIN);
}
