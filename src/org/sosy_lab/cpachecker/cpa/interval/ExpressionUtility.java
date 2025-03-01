// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interval;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * Utility class for working with expression.
 */
public class ExpressionUtility {

  private static final CType CONCRETE_INDEX_TYPE = new CSimpleType(
      false, false, CBasicType.INT, false, false, true, false, false, false, false);

  private ExpressionUtility() {}

  public static CIntegerLiteralExpression getIntegerExpression(long value) {
    return CIntegerLiteralExpression.createDummyLiteral(value, CONCRETE_INDEX_TYPE);
  }

  public static CBinaryExpression incrementExpression(CExpression expression, long amount) {
    return new CBinaryExpression(
        expression.getFileLocation(),
        expression.getExpressionType(),
        expression.getExpressionType(),
        expression,
        getIntegerExpression(amount),
        BinaryOperator.PLUS
    );
  }

  public static CExpression incrementExpression(CExpression expression) {
    return incrementExpression(expression, 1);
  }

}
