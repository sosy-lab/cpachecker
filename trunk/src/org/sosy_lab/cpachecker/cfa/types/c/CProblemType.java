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
import org.checkerframework.checker.nullness.qual.Nullable;

/** This type is used when the parser could not determine the correct type. */
public final class CProblemType implements CType {

  private static final long serialVersionUID = -5658149239682173246L;
  private final String typeName;

  public CProblemType(String pTypeName) {
    typeName = checkNotNull(pTypeName);
  }

  @Override
  public String toString() {
    return typeName;
  }

  @Override
  public boolean isConst() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isVolatile() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isIncomplete() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toASTString(String pDeclarator) {
    checkNotNull(pDeclarator);
    return typeName + " " + pDeclarator;
  }

  @Override
  public <R, X extends Exception> R accept(CTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    return typeName.hashCode();
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

    if (!(obj instanceof CProblemType)) {
      return false;
    }

    CProblemType other = (CProblemType) obj;

    return Objects.equals(typeName, other.typeName);
  }

  @Override
  public CProblemType getCanonicalType() {
    return this;
  }

  @Override
  public CProblemType getCanonicalType(boolean pForceConst, boolean pForceVolatile) {
    return this;
  }
}
