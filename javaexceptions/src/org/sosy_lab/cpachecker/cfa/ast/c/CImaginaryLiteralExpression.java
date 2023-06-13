// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.ALiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public final class CImaginaryLiteralExpression extends ALiteralExpression
    implements CLiteralExpression {

  private static final long serialVersionUID = -3248391757986816857L;
  private final CLiteralExpression value;

  public CImaginaryLiteralExpression(
      FileLocation pFileLocation, CType pType, CLiteralExpression pValue) {
    super(pFileLocation, pType);
    value = pValue;
  }

  @Override
  public CType getExpressionType() {
    return (CType) super.getExpressionType();
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
  public String toASTString() {
    return getValue() + "i";
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(value) * 31 + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CImaginaryLiteralExpression) || !super.equals(obj)) {
      return false;
    }

    CImaginaryLiteralExpression other = (CImaginaryLiteralExpression) obj;

    return Objects.equals(other.value, value);
  }

  @Override
  public CLiteralExpression getValue() {
    return value;
  }
}
