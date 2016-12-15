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

/**
 * This represents a type which was created by using typedef.
 */
public final class CTypedefType implements CType, Serializable {

  private static final long serialVersionUID = -3461236537115147688L;
  private final String name; // the typedef name
  private final CType realType; // the real type this typedef points to
  private boolean   isConst;
  private boolean   isVolatile;
  private int hashCache = 0;

  public CTypedefType(final boolean pConst, final boolean pVolatile,
      final String pName, CType pRealType) {

    isConst = pConst;
    isVolatile = pVolatile;
    name = pName.intern();
    realType = checkNotNull(pRealType);
  }

  @Override
  public CType withBitFieldSize(int pBitFieldSize) {
    if (isBitField() && getBitFieldSize() == pBitFieldSize) {
      return this;
    }
    return new CTypedefType(isConst, isVolatile, name, realType.withBitFieldSize(pBitFieldSize));
  }
  public String getName() {
    return name;
  }

  public CType getRealType() {
    return realType;
  }

  @Override
  public String toString() {
    return toASTString("");
  }

  @Override
  public String toASTString(String pDeclarator) {
    checkNotNull(pDeclarator);
    return (isConst() ? "const " : "")
        + (isVolatile() ? "volatile " : "")
        + name
        + " " + pDeclarator
        + (isBitField() ? " : " + getBitFieldSize() : "");
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
    return realType.isIncomplete();
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
      result = prime * result + Objects.hashCode(name);
      result = prime * result + Objects.hashCode(isConst);
      result = prime * result + Objects.hashCode(isVolatile);
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
    if (obj == this) {
      return true;
    }

    if (!(obj instanceof CTypedefType)) {
      return false;
    }

    CTypedefType other = (CTypedefType) obj;

    return Objects.equals(name, other.name) && isConst == other.isConst
           && isVolatile == other.isVolatile
           && Objects.equals(realType, other.realType);
  }

  @Override
  public CType getCanonicalType() {
    return getCanonicalType(false, false);
  }

  @Override
  public CType getCanonicalType(boolean pForceConst, boolean pForceVolatile) {
    return realType.getCanonicalType(isConst || pForceConst, isVolatile || pForceVolatile);
  }

  @Override
  public boolean isBitField() {
    return realType.isBitField();
  }

  @Override
  public int getBitFieldSize() {
    return realType.getBitFieldSize();
  }
}
