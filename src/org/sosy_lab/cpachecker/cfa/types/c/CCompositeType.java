// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class CCompositeType implements CComplexType {

  private static final long serialVersionUID = -839957929135012583L;
  private final CComplexType.ComplexTypeKind kind;
  private @Nullable List<CCompositeTypeMemberDeclaration> members = null;
  private final String name;
  private final String origName;
  private final boolean isConst;
  private final boolean isVolatile;

  public CCompositeType(
      final boolean pConst,
      final boolean pVolatile,
      final CComplexType.ComplexTypeKind pKind,
      final String pName,
      final String pOrigName) {

    checkNotNull(pKind);
    checkArgument(pKind == ComplexTypeKind.STRUCT || pKind == ComplexTypeKind.UNION);
    isConst = pConst;
    isVolatile = pVolatile;
    kind = pKind;
    name = pName.intern();
    origName = pOrigName.intern();
  }

  public CCompositeType(
      final boolean pConst,
      final boolean pVolatile,
      final CComplexType.ComplexTypeKind pKind,
      final List<CCompositeTypeMemberDeclaration> pMembers,
      final String pName,
      final String pOrigName) {
    this(pConst, pVolatile, pKind, pName, pOrigName);
    checkMembers(pMembers);
    members = ImmutableList.copyOf(pMembers);
  }

  private void checkMembers(List<CCompositeTypeMemberDeclaration> pMembers) {
    for (Iterator<CCompositeTypeMemberDeclaration> it = pMembers.iterator(); it.hasNext(); ) {
      CCompositeTypeMemberDeclaration member = it.next();
      if (member.getType().isIncomplete()) {
        checkArgument(kind == ComplexTypeKind.STRUCT, "incomplete member %s in %s", member, this);
        checkArgument(
            !it.hasNext(), "incomplete member %s in non-last position of %s", member, this);
        checkArgument(
            member.getType().getCanonicalType() instanceof CArrayType,
            "incomplete non-array member %s in last position of %s",
            member,
            this);
      }
    }
  }

  @Override
  public CComplexType.ComplexTypeKind getKind() {
    return kind;
  }

  public List<CCompositeTypeMemberDeclaration> getMembers() {
    checkState(members != null, "list of CCompositeType members not yet initialized");
    return members;
  }

  public void setMembers(List<CCompositeTypeMemberDeclaration> list) {
    checkState(members == null, "list of CCompositeType members already initialized");
    checkMembers(list);
    members = ImmutableList.copyOf(list);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getQualifiedName() {
    return (kind.toASTString() + " " + name).trim();
  }

  @Override
  public String getOrigName() {
    return origName;
  }

  @Override
  public boolean isIncomplete() {
    return false;
  }

  @Override
  public boolean hasKnownConstantSize() {
    // forbidden by C standard, but a GCC extension:
    // https://gcc.gnu.org/onlinedocs/gcc/Variable-Length.html
    for (Iterator<CCompositeTypeMemberDeclaration> it = getMembers().iterator(); it.hasNext(); ) {
      CCompositeTypeMemberDeclaration member = it.next();
      if (!member.getType().hasKnownConstantSize()) {
        // special case: if this is a "flexible array member" as last member, then size is known
        if (it.hasNext() || !isFlexibleArray(member.getType())) {
          return false;
        }
      }
    }

    return true;
  }

  private static boolean isFlexibleArray(CType type) {
    type = type.getCanonicalType();
    return type instanceof CArrayType && ((CArrayType) type).getLength() == null;
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();

    if (isConst()) {
      result.append("const ");
    }
    if (isVolatile()) {
      result.append("volatile ");
    }

    result.append(kind.toASTString());
    result.append(' ');
    result.append(name);

    return result.toString();
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

    lASTString.append(kind.toASTString());
    lASTString.append(' ');
    lASTString.append(name);

    if (members == null) {
      lASTString.append("/* missing member initialization */ ");
    } else {
      lASTString.append(" {\n");
      for (CCompositeTypeMemberDeclaration lMember : members) {
        lASTString.append("  ");
        lASTString.append(lMember.toASTString());
        lASTString.append("\n");
      }
      lASTString.append("} ");
    }
    lASTString.append(pDeclarator);

    return lASTString.toString();
  }

  /**
   * This is the declaration of a member of a composite type. It contains a type and an optional
   * name.
   */
  public static final class CCompositeTypeMemberDeclaration implements Serializable {

    private static final long serialVersionUID = 8647666228796784933L;
    private final CType type;
    private final String name;

    public CCompositeTypeMemberDeclaration(CType pType, String pName) {

      type = checkNotNull(pType);
      name = pName;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 7;
      result = prime * result + Objects.hashCode(name);
      result = prime * result + Objects.hashCode(type);
      return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      CCompositeTypeMemberDeclaration other = (CCompositeTypeMemberDeclaration) obj;
      return Objects.equals(name, other.name)
          && type.getCanonicalType().equals(other.type.getCanonicalType());
    }

    public CType getType() {
      return type;
    }

    public String getName() {
      return name;
    }

    public String toASTString() {
      return getType().toASTString(Strings.nullToEmpty(getName())) + ";";
    }

    @Override
    public String toString() {
      return toASTString();
    }
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
  public <R, X extends Exception> R accept(CTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    return Objects.hash(isConst, isVolatile, kind, name);
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

    if (!(obj instanceof CCompositeType)) {
      return false;
    }

    CCompositeType other = (CCompositeType) obj;

    return isConst == other.isConst
        && isVolatile == other.isVolatile
        && kind == other.kind
        && Objects.equals(name, other.name);
  }

  @Override
  public boolean equalsWithOrigName(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CCompositeType)) {
      return false;
    }

    CCompositeType other = (CCompositeType) obj;

    return isConst == other.isConst
        && isVolatile == other.isVolatile
        && kind == other.kind
        && (Objects.equals(name, other.name) || (origName.isEmpty() && other.origName.isEmpty()));
  }

  @Override
  public CCompositeType getCanonicalType() {
    return getCanonicalType(false, false);
  }

  @Override
  public CCompositeType getCanonicalType(boolean pForceConst, boolean pForceVolatile) {
    if ((isConst == pForceConst) && (isVolatile == pForceVolatile)) {
      return this;
    }
    CCompositeType result =
        new CCompositeType(
            isConst || pForceConst, isVolatile || pForceVolatile, kind, name, origName);
    if (members != null) {
      result.setMembers(members);
    }
    return result;
  }
}
