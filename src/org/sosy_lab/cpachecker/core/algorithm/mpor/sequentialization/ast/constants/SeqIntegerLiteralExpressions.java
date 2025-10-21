// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants;

import static org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder.buildIntegerLiteralExpression;

import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;

public class SeqIntegerLiteralExpressions {

  public static final CIntegerLiteralExpression INT_INIT_PC =
      buildIntegerLiteralExpression(Sequentialization.INIT_PC);

  public static final CIntegerLiteralExpression INT_EXIT_PC =
      buildIntegerLiteralExpression(Sequentialization.EXIT_PC);

  public static final CIntegerLiteralExpression INT_0 = buildIntegerLiteralExpression(0);

  public static final CIntegerLiteralExpression INT_1 = buildIntegerLiteralExpression(1);
}
