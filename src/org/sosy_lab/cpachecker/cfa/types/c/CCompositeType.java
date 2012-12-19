/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

public final class CCompositeType implements CType {

  @Override
  public <R, X extends Exception> R accept(CTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + key;
    result = prime * result + ((members == null) ? 0 : members.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    CCompositeType other = (CCompositeType) obj;
    if (key != other.key)
      return false;
    if (members == null) {
      if (other.members != null)
        return false;
    } else if (!members.equals(other.members))
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    return true;
  }

  private final int                   key;
  private List<CCompositeTypeMemberDeclaration> members;
  private final String                name;
  private boolean   isConst;
  private boolean   isVolatile;

  public CCompositeType(final boolean pConst, final boolean pVolatile,
      final int pKey, final List<CCompositeTypeMemberDeclaration> pMembers, final String pName) {

    isConst= pConst;
    isVolatile=pVolatile;
    key = pKey;
    members = ImmutableList.copyOf(pMembers);
    name = pName.intern();
  }

  public int getKey() {
    return key;
  }

  public List<CCompositeTypeMemberDeclaration> getMembers() {
    return members;
  }

  public void setMembers(List<CCompositeTypeMemberDeclaration> list) {
    members = ImmutableList.copyOf(list);
  }

  public String getName() {
    return name;
  }

  public static final int k_struct = 1;
  public static final int k_union  = 2;

  @Override
  public String toASTString(String pDeclarator) {
    StringBuilder lASTString = new StringBuilder();

    if (isConst()) {
      lASTString.append("const ");
    }
    if (isVolatile()) {
      lASTString.append("volatile ");
    }

    if (key == k_struct) {
      lASTString.append("struct ");
    } else if (key == k_union) {
      lASTString.append("union ");
    } else {
      lASTString.append("unknown ");
    }

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
  public static final class CCompositeTypeMemberDeclaration {



    private final CType    type;
    private final String   name;

    public CCompositeTypeMemberDeclaration(CType pType,
                                           String pName) {

      type = checkNotNull(pType);
      name = pName;

    }



    public CType getType(){
      return type;
  }


    public String getName() {
      return name;
    }


    public String toASTString() {
      String name = Strings.nullToEmpty(getName());
      return getType().toASTString(name) + ";";
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
}
