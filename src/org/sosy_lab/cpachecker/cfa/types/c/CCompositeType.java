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
import java.io.Serial;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A struct or union type.
 *
 * <p>The GCC attributes {@code packed} and {@code aligned} and the alignment specifier {@code
 * _Alignas} affect the layout of a composite type. They are stored only in its members ({@link
 * CCompositeTypeMemberDeclaration#isPacked()} and {@link
 * CCompositeTypeMemberDeclaration#getAlignment()}). This includes the attributes of the composite
 * type itself, because GCC defines the following as equivalent:
 *
 * <ul>
 *   <li>{@code packed} on a struct or union and {@code packed} on each of its members
 *       (https://gcc.gnu.org/onlinedocs/gcc/Common-Attributes.html#index-packed),
 *   <li>{@code aligned(N)} on a struct or union and {@code aligned(N)} on one of its members
 *       (https://gcc.gnu.org/onlinedocs/gcc/Common-Attributes.html#index-aligned). We use the first
 *       member, which has offset 0 anyway.
 * </ul>
 *
 * <p>Thus, this class needs no field for these attributes, which would have to be kept wherever
 * composite types are copied. The layout is computed from the members by {@link
 * org.sosy_lab.cpachecker.cfa.types.MachineModel}.
 */
public final class CCompositeType implements CComplexType {

  @Serial private static final long serialVersionUID = -839957929135012583L;
  private final CComplexType.ComplexTypeKind kind;
  private @Nullable List<CCompositeTypeMemberDeclaration> members = null;
  private final String name;
  private final String origName;
  private final CTypeQualifiers qualifiers;

  public CCompositeType(
      final CTypeQualifiers pQualifiers,
      final CComplexType.ComplexTypeKind pKind,
      final String pName,
      final String pOrigName) {

    checkNotNull(pKind);
    checkArgument(pKind == ComplexTypeKind.STRUCT || pKind == ComplexTypeKind.UNION);
    qualifiers = checkNotNull(pQualifiers);
    kind = pKind;
    name = pName.intern();
    origName = pOrigName.intern();
  }

  public CCompositeType(
      final CTypeQualifiers pQualifiers,
      final CComplexType.ComplexTypeKind pKind,
      final List<CCompositeTypeMemberDeclaration> pMembers,
      final String pName,
      final String pOrigName) {
    this(pQualifiers, pKind, pName, pOrigName);
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
            member.isFlexibleArrayMember(),
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

  public CCompositeTypeMemberDeclaration getMember(String pMemberName) {
    checkNotNull(pMemberName);
    for (CCompositeTypeMemberDeclaration member : getMembers()) {
      if (member.getName().equals(pMemberName)) {
        return member;
      }
    }
    throw new IllegalArgumentException("No member " + pMemberName + " in " + this);
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
        if (it.hasNext() || !member.isFlexibleArrayMember()) {
          return false;
        }
      }
    }

    return true;
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    result.append(qualifiers.toASTStringPrefix());
    result.append(kind.toASTString());
    result.append(' ');
    result.append(name);

    return result.toString();
  }

  @Override
  public String toASTString(String pDeclarator) {
    checkNotNull(pDeclarator);
    StringBuilder lASTString = new StringBuilder();
    lASTString.append(qualifiers.toASTStringPrefix());
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
   * name, and the GCC attributes packed and aligned, which affect the layout of the composite type.
   * These also encode the attributes of the composite type itself, cf. {@link CCompositeType}.
   */
  public static final class CCompositeTypeMemberDeclaration implements Serializable {

    @Serial private static final long serialVersionUID = 8647666228796784933L;
    private final CType type;
    private final String name;
    private final boolean packed;
    private final int alignment;

    public CCompositeTypeMemberDeclaration(CType pType, String pName) {
      this(pType, pName, false, 0);
    }

    /**
     * @param pPacked whether the member is packed, i.e., its alignment is 1 instead of the
     *     alignment of its type
     * @param pAlignment the minimum alignment of the member from an aligned attribute or an
     *     alignment specifier, or 0 if none
     */
    public CCompositeTypeMemberDeclaration(
        CType pType, String pName, boolean pPacked, int pAlignment) {
      checkArgument(pAlignment == 0 || Integer.bitCount(pAlignment) == 1);
      type = checkNotNull(pType);
      name = pName;
      packed = pPacked;
      alignment = pAlignment;
    }

    /** Return a copy of this member with a different type. */
    public CCompositeTypeMemberDeclaration withType(CType pType) {
      return new CCompositeTypeMemberDeclaration(pType, name, packed, alignment);
    }

    /** Return this member without the attributes packed and aligned. */
    public CCompositeTypeMemberDeclaration withoutAttributes() {
      if (!packed && alignment == 0) {
        return this;
      }
      return new CCompositeTypeMemberDeclaration(type, name);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 7;
      result = prime * result + Objects.hashCode(name);
      result = prime * result + Objects.hashCode(type);
      result = prime * result + Boolean.hashCode(packed);
      result = prime * result + alignment;
      return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      CCompositeTypeMemberDeclaration other = (CCompositeTypeMemberDeclaration) obj;
      return Objects.equals(name, other.name)
          && packed == other.packed
          && alignment == other.alignment
          && type.getCanonicalType().equals(other.type.getCanonicalType());
    }

    public CType getType() {
      return type;
    }

    public String getName() {
      return name;
    }

    public boolean isPacked() {
      return packed;
    }

    /** Return the minimum alignment from an aligned attribute or alignment specifier, or 0. */
    public int getAlignment() {
      return alignment;
    }

    /**
     * Whether this is a flexible array member as defined by § 6.7.2.1 (18). Such members are only
     * allowed as last members of a struct.
     */
    public boolean isFlexibleArrayMember() {
      return type.getCanonicalType() instanceof CArrayType arrayType
          && arrayType.getLength() == null;
    }

    public String toASTString() {
      String attributes = "";
      if (packed && alignment > 0) {
        attributes = " __attribute__((packed, aligned(" + alignment + ")))";
      } else if (packed) {
        attributes = " __attribute__((packed))";
      } else if (alignment > 0) {
        attributes = " __attribute__((aligned(" + alignment + ")))";
      }
      return getType().toASTString(Strings.nullToEmpty(getName())) + attributes + ";";
    }

    @Override
    public String toString() {
      return toASTString();
    }
  }

  @Override
  public CTypeQualifiers getQualifiers() {
    return qualifiers;
  }

  @Override
  public <R, X extends Exception> R accept(CTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    return Objects.hash(qualifiers, kind, name);
  }

  /**
   * Be careful, this method compares the CType as it is to the given object, typedefs won't be
   * resolved. If you want to compare the type without having typedefs in it use
   * #getCanonicalType().equals()
   *
   * <p>Note that this ignores the members of the struct/union. This should usually be fine because
   * we make type names unique by renaming while parsing the program. (This differs from {@link
   * CEnumType#equals(Object)} currently, cf. #1376.)
   */
  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof CCompositeType other
        && qualifiers.equals(other.qualifiers)
        && kind == other.kind
        && Objects.equals(name, other.name);
  }

  @Override
  public boolean equalsWithOrigName(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof CCompositeType other
        && qualifiers.equals(other.qualifiers)
        && kind == other.kind
        && (Objects.equals(name, other.name) || (origName.isEmpty() && other.origName.isEmpty()));
  }

  @Override
  public CCompositeType getCanonicalType() {
    return this;
  }

  @Override
  public CCompositeType getCanonicalType(CTypeQualifiers pQualifiersToAdd) {
    // Here we keep the same set of members, including with their non-canonicalized types.
    // We need a canonical representation of types mostly for easier type comparisons,
    // but composite types should be declared exactly once anyway, so their CCompositeType instance
    // should already be canonical in the sense that if we refer to the same type we already have
    // an equal instance (modulo qualifiers).
    CTypeQualifiers newQualifiers = CTypeQualifiers.union(qualifiers, pQualifiersToAdd);
    if (qualifiers.equals(newQualifiers)) {
      return this;
    }
    CCompositeType result = new CCompositeType(newQualifiers, kind, name, origName);
    if (members != null) {
      result.setMembers(members);
    }
    return result;
  }

  @Override
  public CCompositeType withoutQualifiers() {
    return withQualifiersSetTo(CTypeQualifiers.NONE);
  }

  @Override
  public CCompositeType asUnqualified() {
    return withQualifiersSetTo(qualifiers.withoutConst().withoutVolatile());
  }

  @Override
  public CCompositeType withQualifiersSetTo(CTypeQualifiers pNewQualifiers) {
    if (pNewQualifiers.equals(qualifiers)) {
      return this;
    }
    return new CCompositeType(pNewQualifiers, getKind(), getMembers(), getName(), getOrigName());
  }
}
