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

public final class CTypeIdExpression extends AbstractExpression implements CExpression {

  private static final long serialVersionUID = -665995216646475799L;
  private final TypeIdOperator operator;
  private final CType type;

  public CTypeIdExpression(
      final FileLocation pFileLocation,
      final CType pExpressionType,
      final TypeIdOperator pOperator,
      final CType pType) {
    super(pFileLocation, pExpressionType);
    operator = pOperator;
    type = pType;
  }

  @Override
  public CType getExpressionType() {
    return (CType) super.getExpressionType();
  }

  public TypeIdOperator getOperator() {
    return operator;
  }

  public CType getType() {
    return type;
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

  public enum TypeIdOperator {
    SIZEOF("sizeof"),
    ALIGNOF("_Alignof"),
    TYPEOF("typeof"),
    ;

    private final String cRepresentation;

    TypeIdOperator(String pCRepresentation) {
      cRepresentation = pCRepresentation;
    }

    /** Returns the string representation of this operator */
    public String getOperator() {
      return cRepresentation;
    }
  }

  @Override
  public String toASTString(boolean pQualified) {
    return toASTString();
  }

  @Override
  public String toASTString() {
    return operator.getOperator() + "(" + type.toASTString("") + ")";
  }

  @Override
  public int hashCode() {
    return Objects.hash(operator, type) * 31 + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CTypeIdExpression) || !super.equals(obj)) {
      return false;
    }

    CTypeIdExpression other = (CTypeIdExpression) obj;

    return Objects.equals(other.operator, operator) && Objects.equals(other.type, type);
  }
}
