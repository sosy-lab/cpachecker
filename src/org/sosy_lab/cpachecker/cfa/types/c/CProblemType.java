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

/**
 * This type is used when the parser could not determine the correct type.
 */
public class CProblemType implements CType {

  @Override
  public <R, X extends Exception> R accept(CTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((typeName == null) ? 0 : typeName.hashCode());
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
    CProblemType other = (CProblemType) obj;
    if (typeName == null) {
      if (other.typeName != null)
        return false;
    } else if (!typeName.equals(other.typeName))
      return false;
    return true;
  }

  private final String typeName;

  public CProblemType(String pTypeName) {
    typeName = pTypeName;
  }

  @Override
  public String toString() {
    return typeName;
  }

  @Override
  public boolean isConst() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isVolatile() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toASTString(String pDeclarator) {
    return typeName + " " + pDeclarator;
  }
}
