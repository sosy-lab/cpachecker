// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpressionVisitor;

public sealed interface AcslExpression extends AExpression, AcslAstNode
    permits AcslBinaryPredicateExpression,
        AcslBinaryTermExpression,
        AcslIdExpression,
        AcslLiteralExpression,
        AcslOldExpression,
        AcslTernaryPredicateExpression,
        AcslUnaryExpression,
        AcslValidExpression {

  <R, X extends Exception> R accept(AcslExpressionVisitor<R, X> v) throws X;

  @Deprecated // Call accept() directly
  @SuppressWarnings("unchecked") // should not be necessary, but javac complains otherwise
  @Override
  default <
          R,
          R1 extends R,
          R2 extends R,
          R3 extends R,
          X1 extends Exception,
          X2 extends Exception,
          X3 extends Exception,
          V extends
              CExpressionVisitor<R1, X1> & JExpressionVisitor<R2, X2>
                  & AcslExpressionVisitor<R3, X3>>
      R accept_(V pV) throws X3 {
    return accept(pV);
  }
}
