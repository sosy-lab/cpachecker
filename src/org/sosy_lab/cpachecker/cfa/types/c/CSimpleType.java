// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

@Immutable
public final class CSimpleType implements CType, Serializable {

  private static final long serialVersionUID = -8279630814725098867L;
  private final CBasicType type;
  private final boolean isLong;
  private final boolean isShort;
  private final boolean isSigned;
  private final boolean isUnsigned;
  private final boolean isComplex;
  private final boolean isImaginary;
  private final boolean isLongLong;
  private final boolean isConst;
  private final boolean isVolatile;

  @LazyInit private int hashCache = 0;

  public CSimpleType(
      final boolean pConst,
      final boolean pVolatile,
      final CBasicType pType,
      final boolean pIsLong,
      final boolean pIsShort,
      final boolean pIsSigned,
      final boolean pIsUnsigned,
      final boolean pIsComplex,
      final boolean pIsImaginary,
      final boolean pIsLongLong) {
    isConst = pConst;
    isVolatile = pVolatile;
    type = checkNotNull(pType);
    isLong = pIsLong;
    isShort = pIsShort;
    isSigned = pIsSigned;
    isUnsigned = pIsUnsigned;
    isComplex = pIsComplex;
    isImaginary = pIsImaginary;
    isLongLong = pIsLongLong;
  }

  @Override
  public boolean isConst() {
    return isConst;
  }

  @Override
  public boolean isVolatile() {
    return isVolatile;
  }

  public CBasicType getType() {
    return type;
  }

  public boolean isLong() {
    return isLong;
  }

  public boolean isShort() {
    return isShort;
  }

  public boolean isSigned() {
    return isSigned;
  }

  public boolean isUnsigned() {
    return isUnsigned;
  }

  public boolean isComplex() {
    return isComplex;
  }

  public boolean isImaginary() {
    return isImaginary;
  }

  public boolean isLongLong() {
    return isLongLong;
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
  public int hashCode() {
    if (hashCache == 0) {
      hashCache =
          Objects.hash(
              isComplex,
              isConst,
              isVolatile,
              isImaginary,
              isLong,
              isLongLong,
              isShort,
              isSigned,
              isUnsigned,
              type);
    }
    return hashCache;
  }

  /**
   * Be careful, this method compares the CType as it is to the given object, typedefs won't be
   * resolved. If you want to compare the type without having typedefs in it use
   * #getCanonicalType().equals()
   */
  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    }

    if (!(obj instanceof CSimpleType)) {
      return false;
    }

    CSimpleType other = (CSimpleType) obj;

    return isComplex == other.isComplex
        && isConst == other.isConst
        && isVolatile == other.isVolatile
        && isImaginary == other.isImaginary
        && isLong == other.isLong
        && isLongLong == other.isLongLong
        && isShort == other.isShort
        && isSigned == other.isSigned
        && isUnsigned == other.isUnsigned
        && type == other.type;
  }

  @Override
  public <R, X extends Exception> R accept(CTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }

  @Override
  public String toString() {
    return toASTString("");
  }

  @Override
  public String toASTString(String pDeclarator) {
    checkNotNull(pDeclarator);
    List<String> parts = new ArrayList<>();

    if (isConst()) {
      parts.add("const");
    }
    if (isVolatile()) {
      parts.add("volatile");
    }

    if (isUnsigned) {
      parts.add("unsigned");
    } else if (isSigned) {
      parts.add("signed");
    }

    if (isLongLong) {
      parts.add("long long");
    } else if (isLong) {
      parts.add("long");
    } else if (isShort) {
      parts.add("short");
    }

    if (isImaginary) {
      parts.add("_Imaginary");
    }
    if (isComplex) {
      parts.add("_Complex");
    }

    parts.add(Strings.emptyToNull(type.toASTString()));
    parts.add(Strings.emptyToNull(pDeclarator));

    return Joiner.on(' ').skipNulls().join(parts);
  }

  @Override
  public CSimpleType getCanonicalType() {
    return getCanonicalType(false, false);
  }

  @Override
  public CSimpleType getCanonicalType(boolean pForceConst, boolean pForceVolatile) {
    CBasicType newType = type;
    if (newType == CBasicType.UNSPECIFIED) {
      newType = CBasicType.INT;
    }

    boolean newIsSigned = isSigned;
    if ((newType == CBasicType.INT || newType == CBasicType.INT128) && !isSigned && !isUnsigned) {
      newIsSigned = true;
    }

    if ((isConst == pForceConst)
        && (isVolatile == pForceVolatile)
        && (type == newType)
        && (isSigned == newIsSigned)) {
      return this;
    }

    return new CSimpleType(
        isConst || pForceConst,
        isVolatile || pForceVolatile,
        newType,
        isLong,
        isShort,
        newIsSigned,
        isUnsigned,
        isComplex,
        isImaginary,
        isLongLong);
  }
}
