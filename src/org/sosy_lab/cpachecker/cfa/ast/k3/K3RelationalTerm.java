// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpressionVisitor;

public sealed interface K3RelationalTerm extends AExpression, K3AstNode
    permits K3GeneralSymbolApplicationTerm, K3OldTerm, K3SymbolApplicationRelationalTerm, K3Term {

  @NonNull
  @Override
  K3Type getExpressionType();

  <R, X extends Exception> R accept(K3TermVisitor<R, X> v) throws X;

  @Deprecated // Call accept() directly
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
      R accept_(V pV) throws X3 {
    return accept(pV);
  }
}
