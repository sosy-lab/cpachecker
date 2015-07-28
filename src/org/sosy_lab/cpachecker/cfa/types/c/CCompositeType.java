/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa.types.c;

import static com.google.common.base.Preconditions.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

public final class CCompositeType implements CComplexType, Serializable {

  private static final long serialVersionUID = -839957929135012583L;
  private final CComplexType.ComplexTypeKind kind;
  private transient List<CCompositeTypeMemberDeclaration> members;
  private final String name;
  private final String origName;
  private boolean   isConst;
  private boolean   isVolatile;

  public CCompositeType(final boolean pConst, final boolean pVolatile,
      final CComplexType.ComplexTypeKind pKind, final List<CCompositeTypeMemberDeclaration> pMembers, final String pName, final String pOrigName) {

    checkArgument(pKind == ComplexTypeKind.STRUCT || pKind == ComplexTypeKind.UNION);
    isConst= pConst;
    isVolatile=pVolatile;
    kind = pKind;
    members = ImmutableList.copyOf(pMembers);
    name = pName.intern();
    origName = pOrigName.intern();
  }

  @Override
  public CComplexType.ComplexTypeKind getKind() {
    return kind;
  }

  public List<CCompositeTypeMemberDeclaration> getMembers() {
    return members;
  }

  public void setMembers(List<CCompositeTypeMemberDeclaration> list) {
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

    lASTString.append(" {\n");
    for (CCompositeTypeMemberDeclaration lMember : members) {
      lASTString.append("  ");
      lASTString.append(lMember.toASTString());
      lASTString.append("\n");
    }
    lASTString.append("} ");
    lASTString.append(pDeclarator);

    return lASTString.toString();
  }

  /**
   * This is the declaration of a member of a composite type.
   * It contains a type and an optional name.
   */
  public static final class CCompositeTypeMemberDeclaration implements Serializable{



    private static final long serialVersionUID = 8647666228796784933L;
    private final CType    type;
    private final String   name;

    public CCompositeTypeMemberDeclaration(CType pType,
                                           String pName) {

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
    public boolean equals(Object obj) {
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
      return
          Objects.equals(name, other.name) &&
          type.getCanonicalType().equals(other.type.getCanonicalType());
    }

    public CType getType() {
      return type;
    }

    public String getName() {
      return name;
    }

    public String toASTString() {
      String name = Strings.nullToEmpty(getName());
      return getType().toASTString(name) + ";";
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
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(isConst);
    result = prime * result + Objects.hashCode(isVolatile);
    result = prime * result + Objects.hashCode(kind);
    result = prime * result + Objects.hashCode(name);
    return result;
  }

  /**
   * Be careful, this method compares the CType as it is to the given object,
   * typedefs won't be resolved. If you want to compare the type without having
   * typedefs in it use #getCanonicalType().equals()
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CCompositeType)) {
      return false;
    }

    CCompositeType other = (CCompositeType) obj;

    boolean fieldNamesEqual = true;
    List<CCompositeTypeMemberDeclaration> otherMembers = other.getMembers();

    if (members.size() == otherMembers.size()){
      for (int i = 0; i < members.size(); ++i){
        CCompositeTypeMemberDeclaration member = members.get(i);
        CCompositeTypeMemberDeclaration otherMember = otherMembers.get(i);
        if (!(member.getName().equals(otherMember.getName()))){
          fieldNamesEqual = false;
          break;
        }
      }
    } else {
      fieldNamesEqual = false;
    }

    return isConst == other.isConst && isVolatile == other.isVolatile
           && kind == other.kind && Objects.equals(name, other.name)
           && fieldNamesEqual;
  }

  @Override
  public boolean equalsWithOrigName(Object obj) {
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
    return new CCompositeType(isConst || pForceConst, isVolatile || pForceVolatile, kind, members, name, origName);
  }

  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();

    out.writeObject(new ArrayList<>(members));
  }

  @SuppressWarnings("unchecked")
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    members = ImmutableList.copyOf((ArrayList<CCompositeTypeMemberDeclaration>)in.readObject());
  }

}
