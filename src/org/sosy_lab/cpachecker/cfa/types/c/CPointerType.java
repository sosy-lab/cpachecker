// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class CPointerType implements CType {

  @Serial private static final long serialVersionUID = -6423006826454509009L;
  public static final CPointerType POINTER_TO_VOID =
      new CPointerType(CTypeQualifiers.NONE, CVoidType.VOID);
  public static final CPointerType POINTER_TO_CHAR =
      new CPointerType(CTypeQualifiers.NONE, CNumericTypes.CHAR);
  public static final CPointerType POINTER_TO_CONST_CHAR =
      new CPointerType(CTypeQualifiers.NONE, CNumericTypes.CONST_CHAR);

  private final CType type;
  private final CTypeQualifiers qualifiers;

  public CPointerType(final CTypeQualifiers pQualifiers, final CType pType) {
    qualifiers = checkNotNull(pQualifiers);
    type = checkNotNull(pType);
  }

  @Override
  public CTypeQualifiers getQualifiers() {
    return qualifiers;
  }

  public CType getType() {
    return type;
  }

  @Override
  public boolean isIncomplete() {
    return false;
  }

  @Override
  public boolean hasKnownConstantSize() {
    return true;
  }

  @Override
  public String toString() {
    return qualifiers.toASTStringPrefix() + "(" + type + ")*";
  }

  @Override
  public String toASTString(String pDeclarator) {
    checkNotNull(pDeclarator);
    // ugly hack, but it works:
    // We need to insert the "*" and qualifiers between the type and the name (e.g. "int *var").
    StringBuilder inner = new StringBuilder("*");
    inner.append(qualifiers.toASTStringPrefix());
    inner.append(pDeclarator);

    if (type instanceof CArrayType) {
      return type.toASTString("(" + inner + ")");
    } else {
      return type.toASTString(inner.toString());
    }
  }

  @Override
  public <R, X extends Exception> R accept(CTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    return Objects.hash(qualifiers, type);
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

    return obj instanceof CPointerType other
        && qualifiers.equals(other.qualifiers)
        && Objects.equals(type, other.type);
  }

  @Override
  public CPointerType getCanonicalType() {
    return getCanonicalType(CTypeQualifiers.NONE);
  }

  @Override
  public CPointerType getCanonicalType(CTypeQualifiers pQualifiersToAdd) {
    return new CPointerType(
        CTypeQualifiers.union(qualifiers, pQualifiersToAdd), type.getCanonicalType());
  }

  @Override
  public CPointerType withQualifiersSetTo(CTypeQualifiers pNewQualifiers) {
    if (pNewQualifiers.equals(qualifiers)) {
      return this;
    }
    return new CPointerType(pNewQualifiers, getType());
  }
}
