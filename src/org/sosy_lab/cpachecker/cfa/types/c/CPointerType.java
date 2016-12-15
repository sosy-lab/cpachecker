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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Objects;

import javax.annotation.Nullable;


public final class CPointerType implements CType, Serializable {

  private static final long serialVersionUID = -6423006826454509009L;
  public static final CPointerType POINTER_TO_VOID = new CPointerType(false, false, CVoidType.VOID);
  public static final CPointerType POINTER_TO_CONST_CHAR = new CPointerType(false, false, CNumericTypes.CHAR.getCanonicalType(true, false));

  private final CType type;
  private boolean   isConst;
  private boolean   isVolatile;

  public CPointerType(final boolean pConst, final boolean pVolatile,
      final CType pType) {
    isConst = pConst;
    isVolatile = pVolatile;
    type = checkNotNull(pType);
  }

  @Override
  public boolean isConst() {
    return isConst;
  }

  @Override
  public boolean isVolatile() {
    return isVolatile;
  }

  public CType getType() {
    return type;
  }

  @Override
  public boolean isIncomplete() {
    return false;
  }

  @Override
  public String toString() {
    String decl;

    decl = "(" + type.toString() + ")*";


    return (isConst() ? "const " : "")
        + (isVolatile() ? "volatile " : "")
        + decl;
  }

  @Override
  public String toASTString(String pDeclarator) {
    checkNotNull(pDeclarator);
    // ugly hack but it works:
    // We need to insert the "*" between the type and the name (e.g. "int *var").
    String decl;

    if (type instanceof CArrayType) {
      decl = type.toASTString("(*" + pDeclarator + ")");
    } else {
      decl = type.toASTString("*" + pDeclarator);
    }

    return (isConst() ? "const " : "")
        + (isVolatile() ? "volatile " : "")
        + decl;
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
      result = prime * result + Objects.hashCode(type);
      return result;
  }

  /**
   * Be careful, this method compares the CType as it is to the given object,
   * typedefs won't be resolved. If you want to compare the type without having
   * typedefs in it use #getCanonicalType().equals()
   */
  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    }

    if (!(obj instanceof CPointerType)) {
      return false;
    }

    CPointerType other = (CPointerType) obj;

    return isConst == other.isConst && isVolatile == other.isVolatile
           && Objects.equals(type, other.type);
  }

  @Override
  public CPointerType getCanonicalType() {
    return getCanonicalType(false, false);
  }

  @Override
  public CPointerType getCanonicalType(boolean pForceConst, boolean pForceVolatile) {
    return new CPointerType(isConst || pForceConst, isVolatile || pForceVolatile, type.getCanonicalType());
  }

  @Override
  public boolean isBitField() {
    return false;
  }

  @Override
  public int getBitFieldSize() {
    return 0;
  }

  @Override
  public CType withBitFieldSize(int pBitFieldSize) {
    // Bit field size not supported
    assert false;
    return this;
  }
}
