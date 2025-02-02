// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.taintanalysis;

import java.math.BigInteger;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class TaintAnalysisUtils {

  /**
   * Collects all variables present in the given pExpression and returns them as {@link
   * CIdExpression}
   *
   * @param pExpression to collect in
   * @return the {@link CIdExpression} present
   */
  public static Set<CIdExpression> getAllVarsAsCExpr(CExpression pExpression) {
    // Initialize and invoke the visitor
    CollectCIdExpressionsVisitor visitor = new CollectCIdExpressionsVisitor();
    return pExpression.accept(visitor);
  }

  public static CIdExpression getCidExpressionForCVarDec(CVariableDeclaration pDec) {
    return new CIdExpression(pDec.getFileLocation(), pDec);
  }

  public static int evaluateExpressionToInteger(CExpression expression)
      throws CPATransferException {
    if (expression instanceof CIntegerLiteralExpression integerLiteral) {
      BigInteger value = integerLiteral.getValue();
      if (value.equals(BigInteger.ZERO) || value.equals(BigInteger.ONE)) {
        return value.intValue();
      } else {
        throw new CPATransferException(
            "Invalid taint assertion: Expected either 0 (not tainted) or 1 (tainted), but got "
                + value);
      }
    }
    throw new CPATransferException(
        "Invalid taint assertion: Second parameter must be an integer literal (0 or 1).");
  }
}
