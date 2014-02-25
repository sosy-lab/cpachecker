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

import java.util.Objects;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.AArrayType;


public class CArrayType extends AArrayType implements CType {


  private final CExpression    length;
  private boolean   isConst;
  private boolean   isVolatile;

  public CArrayType(boolean pConst, boolean pVolatile,
      CType pType, CExpression pLength) {
    super(pType);
    isConst = pConst;
    isVolatile = pVolatile;
    length = pLength;
  }

  @Override
  public CType getType() {
    return (CType) super.getType();
  }

  public CExpression getLength() {
    return length;
  }

  @Override
  public String toASTString(String pDeclarator) {
    return (isConst() ? "const " : "")
        + (isVolatile() ? "volatile " : "")
        +  getType().toASTString(pDeclarator+ ("[" + (length != null ? length.toASTString() : "") + "]"))
        ;
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
  public String toString() {
    return (isConst() ? "const " : "")
        + (isVolatile() ? "volatile " : "")
        + "("+ getType().toString() + (")[" + (length != null ? length.toASTString() : "") + "]");
  }

  @Override
  public <R, X extends Exception> R accept(CTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(length);
    result = prime * result + Objects.hashCode(isConst);
    result = prime * result + Objects.hashCode(isVolatile);
    result = prime * result + super.hashCode();
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

    if (!(obj instanceof CArrayType) || !super.equals(obj)) {
      return false;
    }

    CArrayType other = (CArrayType) obj;

    if (length instanceof CIntegerLiteralExpression && other.length instanceof CIntegerLiteralExpression) {
      if (!((CIntegerLiteralExpression)length).getValue().equals(((CIntegerLiteralExpression)other.length).getValue())) {
        return false;
      }
    } else {
      if (!Objects.equals(length, other.length)) {
        return false;
      }
    }

    return isConst == other.isConst && isVolatile == other.isVolatile;
  }

  @Override
  public CArrayType getCanonicalType() {
    return getCanonicalType(false, false);
  }

  @Override
  public CArrayType getCanonicalType(boolean pForceConst, boolean pForceVolatile) {
    return new CArrayType(isConst || pForceConst, isVolatile || pForceVolatile, getType().getCanonicalType(), length);
  }
}
