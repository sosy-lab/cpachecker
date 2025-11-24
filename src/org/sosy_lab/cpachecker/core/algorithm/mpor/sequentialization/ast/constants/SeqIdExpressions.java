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

  // variables:

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

  public static final CIdExpression VERIFIER_NONDET_INT =
      buildIdExpression(SeqFunctionDeclarations.VERIFIER_NONDET_INT);

  public static final CIdExpression VERIFIER_NONDET_UINT =
      buildIdExpression(SeqFunctionDeclarations.VERIFIER_NONDET_UINT);
}
