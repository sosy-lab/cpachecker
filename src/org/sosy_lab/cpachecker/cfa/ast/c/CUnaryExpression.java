// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class CUnaryExpression extends AUnaryExpression implements CExpression {

  private static final long serialVersionUID = -7701970127701577207L;

  public CUnaryExpression(
      final FileLocation pFileLocation,
      final CType pType,
      final CExpression pOperand,
      final UnaryOperator pOperator) {
    super(pFileLocation, pType, pOperand, pOperator);
    assert pOperator != UnaryOperator.AMPER || pType.getCanonicalType() instanceof CPointerType
        : "Expression " + this + " has unexpected non-pointer type " + pType;
  }

  @Override
  public CExpression getOperand() {
    return (CExpression) super.getOperand();
  }

  @Override
  public UnaryOperator getOperator() {
    return (UnaryOperator) super.getOperator();
  }

  @Override
  public CType getExpressionType() {
    return (CType) super.getExpressionType();
  }

  @Override
  public String toASTString(boolean pQualified) {
    if (getOperator() == UnaryOperator.SIZEOF || getOperator() == UnaryOperator.ALIGNOF) {
      return getOperator().getOperator() + "(" + getOperand().toASTString(pQualified) + ")";
    } else {
      return getOperator().getOperator() + getOperand().toParenthesizedASTString(pQualified);
    }
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

  public enum UnaryOperator implements AUnaryExpression.AUnaryOperator {
    MINUS("-"),
    AMPER("&"),
    TILDE("~"),
    SIZEOF("sizeof"),
    ALIGNOF("__alignof__"),
    ;

    private final String mOp;

    UnaryOperator(String pOp) {
      mOp = pOp;
    }

    /** Returns the string representation of this operator (e.g. "*", "+"). */
    @Override
    public String getOperator() {
      return mOp;
    }
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

    if (!(obj instanceof CUnaryExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}
