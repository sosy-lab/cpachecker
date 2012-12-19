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

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.types.AArrayType;


public class CArrayType extends AArrayType implements CType {


  @Override
  public <R, X extends Exception> R accept(CTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((length == null) ? 0 : length.hashCode());
    result = prime * result + ((getType() == null) ? 0 : getType().hashCode());
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
    CArrayType other = (CArrayType) obj;
    if (length == null) {
      if (other.length != null)
        return false;
    } else if (!length.equals(other.length))
      return false;
    if (getType() == null) {
      if (other.getType() != null)
        return false;
    } else if (!getType().equals(other.getType()))
      return false;
    return true;
  }

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
    return (CType) elementType;
  }

  public CExpression getLength() {
    return length;
  }

  @Override
  public String toASTString(String pDeclarator) {
    return (isConst() ? "const " : "")
        + (isVolatile() ? "volatile " : "")
        +  elementType.toASTString(pDeclarator+ ("[" + (length != null ? length.toASTString() : "") + "]"))
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
}
