// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3TermVisitor;

/** Interface for side-effect free expressions. */
public sealed interface CExpression extends CRightHandSide, AExpression
    permits CAddressOfLabelExpression,
        CBinaryExpression,
        CCastExpression,
        CLeftHandSide,
        CLiteralExpression,
        CTypeIdExpression,
        CUnaryExpression {

  <R, X extends Exception> R accept(CExpressionVisitor<R, X> v) throws X;

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
          V extends CExpressionVisitor<R1, X1> & JExpressionVisitor<R2, X2> & K3TermVisitor<R3, X3>>
      R accept_(V pV) throws X1 {
    return accept(pV);
  }
}
