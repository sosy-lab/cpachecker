// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;
import java.util.OptionalInt;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.AArrayType;

public final class CArrayType extends AArrayType implements CType {

  private static final long serialVersionUID = -6314468260643330323L;

  private final @Nullable CExpression length;
  private final boolean isConst;
  private final boolean isVolatile;

  public CArrayType(boolean pConst, boolean pVolatile, CType pType, @Nullable CExpression pLength) {
    super(pType);
    isConst = pConst;
    isVolatile = pVolatile;
    length = pLength;
  }

  @Override
  public CType getType() {
    return (CType) super.getType();
  }

  public @Nullable CExpression getLength() {
    return length;
  }

  /** Return the length of this array if statically known and small enough for an int. */
  public OptionalInt getLengthAsInt() {
    return length instanceof CIntegerLiteralExpression
        ? OptionalInt.of(((CIntegerLiteralExpression) length).getValue().intValueExact())
        : OptionalInt.empty();
  }

  /**
   * Convert this array type to a pointer type with the same target type. Note that in most cases
   * the method {@link CTypes#adjustFunctionOrArrayType(CType)} should be used instead, which
   * implements this conversion properly and also the similar conversion for function types.
   */
  public CPointerType asPointerType() {
    return new CPointerType(isConst, isVolatile, getType());
  }

  @Override
  public String toASTString(String pDeclarator) {
    return toASTString(pDeclarator, false);
  }

  private String toASTString(String pDeclarator, boolean pQualified) {
    checkNotNull(pDeclarator);
    return (isConst() ? "const " : "")
        + (isVolatile() ? "volatile " : "")
        + getType()
            .toASTString(
                pDeclarator + ("[" + (length != null ? length.toASTString(pQualified) : "") + "]"));
  }

  public String toQualifiedASTString(String pDeclarator) {
    return toASTString(pDeclarator, true);
  }

  @Override
  public boolean isConst() {
    return isConst;
  }

  @Override
  public boolean isVolatile() {
    return isVolatile;
  }

  @Override
  public boolean isIncomplete() {
    return length == null; // C standard § 6.2.5 (22)
  }

  @Override
  public boolean hasKnownConstantSize() {
    // C standard § 6.7.6.2 (4)
    return length instanceof CIntegerLiteralExpression && getType().hasKnownConstantSize();
  }

  @Override
  public String toString() {
    return (isConst() ? "const " : "")
        + (isVolatile() ? "volatile " : "")
        + "("
        + getType()
        + (")[" + (length != null ? length.toASTString() : "") + "]");
  }

  @Override
  public <R, X extends Exception> R accept(CTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    return Objects.hash(length, isConst, isVolatile) * 31 + super.hashCode();
  }

  /**
   * Be careful, this method compares the CType as it is to the given object, typedefs won't be
   * resolved. If you want to compare the type without having typedefs in it use
   * #getCanonicalType().equals()
   */
  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CArrayType) || !super.equals(obj)) {
      return false;
    }

    CArrayType other = (CArrayType) obj;

    if (length instanceof CIntegerLiteralExpression
        && other.length instanceof CIntegerLiteralExpression) {
      if (!((CIntegerLiteralExpression) length)
          .getValue()
          .equals(((CIntegerLiteralExpression) other.length).getValue())) {
        return false;
      }
    } else {
      if (!Objects.equals(length, other.length)) {
        return false;
      }
    }

    return isConst == other.isConst && isVolatile == other.isVolatile;
  }

  @Override
  public CArrayType getCanonicalType() {
    return getCanonicalType(false, false);
  }

  @Override
  public CArrayType getCanonicalType(boolean pForceConst, boolean pForceVolatile) {
    // C11 standard 6.7.3 (9) specifies that qualifiers like const and volatile
    // on an array type always refer to the element type, not the array type.
    // So we push these modifiers down to the element type here.
    return new CArrayType(
        false,
        false,
        getType().getCanonicalType(isConst || pForceConst, isVolatile || pForceVolatile),
        length);
  }
}
