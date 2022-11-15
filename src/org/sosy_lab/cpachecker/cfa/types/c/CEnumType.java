// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.transform;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AbstractSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclarationVisitor;
import org.sosy_lab.cpachecker.cfa.types.Type;

public final class CEnumType implements CComplexType {

  private static final long serialVersionUID = -986078271714119880L;

  private final ImmutableList<CEnumerator> enumerators;
  private final String name;
  private final String origName;
  private final boolean isConst;
  private final boolean isVolatile;
  private int hashCache = 0;

  public CEnumType(
      final boolean pConst,
      final boolean pVolatile,
      final List<CEnumerator> pEnumerators,
      final String pName,
      final String pOrigName) {
    isConst = pConst;
    isVolatile = pVolatile;
    enumerators = ImmutableList.copyOf(pEnumerators);
    name = pName.intern();
    origName = pOrigName.intern();
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
    return false;
  }

  @Override
  public boolean hasKnownConstantSize() {
    return true;
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

    if (isConst()) {
      lASTString.append("const ");
    }
    if (isVolatile()) {
      lASTString.append("volatile ");
    }

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
    return (isConst() ? "const " : "") + (isVolatile() ? "volatile " : "") + "enum " + name;
  }

  public static final class CEnumerator extends AbstractSimpleDeclaration
      implements CSimpleDeclaration {

    private static final long serialVersionUID = -2526725372840523651L;

    private final @Nullable Long value;
    private @Nullable CEnumType enumType;
    private final String qualifiedName;

    public CEnumerator(
        final FileLocation pFileLocation,
        final String pName,
        final String pQualifiedName,
        final @Nullable CType pType,
        final @Nullable Long pValue) {
      super(pFileLocation, pType, pName);

      checkNotNull(pName);
      value = pValue;
      qualifiedName = checkNotNull(pQualifiedName);
    }

    /** Get the enum that declared this enumerator. */
    public CEnumType getEnum() {
      return enumType;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }

      if (!(obj instanceof CEnumerator) || !super.equals(obj)) {
        return false;
      }

      CEnumerator other = (CEnumerator) obj;

      return Objects.equals(value, other.value) && qualifiedName.equals(other.qualifiedName);
      // do not compare the enumType, comparing it with == is wrong because types which
      // are the same but not identical would lead to wrong results
      // comparing it with equals is no good choice, too. This would lead to a stack
      // overflow
      //  && (enumType == other.enumType);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value, enumType, qualifiedName) * 31 + super.hashCode();
    }

    /** This method should be called only during parsing. */
    public void setEnum(CEnumType pEnumType) {
      checkState(enumType == null);
      enumType = checkNotNull(pEnumType);
    }

    @Override
    public void setType(Type pType) {
      super.setType(checkNotNull(pType));
    }

    @Override
    public String getQualifiedName() {
      return qualifiedName;
    }

    @Override
    public CType getType() {
      return (CType) super.getType();
    }

    public long getValue() {
      checkState(value != null, "Need to check hasValue() before calling getValue()");
      return value;
    }

    public boolean hasValue() {
      return value != null;
    }

    @Override
    public String toASTString() {
      return getQualifiedName().replace("::", "__") + (hasValue() ? " = " + value : "");
    }

    @Override
    public <R, X extends Exception> R accept(CSimpleDeclarationVisitor<R, X> pV) throws X {
      return pV.visit(this);
    }

    @Override
    public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
      return pV.visit(this);
    }
  }

  @Override
  public <R, X extends Exception> R accept(CTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    if (hashCache == 0) {
      hashCache = Objects.hash(isConst, isVolatile, name);
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

    if (!(obj instanceof CEnumType)) {
      return false;
    }

    CEnumType other = (CEnumType) obj;

    return isConst == other.isConst
        && isVolatile == other.isVolatile
        && Objects.equals(name, other.name)
        && Objects.equals(enumerators, other.enumerators);
  }

  @Override
  public boolean equalsWithOrigName(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CEnumType)) {
      return false;
    }

    CEnumType other = (CEnumType) obj;

    return isConst == other.isConst
        && isVolatile == other.isVolatile
        && (Objects.equals(name, other.name) || (origName.isEmpty() && other.origName.isEmpty()))
        && Objects.equals(enumerators, other.enumerators);
  }

  @Override
  public CEnumType getCanonicalType() {
    return getCanonicalType(false, false);
  }

  @Override
  public CEnumType getCanonicalType(boolean pForceConst, boolean pForceVolatile) {
    if ((isConst == pForceConst) && (isVolatile == pForceVolatile)) {
      return this;
    }
    return new CEnumType(
        isConst || pForceConst, isVolatile || pForceVolatile, enumerators, name, origName);
  }
}
