// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import java.io.Serial;
import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.AIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public final class CIntegerLiteralExpression extends AIntegerLiteralExpression
    implements CLiteralExpression {

  /**
   * An enum for the base of an integer literal, e.g. binary for {@code 0b01010101}.
   *
   * <p>Reference: <a
   * href="https://en.cppreference.com/w/c/language/integer_constant.html">https://en.cppreference.com/w/c/language/integer_constant.html</a>
   */
  public enum CIntegerLiteralBase {
    BINARY("0b", 2),
    OCTAL("0", 8),
    DECIMAL("", 10),
    HEXADECIMAL("0x", 16);

    public final String prefix;

    public final int radix;

    CIntegerLiteralBase(String pPrefix, int pRadix) {
      prefix = pPrefix;
      radix = pRadix;
    }
  }

  @Serial private static final long serialVersionUID = 7691279268370356228L;
  public static final CIntegerLiteralExpression ONE = createDummyLiteral(1L, CNumericTypes.INT);
  public static final CIntegerLiteralExpression ZERO = createDummyLiteral(0L, CNumericTypes.INT);

  /**
   * Note that this base is used only in {@link CIntegerLiteralExpression#toASTString()} and is
   * intentionally not considered in {@link CIntegerLiteralExpression#equals(Object)} and {@link
   * CIntegerLiteralExpression#hashCode()}. Two {@link CIntegerLiteralExpression} are thus
   * considered equal by value, not syntax.
   */
  private final CIntegerLiteralBase base;

  public static CIntegerLiteralExpression createDummyLiteral(long value, CType type) {
    return new CIntegerLiteralExpression(FileLocation.DUMMY, type, BigInteger.valueOf(value));
  }

  /**
   * Returns a new {@link CIntegerLiteralExpression}.
   *
   * <p>{@code pBase} is used for the string representation of this literal, not in {@link
   * CIntegerLiteralExpression#equals(Object)} and {@link CIntegerLiteralExpression#hashCode()}. For
   * example, {@code 0x0} and {@code 0} are considered equal and cannot both be stored in a set.
   */
  public CIntegerLiteralExpression(
      FileLocation pFileLocation, CType pType, BigInteger pValue, CIntegerLiteralBase pBase) {

    super(pFileLocation, pType, pValue);
    base = pBase;
  }

  public CIntegerLiteralExpression(FileLocation pFileLocation, CType pType, BigInteger pValue) {
    super(pFileLocation, pType, pValue);
    base = CIntegerLiteralBase.DECIMAL;
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
    StringBuilder result = new StringBuilder();

    if (base.prefix.isEmpty()) {
      // if there is no prefix, add the value including its '-' sign if the number is negative
      result.append(getValue().toString(base.radix));

    } else {
      // if there is a prefix, add the '-' sign before the prefix, if the number is negative
      if (getValue().signum() < 0) {
        result.append("-");
      }
      result.append(base.prefix);
      // use the absolute, so that no additional '-' is used after the prefix
      result.append(getValue().abs().toString(base.radix));
    }

    CType cType = getExpressionType();
    if (cType instanceof CSimpleType type) {
      if (type.hasUnsignedSpecifier()) {
        result.append("U");
      }
      if (type.hasLongSpecifier()) {
        result.append("L");
      } else if (type.hasLongLongSpecifier()) {
        result.append("LL");
      }
    }

    return result.toString();
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

    return obj instanceof CIntegerLiteralExpression && super.equals(obj);
  }
}
