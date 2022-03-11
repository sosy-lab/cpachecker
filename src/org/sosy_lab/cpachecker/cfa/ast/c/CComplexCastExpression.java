// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.AbstractExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public final class CComplexCastExpression extends AbstractExpression implements CLeftHandSide {

  private static final long serialVersionUID = -3131719369492162894L;
  private final CExpression operand;
  private final CType type;
  /** When isReal is false this is a cast to get the imaginary Part of the complex number */
  private final boolean isReal;

  public CComplexCastExpression(
      final FileLocation pFileLocation,
      final CType pExpressionType,
      final CExpression pOperand,
      final CType pType,
      final boolean pIsRealCast) {
    super(pFileLocation, pExpressionType);

    isReal = pIsRealCast;
    operand = pOperand;
    type = pType;
  }

  @Override
  public CType getExpressionType() {
    return (CType) super.getExpressionType();
  }

  public CExpression getOperand() {
    return operand;
  }

  public CType getType() {
    return type;
  }

  public boolean isImaginaryCast() {
    return !isReal;
  }

  public boolean isRealCast() {
    return isReal;
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
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
  public <R, X extends Exception> R accept(CLeftHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString(boolean pQualified) {
    if (isReal) {
      return "__real__ " + operand.toASTString(pQualified);
    } else {
      return "__imag__ " + operand.toASTString(pQualified);
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(operand, type, isReal) * 31 + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CComplexCastExpression) || !super.equals(obj)) {
      return false;
    }

    CComplexCastExpression other = (CComplexCastExpression) obj;

    return Objects.equals(other.operand, operand)
        && Objects.equals(other.type, type)
        && other.isReal == isReal;
  }
}
