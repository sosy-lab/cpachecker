// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.AIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class CIntegerLiteralExpression extends AIntegerLiteralExpression
    implements CLiteralExpression {

  private static final long serialVersionUID = 7691279268370356228L;
  public static final CIntegerLiteralExpression ONE = createDummyLiteral(1L, CNumericTypes.INT);
  public static final CIntegerLiteralExpression ZERO = createDummyLiteral(0L, CNumericTypes.INT);

  public static CIntegerLiteralExpression createDummyLiteral(long value, CType type) {
    return new CIntegerLiteralExpression(FileLocation.DUMMY, type, BigInteger.valueOf(value));
  }

  public CIntegerLiteralExpression(FileLocation pFileLocation, CType pType, BigInteger pValue) {
    super(pFileLocation, pType, pValue);
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
    String suffix = "";

    CType cType = getExpressionType();
    if (cType instanceof CSimpleType) {
      CSimpleType type = (CSimpleType) cType;
      if (type.isUnsigned()) {
        suffix += "U";
      }
      if (type.isLong()) {
        suffix += "L";
      } else if (type.isLongLong()) {
        suffix += "LL";
      }
    }

    return getValue().toString() + suffix;
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

    if (!(obj instanceof CIntegerLiteralExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}
