// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpressionVisitor;

/** Abstract interface for side-effect free expressions. */
public interface AExpression extends ARightHandSide {

  /**
   * Accept methods for visitors that works with expression of all languages. It requires a visitor
   * that implements the respective visitor interfaces for all languages. If you can, do not call
   * this method but one of the normal "accept" methods.
   *
   * @param v The visitor.
   * @return Returns the object returned by the visit method.
   */
  <
          R,
          R1 extends R,
          R2 extends R,
          X1 extends Exception,
          X2 extends Exception,
          V extends CExpressionVisitor<R1, X1> & JExpressionVisitor<R2, X2>>
      R accept_(V v) throws X1, X2;
}
