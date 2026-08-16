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

/** This represents a type which was created by using typedef. */
public final class CTypedefType implements CType {

  @Serial private static final long serialVersionUID = -3461236537115147688L;
  private final String name; // the typedef name
  private final CType realType; // the real type this typedef points to
  private final CTypeQualifiers qualifiers;
  private int hashCache = 0;

  public CTypedefType(final CTypeQualifiers pQualifiers, final String pName, CType pRealType) {

    qualifiers = checkNotNull(pQualifiers);
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
    return qualifiers.toASTStringPrefix() + name + " " + pDeclarator;
  }

  @Override
  public CTypeQualifiers getQualifiers() {
    return qualifiers;
  }

  @Override
  public boolean isIncomplete() {
    return realType.isIncomplete();
  }

  @Override
  public boolean hasKnownConstantSize() {
    return realType.hasKnownConstantSize();
  }

  @Override
  public <R, X extends Exception> R accept(CTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    if (hashCache == 0) {
      hashCache = Objects.hash(name, qualifiers, realType);
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
    if (this == obj) {
      return true;
    }

    return obj instanceof CTypedefType other
        && Objects.equals(name, other.name)
        && qualifiers.equals(other.qualifiers)
        && Objects.equals(realType, other.realType);
  }

  @Override
  public CType getCanonicalType(CTypeQualifiers pQualifiersToAdd) {
    return realType.getCanonicalType(CTypeQualifiers.union(qualifiers, pQualifiersToAdd));
  }

  @Override
  public CTypedefType withQualifiersSetTo(CTypeQualifiers pNewQualifiers) {
    if (pNewQualifiers.equals(qualifiers)) {
      return this;
    }
    return new CTypedefType(pNewQualifiers, getName(), getRealType());
  }
}
