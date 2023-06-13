// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import java.math.BigDecimal;
import org.sosy_lab.cpachecker.cfa.ast.AFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public final class CFloatLiteralExpression extends AFloatLiteralExpression
    implements CLiteralExpression {

  private static final long serialVersionUID = 5021145411123854111L;

  public CFloatLiteralExpression(FileLocation pFileLocation, CType pType, BigDecimal pValue) {
    super(pFileLocation, pType, pValue);
  }

  /** Returns a <code>CFloatLiteralExpression</code> for positive infinity. */
  public static CFloatLiteralExpression forPositiveInfinity(FileLocation pFileLocation, CType pType)
      throws NumberFormatException {
    // TODO: This method is a temporary hack until 'BigDecimal's are fully replaced by 'CFloat's in
    // AFloatLiteralExpression class.

    return new CFloatLiteralExpression(pFileLocation, pType, getInfinityApprox(false, pType));
  }

  /** Returns a <code>CFloatLiteralExpression</code> for negative infinity. */
  public static CFloatLiteralExpression forNegativeInfinity(FileLocation pFileLocation, CType pType)
      throws NumberFormatException {
    // TODO: This method is a temporary hack until 'BigDecimal's are fully replaced by 'CFloat's in
    // AFloatLiteralExpression class.

    return new CFloatLiteralExpression(pFileLocation, pType, getInfinityApprox(true, pType));
  }

  private static BigDecimal getInfinityApprox(boolean pIsNegative, CType pType) {
    // TODO: This method is a temporary hack until 'BigDecimal's are replaced by 'CFloat's in
    // AFloatLiteralExpression class.
    // This is necessary because CFloats are already able to return "inf" as a value, however, the
    // BigDecimal object requires a concrete numerical value instead. The code below thus returns a
    // placeholder value in the meantime.
    BigDecimal APPROX_INFINITY =
        BigDecimal.valueOf(Double.MAX_VALUE).add(BigDecimal.valueOf(Double.MAX_VALUE));

    CBasicType basicType = ((CSimpleType) pType).getType();
    switch (basicType) {
      case FLOAT:
      case DOUBLE:
        if (pIsNegative) {
          return APPROX_INFINITY.negate();
        } else {
          return APPROX_INFINITY;
        }
      default:
        // unsupported operation
        break;
    }

    throw new NumberFormatException(String.format("Invalid type for float infinity: %s", pType));
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
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CFloatLiteralExpression)) {
      return false;
    }

    return super.equals(obj);
  }
}
