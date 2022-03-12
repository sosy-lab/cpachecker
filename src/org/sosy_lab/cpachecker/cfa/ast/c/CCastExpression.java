// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import org.sosy_lab.cpachecker.cfa.ast.ACastExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public final class CCastExpression extends ACastExpression implements CExpression {

  private static final long serialVersionUID = 3935774068573745972L;

  /**
   * Create instance.
   *
   * @param pFileLocation where is this cast?
   * @param pExpressionType target-type of the cast
   * @param pOperand is casted to target-type
   */
  public CCastExpression(
      final FileLocation pFileLocation, final CType pExpressionType, final CExpression pOperand) {
    super(pFileLocation, pExpressionType, pOperand);
  }

  /** returns the target-type of the cast-expression. The operand is casted to this type. */
  @Override
  public CType getExpressionType() {
    return (CType) super.getExpressionType();
  }

  @Override
  public CExpression getOperand() {
    return (CExpression) super.getOperand();
  }

  @Override
  public CType getCastType() {
    return (CType) super.getCastType();
  }

  @Override
  public <R, X extends Exception> R accept(CExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CCastExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}
