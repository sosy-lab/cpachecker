// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public final class CArraySubscriptExpression extends AArraySubscriptExpression
    implements CLeftHandSide {

  private static final long serialVersionUID = 129923340158722862L;

  public CArraySubscriptExpression(
      final FileLocation pFileLocation,
      final CType pType,
      final CExpression pArrayExpression,
      final CExpression pSubscriptExpression) {
    super(pFileLocation, pType, pArrayExpression, pSubscriptExpression);
  }

  @Override
  public CType getExpressionType() {
    return (CType) super.getExpressionType();
  }

  @Override
  public CExpression getArrayExpression() {
    return (CExpression) super.getArrayExpression();
  }

  @Override
  public CExpression getSubscriptExpression() {
    return (CExpression) super.getSubscriptExpression();
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

    if (!(obj instanceof CArraySubscriptExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}
