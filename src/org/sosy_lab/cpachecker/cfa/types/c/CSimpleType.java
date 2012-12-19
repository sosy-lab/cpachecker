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

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

public final class CSimpleType implements CType {

  @Override
  public <R, X extends Exception> R accept(CTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }

  private final CBasicType type;
  private final boolean isLong;
  private final boolean isShort;
  private final boolean isSigned;
  private final boolean isUnsigned;
  private final boolean isComplex;
  private final boolean isImaginary;
  private final boolean isLongLong;
  private boolean   isConst;
  private boolean   isVolatile;

  public CSimpleType(final boolean pConst, final boolean pVolatile,
      final CBasicType pType, final boolean pIsLong, final boolean pIsShort,
      final boolean pIsSigned, final boolean pIsUnsigned,
      final boolean pIsComplex, final boolean pIsImaginary,
      final boolean pIsLongLong) {
    isConst = pConst;
    isVolatile = pVolatile;
    type = pType;
    isLong = pIsLong;
    isShort = pIsShort;
    isSigned = pIsSigned;
    isUnsigned = pIsUnsigned;
    isComplex = pIsComplex;
    isImaginary = pIsImaginary;
    isLongLong = pIsLongLong;
  }

  @Override
  public boolean isConst() {
    return isConst;
  }

  @Override
  public boolean isVolatile() {
    return isVolatile;
  }

  public CBasicType getType() {
    return type;
  }

  public boolean isLong() {
    return isLong;
  }

  public boolean isShort() {
    return isShort;
  }

  public boolean isSigned() {
    return isSigned;
  }

  public boolean isUnsigned() {
    return isUnsigned;
  }

  public boolean isComplex() {
    return isComplex;
  }

  public boolean isImaginary() {
    return isImaginary;
  }

  public boolean isLongLong() {
    return isLongLong;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (isComplex ? 1231 : 1237);
    result = prime * result + (isImaginary ? 1231 : 1237);
    result = prime * result + (isLong ? 1231 : 1237);
    result = prime * result + (isLongLong ? 1231 : 1237);
    result = prime * result + (isShort ? 1231 : 1237);
    result = prime * result + (isSigned ? 1231 : 1237);
    result = prime * result + (isUnsigned ? 1231 : 1237);
    result = prime * result + ((type == null) ? 0 : type.hashCode());
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
    CSimpleType other = (CSimpleType) obj;
    if (isComplex != other.isComplex)
      return false;
    if (isImaginary != other.isImaginary)
      return false;
    if (isLong != other.isLong)
      return false;
    if (isLongLong != other.isLongLong)
      return false;
    if (isShort != other.isShort)
      return false;
    if (isSigned != other.isSigned)
      return false;
    if (isUnsigned != other.isUnsigned)
      return false;
    if (type != other.type)
      return false;
    return true;
  }

  @Override
  public String toASTString(String pDeclarator) {
    List<String> parts = new ArrayList<String>();

    if (isConst()) {
      parts.add("const");
    }
    if (isVolatile()) {
      parts.add("volatile");
    }

    if (isUnsigned) {
      parts.add("unsigned");
    } else if (isSigned) {
      parts.add("signed");
    }

    if (isLongLong) {
      parts.add("long long");
    } else if (isLong) {
      parts.add("long");
    } else if (isShort) {
      parts.add("short");
    }

    if (isImaginary) {
      parts.add("_Imaginary");
    }
    if (isComplex) {
      parts.add("_Complex");
    }

    parts.add(Strings.emptyToNull(type.toASTString()));
    parts.add(Strings.emptyToNull(pDeclarator));

    return Joiner.on(' ').skipNulls().join(parts);
  }
}
