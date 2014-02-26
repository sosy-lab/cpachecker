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
package org.sosy_lab.cpachecker.cfa.ast.c;

import java.util.Objects;

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public final class CFieldReference extends AExpression implements CLeftHandSide {

  private final String         name;
  private final CExpression owner;
  private final boolean        isPointerDereference;

  public CFieldReference(final FileLocation pFileLocation,
                            final CType pType,
                            final String pName,
                            final CExpression pOwner,
                            final boolean pIsPointerDereference) {
    super(pFileLocation, pType);
    name = pName;
    owner = pOwner;
    isPointerDereference = pIsPointerDereference;

    assert checkFieldAccess();
  }

  private boolean checkFieldAccess() throws IllegalArgumentException {
    CType structType = owner.getExpressionType().getCanonicalType();
    if (structType instanceof CCompositeType) {
      boolean found = false;
      for (CCompositeTypeMemberDeclaration field : ((CCompositeType)structType).getMembers()) {
        if (field.getName().equals(name)) {
          found = true;
          break;
        }
      }
      if (!found) {
        throw new IllegalArgumentException("Accessing unknown field " + name + " in " + structType);
      }
    }
    return true;
  }

  @Override
  public CType getExpressionType() {
    return (CType) super.getExpressionType();
  }

  public String getFieldName() {
    return name;
  }

  public CExpression getFieldOwner() {
    return owner;
  }

  public boolean isPointerDereference() {
    return isPointerDereference;
  }

  @Override
  public <R, X extends Exception> R accept(CExpressionVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CRightHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CLeftHandSideVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public String toASTString() {
    String left = (owner instanceof CFieldReference) ? owner.toASTString() : owner.toParenthesizedASTString();
    String op = isPointerDereference ? "->" : ".";
    return left + op  + name;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + (isPointerDereference ? 1231 : 1237);
    result = prime * result + Objects.hashCode(name);
    result = prime * result + Objects.hashCode(owner);
    result = prime * result + super.hashCode();
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CFieldReference)
        || !super.equals(obj)) {
      return false;
    }

    CFieldReference other = (CFieldReference) obj;

    return Objects.equals(other.isPointerDereference, isPointerDereference)
            && Objects.equals(other.name, name)
            && Objects.equals(other.owner, owner);
  }

}
