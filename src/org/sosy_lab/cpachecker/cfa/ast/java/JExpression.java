// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 * Interface of Side effect free Expressions.
 */
@SuppressWarnings("serial") // we cannot set a UID for an interface
public interface JExpression extends JRightHandSide, AExpression {

  <R, X extends Exception> R accept(JExpressionVisitor<R, X> v) throws X;

  @Override
  default <R, X extends Exception> R accept(JRightHandSideVisitor<R, X> pV) throws X {
    return accept((JExpressionVisitor<R, X>) pV);
  }

  @Deprecated // Call accept() directly
  @Override
  default <
          R,
          R1 extends R,
          R2 extends R,
          X1 extends Exception,
          X2 extends Exception,
          V extends CExpressionVisitor<R1, X1> & JExpressionVisitor<R2, X2>>
      R accept_(V pV) throws X2 {
    return accept(pV);
  }

  @Override
  JType getExpressionType();
}
