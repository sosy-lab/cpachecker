/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;

public class CBitFieldType implements CType {

  private static final long serialVersionUID = 1L;

  private final CType type;

  private final int bitFieldSize;

  public CBitFieldType(CType pBitFieldType, int pBitFieldSize) {
    type = checkType(pBitFieldType);
    Preconditions.checkArgument(pBitFieldSize >= 0, "Bit-field size must not be negative, but was %s", pBitFieldSize);
    bitFieldSize = pBitFieldSize;
  }

  private CType checkType(CType pBitFieldType) {
    CType canonicalType = pBitFieldType.getCanonicalType();
    if (canonicalType instanceof CSimpleType) {
      CSimpleType simpleType = (CSimpleType) canonicalType;
      CBasicType basicType = simpleType.getType();
      switch (basicType) {
        case BOOL:
        case CHAR:
        case INT:
          return pBitFieldType;
        default:
          break;
      }
    } else if (canonicalType instanceof CEnumType) {
      return pBitFieldType;
    } else if (canonicalType instanceof CElaboratedType) {
      CElaboratedType elaboratedType = (CElaboratedType) canonicalType;
      if (elaboratedType.getKind() == ComplexTypeKind.ENUM) {
        return pBitFieldType;
      }
    }
    throw new IllegalArgumentException("Not a valid bit-field type: " + pBitFieldType);
  }

  @Override
  public String toASTString(String pDeclarator) {
    return type.toASTString(pDeclarator) + " : " + bitFieldSize;
  }

  @Override
  public boolean isConst() {
    return type.isConst();
  }

  @Override
  public boolean isVolatile() {
    return type.isVolatile();
  }

  @Override
  public boolean isIncomplete() {
    return type.isIncomplete();
  }

  @Override
  public <R, X extends Exception> R accept(CTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }

  @Override
  public CType getCanonicalType() {
    return getCanonicalType(false, false);
  }

  @Override
  public CType getCanonicalType(boolean pForceConst, boolean pForceVolatile) {
    CType canonicalBitFieldType = type.getCanonicalType(pForceConst, pForceVolatile);
    if (type == canonicalBitFieldType) {
      return this;
    }
    return new CBitFieldType(canonicalBitFieldType, bitFieldSize);
  }

  public int getBitFieldSize() {
    return bitFieldSize;
  }

  public CType getType() {
    return type;
  }

  @Override
  public String toString() {
    return getType() + " : " + getBitFieldSize();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + bitFieldSize;
    return prime * result + type.hashCode();
  }

  @Override
  public boolean equals(Object pObj) {
    if (pObj == this) {
      return true;
    }
    if (pObj instanceof CBitFieldType) {
      CBitFieldType other = (CBitFieldType) pObj;
      return bitFieldSize == other.bitFieldSize
          && type.equals(other.type);
    }
    return false;
  }

}
