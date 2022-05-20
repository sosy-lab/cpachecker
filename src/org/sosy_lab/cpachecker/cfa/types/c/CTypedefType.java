// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This represents a type which was created by using typedef.
 */
public final class CTypedefType implements CType, Serializable {

  private static final long serialVersionUID = -3461236537115147688L;
  private final String name; // the typedef name
  private final CType realType; // the real type this typedef points to
  private final boolean isConst;
  private final boolean isVolatile;
  private int hashCache = 0;

  public CTypedefType(final boolean pConst, final boolean pVolatile,
      final String pName, CType pRealType) {

    isConst = pConst;
    isVolatile = pVolatile;
    name = pName.intern();
    realType = checkNotNull(pRealType);
  }

  public String getName() {
    return name;
  }

  public CType getRealType() {
    return realType;
  }

  @Override
  public String toString() {
    return toASTString("");
  }

  @Override
  public String toASTString(String pDeclarator) {
    checkNotNull(pDeclarator);
    return (isConst() ? "const " : "")
        + (isVolatile() ? "volatile " : "")
        + name
        + " " + pDeclarator;
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
    return realType.isIncomplete();
  }

  @Override
  public <R, X extends Exception> R accept(CTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    if (hashCache == 0) {
      hashCache = Objects.hash(name, isConst, isVolatile, realType);
    }
    return hashCache;
  }

  /**
   * Be careful, this method compares the CType as it is to the given object,
   * typedefs won't be resolved. If you want to compare the type without having
   * typedefs in it use #getCanonicalType().equals()
   */
  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    }

    if (!(obj instanceof CTypedefType)) {
      return false;
    }

    CTypedefType other = (CTypedefType) obj;

    return Objects.equals(name, other.name) && isConst == other.isConst
           && isVolatile == other.isVolatile
           && Objects.equals(realType, other.realType);
  }

  @Override
  public CType getCanonicalType() {
    return getCanonicalType(false, false);
  }

  @Override
  public CType getCanonicalType(boolean pForceConst, boolean pForceVolatile) {
    return realType.getCanonicalType(isConst || pForceConst, isVolatile || pForceVolatile);
  }
}
