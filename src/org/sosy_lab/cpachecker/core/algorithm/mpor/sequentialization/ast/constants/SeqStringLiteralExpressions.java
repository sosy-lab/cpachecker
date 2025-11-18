// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants;

import static org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder.buildStringLiteralExpression;

import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;

public class SeqStringLiteralExpressions {

  private static final String ZERO_STRING = "\"0\"";

  public static final CStringLiteralExpression STRING_0_PARAMETER_ASSERT_FAIL =
      buildStringLiteralExpression(ZERO_STRING);
}
