/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import java.util.Objects;


public final class CPointerType implements CType {


  private final CType type;
  private boolean   isConst;
  private boolean   isVolatile;

  public CPointerType(final boolean pConst, final boolean pVolatile,
      final CType pType) {
    isConst = pConst;
    isVolatile = pVolatile;
    type = pType;
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
  public String toString() {
    String decl;

    decl = "(" + type.toString() + ")*";


    return (isConst() ? "const " : "")
        + (isVolatile() ? "volatile " : "")
        + decl;
  }

  @Override
  public String toASTString(String pDeclarator) {
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
    throw new UnsupportedOperationException("Do not use hashCode of CType");
  }

  /**
   * Be careful, this method compares the CType as it is to the given object,
   * typedefs won't be resolved. If you want to compare the type without having
   * typedefs in it use #getCanonicalType().equals()
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }

    if (!(obj instanceof CPointerType)) {
      return false;
    }

    CPointerType other = (CPointerType) obj;

    return Objects.equals(isConst, other.isConst) && Objects.equals(isVolatile, other.isVolatile)
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
}
