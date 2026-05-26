// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.transform;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CEnumerator;

public final class CEnumType implements CComplexType {

  @Serial private static final long serialVersionUID = -986078271714119880L;

  private final CSimpleType compatibleType;
  private final ImmutableList<CEnumerator> enumerators;
  private final String name;
  private final String origName;
  private final CTypeQualifiers qualifiers;
  private int hashCache = 0;

  public CEnumType(
      final CTypeQualifiers pQualifiers,
      final CSimpleType pCompatibleType,
      final List<CEnumerator> pEnumerators,
      final String pName,
      final String pOrigName) {
    qualifiers = checkNotNull(pQualifiers);
    compatibleType = checkNotNull(pCompatibleType);
    enumerators = ImmutableList.copyOf(pEnumerators);
    name = pName.intern();
    origName = pOrigName.intern();
  }

  @Override
  public CTypeQualifiers getQualifiers() {
    return qualifiers;
  }

  @Override
  public boolean isIncomplete() {
    return false;
  }

  @Override
  public boolean hasKnownConstantSize() {
    return true;
  }

  /**
   * Returns the integer type with which this enum is compatible (C11 § 6.7.2.2 (4)). Note that the
   * returned type depends only on the enumerators, it does not reflect the const and volatile
   * modifiers of this enum type.
   */
  public CSimpleType getCompatibleType() {
    return compatibleType;
  }

  public ImmutableList<CEnumerator> getEnumerators() {
    return enumerators;
  }

  @Override
  public ComplexTypeKind getKind() {
    return ComplexTypeKind.ENUM;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getQualifiedName() {
    return ("enum " + name).trim();
  }

  @Override
  public String getOrigName() {
    return origName;
  }

  @Override
  public String toASTString(String pDeclarator) {
    checkNotNull(pDeclarator);
    StringBuilder lASTString = new StringBuilder();
    lASTString.append(qualifiers.toASTStringPrefix());
    lASTString.append("enum ");
    lASTString.append(name);

    lASTString.append(" {\n  ");
    Joiner.on(",\n  ").appendTo(lASTString, transform(enumerators, CEnumerator::toASTString));
    lASTString.append("\n} ");
    lASTString.append(pDeclarator);

    return lASTString.toString();
  }

  @Override
  public String toString() {
    return qualifiers.toASTStringPrefix() + "enum " + name;
  }

  @Override
  public <R, X extends Exception> R accept(CTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    if (hashCache == 0) {
      hashCache = Objects.hash(qualifiers, name);
    }
    return hashCache;
  }

  /**
   * Be careful, this method compares the CType as it is to the given object, typedefs won't be
   * resolved. If you want to compare the type without having typedefs in it use
   * #getCanonicalType().equals()
   *
   * <p>Note that this method also compares the enumerators (and their values). Two enum types will
   * be treated as equal only if they match exactly. (This differs from {@link
   * CCompositeType#equals(Object)} currently, cf. #1376.)
   */
  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof CEnumType other
        && qualifiers.equals(other.qualifiers)
        && Objects.equals(name, other.name)
        && Objects.equals(enumerators, other.enumerators);
  }

  @Override
  public boolean equalsWithOrigName(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof CEnumType other
        && qualifiers.equals(other.qualifiers)
        && (Objects.equals(name, other.name) || (origName.isEmpty() && other.origName.isEmpty()))
        && Objects.equals(enumerators, other.enumerators);
  }

  @Override
  public CEnumType getCanonicalType() {
    return getCanonicalType(CTypeQualifiers.NONE);
  }

  @Override
  public CEnumType getCanonicalType(CTypeQualifiers pQualifiersToAdd) {
    CTypeQualifiers newQualifiers = CTypeQualifiers.union(qualifiers, pQualifiersToAdd);
    if (qualifiers.equals(newQualifiers)) {
      return this;
    }
    return new CEnumType(newQualifiers, compatibleType, enumerators, name, origName);
  }

  @Override
  public CEnumType withQualifiersSetTo(CTypeQualifiers pNewQualifiers) {
    if (pNewQualifiers.equals(qualifiers)) {
      return this;
    }
    return new CEnumType(
        pNewQualifiers, getCompatibleType(), getEnumerators(), getName(), getOrigName());
  }
}
