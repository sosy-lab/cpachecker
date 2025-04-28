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

public abstract sealed interface AcslTerm extends AcslAstNode, AExpression
    permits AcslArraySubscriptTerm,
        AcslAtTerm,
        AcslBinaryTerm,
        AcslFunctionCallTerm,
        AcslIdTerm,
        AcslLiteralTerm,
        AcslOldTerm,
        AcslResultTerm,
        AcslTernaryTerm,
        AcslUnaryTerm {

  <R, X extends Exception> R accept(AcslTermVisitor<R, X> v) throws X;

  @Override
  AcslType getExpressionType();

  @Override
  default <
          R,
          R1 extends R,
          R2 extends R,
          R3 extends R,
          R4 extends R,
          X1 extends Exception,
          X2 extends Exception,
          X3 extends Exception,
          X4 extends Exception,
          V extends
              CExpressionVisitor<R1, X1> & JExpressionVisitor<R2, X2> & AcslPredicateVisitor<R3, X3>
                  & AcslTermVisitor<R4, X4>>
      R accept_(V pV) throws X4 {
    return accept(pV);
  }
}
