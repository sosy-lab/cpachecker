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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Objects;
import javax.annotation.Nullable;


public final class CElaboratedType implements CComplexType {

  private static final long serialVersionUID = -3566628634889842927L;
  private final ComplexTypeKind kind;
  private String name;
  private final String origName;
  private final boolean isConst;
  private final boolean isVolatile;

  private int hashCache = 0;

  private CComplexType realType = null;

  public CElaboratedType(boolean pConst, final boolean pVolatile,
      final ComplexTypeKind pKind, final String pName, final String pOrigName,
      final @Nullable CComplexType pRealType) {
    isConst = pConst;
    isVolatile = pVolatile;
    kind = checkNotNull(pKind);
    name = pName.intern();
    origName = pOrigName.intern();
    realType = pRealType;
  }

  @Override
  public String getName() {
    if (realType != null) {
      return realType.getName();
    }
    return name;
  }

  @Override
  public String getQualifiedName() {
    return (kind.toASTString() + " " + name).trim();
  }

  @Override
  public String getOrigName() {
    if (realType != null) {
      return realType.getOrigName();
    }
    return origName;
  }

  @Override
  public ComplexTypeKind getKind() {
    return kind;
  }

  /**
   * Get the real type which this type references
   * (either a CCompositeType or a CEnumType, or null if unknown).
   */
  public @Nullable CComplexType getRealType() {
    if (realType instanceof CElaboratedType) {
      // resolve chains of elaborated types
      return ((CElaboratedType)realType).getRealType();
    }
    return realType;
  }

  /**
   * This method should be called only during parsing.
   */
  public void setRealType(CComplexType pRealType) {
    checkState(getRealType() == null);
    checkNotNull(pRealType);
    checkArgument(pRealType != this);
    checkArgument(pRealType.getKind() == kind);

    // all elaborated types are renamed such that they only match on the struct
    // name suffixed with the filename, when setting the realtype the name
    // may change to be only the struct name without the suffix
    checkArgument(name.contains(pRealType.getName()));
    realType = pRealType;
    name = realType.getName();
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
    lASTString.append(" ");
    lASTString.append(name);
    lASTString.append(" ");
    lASTString.append(pDeclarator);

    return lASTString.toString();
  }
  @Override
  public String toString() {
    return getKind().toASTString() + " " + getName();
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
    if (realType == null) {
      return kind != ComplexTypeKind.ENUM; // enums are always complete
    } else {
      return realType.isIncomplete();
    }
  }

  @Override
  public <R, X extends Exception> R accept(CTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    if (hashCache == 0) {
      final int prime = 31;
      int result = 7;
      result = prime * result + Objects.hashCode(isConst);
      result = prime * result + Objects.hashCode(isVolatile);
      result = prime * result + Objects.hashCode(kind);
      result = prime * result + Objects.hashCode(name);
      result = prime * result + Objects.hashCode(realType);
      hashCache = result;
    }
    return hashCache;
  }

  /**
   * Be careful, this method compares the CType as it is to the given object,
   * typedefs won't be resolved. If you want to compare the type without having
   * typedefs in it use #getCanonicalType().equals()
   */
  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CElaboratedType)) {
      return false;
    }

    CElaboratedType other = (CElaboratedType) obj;

    return isConst == other.isConst && isVolatile == other.isVolatile
           && kind == other.kind && Objects.equals(name, other.name)
           && Objects.equals(realType, other.realType);
  }

  @Override
  public boolean equalsWithOrigName(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CElaboratedType)) {
      return false;
    }

    CElaboratedType other = (CElaboratedType) obj;

    return isConst == other.isConst
           && isVolatile == other.isVolatile
           && kind == other.kind
           && (Objects.equals(name, other.name) || (origName.isEmpty() && other.origName.isEmpty()))
           && Objects.equals(realType, other.realType);
  }

  @Override
  public CType getCanonicalType() {
    return getCanonicalType(false, false);
  }

  @Override
  public CType getCanonicalType(boolean pForceConst, boolean pForceVolatile) {
    if (realType == null) {
      if ((isConst == pForceConst) && (isVolatile == pForceVolatile)) {
        return this;
      }
      return new CElaboratedType(isConst || pForceConst, isVolatile || pForceVolatile, kind, name, origName, null);
    } else {
      return realType.getCanonicalType(isConst || pForceConst, isVolatile || pForceVolatile);
    }
  }
}
